package io.github.tkjonesy.utils.settings;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import static io.github.tkjonesy.utils.Paths.*;

public class SettingsLoader {

    // Default model to use if none is specified
    private static final String DEFAULT_MODEL = "yolo11m";

    public static void resetToDefaultSettings(){
        // Load default settings from resources
        ObjectMapper objectMapper = new ObjectMapper();
        ProgramSettings defaultSettings = loadSettingsFromResource(objectMapper);

        // Save the default settings to the file
        if(defaultSettings != null){
            saveSettings(defaultSettings);
            ProgramSettings.setCurrentSettings(defaultSettings);
            System.out.println("Reset settings to default.");
        }
    }

    // Method to load the settings
    public static ProgramSettings loadSettings(){
        // Initialize the AIMs directory
        initializeParentDirectory();

        // Load settings from the Settings file
        ObjectMapper objectMapper = new ObjectMapper();
        ProgramSettings settings = loadSettingsFromFile(objectMapper);

        // If settings are null, load default settings from resources
        if(settings == null){
            System.out.println("No settings file found, loading default settings.");
            settings = loadSettingsFromResource(objectMapper);
        }

        // Save the settings to the file, then verify that the specified model and label files exist
        if(settings != null){
            saveSettings(settings);
            verifyModelAndLabels(settings);
        }

        return settings;
    }

    private static void initializeParentDirectory() {
        try {
            Path parentDirectory = Paths.get(AIMS_DIRECTORY);
            if (!Files.exists(parentDirectory)) {
                Files.createDirectories(parentDirectory);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to create settings directory: " + AIMS_DIRECTORY, e);
        }
    }

    private static ProgramSettings loadSettingsFromFile(ObjectMapper objectMapper) {
        File settingsFile = new File(AIMS_SETTINGS_FILE_PATH);
        if (settingsFile.exists()) {
            try {
                return objectMapper.readValue(settingsFile, ProgramSettings.class);
            } catch (IOException e) {
                throw new RuntimeException("Failed to load settings from file: " + AIMS_DIRECTORY, e);
            }
        }
        return null;
    }

    private static ProgramSettings loadSettingsFromResource(ObjectMapper objectMapper) {
        try (InputStream inputStream = SettingsLoader.class.getResourceAsStream(RESOURCE_DEFAULT_SETTINGS_PATH)) {
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

    public static void saveSettings(ProgramSettings settings){
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.writeValue(new File(AIMS_SETTINGS_FILE_PATH), settings);
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
            System.out.println("Model file not found, extracting default model.");
            modelPath = extractResourceIfMissing(RESOURCE_DEFAULT_MODEL_PATH, AIMS_MODELS_DIRECTORY + "/" + DEFAULT_MODEL + ".onnx");
            settings.setModelPath(modelPath);
        }

        File labelFile = new File(labelPath);
        if (!labelFile.exists()) {
            System.out.println("Label file not found, extracting default labels.");
            labelPath = extractResourceIfMissing(RESOURCE_DEFAULT_LABELS_PATH, AIMS_MODELS_DIRECTORY + "/" + DEFAULT_MODEL + ".names");
            settings.setLabelPath(labelPath);
        }
    }

    private static String extractResourceIfMissing(String resourcePath, String targetPath) {
        File targetFile = new File(targetPath);

        if (!targetFile.exists()) {
            try (InputStream in = SettingsLoader.class.getResourceAsStream(resourcePath)) {
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
