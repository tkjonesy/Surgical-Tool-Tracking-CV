package io.github.tkjonesy.frontend.models;

import io.github.tkjonesy.ONNX.models.Log;
import io.github.tkjonesy.ONNX.models.OnnxRunner;
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
import java.util.HashMap;
import java.util.HashSet;
import java.time.Duration;
import java.time.Instant;

/**
 * Represents a session for saving video and log files. Handles session lifecycle, including
 * initialization, writing to files, and cleanup of resources.
 */
public class FileSession {

    private static final String ROOT_DIRECTORY = Settings.FILE_DIRECTORY;

    private Instant startTime;
    private final OnnxRunner onnxRunner;
    private final String title;
    private String sessionDirectory;

    public FileSession(OnnxRunner onnxRunner, String title)  {
        this.onnxRunner = onnxRunner;
        this.title = title;
        try{
            startNewSession(); // Throws IOException if fails
        }catch (IOException e) {
            throw new RuntimeException("Failed to start new FileSession", e);
        }
    }


    /** VideoWriter for saving video frames to a file. */
    @Getter
    private VideoWriter videoWriter = null;

    /** BufferedWriter for saving log messages to a .log file. */
    private BufferedWriter logBufferedWriter = null;


    /**
     * Starts a new session by creating a directory and initializing resources for saving video and log files.
     */
    public void startNewSession() throws IOException {
        System.out.println("\u001B[33m☐ Starting new FileSession...\u001B[0m");
        startTime = Instant.now();

        // Ensure the parent directory exists
        Files.createDirectories(Paths.get(ROOT_DIRECTORY));
        Files.createDirectories(Paths.get(ROOT_DIRECTORY+"/sessions"));

        String dateTime = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HHmm"));
        this.sessionDirectory = ROOT_DIRECTORY + "/sessions/" + this.title + "_" + dateTime;

        // Create the session directory
        if (!new java.io.File(sessionDirectory).mkdir()) {
            throw new IOException("Failed to create directory: " + sessionDirectory);
        }

        // Initialize BufferedWriter for saving logs
        this.logBufferedWriter = new BufferedWriter(new FileWriter(sessionDirectory + "/logfile.log", true));

        onnxRunner.startTracking();

        System.out.println("\u001B[32m☑ FileSession started successfully. Files will be saved to: " + sessionDirectory + "\u001B[0m");
    }


    /**
     * Initializes the VideoWriter for saving video frames.
     *
     * @param frame The first frame, used to determine video properties such as size and format.
     * @throws IllegalStateException if the session is not active.
     */
    protected void initVideoWriter(Mat frame) throws IllegalStateException {
        final Size frameSize = new Size(frame.cols(), frame.rows());
        String videoPath = sessionDirectory + "/recording.mp4";
        int codec = VideoWriter.fourcc((byte) 'a', (byte) 'v', (byte) 'c', (byte) '1');

        videoWriter = new VideoWriter(videoPath, codec, 30.0, frameSize, true);

        if (!videoWriter.isOpened()) {
            throw new IllegalStateException("Failed to open VideoWriter with path: " + videoPath);
        }
    }

    public void destroyVideoWriter(){
        if(videoWriter != null){
            videoWriter.release();
            videoWriter = null;

            System.out.println("\u001B[32m☑ Video recording ended. Video saved to: " + sessionDirectory + "/recording.mp4\u001B[0m");
        }
    }

    /**
     * Writes a video frame to the video file.
     *
     * @param frame The video frame to write.
     */
    protected void writeVideoFrame(Mat frame) {
        if (videoWriter != null && videoWriter.isOpened()) {
            videoWriter.write(frame);
        }
    }

