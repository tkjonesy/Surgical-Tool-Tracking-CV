package io.github.tkjonesy.ONNX.models;

import ai.onnxruntime.OrtException;
import io.github.tkjonesy.ONNX.Detection;
import io.github.tkjonesy.ONNX.Yolo;
import io.github.tkjonesy.ONNX.YoloV8;
import io.github.tkjonesy.utils.settings.DefaultSettings;
import lombok.Getter;

import org.bytedeco.opencv.opencv_core.Mat;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.time.Duration;
import java.time.Instant;

/**
 * The {@code OnnxRunner} class provides a wrapper for running YOLO-based inference
 * using ONNX models. It manages model inference sessions, logging, and tracking of
 * detected classes.
 */
public class OnnxRunner {
    private HashSet<String> initialToolSet = new HashSet<>();
    private HashSet<String> lastKnownTools = new HashSet<>();
    private Instant startTime;

    /** The YOLO inference session used to run the YOLO model. */
    private Yolo inferenceSession;

    /** A queue of logs to be displayed in the UI. */
    @Getter
    private final LogQueue logQueue;

    /** A hashmap for tracking detected classes and their counts. */
    @Getter
    private final HashMap<String, Integer> classes;

    /**
     * ✅ Starts tracking tools when session begins.
     */
    public void startTracking() {
        startTime = Instant.now();
        initialToolSet.clear();
        lastKnownTools.clear();

        // ✅ Capture initial tool set after 1 sec
        new Thread(() -> {
            try {
                Thread.sleep(1000);
                initialToolSet.addAll(getClasses().keySet());
                System.out.println("✅ Initial tools captured: " + initialToolSet);
            } catch (InterruptedException e) {
                System.err.println("Failed to capture initial tool set: " + e.getMessage());
            }
        }).start();

        // ✅ Continuously track last known tools
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(500);
                    lastKnownTools.clear();
                    lastKnownTools.addAll(getClasses().keySet());
                    System.out.println("🔍 Updated Last Known Tools: " + lastKnownTools);
                } catch (InterruptedException e) {
                    System.err.println("Error updating last known tools: " + e.getMessage());
                }
            }
        }).start();
    }

    /**
     * ✅ Captures final tools when session ends.
     */
    public void captureFinalTools() {
        try {
            Thread.sleep(500); // Small delay for final detection update
        } catch (InterruptedException e) {
            System.err.println("Warning: Delay interrupted before final tool capture.");
        }

        lastKnownTools.clear();
        lastKnownTools.addAll(getClasses().keySet());
        System.out.println("🔍 Final tools detected: " + lastKnownTools);
    }

    /**
     * ✅ Returns initial tools detected.
     */
    public HashSet<String> getInitialToolSet() {
        return new HashSet<>(initialToolSet);
    }

    /**
     * ✅ Returns final tools detected.
     */
    public HashSet<String> getFinalToolSet() {
        return new HashSet<>(lastKnownTools);
    }

    /**
     * ✅ Returns session duration.
     */
    public Duration getSessionDuration() {
        return Duration.between(startTime, Instant.now());
    }

    @Getter
    private final HashMap<String, Integer> detectedClasses = new HashMap<>();

    /**
     * ✅ Simulated function that returns detected tools.
     */
    public HashMap<String, Integer> getClasses() {
        System.out.println("🔹 getClasses() called. Detected classes: " + detectedClasses);
        return detectedClasses;
    }


    private HashSet<String> knownClasses = new HashSet<>();
    private HashMap<String, Integer> previousClasses;
    private final HashMap<String, Integer> totalObjectAppearances = new HashMap<>();

    // Counter for numbering log entries
    private int logCounter=1;

    public OnnxRunner(LogQueue logQueue) {

        this.logQueue = logQueue;
        classes = new HashMap<>();
        previousClasses = new HashMap<>(); // Initializes previousClasses to store previous frame detections
        try {
            this.inferenceSession = new YoloV8(DefaultSettings.modelPath, DefaultSettings.labelPath);
        } catch (OrtException | IOException exception) {
            System.err.println("Error initializing YOLO model: " + exception.getMessage());
            System.exit(1);
        }
        printHeader();
    }

    /**
     * Clears the classes hashmap, removing all tracked classes.
     */
    public void clearClasses() {
        classes.clear();
        previousClasses.clear();
        knownClasses.clear();
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

            logQueue.addRedLog("Error running inference: " + ortException.getMessage());
            System.err.println("Error running inference: " + ortException.getMessage());
        }

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
    public void processDetections(List<Detection> detections) {
        detectedClasses.clear(); // Reset previous detections
        DateTimeFormatter formatterDate = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter formatterTime = DateTimeFormatter.ofPattern("HH:mm:ss");

        for (Detection detection : detections) {
            detectedClasses.put(detection.label(), detectedClasses.getOrDefault(detection.label(), 0) + 1);
        }

        System.out.println("🔹 Detected Classes Updated: " + detectedClasses);

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
                logQueue.addYellowLog(logMessage);
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
                logQueue.addGreenLog(logMessage);
                knownClasses.add(label); // Mark as known for future detections
            } else if (!previousClasses.containsKey(label)) {
                // Object was previously seen, left view, and now reappeared
                String logMessage = formatLogMessage(logCounter++, label, "Reappeared in camera view");
                logQueue.addGreenLog(logMessage);
            }
            //classes.put(label, classes.getOrDefault(label, 0) + 1);
        }

        for (String label : previousClasses.keySet()) {
            if (!updatedClasses.containsKey(label)) {
                // Object left camera view, log in red
                String exitMessage = formatLogMessage(logCounter++, label, "Left Camera View");
                logQueue.addRedLog(exitMessage);
            }
        }

        // Update the previousClasses with the current frame's classes for the next iteration
        previousClasses = new HashMap<>(currentFrameClasses);
        // Update previousClasses for the next frame comparison
        previousClasses.clear();
        previousClasses.putAll(updatedClasses);
    }
}