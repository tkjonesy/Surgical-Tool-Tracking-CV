package io.github.tkjonesy.ONNX.models;

import ai.onnxruntime.OrtException;
import io.github.tkjonesy.ONNX.Detection;
import io.github.tkjonesy.ONNX.Yolo;
import io.github.tkjonesy.ONNX.YoloV8;
import io.github.tkjonesy.ONNX.settings.Settings;
import lombok.Getter;
import org.opencv.core.Mat;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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

    private HashSet<String> knownClasses = new HashSet<>();
    private HashMap<String, Integer> previousClasses;

    // Counter for numbering log entries
    private int logCounter;

    public OnnxRunner(LogQueue logger) {
        this.logger = logger;
        classes = new HashMap<>();
        previousClasses = new HashMap<>(); // Initializes previousClasses to store previous frame detections
        logCounter = 1;
        try {
            this.inferenceSession = new YoloV8(Settings.modelPath, Settings.labelPath);
        } catch (OrtException | IOException exception) {
            System.exit(1);
        }
        printHeader();
    }


    // Utility method to format log messages
    private String formatLogMessage(int logIndex, String label, String action) {
        return String.format(
                "Log #%d    Object: %-15s    Action: %-25s",
                logIndex, label, action
        );
    }

    /**
     * Runs inference on the given frame and returns the detected objects.
     *
     * @param frame The {@link Mat} object representing the image frame to be processed.
     * @return An {@link OnnxOutput} object containing the list of detections.
     */
    public OnnxOutput runInference(Mat frame) {
        List<Detection> detectionList = new ArrayList<>();

        try {
            detectionList = inferenceSession.run(frame);
        } catch (OrtException ortException) {
            logger.addRedLog("Error running inference: " + ortException.getMessage());
        }

        // Process classes with the detected items and update logs if objects leave the view
        processClasses(detectionList);

        return new OnnxOutput(detectionList);
    }

    // Method to print the header row
    private void printHeader() {
        String header = String.format("%-10s %-20s %-20s",
                "Index", "Object", "Log Action");
        System.out.println(header);
        System.out.println("=".repeat(header.length()));  // Underline the header with equals signs
    }

    /**
     * Processes the detected classes, logging any changes in classes, such as additions,
     * removals, or exits from view.
     *
     * @param detections A list of {@link Detection} objects representing the detected items.
     */
    private void processClasses(List<Detection> detections) {
        DateTimeFormatter formatterDate = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter formatterTime = DateTimeFormatter.ofPattern("HH:mm:ss");

        // Count detections for this frame
        HashMap<String, Integer> updatedClasses = new HashMap<>();
        for (Detection detection : detections) {
            String label = detection.label();
            updatedClasses.put(label, updatedClasses.getOrDefault(label, 0) + 1);
        }

        HashMap<String, Integer> currentFrameClasses = new HashMap<>();
        for (Detection detection : detections) {
            String label = detection.label();
            currentFrameClasses.put(label, currentFrameClasses.getOrDefault(label, 0) + 1);
        }

        // Check for new detections or count updates
        for (String label : updatedClasses.keySet()) {
            String date = LocalDateTime.now().format(formatterDate);
            String time = LocalDateTime.now().format(formatterTime);
            String logMessage = "";
            String logAction = "";

            if (!classes.containsKey(label)) {
                // New class detected, print in green
                logAction = "New class detected";
                //logger.addGreenLog(logMessage);
            } else if (!updatedClasses.get(label).equals(classes.get(label))) {
                // Class count updated, print in yellow
                logMessage = String.format("Log #%-10d Object: %-20s Action: %-10s", logCounter++, label, "Class count updated: " + updatedClasses.get(label));
                logAction = "Class count updated";
                logger.addYellowLog(logMessage);
            }

            if (!logMessage.isEmpty()) {  // Only log if a change was detected
                System.out.println(logMessage);
            }
            classes.put(label, updatedClasses.get(label));
        }

        // Log objects that are no longer present
        String date = LocalDateTime.now().format(formatterDate);
        String time = LocalDateTime.now().format(formatterTime);

        // Check for new or reappearing objects in the current frame
        for (String label : currentFrameClasses.keySet()) {
            if (!knownClasses.contains(label)) {
                // First time seeing this object type, log as new detection
                String logMessage = formatLogMessage(logCounter++, label, "New Object Detected");
                logger.addGreenLog(logMessage);
                knownClasses.add(label); // Mark as known for future detections
            } else if (!previousClasses.containsKey(label)) {
                // Object was previously seen, left view, and now reappeared
                String logMessage = formatLogMessage(logCounter++, label, "Reappeared in camera view");
                logger.addGreenLog(logMessage);
            }
        }

        for (String label : previousClasses.keySet()) {
            if (!updatedClasses.containsKey(label)) {
                // Object left camera view, log in red
                String exitMessage = formatLogMessage(logCounter++, label, "Left Camera View");;
                logger.addRedLog(exitMessage);
            }
        }

        // Update the previousClasses with the current frame's classes for the next iteration
        previousClasses = new HashMap<>(currentFrameClasses);
        // Update previousClasses for the next frame comparison
        previousClasses.clear();
        previousClasses.putAll(updatedClasses);
    }
}
