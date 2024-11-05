package io.github.tkjonesy.ONNX.models;

import ai.onnxruntime.OrtException;
import io.github.tkjonesy.ONNX.Detection;
import io.github.tkjonesy.ONNX.ModelFactory;
import io.github.tkjonesy.ONNX.Yolo;
import io.github.tkjonesy.ONNX.enums.LogEnum;
import lombok.Getter;
import org.opencv.core.Mat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class OnnxRunner {

    private Yolo inferenceSession;

    @Getter
    private final LogQueue logger;
    @Getter
    private HashMap<String, Integer> classes;

    public OnnxRunner(){
        ModelFactory modelFactory = new ModelFactory();
        logger = new LogQueue();
        classes = new HashMap<>();
        try {
            this.inferenceSession = modelFactory.getModel();
        } catch (OrtException | IOException exception) {
            System.exit(1);
        }
    }

    public OnnxOutput runInference(Mat frame){
        List<Detection> detectionList = new ArrayList<>();
        updateClasses(detectionList);

        try {
            detectionList = inferenceSession.run(frame);
        } catch (OrtException ortException) {
            logger.addError("Error running inference: " + ortException.getMessage());
        }

        return new OnnxOutput(detectionList);
    }

    private void updateClasses(List<Detection> detections){
        for(Detection detection : detections){
            String label = detection.label();
            if(!classes.containsKey(label)){
                classes.put(label, classes.size());
            }
        }
    }

    public Log getLatestLog(){
        Log log = logger.getLog();
        if(log == null){
            log = new Log(LogEnum.DEFAULT, "No logs available");
        }
        return log;
    }

    // Implement algorithm to read the classes
    // and check if classes have been removed/added/info
    // then add a log to the logger for respective action
    public void processClasses(){

    }

}
