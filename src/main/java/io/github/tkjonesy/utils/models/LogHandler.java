package io.github.tkjonesy.utils.models;

import io.github.tkjonesy.ONNX.models.Log;
import io.github.tkjonesy.ONNX.models.LogQueue;

import lombok.Getter;
import lombok.Setter;

import javax.swing.*;

public class LogHandler {

    @Getter
    private static JTextPane logTextPane;
    @Getter
    private static final LogQueue logQueue = new LogQueue();

    // New method to set FileSession after initialization
    @Setter
    private FileSession fileSession;
    private Timer timer;

    // This StringBuilder accumulates the log messages in HTML format
    private static final StringBuilder logHtmlContent = new StringBuilder("<html><body style='color:white;'>");

    public LogHandler(JTextPane textPane) {
        this.logTextPane = textPane;

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

    public static void forceProcessNextLog(){
        Log nextLog = logQueue.getNextLog();
        if(nextLog != null){
            appendLogToPane(nextLog);
        }
    }

    /**
     * Appends a new log entry as colored HTML text to the log text pane.
     *
     * @param log The log entry to display.
     */
    private static void appendLogToPane(Log log) {
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


    /**
     * Clears the log display when a session ends.
     */
    public void clearLogPane() {
        logHtmlContent.setLength(0); // Reset log HTML content
        logHtmlContent.append("<html><body style='color:white;'>"); // Keep formatting
        logTextPane.setText(logHtmlContent + "</body></html>");

        //logQueue.flushLogs();
    }
}
