package io.github.tkjonesy.utils.models;

import io.github.tkjonesy.ONNX.models.OnnxRunner;
import lombok.Getter;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

@Getter
public class SessionHandler {

    private FileSession fileSession;
    private String sessionTitle;
    private String sessionDescription;
    private final LogHandler logHandler;

    private AtomicBoolean isActive = new AtomicBoolean(false);
    private Instant startTime;

    public SessionHandler(LogHandler logHandler) {
        this.logHandler = logHandler;
    }

    public boolean startNewSession(String title, String description, OnnxRunner onnxRunner) {
        this.sessionTitle = title;
        this.sessionDescription = description;
        this.startTime = Instant.now();

        try{
            this.fileSession = new FileSession(onnxRunner, title, description, logHandler); // Throws RunTimeException if fails
            this.logHandler.setFileSession(fileSession);

        }catch (RuntimeException e) {
            System.err.println("Failed to start new SessionHandler: " + e.getMessage());
            return false;
        }

        onnxRunner.getLogQueue().addGreenLog("---Session started.---");
        this.isActive = new AtomicBoolean(true);
        return true;
    }

    public void endSession() {
        isActive.set(false);
        fileSession.endSession();
        fileSession.destroyVideoWriter();
        fileSession = null;
        startTime = null;
        this.logHandler.setFileSession(null);
    }

    public boolean isSessionActive() {
        return isActive.get();
    }

    public String getSessionDuration() {
        if (startTime == null) return "00:00";
        Duration duration = Duration.between(startTime, Instant.now());
        long minutes = duration.toMinutes();
        long seconds = duration.getSeconds() % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

}
