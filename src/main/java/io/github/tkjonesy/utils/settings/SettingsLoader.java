package io.github.tkjonesy.utils.settings;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class SettingsLoader {

    private static final String AIMS_Directory = System.getProperty("user.home") + "/AIMs";
    private static final String SETTINGS_FILE_PATH = AIMS_Directory + "/settings.json";
    private static final String DEFAULT_SETTINGS_FILE_PATH = "/defaultSettings.json";

    private static final String DEFAULT_MODEL = "yolo11m";

    public static ProgramSettings loadSettings(){
        initializeParentDirectory();

        ObjectMapper objectMapper = new ObjectMapper();
        ProgramSettings settings = loadSettingsFromFile(objectMapper, SETTINGS_FILE_PATH);

        if(settings == null){
            settings = loadSettingsFromResource(objectMapper, DEFAULT_SETTINGS_FILE_PATH);
        }

        if(settings != null){
            verifyModelAndLabels(settings);
            saveSettings(settings, SETTINGS_FILE_PATH);
        }

        return settings;
    }

    private static void initializeParentDirectory() {
        try {
            Path parentDirectory = Paths.get(AIMS_Directory);
            if (!Files.exists(parentDirectory)) {
                Files.createDirectories(parentDirectory);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to create settings directory: " + AIMS_Directory, e);
        }
    }

    private static ProgramSettings loadSettingsFromFile(ObjectMapper objectMapper, String filePath) {
        File settingsFile = new File(filePath);
        if (settingsFile.exists()) {
            try {
                return objectMapper.readValue(settingsFile, ProgramSettings.class);
            } catch (IOException e) {
                throw new RuntimeException("Failed to load settings from file: " + filePath, e);
            }
        }
        return null;
    }

    private static ProgramSettings loadSettingsFromResource(ObjectMapper objectMapper, String resourcePath) {
        try (InputStream inputStream = SettingsLoader.class.getResourceAsStream(resourcePath)) {
            if (inputStream != null) {
                return objectMapper.readValue(inputStream, ProgramSettings.class);
            } else {
                System.err.println("Default settings file not found in resources.");
            }
        } catch (IOException e) {
            System.err.println("Failed to load default settings: " + e.getMessage());
        }
        return null;
    }

    private static void saveSettings(ProgramSettings settings, String filePath){
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.writeValue(new File(filePath), settings);
        } catch (IOException e) {
            System.err.println("Failed to save settings: " + e.getMessage());
        }
    }

    // Verify that the model and label files exist, extracting them from resources if they don't
    private static void verifyModelAndLabels(ProgramSettings settings) {
        String modelPath = settings.getModelPath();
        String labelPath = settings.getLabelPath();

        File modelFile = new File(modelPath);
        if (!modelFile.exists()) {
            modelPath = extractResourceIfMissing("/ai_models/" + DEFAULT_MODEL + ".onnx", modelPath);
            settings.setModelPath(modelPath);
        }

        File labelFile = new File(labelPath);
        if (!labelFile.exists()) {
            labelPath = extractResourceIfMissing("/ai_models/" + DEFAULT_MODEL + ".names", labelPath);
            settings.setLabelPath(labelPath);
        }
    }

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
