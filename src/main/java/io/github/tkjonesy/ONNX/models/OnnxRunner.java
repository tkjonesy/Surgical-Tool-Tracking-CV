package io.github.tkjonesy.ONNX.models;

import ai.onnxruntime.OrtException;
import io.github.tkjonesy.ONNX.Detection;
import io.github.tkjonesy.ONNX.Yolo;
import io.github.tkjonesy.ONNX.YoloV8;
import io.github.tkjonesy.ONNX.enums.LogEnum;
import io.github.tkjonesy.ONNX.settings.Settings;
import lombok.Getter;
import org.opencv.core.Mat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * The {@code OnnxRunner} class provides a wrapper for running YOLO-based inference
 * using ONNX models. It manages model inference sessions, logging, and tracking of
 * detected classes.
 */
public class OnnxRunner {

    /** The YOLO inference session used to run the YOLO model. */
    private Yolo inferenceSession;

    /** A queue of logs to be displayed in the UI. */
    @Getter
    private final LogQueue logger;

    /** A hashmap for tracking detected classes and their counts. */
    @Getter
    private final HashMap<String, Integer> classes;

    /**
     * Initializes a new {@code OnnxRunner} instance, setting up the model and logger.
     * If model loading fails, the application exits with an error.
     */
    public OnnxRunner(LogQueue logQueue){
        this.logger = logQueue;
        classes = new HashMap<>();

        try {
            this.inferenceSession = new YoloV8(Settings.modelPath, Settings.labelPath);
            logger.addGreenLog("Model loaded successfully");
        } catch (OrtException | IOException exception) {
            logger.addRedLog("Error loading model: " + exception.getMessage());
            System.exit(1);
        }
    }

    /**
     * Runs inference on the given frame and returns the detected objects.
     *
     * @param frame The {@link Mat} object representing the image frame to be processed.
     * @return An {@link OnnxOutput} object containing the list of detections.
     */
    public OnnxOutput runInference(Mat frame){
        List<Detection> detectionList = new ArrayList<>();
        try {
            detectionList = inferenceSession.run(frame);
            updateClasses(detectionList);
        } catch (OrtException ortException) {
            logger.addRedLog("Error running inference: " + ortException.getMessage());
        }

        return new OnnxOutput(detectionList);
    }

    /**
     * Updates the {@code classes} hashmap with the latest detections.
     *
     * @param detections A list of {@link Detection} objects representing the detected items.
     */
    private void updateClasses(List<Detection> detections){
        for (Detection detection : detections) {
            String label = detection.label();
            if (!classes.containsKey(label)) {
                classes.put(label, classes.size());
                logger.addGreenLog("New class detected: "+label);
            }
        }
    }

    /**
     * Processes the detected classes, logging any changes in classes, such as additions
     * or removals. This method is a placeholder for future implementation.
     */
    public void processClasses(){

    }
}