    /**
     * Writes a log message to the log file.
     *
     * @param log The log entry to write.
     */
    protected void writeLogToFile(Log log) {
        if (logBufferedWriter != null) {
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
        closeLogWriter();

        Duration recordDuration = Duration.between(startTime, Instant.now());
        HashSet<String> initialTools = onnxRunner.getInitialToolSet();
        HashSet<String> finalTools = onnxRunner.getFinalToolSet();

        generateAAR(initialTools, finalTools, recordDuration);

        if(logBufferedWriter == null) {
            System.out.println("\u001B[32m☑ FileSession ended successfully. Log file saved to: " + sessionDirectory + "/logfile.log\u001B[0m");
        }
    }

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

    private void generateAAR(HashSet<String> initialTools, HashSet<String> finalTools, Duration recordDuration) {
        HashMap<String, Integer> initialToolCounts = new HashMap<>();
        HashMap<String, Integer> finalToolCounts = new HashMap<>();

        for (String tool : initialTools) {
            initialToolCounts.put(tool, onnxRunner.getDetectedClasses().getOrDefault(tool, 1));
        }

        int peakObjects = onnxRunner.getPeakObjectsSeen();
        HashMap<String, Integer> detectedTools = onnxRunner.getMaxToolCounts(); // Get max count seen at once
        HashMap<String, Integer> lastDetectedTools = onnxRunner.getDetectedClasses(); // Tracks last detected count

        // Populate "finalToolCounts" using last detected count
        for (String tool : finalTools) {
            finalToolCounts.put(tool, lastDetectedTools.getOrDefault(tool, 1));
        }

        // Get the correct total number of times a tool was added
        HashMap<String, Integer> totalToolsAdded = onnxRunner.getTotalTimesAdded();

        // Identify "New Tools Introduced" (Tools that were not present at the start but appeared later)
        HashMap<String, Integer> newToolsIntroduced = new HashMap<>();
        for (String tool : totalToolsAdded.keySet()) {
            int totalAdded = totalToolsAdded.getOrDefault(tool, 0);
            int startCount = initialToolCounts.getOrDefault(tool, 0);

            // Only include tools that were NOT present at the start
            if (totalAdded > startCount && !initialTools.contains(tool)) {
                newToolsIntroduced.put(tool, totalAdded);
            }
        }


        // Calculate tools removed using (Total Instances - Final Count)
        HashMap<String, Integer> toolsRemoved = new HashMap<>();
        for (String tool : totalToolsAdded.keySet()) {
            int totalAdded = totalToolsAdded.getOrDefault(tool, 0);
            int finalCount = finalToolCounts.getOrDefault(tool, 0);

            if (totalAdded > finalCount) {
                toolsRemoved.put(tool, totalAdded - finalCount); // Tools removed = Total added - Final count
            }
        }

        String formattedSessionTime = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")); // format date and time
        String aarPath = sessionDirectory + "/AAR.txt";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(aarPath))) {
            writer.write("After Action Report (AAR)\n");
            writer.write("==========================\n");
            writer.write("Session Name: " + (title != null ? title : "Unknown") + "\n\n");
            writer.write("Recording Duration: " + formatDuration(recordDuration) + "\n\n");
            writer.write("Session Time: " + formattedSessionTime + "\n\n");
            writer.write("Peak Objects Seen at Once: " + peakObjects + "\n\n");


            writer.write("Total Instances of Each Tool Ever Added:\n");
            writer.write("-----------------------------------------------------\n");
            for (var entry : totalToolsAdded.entrySet()) {
                writer.write(entry.getKey() + ": " + entry.getValue() + "\n");
            }
            writer.write("-----------------------------------------------------\n\n");

            // Displays Tools Present at Start of Recording
            writer.write("Objects Present at Start:\n");
            writer.write("-----------------------------------------------------\n");
            for (String tool : initialTools) {
                writer.write(tool + ": " + detectedTools.getOrDefault(tool, 1)+ "\n");
            }
            writer.write("-----------------------------------------------------\n\n");

            // Displays Tools Present at End of Recording
            writer.write("Objects Present at End:\n");
            writer.write("------------------------\n");
            for (var entry : finalToolCounts.entrySet()) {
                writer.write(entry.getKey() + ": " + entry.getValue() + "\n");
            }
            writer.write("------------------------\n\n");

            // Displays New Tools Introduced During Session
            writer.write("New Objects Introduced During Session:\n");
            writer.write("-----------------------------------------------------\n");
            if (newToolsIntroduced.isEmpty()) {
                writer.write("None\n");
            } else {
                for (var entry : newToolsIntroduced.entrySet()) {
                    writer.write(entry.getKey() + ": " + entry.getValue() + "\n");
                }
            }
            writer.write("-----------------------------------------------------\n\n");

            // Displays Tools That Have Been Removed in the Session
            writer.write("Objects remove during session\n");
            writer.write("-----------------------------------------------------\n");
            if (toolsRemoved.isEmpty()) {
                writer.write("None\n");
            } else {
                for (var entry : toolsRemoved.entrySet()) {
                    writer.write(entry.getKey() + ": " + entry.getValue() + "\n");
                }
            }
            writer.write("-----------------------------------------------------\n");

            System.out.println("✅ AAR saved to: " + aarPath);
        } catch (IOException e) {
            System.err.println("❌ Failed to write AAR: " + e.getMessage());
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