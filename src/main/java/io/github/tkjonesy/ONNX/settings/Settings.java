package io.github.tkjonesy.ONNX.settings;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

/**
 * Settings class that loads "onnxSettings.properties" from resources,
 * but also extracts "ai_models/yolo11m.onnx" AND "ai_models/coco.names"
 * from inside the JAR to temp files, so ONNX runtime can read them.
 */
public class Settings {

    public static final String modelPath;   // path to the temp .onnx file
    public static final String labelPath;   // path to the temp .names file

    public static final int PROCESS_EVERY_NTH_FRAME;
    public static final int CAMERA_FRAME_RATE;
    public static final int VIDEO_CAPTURE_DEVICE_ID;

    public static final float confThreshold;
    public static final float nmsThreshold;
    public static final int gpuDeviceId;
    public static final int INPUT_SIZE;
    public static final int NUM_INPUT_ELEMENTS;
    public static final long[] INPUT_SHAPE;

    public static final String FILE_DIRECTORY;

    static {
        Properties properties = new Properties();
        try (InputStream input = Settings.class.getClassLoader().getResourceAsStream("onnxSettings.properties")) {

            if (input == null) {
                throw new RuntimeException("Failed to load settings: 'onnxSettings.properties' not found in resources.");
            }
            properties.load(input);

            // We'll ignore the old 'modelPath' and 'labelPath' from properties,
            // because we want to extract them from resources in the JAR.
            PROCESS_EVERY_NTH_FRAME = Integer.parseInt(properties.getProperty("PROCESS_EVERY_NTH_FRAME", "30"));
            CAMERA_FRAME_RATE = Integer.parseInt(properties.getProperty("CAMERA_FRAME_RATE", "30"));
            VIDEO_CAPTURE_DEVICE_ID = Integer.parseInt(properties.getProperty("VIDEO_CAPTURE_DEVICE_ID", "0"));

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

            FILE_DIRECTORY = System.getProperty("user.home") + "/SurgicalToolTrackingFiles";

        } catch (IOException e) {
            throw new RuntimeException("Failed to load settings from properties file", e);
        }

        // Verify the model and label files exist in the user directory
        modelPath = verifyFileExists(FILE_DIRECTORY + "/ai_models/person_hand_face.onnx");
        labelPath = verifyFileExists(FILE_DIRECTORY + "/ai_models/human.names");
    }

    /**
     * Verifies that a file exists at the given path.
     * Throws an exception if the file is missing.
     *
     * @param filePath The path to the file to verify.
     * @return The file path if the file exists.
     */
    private static String verifyFileExists(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new RuntimeException("Required file not found: " + filePath + ". Please ensure it exists.");
        }
        return filePath;
    }
}
