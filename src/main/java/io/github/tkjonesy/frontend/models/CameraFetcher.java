package io.github.tkjonesy.frontend.models;

import io.github.tkjonesy.ONNX.Detection;
import io.github.tkjonesy.ONNX.ImageUtil;
import io.github.tkjonesy.ONNX.models.OnnxOutput;
import io.github.tkjonesy.ONNX.models.OnnxRunner;

import io.github.tkjonesy.utils.settings.ProgramSettings;
import org.bytedeco.javacpp.BytePointer;

import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.opencv_videoio.VideoCapture;
import org.bytedeco.opencv.opencv_videoio.VideoWriter;

import static org.bytedeco.opencv.global.opencv_imgproc.resize;
import static org.bytedeco.opencv.global.opencv_videoio.CAP_PROP_FRAME_WIDTH;
import static org.bytedeco.opencv.global.opencv_videoio.CAP_PROP_FRAME_HEIGHT;

import org.bytedeco.opencv.global.opencv_core;


import javax.swing.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class CameraFetcher implements Runnable {

    private final JLabel cameraFeed;
    private final VideoCapture camera;
    private final Timer timer;
    private final SessionHandler sessionHandler;
    private final OnnxRunner onnxRunner;

    private final ProgramSettings settings = ProgramSettings.getCurrentSettings();

    public CameraFetcher(JLabel cameraFeed, VideoCapture camera, OnnxRunner onnxRunner, SessionHandler sessionHandler) {
        this.cameraFeed = cameraFeed;
        this.camera = camera;
        this.timer = new Timer();
        this.onnxRunner = onnxRunner;
        this.sessionHandler = sessionHandler;
    }

    /**
     * Convert Bytedeco Mat to BufferedImage
     */
    private static BufferedImage cvt2bi(Mat frame) {
        // Dimensions
        int width = frame.cols();
        int height = frame.rows();
        int channels = frame.channels();

        BytePointer dataPtr = frame.data();
        byte[] b = new byte[width * height * channels];
        dataPtr.get(b);

        // Determine the correct BufferedImage type
        int type = (channels > 1) ? BufferedImage.TYPE_3BYTE_BGR : BufferedImage.TYPE_BYTE_GRAY;
        BufferedImage image = new BufferedImage(width, height, type);

        // Copy the raw bytes into the BufferedImage
        byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        System.arraycopy(b, 0, targetPixels, 0, b.length);

        return image;
    }

    @Override
    public void run() {
        // Configure camera resolution
        camera.set(CAP_PROP_FRAME_WIDTH, cameraFeed.getWidth());
        camera.set(CAP_PROP_FRAME_HEIGHT, cameraFeed.getHeight());

        TimerTask task = new TimerTask() {

            // Required objects for detections & display
            private static final Mat frame = new Mat();
            private static int currentFrame = 0;
            private static OnnxOutput onnxOutput;
            private static List<Detection> detections = new ArrayList<>();

            @Override
            public void run() {
                if (!Thread.currentThread().isInterrupted()) {
                    camera.read(frame);
                    Mat inferenceFrame = frame.clone();

                    // Every Nth frame, run object detection
                    if (++currentFrame % settings.getProcessEveryNthFrame() == 0) {
                        new Thread(() -> {
                            onnxOutput = onnxRunner.runInference(inferenceFrame);
                            detections = onnxOutput.getDetectionList();
                            inferenceFrame.deallocate();
                        }).start();
                        currentFrame = 0;
                    }

                    // Overlay predictions & resize
                    if(settings.isShowBoundingBoxes())
                        ImageUtil.drawPredictions(frame, detections);
                    try {
                        resize(frame, frame, new Size(cameraFeed.getWidth(), cameraFeed.getHeight()));

                        int settingsRotation = settings.getCameraRotation();
                        int ROTA = 3;
                        switch (settingsRotation) {
                            case 90 -> ROTA = opencv_core.ROTATE_90_CLOCKWISE;
                            case 180 -> ROTA = opencv_core.ROTATE_180;
                            case 270 -> ROTA = opencv_core.ROTATE_90_COUNTERCLOCKWISE;
                        }

                        opencv_core.rotate(frame, frame, ROTA);
                        // Show frame in label
                        BufferedImage biFrame = cvt2bi(frame);
                        ImageIcon icon = new ImageIcon(biFrame);

                        cameraFeed.setIcon(new ImageIcon(biFrame));
                    } catch (Exception e ){
                        System.out.println("Camera Fetcher had to stop! If you are closing the program, this is expected.");
                        this.cancel();
                    }
                    // Write the frame to the video file if the session is active
                    if (sessionHandler.isSessionActive()) {
                        FileSession fileSession = sessionHandler.getFileSession();
                        VideoWriter writer = fileSession.getVideoWriter();
                        // Initializes the video writer
                        if ((writer == null || !writer.isOpened())) {
                            fileSession.initVideoWriter(frame);
                            onnxRunner.getLogQueue().addGreenLog("---Video recording started.---");
                        }
                        fileSession.writeVideoFrame(frame);
                        if (currentFrame % settings.getProcessEveryNthFrame() == 0)
                            onnxRunner.processDetections(detections);


                    }
                }
            }
        };
        // Schedule the capture task
        timer.schedule(task,0,1000/settings.getCameraFps());
    }
}
