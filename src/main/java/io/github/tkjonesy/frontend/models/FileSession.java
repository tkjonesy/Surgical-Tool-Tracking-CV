package io.github.tkjonesy.frontend.models;

import io.github.tkjonesy.ONNX.models.Log;
import io.github.tkjonesy.ONNX.models.OnnxRunner;
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
import java.time.Duration;
import java.time.Instant;

import static io.github.tkjonesy.utils.Paths.AIMS_DIRECTORY;
import static io.github.tkjonesy.utils.Paths.AIMS_SESSIONS_DIRECTORY;

/**
 * Represents a session for saving video and log files. Handles session lifecycle, including
 * initialization, writing to files, and cleanup of resources.
 */
public class FileSession {

    private Instant startTime;
    private final OnnxRunner onnxRunner;
    private final LogHandler logHandler;
    private final String title;
    private String sessionDirectory;

    public FileSession(OnnxRunner onnxRunner, String title, LogHandler logHandler)  {
        this.onnxRunner = onnxRunner;
        this.title = title;
        this.logHandler = logHandler;
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
    private BufferedWriter csvBufferedWriter = null;




    /**
     * Starts a new session by creating a directory and initializing resources for saving video and log files.
     */
    public void startNewSession() throws IOException {
        System.out.println("\u001B[33m☐ Starting new FileSession...\u001B[0m");

        onnxRunner.startSession();

        if (logHandler != null) {
            logHandler.clearLogPane();
            System.out.println("🔄 Log panel fully reset.");
        }

        startTime = Instant.now();


        // Ensure the parent directory exists
        Files.createDirectories(Paths.get(AIMS_DIRECTORY));
        Files.createDirectories(Paths.get(AIMS_SESSIONS_DIRECTORY));

        String dateTime = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HHmm"));
        this.sessionDirectory = AIMS_SESSIONS_DIRECTORY + "/" + this.title + "_" + dateTime;

        // Create the session directory
        if (!new java.io.File(sessionDirectory).mkdir()) {
            throw new IOException("Failed to create directory: " + sessionDirectory);
        }

        // Initialize BufferedWriter for saving logs
        this.logBufferedWriter = new BufferedWriter(new FileWriter(sessionDirectory + "/logfile.log", true));

        // Initialize BufferedWriter for saving CSVs
        this.csvBufferedWriter = new BufferedWriter(new FileWriter(sessionDirectory + "/log.csv", true));
        csvBufferedWriter.write("Timestamp,LogNumber,Object,Action,ActionType\n");

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
            Mat formattedFrame = new Mat();
            frame.convertTo(formattedFrame, org.bytedeco.opencv.global.opencv_core.CV_8UC3);
            videoWriter.write(formattedFrame);
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
                String[]parsedMessage = parseLogMessage(log.getMessage());
                this.csvBufferedWriter.write(log.getTimeStamp() + "," + parsedMessage[0] + "," + parsedMessage[1] + "," + parsedMessage[2] + "," + log.getLogType() + "\n");
            } catch (IOException e) {
                System.err.println("IO Exception writing log to file: " + e.getMessage());
            }
        }
    }

    private String[] parseLogMessage(String message){
        String logNumber = "";
        String object = "";
        String action = "";

        try{
            //Extract Log Number, Object, and Action
            if(message.contains("Log #") && message.contains("Object:") && message.contains("Action:")){
                logNumber = message.split(" ")[1];
                object = message.split("Object:")[1].split("Action:")[0];
                action = message.split("Action:")[1];
            }
            else{
                logNumber = message;
            }
        } catch(Exception e){
            System.err.println("Erorr parsing log message: " + e.getMessage());
        }
        return new String[]{logNumber, object, action};
    }

    /**
     * Ends the current session by releasing resources such as the VideoWriter and BufferedWriter.
     */
    public void endSession() {
        System.out.println("\u001B[33m☐ Ending current FileSession...\u001B[0m");

        destroyVideoWriter();

        closeLogWriter();
        closeCsvWriter();

        Duration recordDuration = Duration.between(startTime, Instant.now());

        generateAAR(recordDuration);

        if(logBufferedWriter == null) {
            System.out.println("\u001B[32m☑ FileSession ended successfully. Log file saved to: " + sessionDirectory + "/logfile.log\u001B[0m");
        }

        onnxRunner.endSession();
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

    private void generateAAR(Duration recordDuration) {
        int peakObjects = onnxRunner.getPeakObjectsSeen();

        // Get correct start and end counts
        HashMap<String, Integer> initialToolCounts = onnxRunner.getStartCountPerClass();

        HashMap<String, Integer> finalToolCounts = new HashMap<>(onnxRunner.getActiveDetections());
        HashMap<String, Integer> totalToolsAdded = new HashMap<>(onnxRunner.getTotalInstancesAdded());

        // Compute "New Tools Introduced"
        HashMap<String, Integer> newToolsIntroduced = new HashMap<>();
        for (String tool : totalToolsAdded.keySet()) {
            int totalAdded = totalToolsAdded.getOrDefault(tool, 0);
            int startCount = initialToolCounts.getOrDefault(tool, 0);

            // Only count tools that had more instances added than the starting count
            if (totalAdded > startCount) {
                newToolsIntroduced.put(tool, totalAdded - startCount);
            }
        }

        // Compute "Tools Removed During Session"
        HashMap<String, Integer> toolsRemoved = new HashMap<>();
        for (String tool : totalToolsAdded.keySet()) {
            int totalAdded = totalToolsAdded.getOrDefault(tool, 0);
            int finalCount = finalToolCounts.getOrDefault(tool, 0);

            // If tool was added more times than it remains at the end, count it as removed
            if (totalAdded > finalCount) {
                toolsRemoved.put(tool, totalAdded - finalCount);
            }
        }


        // 🔍 Debugging output
        System.out.println("🔍 DEBUG: Start Count Per Class: " + initialToolCounts);
        System.out.println("🔍 DEBUG: Final Tool Counts: " + finalToolCounts);

        String formattedSessionTime = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
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
            if (totalToolsAdded.isEmpty()) {
                writer.write("None\n");
            } else {
                for (var entry : totalToolsAdded.entrySet()) {
                    writer.write(entry.getKey() + ": " + entry.getValue() + "\n");
                }
            }
            writer.write("-----------------------------------------------------\n\n");

            // Objects Present at Start
            writer.write("Objects Present at Start:\n");
            writer.write("-----------------------------------------------------\n");
            if (initialToolCounts.isEmpty()) {
                writer.write("None\n");
            } else {
                for (var entry : initialToolCounts.entrySet()) {
                    writer.write(entry.getKey() + ": " + entry.getValue() + "\n");
                }
            }
            writer.write("-----------------------------------------------------\n\n");

            // Objects Present at End
            writer.write("Objects Present at End:\n");
            writer.write("------------------------\n");
            if (finalToolCounts.isEmpty()) {
                writer.write("None\n");
            } else {
                for (var entry : finalToolCounts.entrySet()) {
                    writer.write(entry.getKey() + ": " + entry.getValue() + "\n");
                }
            }
            writer.write("------------------------\n\n");

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

            writer.write("Objects Removed During Session:\n");
            writer.write("-----------------------------------------------------\n");
            if (toolsRemoved.isEmpty()) {
                writer.write("None\n");
            } else {
                for (var entry : toolsRemoved.entrySet()) {
                    writer.write(entry.getKey() + ": " + entry.getValue() + "\n");
                }
            }
            writer.write("-----------------------------------------------------\n\n");

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

    private void closeCsvWriter() {
        try {
            if (csvBufferedWriter != null) {
                csvBufferedWriter.flush();
                csvBufferedWriter.close();
            }
        } catch (IOException e) {
            System.err.println("Failed to close csv writer: " + e.getMessage());
        } finally {
            csvBufferedWriter = null;
        }
    }
}

/*
    Video saving logic was written by @j-mckiern.
 */