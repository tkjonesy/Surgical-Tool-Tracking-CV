package io.github.tkjonesy.frontend.models;

import io.github.tkjonesy.ONNX.models.Log;
import io.github.tkjonesy.ONNX.settings.Settings;
import lombok.Getter;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.videoio.VideoWriter;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Represents a session for saving video and log files. Handles session lifecycle, including
 * initialization, writing to files, and cleanup of resources.
 */
public class FileSession {

    // Save directory
    private String saveDir;

    /** VideoWriter for saving video frames to a file. */
    @Getter
    private VideoWriter videoWriter = null;

    /** BufferedWriter for saving log messages to a .log file. */
    private BufferedWriter logBufferedWriter = null;

    /** Tracks whether a session is active. */
    private final AtomicBoolean sessionActive = new AtomicBoolean(false);

    /**
     * Checks if the session is currently active.
     *
     * @return true if the session is active, false otherwise.
     */
    protected boolean isSessionActive() {
        return sessionActive.get();
    }

    /**
     * Starts a new session by creating a directory and initializing resources for saving video and log files.
     */
    public void startNewSession() {
        try {
            String FILE_DIRECTORY = Settings.FILE_DIRECTORY;

            // Ensure the parent directory exists
            Files.createDirectories(Paths.get(FILE_DIRECTORY));

            // Generate a unique directory name based on the current date and time
            String dateTime = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HHmm"));
            saveDir = FILE_DIRECTORY + "/" + dateTime;

            // Create the session directory
            if (!new java.io.File(saveDir).mkdir()) {
                throw new IOException("Failed to create directory: " + saveDir);
            }

            // Initialize BufferedWriter for saving logs
            this.logBufferedWriter = new BufferedWriter(new FileWriter(saveDir + "/logfile.log", true));

            // Mark the session as active
            sessionActive.set(true);
        } catch (IOException e) {
            System.err.println("Failed to initialize session: " + e.getMessage());
        }
    }

    /**
     * Initializes the VideoWriter for saving video frames.
     *
     * @param frame The first frame, used to determine video properties such as size and format.
     * @throws IllegalStateException if the session is not active.
     */
    protected void initVideoWriter(Mat frame) throws IllegalStateException {
        if (!isSessionActive()) {
            throw new IllegalStateException("Session is not active. Start a session before initializing the VideoWriter.");
        }

        final Size frameSize = new Size(frame.width(), frame.height());
        String videoPath = saveDir + "/recording.mp4";
        videoWriter = new VideoWriter(
                videoPath,
                VideoWriter.fourcc('X', '2', '6', '4'),
                30, // FPS
                frameSize
        );
    }

    /**
     * Writes a video frame to the video file.
     *
     * @param frame The video frame to write.
     */
    protected void writeVideoFrame(Mat frame) {
        if (videoWriter != null && videoWriter.isOpened() && isSessionActive()) {
            videoWriter.write(frame);
        }
    }

    /**
     * Writes a log message to the log file.
     *
     * @param log The log entry to write.
     */
    protected void writeLogToFile(Log log) {
        if (logBufferedWriter != null && isSessionActive()) {
            try {
                String fullMessage = log.getTimeStamp() + " - " + log.getMessage();
                this.logBufferedWriter.write(fullMessage + "\n");
            } catch (IOException e) {
                System.err.println("IO Exception writing log to file: " + e.getMessage());
            }
        }
    }

    /**
     * Ends the current session by releasing resources such as the VideoWriter and BufferedWriter.
     */
    public void endSession() {
        sessionActive.set(false);
        releaseVideoWriter();
        closeLogWriter();
    }

    /**
     * Releases the VideoWriter resource.
     */
    private void releaseVideoWriter() {
        if (videoWriter != null) {
            videoWriter.release();
            videoWriter = null;
        }
    }

    /**
     * Closes the BufferedWriter used for saving logs.
     */
    private void closeLogWriter() {
        try {
            if (logBufferedWriter != null) {
                logBufferedWriter.flush();
                logBufferedWriter.close();
            }
        } catch (IOException e) {
            System.err.println("Failed to close log writer: " + e.getMessage());
        } finally {
            logBufferedWriter = null;
        }
    }
}
