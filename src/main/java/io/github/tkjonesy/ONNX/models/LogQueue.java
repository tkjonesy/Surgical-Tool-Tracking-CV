package io.github.tkjonesy.ONNX.models;

import io.github.tkjonesy.ONNX.enums.LogEnum;

import java.util.LinkedList;
import java.util.Queue;

public class LogQueue {

    private final Queue<Log> logs;

    public LogQueue() {
        this.logs = new LinkedList<>();
    }

    public void addError(String message) {
        logs.add(new Log(LogEnum.ERROR, message));
    }

    public void addInfo(String message) {
        logs.add(new Log(LogEnum.INFO, message));
    }

    public void addSuccess(String message) {
        logs.add(new Log(LogEnum.SUCCESS, message));
    }

    public Log getLog(){
        return logs.poll();
    }
}
