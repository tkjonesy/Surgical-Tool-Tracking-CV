package io.github.tkjonesy.ONNX;

import io.github.tkjonesy.ONNX.models.OnnxOutput;
import io.github.tkjonesy.ONNX.models.OnnxRunner;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

import static io.github.tkjonesy.ONNX.settings.Settings.PROCESS_EVERY_NTH_FRAME;

/*

    This class is a simple video application that uses the Yolo model to
    detect objects in a video stream. The bounding boxes and classes come from
    Java itself using ONNX runtime

    Pros:
        - The application is self-contained and does not require a separate server

    Cons:
        - The video feed is not as smooth as the Python version
        - The ONNX model is not as fast as the PyTorch model
        - Very complicated and hard to understand

 */

public class VideoApp  {

    public static void main(String[] args) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        VideoCapture cap = new VideoCapture(0);
        cap.set(Videoio.CAP_PROP_FRAME_WIDTH, 1280);
        cap.set(Videoio.CAP_PROP_FRAME_HEIGHT, 800);

        JFrame jframe = new JFrame("YOLO Object Detection");
        jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JLabel vidpanel = new JLabel();
        jframe.setContentPane(vidpanel);
        jframe.setVisible(true);
        jframe.setResizable(true);
        jframe.setSize(1280, 800);

        if (!cap.isOpened()) {
            System.exit(1);
        }

        Mat frame = new Mat();
        MatOfByte matofByte = new MatOfByte();
        OnnxRunner onnxRunner = new OnnxRunner();

        int currentFrame = 0;
        OnnxOutput onnxOutput;
        List<Detection> detections = new ArrayList<>();
        while ( cap.read(frame) ) {
            ImageUtil.resizeWithPadding(frame, frame, 1280, 800);

            if(++currentFrame % PROCESS_EVERY_NTH_FRAME == 0) {
                onnxOutput = onnxRunner.runInference(frame);
                detections = onnxOutput.getDetectionList();
                currentFrame = 0;
            }

            ImageUtil.drawPredictions(frame, detections);
            Imgcodecs.imencode(".jpg", frame, matofByte);
            ImageIcon imageIcon = new ImageIcon(matofByte.toArray());

            vidpanel.setIcon(imageIcon);
            vidpanel.repaint();
        }

        cap.release();
    }
}