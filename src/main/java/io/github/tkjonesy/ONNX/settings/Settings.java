package io.github.tkjonesy.ONNX.settings;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Settings {
    public static final String modelPath;
    public static final String labelPath;
    public static final int PROCESS_EVERY_NTH_FRAME;

    public static final float confThreshold;
    public static final float nmsThreshold;
    public static final int gpuDeviceId;
    public static final int INPUT_SIZE;
    public static final int NUM_INPUT_ELEMENTS;
    public static final long[] INPUT_SHAPE;

    static {
        Properties properties = new Properties();
        try (FileInputStream input = new FileInputStream("src/main/resources/onnxSettings.properties")) {
            properties.load(input);

            modelPath = properties.getProperty("modelPath", "./ai_models/yolo11m.onnx");
            labelPath = properties.getProperty("labelPath", "./src/main/resources/coco.names");
            PROCESS_EVERY_NTH_FRAME = Integer.parseInt(properties.getProperty("PROCESS_EVERY_NTH_FRAME", "30"));

            confThreshold = Float.parseFloat(properties.getProperty("confThreshold", "0.3"));
            nmsThreshold = Float.parseFloat(properties.getProperty("nmsThreshold", "0.4"));
            gpuDeviceId = Integer.parseInt(properties.getProperty("gpuDeviceId", "0"));
            INPUT_SIZE = Integer.parseInt(properties.getProperty("INPUT_SIZE", "640"));
            NUM_INPUT_ELEMENTS = Integer.parseInt(properties.getProperty("NUM_INPUT_ELEMENTS", String.valueOf(3 * 640 * 640)));

            String[] shapeValues = properties.getProperty("INPUT_SHAPE", "1,3,640,640").split(",");
            INPUT_SHAPE = new long[shapeValues.length];
            for (int i = 0; i < shapeValues.length; i++) {
                INPUT_SHAPE[i] = Long.parseLong(shapeValues[i]);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load settings from properties file", e);
        }
    }
}
