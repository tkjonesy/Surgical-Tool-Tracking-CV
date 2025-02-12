package io.github.tkjonesy.ONNX.settings;

import lombok.Getter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

/**
 * Settings class that loads "onnxSettings.properties" from resources,
 * but also extracts "ai_models/person_hand_face.onnx" AND "ai_models/human.names"
 * from inside the JAR to "User/SurgicalToolTrackingFiles/ai_models/" if they don't exist.
 */
public class DefaultSettings {

    @Getter
    private static ProgramSettings defaultSettings;

    public static final String modelName;
    public static String modelPath;
    public static String labelPath;
    public static final String FILE_DIRECTORY;


    public static final int PROCESS_EVERY_NTH_FRAME;
    public static final int CAMERA_FRAME_RATE;
    public static final int VIDEO_CAPTURE_DEVICE_ID;
    public static final int GPU_DEVICE_ID;
    public static final int INPUT_SIZE;
    public static final int NUM_INPUT_ELEMENTS;

    public static final float confThreshold;
    public static final float nmsThreshold;

    public static final long[] INPUT_SHAPE;

    public static final boolean DISPLAY_BOUNDING_BOXES;

    static {
        Properties properties = new Properties();
        try (InputStream input = DefaultSettings.class.getClassLoader().getResourceAsStream("onnxSettings.properties")) {

            if (input == null) {
                throw new RuntimeException("Failed to load settings: 'onnxSettings.properties' not found in resources.");
            }
            properties.load(input);

            PROCESS_EVERY_NTH_FRAME = Integer.parseInt(properties.getProperty("PROCESS_EVERY_NTH_FRAME", "30"));
            CAMERA_FRAME_RATE = Integer.parseInt(properties.getProperty("CAMERA_FRAME_RATE", "30"));
            VIDEO_CAPTURE_DEVICE_ID = Integer.parseInt(properties.getProperty("VIDEO_CAPTURE_DEVICE_ID", "0"));

            confThreshold = Float.parseFloat(properties.getProperty("confThreshold", "0.1"));
            nmsThreshold = Float.parseFloat(properties.getProperty("nmsThreshold", "0.4"));
            GPU_DEVICE_ID = Integer.parseInt(properties.getProperty("GPU_DEVICE_ID", "0"));
            INPUT_SIZE = Integer.parseInt(properties.getProperty("INPUT_SIZE", "640"));
            NUM_INPUT_ELEMENTS = Integer.parseInt(properties.getProperty("NUM_INPUT_ELEMENTS", String.valueOf(3 * 640 * 640)));

            String[] shapeValues = properties.getProperty("INPUT_SHAPE", "1,3,640,640").split(",");
            INPUT_SHAPE = new long[shapeValues.length];
            for (int i = 0; i < shapeValues.length; i++) {
                INPUT_SHAPE[i] = Long.parseLong(shapeValues[i]);
            }

            modelName = properties.getProperty("modelName", "person_hand_face.onnx");

            FILE_DIRECTORY = System.getProperty("user.home") + "/AIMs";

            Path aiModelDir = Paths.get(FILE_DIRECTORY, "ai_models");
            if (!Files.exists(aiModelDir)) {
                Files.createDirectories(aiModelDir);
            }

            DISPLAY_BOUNDING_BOXES = Boolean.parseBoolean(properties.getProperty("DISPLAY_BOUNDING_BOXES", "true"));

        } catch (IOException e) {
            throw new RuntimeException("Failed to load settings from properties file", e);
        }

        // Extract AI model and label files if they don't exist
        modelPath = FILE_DIRECTORY + "/ai_models/" + modelName+".onnx";
        File modelFile = new File(modelPath);
        if(!modelFile.exists()){
            modelPath = extractResourceIfMissing("/ai_models/yolo11m.onnx", FILE_DIRECTORY + "/ai_models/yolo11m.onnx");
        }

        labelPath = FILE_DIRECTORY + "/ai_models/" + modelName + ".names";
        File labelFile = new File(labelPath);
        if(!labelFile.exists()){
            labelPath = extractResourceIfMissing("/ai_models/yolo11m.names", FILE_DIRECTORY + "/ai_models/yolo11m.names");
        }
    }

    /**
     * Checks if a file exists at the given path. If it does not exist,
     * it extracts it from the JAR's resources and places it in the correct directory.
     *
     * @param resourcePath The path of the resource inside the JAR (e.g., "/ai_models/person_hand_face.onnx").
     * @param targetPath The target path in the user directory.
     * @return The file path that can be used in the application.
     */
    private static String extractResourceIfMissing(String resourcePath, String targetPath) {
        File targetFile = new File(targetPath);

        if (!targetFile.exists()) {
            try (InputStream in = DefaultSettings.class.getResourceAsStream(resourcePath)) {
                if (in == null) {
                    throw new IOException("Resource not found inside JAR: " + resourcePath);
                }
                Files.copy(in, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Extracted resource: " + resourcePath + " -> " + targetPath);
            } catch (IOException e) {
                throw new RuntimeException("Failed to extract required resource: " + resourcePath, e);
            }
        }

        return targetPath;
    }
}
