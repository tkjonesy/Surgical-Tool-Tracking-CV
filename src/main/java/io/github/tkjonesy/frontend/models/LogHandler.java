package io.github.tkjonesy.frontend.models;

import io.github.tkjonesy.ONNX.models.Log;
import io.github.tkjonesy.ONNX.models.LogQueue;

import lombok.Getter;
import lombok.Setter;

import javax.swing.*;

public class LogHandler {

    @Getter
    private final JTextPane logTextPane;
    @Getter
    private final LogQueue logQueue;

    // New method to set FileSession after initialization
    @Setter
    private FileSession fileSession;
    private Timer timer;

    // This StringBuilder accumulates the log messages in HTML format
    private final StringBuilder logHtmlContent = new StringBuilder("<html><body style='color:white;'>");

    public LogHandler(JTextPane textPane) {
        this.logTextPane = textPane;
        this.logQueue = new LogQueue();

        startLogProcessing();
    }

    /**
     * Processes a log entry by appending it to the log text pane and saving it to a file.
     *
     * @param log The log entry to process.
     */
    private void processLog(Log log){
        appendLogToPane(log);
        saveLogToFile(log);
    }

    /**
     * Appends a new log entry as colored HTML text to the log text pane.
     *
     * @param log The log entry to display.
     */
    private void appendLogToPane(Log log) {
        // Get the color of the log type as a hex code
        String colorHex = "#" + Integer.toHexString(log.getLogType().getColor().getRGB()).substring(2);

        // Format the log entry as an HTML line with timestamp and message
        String logMessage = String.format("<span style='color:%s'>%s - %s</span><br>",
                colorHex, log.getTimeStamp(), log.getMessage());

        // Append the log message to the accumulated HTML content
        logHtmlContent.append(logMessage);
        logTextPane.setText(logHtmlContent + "</body></html>");

        // Auto-scroll to the bottom of the JTextPane
        logTextPane.setCaretPosition(logTextPane.getDocument().getLength());
    }

    /**
     * Saves a log entry to a file.
     *
     * @param log The log entry to save.
     */
    private void saveLogToFile(Log log){
        fileSession.writeLogToFile(log);
    }

    /**
     * Starts a timer that processes logs from the log queue every second.
     */
    public void startLogProcessing() {
        this.timer = new Timer(1000, e ->
        {
            // Process logs from the queue
            Log nextLog;
            while ((nextLog = logQueue.getNextLog()) != null && fileSession != null){
                processLog(nextLog);
            }
        });
        timer.start();
    }

    public void endLogProcessing() {
        if(timer != null){
            timer.stop();
        }
    }
}
