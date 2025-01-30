package io.github.tkjonesy.utils;

import io.github.tkjonesy.ONNX.models.LogQueue;

import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class AARGenerator {

    private final StringBuilder sessionLogs; // To store session logs
    private final Map<String, Integer> objectSummary; // To store object counts
    private LocalDateTime sessionStartTime; // Start time of the recording session
    private LocalDateTime sessionEndTime;   // End time of the recording session

    public AARGenerator() {
        this.sessionLogs = new StringBuilder();
        this.objectSummary = new HashMap<>();
    }


    /**
     * Sets the start time of the recording session.
     *
     * @param sessionStartTime The start time of the session.
     */
    public void setSessionStartTime(LocalDateTime sessionStartTime) {
        this.sessionStartTime = sessionStartTime;
    }

    /**
     * Sets the end time of the recording session.
     *
     * @param sessionEndTime The end time of the session.
     */
    public void setSessionEndTime(LocalDateTime sessionEndTime) {
        this.sessionEndTime = sessionEndTime;
    }

    /**
     * Adds a log to the session logs.
     *
     * @param log The log message to add.
     */
    public void addLog(String log) {
        sessionLogs.append(log).append(System.lineSeparator());
    }

    /**
     * Adds an object and its count to the object summary.
     *
     * @param objectName The name of the detected object.
     * @param count      The number of times the object was detected.
     */
    public void addObjectSummary(String objectName, int count) {
        objectSummary.put(objectName, objectSummary.getOrDefault(objectName, 0) + count);
    }

    /**
     * Generates and saves the After Action Report to a file.
     *
     * @param filePath  The path where the report should be saved.
     * @param logQueue  The log queue containing session logs.
     */
    public void generateAAR(String filePath, LogQueue logQueue) {
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write("After Action Report\n");
            writer.write("====================\n\n");

            // Recording Duration Section
            if (sessionStartTime != null && sessionEndTime != null) {
                Duration duration = Duration.between(sessionStartTime, sessionEndTime);
                writer.write("Recording Duration: " + formatDuration(duration) + "\n");
                writer.write("Session Start: " + sessionStartTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\n");
                writer.write("Session End: " + sessionEndTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\n\n");
            } else {
                writer.write("Recording Duration: Not Available\n\n");
            }

            // Session Logs
            writer.write("Session Logs:\n");
            writer.write(sessionLogs.length() > 0 ? sessionLogs.toString() : "No logs available.\n");

            // Summary
            writer.write("\nSummary:\n");
            //writer.write("Total Logs: " + logQueue.getAllLogs() + "\n");

            if (!objectSummary.isEmpty()) {
                writer.write("Objects Detected:\n");
                for (Map.Entry<String, Integer> entry : objectSummary.entrySet()) {
                    writer.write("- " + entry.getKey() + ": " + entry.getValue() + "\n");
                }
            } else {
                writer.write("No objects detected.\n");
            }

            System.out.println("AAR successfully saved to: " + filePath);
        } catch (IOException e) {
            System.err.println("Error writing AAR file: " + e.getMessage());
        }
    }

    /**
     * Formats a Duration object into a human-readable string.
     *
     * @param duration The duration to format.
     * @return A formatted string (e.g., "5 minutes 24 seconds").
     */
    private String formatDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;
        long seconds = duration.getSeconds() % 60;

        StringBuilder formattedDuration = new StringBuilder();
        if (hours > 0) {
            formattedDuration.append(hours).append(" hours ");
        }
        if (minutes > 0) {
            formattedDuration.append(minutes).append(" minutes ");
        }
        formattedDuration.append(seconds).append(" seconds");
        return formattedDuration.toString().trim();
    }
}
