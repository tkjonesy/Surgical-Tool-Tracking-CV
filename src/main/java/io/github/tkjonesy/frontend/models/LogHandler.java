package io.github.tkjonesy.frontend.models;

import io.github.tkjonesy.ONNX.models.Log;
import io.github.tkjonesy.ONNX.models.LogQueue;
import lombok.Getter;

import javax.swing.*;

public class LogHandler {

    @Getter
    private final JTextPane logTextPane;
    private final LogQueue logQueue;

    // This StringBuilder accumulates the log messages in HTML format
    private final StringBuilder logHtmlContent;

    public LogHandler(JTextPane textPane, LogQueue logQueue) {
        this.logTextPane = textPane;
        this.logQueue = logQueue;

        // Initialize the HTML content structure
        this.logHtmlContent = new StringBuilder("<html><body style='color:white;'>");

        startLogUpdater();
    }

    /**
     * Appends a new log entry as colored HTML text to the log text pane.
     *
     * @param log The log entry to display.
     */
    private void addLog(Log log) {
        // Get the color of the log type as a hex code
        String colorHex = "#" + Integer.toHexString(log.getLogType().getColor().getRGB()).substring(2);

        // Format the log entry as an HTML line with timestamp and message
        String logMessage = String.format("<span style='color:%s'>%s - %s</span><br>",
                colorHex, log.getTimeStamp(), log.getMessage());

        // Append the log message to the accumulated HTML content
        logHtmlContent.append(logMessage);

        // Update the JTextPane with the new HTML content
        logTextPane.setText(logHtmlContent + "</body></html>");

        // Auto-scroll to the bottom of the JTextPane
        logTextPane.setCaretPosition(logTextPane.getDocument().getLength());
    }

    /**
     * Periodically fetches logs from the LogQueue and updates the tracking panel.
     */
    private void startLogUpdater() {
        Timer timer = new Timer(1000, e -> {
            // Simulate log addition for testing purposes
            logQueue.addGreenLog("Green log message");
            logQueue.addYellowLog("Yellow log message");
            logQueue.addRedLog("Red log message");

            // Process logs from the queue
            Log nextLog;
            while ((nextLog = logQueue.getNextLog()) != null) {
                addLog(nextLog);
            }
        });
        timer.start();
    }
}
