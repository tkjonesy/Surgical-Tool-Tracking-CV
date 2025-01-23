package io.github.tkjonesy.frontend.models;

import io.github.tkjonesy.ONNX.Detection;
import io.github.tkjonesy.ONNX.ImageUtil;
import io.github.tkjonesy.ONNX.models.LogQueue;
import io.github.tkjonesy.ONNX.models.OnnxOutput;
import io.github.tkjonesy.ONNX.models.OnnxRunner;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.VideoWriter;
import org.opencv.videoio.Videoio;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static io.github.tkjonesy.ONNX.settings.Settings.CAMERA_FRAME_RATE;
import static io.github.tkjonesy.ONNX.settings.Settings.PROCESS_EVERY_NTH_FRAME;

public class CameraFetcher implements Runnable {

    private final JLabel cameraFeed;
    private final VideoCapture camera;
    private final java.util.Timer timer;
    private static LogQueue logger;
    private final FileSession fs;

    private int logCounter = 0;

    public CameraFetcher(JLabel cameraFeed, VideoCapture camera, LogQueue logQueue, FileSession fileSession) {
        this.cameraFeed = cameraFeed;
        this.camera = camera;
        this.timer = new Timer();
        logger = logQueue;
        this.fs = fileSession;
    }

    private static BufferedImage cvt2bi(Mat frame) {
        // Select grayscale or color based on incoming frame
        int type = BufferedImage.TYPE_BYTE_GRAY;
        if (frame.channels() > 1) {
            type = BufferedImage.TYPE_3BYTE_BGR;
        }

        // Create buffer to store bytes
        int bufferSize = frame.channels() * frame.cols() * frame.rows();
        byte[] b = new byte[bufferSize];

        // Copy data from Mat into the BufferedImage
        frame.get(0, 0, b);
        BufferedImage image = new BufferedImage(frame.cols(), frame.rows(), type);
        final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        System.arraycopy(b, 0, targetPixels, 0, b.length);
        return image;
    }

    @Override
    public void run() {
        // New task to run at frame rate specified in Settings file

        camera.set(Videoio.CAP_PROP_FRAME_WIDTH, cameraFeed.getWidth());
        camera.set(Videoio.CAP_PROP_FRAME_HEIGHT, cameraFeed.getHeight());

        TimerTask task = new TimerTask() {

            // Required objects for detections & display
            private static final Mat frame = new Mat();
            private static int currentFrame = 0;
            private static final OnnxRunner onnxRunner = new OnnxRunner(logger);
            private static OnnxOutput onnxOutput;
            private static List<Detection> detections = new ArrayList<>();
            //private VideoWriter writer = null;
            private final FileSession fileSession = fs;
            @Override
            public void run() {
                // Run if the thread hasn't been interrupted, otherwise purge the timer's schedule
                if(!Thread.currentThread().isInterrupted()) {

                    // Resize camera size to whatever the current feed window size is
//                        camera.set(Videoio.CAP_PROP_FRAME_WIDTH, cameraFeed.getWidth());
//                        camera.set(Videoio.CAP_PROP_FRAME_HEIGHT, cameraFeed.getHeight());
                    // Pull frame and run through Onnx
                    camera.read(frame);


                    // Every Nth frame, we run the object detection on it
                    if (++currentFrame % PROCESS_EVERY_NTH_FRAME == 0) {
                        new Thread(() -> {
                            onnxOutput = onnxRunner.runInference(frame);
                            detections = onnxOutput.getDetectionList();
                        }).start();
                        currentFrame = 0;
                    }

                    // Overlay the predictions, and display the frame on the label
                    ImageUtil.drawPredictions(frame, detections);
                    Imgproc.resize(frame, frame, new Size(cameraFeed.getWidth(), cameraFeed.getHeight()));
                    BufferedImage biFrame = cvt2bi(frame);
                    cameraFeed.setIcon(new ImageIcon(biFrame));

                    //Creates video writer
                    VideoWriter writer = fileSession.getVideoWriter();
                    if(fileSession.isSessionActive() && (writer == null || !writer.isOpened())) {
                        try{
                            fileSession.initVideoWriter(frame);     // (can throw IllegalStateException)
                            onnxRunner.clearClasses();
                        }catch (IllegalStateException e){
                            logger.addRedLog("Error initializing video writer: " + e.getMessage());
                        }
                    }else{
                        fileSession.writeVideoFrame(frame);
                    }
                }
                else {
                    timer.cancel();
                    timer.purge();
                }
            }
        };
        // Set the task to execute every CAMERA_FRAME_RATE frames per second
        timer.schedule(task, 0, 1000 / CAMERA_FRAME_RATE);
    }
}
