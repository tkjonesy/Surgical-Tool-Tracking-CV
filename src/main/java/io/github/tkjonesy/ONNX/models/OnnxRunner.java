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

/**
 * The {@code OnnxRunner} class provides a wrapper for running YOLO-based inference
 * using ONNX models. It manages model inference sessions, logging, and tracking of
 * detected classes.
 */
public class OnnxRunner {

    private int logCounter = 1;
    @Getter
    private int peakObjectsSeen = 0; // Tracks the highest number of objects seen at once

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
    private HashMap<String, Integer> startCountPerClass;

    @Getter
    private final HashMap<String, Integer> totalInstancesAdded = new HashMap<>();

    @Setter
    private int bufferThreshold = 3;

    private boolean sessionActive = false;

    public OnnxRunner(LogQueue logQueue) {

        this.logQueue = logQueue;
        this.activeDetections = new HashMap<>();
        this.detectionBuffer = new HashMap<>();

        this.startCountPerClass = new HashMap<>();

        try {
            ProgramSettings settings = ProgramSettings.getCurrentSettings();
            this.inferenceSession = new YoloV8(settings.getModelPath(), settings.getLabelPath());
        } catch (OrtException | IOException exception) {
            System.err.println("Error initializing YOLO model: " + exception.getMessage());
            System.exit(1);
        }
        printHeader();
    }

    public void startSession(){
       System.out.println("🔄 Starting new tracking session.");
    }

    public void endSession() {
        sessionActive = false;
        startCountPerClass.clear();
        activeDetections.clear();
        detectionBuffer.clear();
        peakObjectsSeen = 0;      // Resets peak object count
        totalInstancesAdded.clear();
        logCounter = 1;
        System.out.println("🔄 Tracking data reset for new session.");
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

    /**
     * Processes the detected classes, logging any changes in classes, such as additions,
     * removals, or exits from view.
     *
     * @param detections A list of {@link Detection} objects representing the detected items.
     */
    public void processDetections(List<Detection> detections) {
        final HashMap<String, Integer> currentDetections = detectionsListToMap(detections);

        //  Update Peak Objects Seen at Once
        int currentObjectsSeen = activeDetections.values().stream().mapToInt(Integer::intValue).sum(); // Count total objects seen
        if (currentObjectsSeen > peakObjectsSeen) {
            peakObjectsSeen = currentObjectsSeen; // Update if higher count found
        }

        // If the session is not active and the current detections are not empty, capture the initial tool set
        if (!sessionActive && !activeDetections.isEmpty()) {
            sessionActive = true;
            startCountPerClass = new HashMap<>();
            startCountPerClass.putAll(activeDetections);
            System.out.println("✅ Initial tools captured: " + startCountPerClass);
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

                // Update the activeDetections
                handleUpdate(detectionWithCount);

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
            int detectionCountInBuffer = detection.getKey().count();
            int detectionCountInCurrentDetections = currentDetections.getOrDefault(detection.getKey().label(), 0);

            // If the label is in the current frame and the count is the same, skip
            if (currentDetections.containsKey(detection.getKey().label()) && detectionCountInBuffer == detectionCountInCurrentDetections) {
                continue;
            }

            queueForBufferRemoval.add(detection.getKey());
        }

        // Remove the detections from the buffer
        while (!queueForBufferRemoval.isEmpty()) {
            DetectionWithCount detection = queueForBufferRemoval.poll();
            detectionBuffer.remove(detection);
        }
    }

    private void handleUpdate(DetectionWithCount detectionWithCount) {
        int originalValue = activeDetections.getOrDefault(detectionWithCount.label(), 0);
        int newValue = detectionWithCount.count();
        int difference = newValue - originalValue;

        //  Green log - New object detected or class count increased
        if (difference > 0) {
            if(originalValue == 0){
                String logMessage = formatLogMessage(logCounter++, detectionWithCount.label(), "New Object Detected: " + newValue);
                logQueue.addGreenLog(logMessage);
                System.out.println("🟢 DEBUG: Added to Log - " + logMessage);
            }else{
                String logMessage = formatLogMessage(logCounter++, detectionWithCount.label(), "Class count increased: " + newValue);
                logQueue.addGreenLog(logMessage);
                System.out.println("🟢 DEBUG: Count Increased - " + logMessage);
            }

            int totalAdded = totalInstancesAdded.getOrDefault(detectionWithCount.label(), 0);
            totalInstancesAdded.put(detectionWithCount.label(), totalAdded + difference);

        //  Red log - Object removed or class count decreased
        } else if (difference < 0) {
            if(newValue == 0) {
                String logMessage = formatLogMessage(logCounter++, detectionWithCount.label(), "Object Removed");
                logQueue.addRedLog(logMessage);
                System.out.println("🔴 DEBUG: Removed from Log - " + logMessage);
            } else{
                String logMessage = formatLogMessage(logCounter++, detectionWithCount.label(), "Class count decreased: " + newValue);
                logQueue.addRedLog(logMessage);
                System.out.println("🔴 DEBUG: Count Decreased - " + logMessage);
            }
        }

        // Update active detections
        if(difference != 0 && newValue != 0){
            activeDetections.put(detectionWithCount.label(), newValue);
        }else if(newValue == 0){
            activeDetections.remove(detectionWithCount.label());
        }
    }
}

record DetectionWithCount(String label, int count) {}