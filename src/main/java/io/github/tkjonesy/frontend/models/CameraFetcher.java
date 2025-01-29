package io.github.tkjonesy.frontend.models;

import io.github.tkjonesy.ONNX.Detection;
import io.github.tkjonesy.ONNX.ImageUtil;
import io.github.tkjonesy.ONNX.models.OnnxOutput;
import io.github.tkjonesy.ONNX.models.OnnxRunner;

import org.bytedeco.javacpp.BytePointer;

import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.opencv_videoio.VideoCapture;
import org.bytedeco.opencv.opencv_videoio.VideoWriter;

import static org.bytedeco.opencv.global.opencv_imgproc.resize;
import static org.bytedeco.opencv.global.opencv_videoio.CAP_PROP_FRAME_WIDTH;
import static org.bytedeco.opencv.global.opencv_videoio.CAP_PROP_FRAME_HEIGHT;

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
    private final Timer timer;

    private final OnnxRunner onnxRunner;
    private final FileSession fileSession;

    public CameraFetcher(JLabel cameraFeed, VideoCapture camera, OnnxRunner onnxRunner, FileSession fileSession) {
        this.cameraFeed = cameraFeed;
        this.camera = camera;
        this.timer = new Timer();
        this.onnxRunner = onnxRunner;
        this.fileSession = fileSession;
    }

    /**
     * Convert Bytedeco Mat to BufferedImage
     */
    private static BufferedImage cvt2bi(Mat frame) {
        // Dimensions
        int width = frame.cols();
        int height = frame.rows();
        int channels = frame.channels();

        // Bytedeco Mat data -> BytePointer
        BytePointer dataPtr = frame.data();
        byte[] b = new byte[width * height * channels];
        dataPtr.get(b); // Copy native memory into Java byte[]

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
                    if (++currentFrame % PROCESS_EVERY_NTH_FRAME == 0) {
                        new Thread(() -> {
                            onnxOutput = onnxRunner.runInference(inferenceFrame);
                            detections = onnxOutput.getDetectionList();
                            inferenceFrame.deallocate();
                        }).start();
                        currentFrame = 0;
                    }

                    // Overlay predictions & resize
                    ImageUtil.drawPredictions(frame, detections);

                    resize(frame, frame, new Size(cameraFeed.getWidth(), cameraFeed.getHeight()));

                    // Show frame in label
                    BufferedImage biFrame = cvt2bi(frame);
                    cameraFeed.setIcon(new ImageIcon(biFrame));

                    // Write the frame to the video file if the session is active
                    VideoWriter writer = fileSession.getVideoWriter();
                    if (fileSession.isSessionActive()) {

                        // Initializes the video writer
                        if ((writer == null || !writer.isOpened())) {
                            fileSession.initVideoWriter(frame);
                            onnxRunner.getLogQueue().addGreenLog("---Video recording started.---");
                        }

                        // Video frame rate
                        if(currentFrame % 2 == 0){
                            fileSession.writeVideoFrame(frame);
                        }
                        onnxRunner.processDetections(detections);

                    } else {
                        fileSession.destroyVideoWriter();
                        onnxRunner.clearClasses();
                    }
                }
            }
        };
        // Schedule the capture task
        timer.schedule(task,0,1000/CAMERA_FRAME_RATE);
    }
}
