package io.github.tkjonesy.ONNX.models;

import ai.onnxruntime.OrtException;
import io.github.tkjonesy.ONNX.Detection;
import io.github.tkjonesy.ONNX.Yolo;
import io.github.tkjonesy.ONNX.YoloV8;
import io.github.tkjonesy.ONNX.settings.Settings;
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
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The {@code OnnxRunner} class provides a wrapper for running YOLO-based inference
 * using ONNX models. It manages model inference sessions, logging, and tracking of
 * detected classes.
 */
public class OnnxRunner {
    private HashSet<String> initialToolSet = new HashSet<>();
    private HashSet<String> lastKnownTools = new HashSet<>();
    private Instant startTime;
    private int peakObjectsSeen = 0;

    private int peakObjectCount = 0;  // Tracks max objects seen at once
    private int logCounter = 0;        // Tracks log numbering

    private final HashSet<String> activeObjects = new HashSet<>();  // Objects currently detected
    private final HashSet<String> knownClasses = new HashSet<>();   // Objects that have been seen before
    private final HashMap<String, Integer> totalToolCounts = new HashMap<>();
    private final HashMap<String, Integer> totalToolsAdded = new HashMap<>();




    private final HashMap<String, Integer> previousCounts = new HashMap<>();  // Tracks previous frame counts
    private HashMap<String, Integer> previousClasses = new HashMap<>(); // Tracks previous frame detections
    private final HashMap<String, Integer> objectPersistence = new HashMap<>(); // Tracks how long an object has been missing

    private static final int STABILITY_THRESHOLD = 3; // How many frames before marking an object as "Left Camera View"

    private final HashMap<String, Integer> totalTimesAdded = new HashMap<>();


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

        // Capture initial tool set after 1 sec
        new Thread(() -> {
            try {
                Thread.sleep(1000);
                initialToolSet.addAll(getClasses().keySet());
                System.out.println("✅ Initial tools captured: " + initialToolSet);
            } catch (InterruptedException e) {
                System.err.println("Failed to capture initial tool set: " + e.getMessage());
            }
        }).start();

        // Continuously track last known tools
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
     * Captures final tools when session ends.
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

    public OnnxRunner(LogQueue logQueue) {

        this.logQueue = logQueue;
        classes = new HashMap<>();
        previousClasses = new HashMap<>(); // Initializes previousClasses to store previous frame detections
        try {
            this.inferenceSession = new YoloV8(Settings.modelPath, Settings.labelPath);
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

    public void updatePeakObjects(int currentCount) {
        peakObjectsSeen = Math.max(peakObjectsSeen, currentCount);
    }


    public int getPeakObjectsSeen() {
        return peakObjectsSeen;
    }

    public HashMap<String, Integer> getMaxToolCounts() {
        return new HashMap<>(totalToolCounts);
    }

    public HashMap<String, Integer> getTotalTimesAdded() {
        return new HashMap<>(totalToolsAdded);
    }

    /**
     * Processes the detected classes, logging any changes in classes, such as additions,
     * removals, or exits from view.
     *
     * @param detections A list of {@link Detection} objects representing the detected items.
     */
    public void processDetections(List<Detection> detections) {
        DateTimeFormatter formatterDate = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter formatterTime = DateTimeFormatter.ofPattern("HH:mm:ss");

        HashMap<String, Integer> updatedClasses = new HashMap<>();
        int currentObjectCount = 0;

        // Count objects in the current frame
        for (Detection detection : detections) {
            String label = detection.label();
            updatedClasses.put(label, updatedClasses.getOrDefault(label, 0) + 1);
            currentObjectCount++;

            totalToolCounts.put(label, Math.max(totalToolCounts.getOrDefault(label, 0), updatedClasses.get(label)));
        }

        // Track the peak number of objects seen at once
        updatePeakObjects(currentObjectCount);

        // Ensure logs start from Log #1 each session
        if (logCounter == 0) {
            logCounter = 1;
        }

        // Process detections & log changes
        for (String label : updatedClasses.keySet()) {
            int newCount = updatedClasses.get(label);
            int oldCount = previousCounts.getOrDefault(label, 0);

            if (newCount > oldCount) {  // If count increased, it means an object was added
                totalToolsAdded.put(label, totalToolsAdded.getOrDefault(label, 0) + (newCount - oldCount));
            }

            if (!activeObjects.contains(label)) {
                // Log either "New Object Detected" or "Reappeared in Camera View"
                String logAction = knownClasses.contains(label) ? "Reappeared in Camera View" : "New Object Detected";
                String logMessage = formatLogMessage(logCounter++, label, logAction);
                logQueue.addGreenLog(logMessage);

                activeObjects.add(label);
                knownClasses.add(label); // Ensures next time it logs as "Reappeared"
                objectPersistence.remove(label); // Reset disappearance counter
            } else if (newCount != oldCount) {
                // Log count changes (e.g., "Class count updated: 2")
                String logMessage = formatLogMessage(logCounter++, label, "Class count updated: " + newCount);
                logQueue.addYellowLog(logMessage);
            }

            classes.put(label, newCount);
        }

        // Handle objects that disappeared from the frame
        for (String label : new HashSet<>(activeObjects)) {
            if (!updatedClasses.containsKey(label)) {
                int missingFrames = objectPersistence.getOrDefault(label, 0) + 1;
                objectPersistence.put(label, missingFrames);

                if (missingFrames >= STABILITY_THRESHOLD) {
                    // Log "Left Camera View" only after STABILITY_THRESHOLD frames
                    String logMessage = formatLogMessage(logCounter++, label, "Left Camera View");
                    logQueue.addRedLog(logMessage);
                    activeObjects.remove(label);
                    objectPersistence.remove(label);
                }
            } else {
                objectPersistence.remove(label); // Reset disappearance counter if object is still present
            }
        }

        // Update previousCounts & previousClasses for the next frame
        previousCounts.clear();
        previousCounts.putAll(updatedClasses);

        // Clear and update, instead of reassigning
        previousClasses.clear();
        previousClasses.putAll(updatedClasses);

        // Update detectedClasses for external usage
        detectedClasses.clear();
        detectedClasses.putAll(updatedClasses);
    }

}