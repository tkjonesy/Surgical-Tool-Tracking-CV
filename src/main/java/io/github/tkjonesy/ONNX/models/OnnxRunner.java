package io.github.tkjonesy.ONNX.models;

import ai.onnxruntime.OrtException;
import io.github.tkjonesy.ONNX.Detection;
import io.github.tkjonesy.ONNX.Yolo;
import io.github.tkjonesy.ONNX.YoloV8;
import io.github.tkjonesy.utils.settings.ProgramSettings;
import lombok.*;

import org.bytedeco.opencv.opencv_core.Mat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    public void clearClasses() {
        this.activeDetections.clear();
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

        System.out.println("BEFORE");
        System.out.println("Current detections: " + currentDetections);
        System.out.println("Detection buffer: " + detectionBuffer);
        System.out.println("Active detections: " + activeDetections);

        for(var detection: activeDetections.entrySet()){
            if(currentDetections.containsKey(detection.getKey())){
                continue;
            }

            currentDetections.put(detection.getKey(), 0);
            System.out.println("Detection " + detection.getKey() + " has exited the view. Adding removal to buffer");

        }

        // Add new detections to the detectionBuffer from the current frame
        for(var detection: currentDetections.entrySet()){

            DetectionWithCount detectionWithCount = new DetectionWithCount(detection.getKey(), detection.getValue());
            int currentCount = detectionBuffer.getOrDefault(detectionWithCount, 0);
            currentCount = Math.max(currentCount, 0) + 1;

            System.out.println("Detection " + detectionWithCount + " has entered the view. Adding to buffer");
            if(currentCount >= bufferThreshold){
                if(detectionWithCount.count == 0){
                    System.out.println("Detection " + detectionWithCount + " has passed through the buffer. Removing from active detections");
                    activeDetections.remove(detection.getKey());
                }else{
                    System.out.println("Detection " + detectionWithCount + " has passed through the buffer. Adding to active detections");
                    activeDetections.put(detection.getKey(), detection.getValue());
                }

                detectionBuffer.remove(detectionWithCount);
            }else{

                System.out.println("Detection " + detectionWithCount + " is still in the buffer. Adding to buffer");
                detectionBuffer.put(detectionWithCount, currentCount);
            }
        }

        // Decrement detections from detectionBuffer that are not present in the current frame
        for(var detection: detectionBuffer.entrySet()){

            int objectCount = detection.getKey().count;
            int currentDetectionCount = currentDetections.getOrDefault(detection.getKey().label, 0);
            if(currentDetections.containsKey(detection.getKey().label) && objectCount == currentDetectionCount){
                continue;
            }

            int currentBufferCount = detection.getValue();
            currentBufferCount = Math.min(currentBufferCount, 0) - 1;

            System.out.println("Detection " + detection.getKey() + " has exited the view. Removing from buffer");
            if(currentBufferCount <= -bufferThreshold) {
                System.out.println("Detection " + detection.getKey() + " has failed the buffer. Removing from active detections");
                detectionBuffer.remove(detection.getKey());

            }else{
                System.out.println("Detection " + detection.getKey() + " is still in the buffer. Adding to buffer");
                detectionBuffer.put(detection.getKey(), currentBufferCount);
            }
        }

        System.out.println("AFTER");
        System.out.println("Detection buffer: " + detectionBuffer);
        System.out.println("Active detections: " + activeDetections);
        System.out.println("Current detections: " + currentDetections);
        System.out.println("\n\n");

    }

    private void handleUpsert(DetectionWithCount detectionWithCount){
        int originalValue = activeDetections.get(detectionWithCount.label);

        activeDetections.put(detectionWithCount.label, detectionWithCount.count);

        int newValue = activeDetections.get(detectionWithCount.label);

    }
}

@AllArgsConstructor
@EqualsAndHashCode
@ToString
class DetectionWithCount {
    public final String label;
    public final int count;
}