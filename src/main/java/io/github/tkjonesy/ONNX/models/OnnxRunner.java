package io.github.tkjonesy.ONNX.models;

import ai.onnxruntime.OrtException;
import io.github.tkjonesy.ONNX.Detection;
import io.github.tkjonesy.ONNX.Yolo;
import io.github.tkjonesy.ONNX.YoloV8;
import io.github.tkjonesy.utils.settings.ProgramSettings;
import lombok.*;

import org.bytedeco.opencv.opencv_core.Mat;

import java.io.IOException;
import java.util.*;
import java.time.Duration;
import java.time.Instant;

/**
 * The {@code OnnxRunner} class provides a wrapper for running YOLO-based inference
 * using ONNX models. It manages model inference sessions, logging, and tracking of
 * detected classes.
 */
public class OnnxRunner {

    private Instant startTime;
    private Instant sessionStartTime;

    private int logCounter = 1;
    private int peakObjectsSeen = 0; // Tracks the highest number of objects seen at once

    private HashSet<String> initialToolSet = new HashSet<>();


    /**
     * The YOLO inference session used to run the YOLO model.
     */
    @Setter
    private Yolo inferenceSession;

    /**
     * A queue of logs to be displayed in the UI.
     */
    @Getter
    private final LogQueue logQueue;

    /**
     * A hashmap for tracking the currently active classes and their counts.
     * Active means that these detections have passed through the buffer
     */
    @Getter
    private final HashMap<String, Integer> activeDetections;
    /**
     * A hashmap to act as a buffer to filter out detection flickers
     */
    @Getter
    private final HashMap<DetectionWithCount, Integer> detectionBuffer;

    // AAR - related maps
    /**
     * A hashmap to track the start count of each class detected.
     */
    @Getter
    private final HashMap<String, Integer> startCountPerClass;
    /**
     * A hashmap to track the end count of each class detected.
     */
    @Getter
    private final HashMap<String, Integer> endCountPerClass;
    /**
     * A hashmap to track the total count of each class detected.
     */
    @Getter
    private final HashMap<String, Integer> totalCountPerClass;

    private final HashMap<String, Integer> totalInstancesAdded = new HashMap<>();

    private final HashMap<String, Integer> totalTimesAdded = new HashMap<>();

    private final HashMap<String, Integer> disappearanceBuffer = new HashMap<>();

    public HashMap<String, Integer> getTotalTimesAdded() {
        return totalTimesAdded;
    }

    public HashMap<String, Integer> getStartCountPerClass() {
        return startCountPerClass;
    }

    public HashMap<String, Integer> getActiveDetections() {
        return activeDetections;
    }

    public HashMap<String, Integer> getTotalCountPerClass() {
        return totalCountPerClass;
    }

    public HashSet<String> getInitialToolSet() {
        synchronized (initialToolSet) {
            return new HashSet<>(initialToolSet);
        }
    }

    public HashSet<String> getFinalToolSet() {
        return new HashSet<>(activeDetections.keySet());
    }

    public HashMap<String, Integer> getTotalInstancesAdded() {
        return new HashMap<>(totalInstancesAdded); // Return a copy to avoid modification
    }


    public int getPeakObjectsSeen() {
        return peakObjectsSeen;
    }



    @Setter
    private int bufferThreshold = 3;

    /**
     * ✅ Returns session duration.
     */
    public Duration getSessionDuration() {
        return Duration.between(startTime, Instant.now());
    }

    public OnnxRunner(LogQueue logQueue) {

        this.logQueue = logQueue;
        this.sessionStartTime = Instant.now();
        this.activeDetections = new HashMap<>();
        this.detectionBuffer = new HashMap<>();

        this.startCountPerClass = new HashMap<>();
        this.endCountPerClass = new HashMap<>();
        this.totalCountPerClass = new HashMap<>();

        try {
            ProgramSettings settings = ProgramSettings.getCurrentSettings();
            this.inferenceSession = new YoloV8(settings.getModelPath(), settings.getLabelPath());
        } catch (OrtException | IOException exception) {
            System.err.println("Error initializing YOLO model: " + exception.getMessage());
            System.exit(1);
        }
        printHeader();
    }

