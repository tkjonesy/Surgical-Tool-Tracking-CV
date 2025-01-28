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

        // Now, extract the ONNX model from the JAR resource to a temp file
        modelPath = extractResourceToTempFile("ai_models/yolo11m.onnx", ".onnx");

        // Also extract the coco.names (or your label file) to a temp file
        labelPath = extractResourceToTempFile("coco.names", ".names");
    }

    /**
     * Extracts a resource from inside the JAR (e.g. /ai_models/yolo11m.onnx)
     * to a temporary file on the local file system, then returns that file's path.
     *
     * @param resourcePath The resource path (relative to src/main/resources), e.g. "ai_models/yolo11m.onnx"
     * @param suffix       The file suffix to give the temp file (e.g. ".onnx" or ".names")
     * @return The absolute path to the extracted temp file.
     */
    private static String extractResourceToTempFile(String resourcePath, String suffix) {
        // Ensure we have a leading slash for getResourceAsStream
        String fullResourcePath = resourcePath.startsWith("/") ? resourcePath : "/" + resourcePath;

        try (InputStream resourceStream = Settings.class.getResourceAsStream(fullResourcePath)) {
            if (resourceStream == null) {
                throw new FileNotFoundException("Cannot find resource: " + resourcePath + " in the JAR.");
            }

            // Create a temp file
            File tempFile = File.createTempFile("onnx_resource_", suffix);
            tempFile.deleteOnExit(); // optional: remove file when JVM exits

            // Copy the resource into that temp file
            Files.copy(resourceStream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            return tempFile.getAbsolutePath();

        } catch (IOException e) {
            throw new RuntimeException("Failed to extract resource to temp file: " + resourcePath, e);
        }
    }
}
