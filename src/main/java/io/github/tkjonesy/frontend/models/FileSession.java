package io.github.tkjonesy.frontend.models;

import io.github.tkjonesy.ONNX.models.Log;
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

    @Getter
    private VideoWriter writer = null;

    private final AtomicBoolean sessionActive = new AtomicBoolean(false);

    public boolean isSessionActive() {
        return sessionActive.get();
    }

    public void startNewSession() {
        try {
            // Ensure the parent directory exists
            Files.createDirectories(Paths.get("archive"));

            // Current date time in ISO 8601 format
            String dateTime = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HHmm"));

            // Create a new directory for the session
            saveDir = "archive/session_" + dateTime;
            if (!new java.io.File(saveDir).mkdir()) {
                throw new IOException("Failed to create directory: " + saveDir);
            }

            sessionActive.set(true);
        } catch (IOException e) {
            System.err.println("Failed to create session directory. Session not started.");
        }
    }

    public void endSession() {
        sessionActive.set(false);
        releaseWriter();
    }

    public void initVideoWriter(Mat frame) throws IllegalStateException {
        if (!isSessionActive()) {
            throw new IllegalStateException("Session is not active. Start a session before initializing the VideoWriter.");
        }

        final Size frameSize = new Size(frame.width(), frame.height());
        String videoPath = saveDir + "/recording.mp4";
        writer = new VideoWriter(
                videoPath,
                VideoWriter.fourcc('X', '2', '6', '4'),
                30, // FPS
                frameSize
        );

        if (!writer.isOpened()) {
            throw new RuntimeException("Failed to open VideoWriter at " + videoPath);
        }
    }


    public void writeFrame(Mat frame){
        if (writer != null) {
            writer.write(frame);
        }
    }

    public void releaseWriter(){
        if (writer != null) {
            writer.release();
            writer = null;
        }
    }

    public void saveLog(Log log) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(saveDir + "/logfile.log", true))) {
            String fullMessage = "["+log.getTimeStamp()+"]" + " - " + log.getMessage();
            writer.write(fullMessage + "\n");
        } catch (IOException e) {
            System.out.println("File Session has not started yet. Cannot save log.");
        }
    }

}