    /**
     * Clears the classes hashmap, removing all tracked classes.
     */
    public void clearOnnxRunnerMaps() {
        this.activeDetections.clear();
        this.detectionBuffer.clear();
        this.startCountPerClass.clear();
        this.totalCountPerClass.clear();
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

    private HashMap<String, Integer> detectionsListToMap(List<Detection> detections) {

        HashMap<String, Integer> currentDetections = new HashMap<>();
        for (Detection detection : detections) {
            int currentCount = currentDetections.getOrDefault(detection.label(), 0);
            currentDetections.put(detection.label(), ++currentCount);
        }

        return currentDetections;
    }


    // Method to print the header row
    private void printHeader() {
        String header = String.format("%-10s %-20s %-20s",
                "Index", "Object", "Log Action");
        System.out.println(header);
        System.out.println("=".repeat(header.length()));  // Underline the header with equals signs
    }

    // Utility method to format log messages
    private String formatLogMessage(int logIndex, String label, String action) {
        return String.format(
                "Log #%d    Object: %-15s    Action: %-25s",
                logIndex, label, action
        );
    }

    public void startTracking() {
        startTime = Instant.now();
        initialToolSet.clear();

        // Capture initial tool set after 1 sec
        new Thread(() -> {
            try {
                Thread.sleep(1000);
                synchronized (initialToolSet) {
                    initialToolSet.addAll(activeDetections.keySet());
                }
                System.out.println("✅ Initial tools captured: " + initialToolSet);
            } catch (InterruptedException e) {
                System.err.println("Failed to capture initial tool set: " + e.getMessage());
            }
        }).start();

    }

    public void captureFinalTools() {
        synchronized (activeDetections) {
            System.out.println("🔍 Capturing final tools...");
            endCountPerClass.clear();
            endCountPerClass.putAll(activeDetections);
            System.out.println("✅ Final tools captured: " + endCountPerClass);
        }
    }


    /**
     * Processes the detected classes, logging any changes in classes, such as additions,
     * removals, or exits from view.
     *
     * @param detections A list of {@link Detection} objects representing the detected items.
     */
    public void processDetections(List<Detection> detections) {
        final HashMap<String, Integer> currentDetections = detectionsListToMap(detections);
        checkForRemovedObjects(currentDetections);

        //  Update Peak Objects Seen at Once
        int currentObjectsSeen = currentDetections.values().stream().mapToInt(Integer::intValue).sum(); // Count total objects seen
        if (currentObjectsSeen > peakObjectsSeen) {
            peakObjectsSeen = currentObjectsSeen; // Update if higher count found
        }


        if (initialToolSet.isEmpty() && !currentDetections.isEmpty()) {
            synchronized (initialToolSet) {
                initialToolSet.addAll(currentDetections.keySet());
            }
            System.out.println("✅ Initial tools captured: " + initialToolSet);
        }

        // <String, Integer> activeDetections
        // key = label
        // value = count
        // For labels that are in activeDetections but not in the current frame, add the label with count 0 to currentDetections
        for (var detection : activeDetections.entrySet()) {
            // If the label is in the current frame, skip
            if (currentDetections.containsKey(detection.getKey())) {
                continue;
            }

            // If the label is not in the current frame, add its removal to the buffer
            currentDetections.put(detection.getKey(), 0);
        }

        /*
         * <String, Integer> currentDetections
         * key = label
         * value = count
         * Add the labels in currentDetections to the detectionBuffer
         *     If the label is already in the buffer, increment its count
         *     If the label is not in the buffer, add it with count 1
         *     If the buffer value is greater than the buffer threshold, remove the label from the buffer and add it to activeDetections
         */
        for (var detection : currentDetections.entrySet()) {

            // Create a DetectionWithCount object to use as a key for the detectionBuffer. example: "person3"
            DetectionWithCount detectionWithCount = new DetectionWithCount(detection.getKey(), detection.getValue());

            // currentBufferValue is the number of consecutive frames the detection has been in the buffer
            int currentBufferValue = detectionBuffer.getOrDefault(detectionWithCount, 0);

            // Increment the buffer value by 1. If the value is negative, default it to 0
            currentBufferValue = Math.max(currentBufferValue, 0) + 1;

            // If the buffer value is greater than or equal to the buffer threshold
            if (currentBufferValue >= bufferThreshold) {

                // If the count is 0, remove the label from activeDetections. Otherwise, add it to activeDetections
                if (detectionWithCount.count == 0) {
                    activeDetections.remove(detection.getKey());
                    System.out.println("Removed " + detection.getKey() + " from active detections.");
                } else {
                    //activeDetections.put(detection.getKey(), detection.getValue());
                    handleUpdate(detectionWithCount);
                }

                // Remove the detection from the buffer
                detectionBuffer.remove(detectionWithCount);
            } else {

                // Update the bufferValue for the detection
                detectionBuffer.put(detectionWithCount, currentBufferValue);
            }
        }

        // Since you can't remove items from a hashmap while iterating through it, we need to store the items to be removed in a queue
        Queue<DetectionWithCount> queueForBufferRemoval = new LinkedList<>();

        // <DetectionWithCount, Integer> detectionBuffer
        // key = label + count
        // value = number of consecutive frames the detection has been in the buffer
        // For labels that are in the buffer but not in the current frame, decrement. Aka decrement detection outliers/flickers
        for (var detection : detectionBuffer.entrySet()) {

            // Grab the count of the detection in the buffer and the current count of the detection
            int detectionCountInBuffer = detection.getKey().count;
            int detectionCountInCurrentDetections = currentDetections.getOrDefault(detection.getKey().label, 0);

            // If the label is in the current frame and the count is the same, skip
            if (currentDetections.containsKey(detection.getKey().label) && detectionCountInBuffer == detectionCountInCurrentDetections) {
                continue;
            }

            // currentBufferValue is the number of consecutive frames the detection has been in the buffer
            int currentBufferValue = detection.getValue();

            // Decrement the buffer value by 1. If the value is positive, default it to 0
            currentBufferValue = Math.min(currentBufferValue, 0) - 1;

            // If the buffer value is less than or equal to the negative buffer threshold, remove it from the buffer
            if (currentBufferValue <= -bufferThreshold) {
                queueForBufferRemoval.add(detection.getKey());

            } else {

                // Update the bufferValue for the detection
                detectionBuffer.put(detection.getKey(), currentBufferValue);
            }
        }

        // Remove the detections from the buffer
        while (!queueForBufferRemoval.isEmpty()) {
            DetectionWithCount detection = queueForBufferRemoval.poll();
            detectionBuffer.remove(detection);
        }
    }

    private void handleUpdate(DetectionWithCount detectionWithCount) {
        int originalValue = activeDetections.getOrDefault(detectionWithCount.label, 0);
        int newValue = detectionWithCount.count;
        int difference = newValue - originalValue;


        //  Green log - New object detected
        if (originalValue == 0 && newValue > 0) {
            String logMessage = formatLogMessage(logCounter++, detectionWithCount.label, "New Object Detected: " + newValue);
            logQueue.addGreenLog(logMessage);
            System.out.println("🟢 DEBUG: Added to Log - " + logMessage);
            int totalAdded = totalInstancesAdded.getOrDefault(detectionWithCount.label, 0);
            totalInstancesAdded.put(detectionWithCount.label, totalAdded + 1);
        }
        else if (difference != 0) {
            if (difference > 0) {
                String logMessage = formatLogMessage(logCounter++, detectionWithCount.label, "Class count increased: " + newValue);
                logQueue.addGreenLog(logMessage);
                System.out.println("🟢 DEBUG: Count Increased - " + logMessage);
                int totalAdded = totalInstancesAdded.getOrDefault(detectionWithCount.label, 0);
                totalInstancesAdded.put(detectionWithCount.label, totalAdded + difference);
            } else {
                String logMessage = formatLogMessage(logCounter++, detectionWithCount.label, "Class count decreased: " + newValue);
                logQueue.addRedLog(logMessage);
                System.out.println("🔴 DEBUG: Count Decreased - " + logMessage);
            }
        }

        // Update active detections
        activeDetections.put(detectionWithCount.label, newValue);
    }



    private void checkForRemovedObjects(HashMap<String, Integer> currentDetections) {
        for (String label : new HashSet<>(activeDetections.keySet())) {
            if (!currentDetections.containsKey(label)) {
                // Track how long it's been missing
                int missingFrames = disappearanceBuffer.getOrDefault(label, 0) + 1;
                disappearanceBuffer.put(label, missingFrames);

                if (missingFrames >= bufferThreshold) {
                    String logMessage = formatLogMessage(logCounter++, label, "Left Camera View");
                    logQueue.addRedLog(logMessage);
                    System.out.println("🔴 DEBUG: Object Left - " + logMessage);

                    activeDetections.remove(label);
                    disappearanceBuffer.remove(label); // Reset tracking
                    System.out.println("🔴 DEBUG: Removed " + label + " from active detections.");
                }
            } else {
                disappearanceBuffer.remove(label); // Reset if detected again
            }
        }
    }

    public void resetTrackingData() {
        totalTimesAdded.clear();
        startCountPerClass.clear();
        endCountPerClass.clear();
        totalCountPerClass.clear();
        activeDetections.clear();
        detectionBuffer.clear();
        disappearanceBuffer.clear();
        initialToolSet.clear();  // Clears the initial tool tracking
        peakObjectsSeen = 0;      // Resets peak object count
        totalInstancesAdded.clear();
        logCounter = 1;


        System.out.println("🔄 Tracking data reset for new session.");
    }

    @AllArgsConstructor
    @EqualsAndHashCode
    @ToString
    class DetectionWithCount {
        public final String label;
        public final int count;
    }
}