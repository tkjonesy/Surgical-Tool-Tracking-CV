package io.github.tkjonesy.frontend.models;

import io.github.tkjonesy.ONNX.models.Log;
import io.github.tkjonesy.ONNX.settings.Settings;
import lombok.Getter;

import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.opencv_videoio.VideoWriter;

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
    public boolean startNewSession() {
        try {
            System.out.println("\u001B[33m☐ Starting new FileSession...\u001B[0m");
            String FILE_DIRECTORY = Settings.FILE_DIRECTORY;

            // Ensure the parent directory exists
            Files.createDirectories(Paths.get(FILE_DIRECTORY));

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

            System.out.println("\u001B[32m☑ FileSession started successfully. Files will be saved to: " + saveDir + "\u001B[0m");

            return true;
        } catch (IOException e) {
            System.err.println("\u001B[31m☒ Failed to initialize session: " + e.getMessage() + "\u001B[0m");
            return false;
        }
    }


    /**
     * Initializes the VideoWriter for saving video frames.
     *
     * @param frame The first frame, used to determine video properties such as size and format.
     * @throws IllegalStateException if the session is not active.
     */
    protected void initVideoWriter(Mat frame) throws IllegalStateException {
        final Size frameSize = new Size(frame.cols(), frame.rows());
        String videoPath = saveDir + "/recording.mp4";
        int codec = VideoWriter.fourcc((byte) 'a', (byte) 'v', (byte) 'c', (byte) '1');

        videoWriter = new VideoWriter(videoPath, codec, 30.0, frameSize, true);

        if (!videoWriter.isOpened()) {
            throw new IllegalStateException("Failed to open VideoWriter with path: " + videoPath);
        }
    }

    protected void destroyVideoWriter(){
        if(videoWriter != null){
            videoWriter.release();
            videoWriter = null;

            System.out.println("\u001B[32m☑ Video recording ended. Video saved to: " + saveDir + "/recording.mp4\u001B[0m");
        }
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
        System.out.println("\u001B[33m☐ Ending current FileSession...\u001B[0m");
        sessionActive.set(false);
        closeLogWriter();

        if(logBufferedWriter == null) {
            System.out.println("\u001B[32m☑ FileSession ended successfully. Log file saved to: " + saveDir + "/logfile.log\u001B[0m");
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

/*
    Video saving logic was written by @j-mckiern.
 */