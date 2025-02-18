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

    /** The YOLO inference session used to run the YOLO model. */
    @Setter
    private Yolo inferenceSession;

    /** A queue of logs to be displayed in the UI. */
    @Getter
    private final LogQueue logQueue;

    /** A hashmap for tracking the currently active classes and their counts.
     * Active means that these detections have passed through the buffer */
    @Getter
    private final HashMap<String, Integer> activeDetections;
    /** A hashmap to act as a buffer to filter out detection flickers*/
    @Getter
    private final HashMap<DetectionWithCount, Integer> detectionBuffer;

    // AAR - related maps
    /** A hashmap to track the start count of each class detected. */
    @Getter
    private final HashMap<String, Integer> startCountPerClass;
    /** A hashmap to track the end count of each class detected. */
    @Getter
    private final HashMap<String, Integer> endCountPerClass;
    /** A hashmap to track the total count of each class detected. */
    @Getter
    private final HashMap<String, Integer> totalCountPerClass;


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

    private HashMap<String, Integer> detectionsListToMap(List<Detection> detections){

        HashMap<String, Integer> currentDetections = new HashMap<>();
        for(Detection detection: detections){
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

        // <String, Integer> activeDetections
        // key = label
        // value = count
        // For labels that are in activeDetections but not in the current frame, add the label with count 0 to currentDetections
        for(var detection: activeDetections.entrySet()){
            // If the label is in the current frame, skip
            if(currentDetections.containsKey(detection.getKey())){
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
        for(var detection: currentDetections.entrySet()){

            // Create a DetectionWithCount object to use as a key for the detectionBuffer. example: "person3"
            DetectionWithCount detectionWithCount = new DetectionWithCount(detection.getKey(), detection.getValue());

            // currentBufferValue is the number of consecutive frames the detection has been in the buffer
            int currentBufferValue = detectionBuffer.getOrDefault(detectionWithCount, 0);

            // Increment the buffer value by 1. If the value is negative, default it to 0
            currentBufferValue = Math.max(currentBufferValue, 0) + 1;

            // If the buffer value is greater than or equal to the buffer threshold
            if(currentBufferValue >= bufferThreshold){

                // If the count is 0, remove the label from activeDetections. Otherwise, add it to activeDetections
                if(detectionWithCount.count == 0){
                    activeDetections.remove(detection.getKey());
                    System.out.println("Removed " + detection.getKey() + " from active detections.");
                }else{
                    //activeDetections.put(detection.getKey(), detection.getValue());
                    handleUpdate(detectionWithCount);
                }

                // Remove the detection from the buffer
                detectionBuffer.remove(detectionWithCount);
            }else{

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
        for(var detection: detectionBuffer.entrySet()){

            // Grab the count of the detection in the buffer and the current count of the detection
            int detectionCountInBuffer = detection.getKey().count;
            int detectionCountInCurrentDetections = currentDetections.getOrDefault(detection.getKey().label, 0);

            // If the label is in the current frame and the count is the same, skip
            if(currentDetections.containsKey(detection.getKey().label) && detectionCountInBuffer == detectionCountInCurrentDetections){
                continue;
            }

            // currentBufferValue is the number of consecutive frames the detection has been in the buffer
            int currentBufferValue = detection.getValue();

            // Decrement the buffer value by 1. If the value is positive, default it to 0
            currentBufferValue = Math.min(currentBufferValue, 0) - 1;

            // If the buffer value is less than or equal to the negative buffer threshold, remove it from the buffer
            if(currentBufferValue <= -bufferThreshold) {
                queueForBufferRemoval.add(detection.getKey());

            }else{

                // Update the bufferValue for the detection
                detectionBuffer.put(detection.getKey(), currentBufferValue);
            }
        }

        // Remove the detections from the buffer
        while(!queueForBufferRemoval.isEmpty()){
            DetectionWithCount detection = queueForBufferRemoval.poll();
            detectionBuffer.remove(detection);
        }
    }

    private void handleUpdate(DetectionWithCount detectionWithCount){
        int originalValue = activeDetections.getOrDefault(detectionWithCount.label, 0);

        activeDetections.put(detectionWithCount.label, detectionWithCount.count);

        int newValue = activeDetections.getOrDefault(detectionWithCount.label, originalValue);

        int difference = newValue - originalValue;

    }
}

@AllArgsConstructor
@EqualsAndHashCode
@ToString
class DetectionWithCount {
    public final String label;
    public final int count;
}