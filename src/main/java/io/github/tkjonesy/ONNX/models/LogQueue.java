package io.github.tkjonesy.ONNX.models;

import io.github.tkjonesy.ONNX.enums.LogEnum;

import java.util.LinkedList;
import java.util.Queue;

/**
 * The {@code LogQueue} class represents a queue of logs that can be added to and
 * retrieved from. It supports adding logs with different log levels (error, info,
 * success) and retrieving logs in a first-in, first-out (FIFO) order.
 */
public class LogQueue {

    /** The queue that stores the logs in FIFO order. */
    private final Queue<Log> logs;

    /**
     * Initializes a new {@code LogQueue} with an empty queue.
     */
    public LogQueue() {
        this.logs = new LinkedList<>();
    }

    /**
     * Flushes all logs from the queue.
     */
    public void flushLogs() {
        logs.clear();
    }

    /**
     * Adds an error log (red) to the queue.
     *
     * @param message The message to be logged with an error level.
     */
    public void addRedLog(String message) {
        logs.add(new Log(LogEnum.ERROR, message));
    }

    /**
     * Adds an informational log (yellow) to the queue.
     *
     * @param message The message to be logged with an informational level.
     */
    public void addYellowLog(String message) {
        logs.add(new Log(LogEnum.INFO, message));
    }

    /**
     * Adds a success log (green) to the queue.
     *
     * @param message The message to be logged with a success level.
     */
    public void addGreenLog(String message) {
        logs.add(new Log(LogEnum.SUCCESS, message));
    }

    /**
     * Retrieves and removes the latest log from the queue.
     * Returns {@code null} if the queue is empty.
     *
     * @return The latest {@code Log} object, or {@code null} if no logs are available.
     */
    public Log getNextLog(){
        System.out.println("Getting next log of queue size " + logs.size());
        return logs.poll();
    }
}
