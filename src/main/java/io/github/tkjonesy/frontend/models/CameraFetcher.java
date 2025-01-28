package io.github.tkjonesy.frontend.models;

import io.github.tkjonesy.ONNX.Detection;
import io.github.tkjonesy.ONNX.ImageUtil;
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

    private final OnnxRunner onnxRunner;
    private final FileSession fileSession;

    public CameraFetcher(JLabel cameraFeed, VideoCapture camera, OnnxRunner onnxRunner, FileSession fileSession) {
        this.cameraFeed = cameraFeed;
        this.camera = camera;
        this.timer = new Timer();
        this.onnxRunner = onnxRunner;
        this.fileSession = fileSession;
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
            private static OnnxOutput onnxOutput;
            private static List<Detection> detections = new ArrayList<>();
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

                    // Write the frame to the video file if the session is active
                    VideoWriter writer = fileSession.getVideoWriter();
                    if(fileSession.isSessionActive()) {

                        // Initializes the video writer
                        if((writer == null || !writer.isOpened())){
                            fileSession.initVideoWriter(frame);
                            onnxRunner.getLogQueue().addGreenLog("---Video recording started.---");
                        }
                        fileSession.writeVideoFrame(frame);
                        onnxRunner.processDetections(detections);

                    }else {
                        fileSession.destroyVideoWriter();
                        onnxRunner.clearClasses();
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
