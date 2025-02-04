package io.github.tkjonesy.frontend.models;

import io.github.tkjonesy.ONNX.models.OnnxRunner;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.concurrent.atomic.AtomicBoolean;

@Getter
public class SessionHandler {

    private FileSession fileSession;
    private String sessionTitle;
    private String sessionDescription;
    private final LogHandler logHandler;

    private AtomicBoolean activeState = new AtomicBoolean(false);

    public SessionHandler(LogHandler logHandler) {
        this.logHandler = logHandler;
    }

    public boolean startNewSession(String title, String description, OnnxRunner onnxRunner) {
        this.sessionTitle = title;
        this.sessionDescription = description;

        try{
            this.fileSession = new FileSession(onnxRunner, title); // Throws RunTimeException if fails
            this.logHandler.setFileSession(fileSession);

        }catch (RuntimeException e) {
            System.err.println("Failed to start new SessionHandler: " + e.getMessage());
            return false;
        }

        this.activeState = new AtomicBoolean(true);
        return true;
    }

    public void endSession() {
        fileSession.endSession();
        fileSession.destroyVideoWriter();
        fileSession = null;
        activeState.set(false);
    }

    public boolean isSessionActive() {
        return activeState.get();
    }

}
