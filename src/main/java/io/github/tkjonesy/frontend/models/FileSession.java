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


public class FileSession {

    // Save directory
    private String saveDir;

    /*
        * videoWriter for saving video frames to a file.
        * logBufferedWriter for saving log messages to a .log file.
        *
        * These are initialized when a session is started and released when the session ends.
    */
    @Getter
    private VideoWriter videoWriter = null;
    private BufferedWriter logBufferedWriter = null;

    // Atomic boolean to track if a session is active
    private final AtomicBoolean sessionActive = new AtomicBoolean(false);

    public boolean isSessionActive() {
        return sessionActive.get();
    }

    // Starts a new Session
    public void startNewSession() {
        try {
            String FILE_DIRECTORY = Settings.FILE_DIRECTORY;

            // Ensure the parent directory exists
            Files.createDirectories(Paths.get(FILE_DIRECTORY));

            // Current date time in ISO 8601 format
            String dateTime = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HHmm"));

            // Create a new directory for the session
            saveDir = FILE_DIRECTORY + "/" + dateTime;
            if (!new java.io.File(saveDir).mkdir()) {
                throw new IOException("Failed to create directory: " + saveDir);
            }

            // Create a buffered writer to save logs to a file
            this.logBufferedWriter = new BufferedWriter(new FileWriter(saveDir + "/logfile.log", true));

            // Set the session as active
            sessionActive.set(true);
        } catch (IOException e) {
            System.err.println("Failed to create session directory.\n"+e.getMessage());
        }
    }

    // Initializes the VideoWriter with the first frame
    public void initVideoWriter(Mat frame) throws IllegalStateException {
        final Size frameSize = new Size(frame.width(), frame.height());
        String videoPath = saveDir + "/recording.mp4";
        videoWriter = new VideoWriter(
                videoPath,
                VideoWriter.fourcc('X', '2', '6', '4'),
                30, // FPS
                frameSize
        );

        if (!videoWriter.isOpened()) {
            throw new RuntimeException("Failed to open VideoWriter at " + videoPath);
        }
    }

    // Writes a video frame to the videoWriter
    public void writeVideoFrame(Mat frame){
        if (videoWriter != null) {
            videoWriter.write(frame);
        }
    }

    // Ends the current session
    public void endSession() {
        sessionActive.set(false);

        releaseVideoWriter();
        closeLogWriter();
    }

    // Releases the videoWriter
    public void releaseVideoWriter(){
        if (videoWriter != null) {
            videoWriter.release();
            videoWriter = null;
        }
    }

    private void closeLogWriter() {
        if (logBufferedWriter != null) {
            try {
                logBufferedWriter.flush();
                logBufferedWriter.close();
            } catch (IOException e) {
                System.err.println("Failed to close log writer: " + e.getMessage());
            } finally {
                logBufferedWriter = null;
            }
        }
    }

    // Writes a log message to the logBufferedWriter
    public void writeLogToFile(Log log) {
        try {
            String fullMessage = log.getTimeStamp() + " - " + log.getMessage();
            this.logBufferedWriter.write(fullMessage + "\n");
        } catch (IOException e) {
            System.out.println("File Session has not started yet. Cannot save log.");
        }
    }

}
