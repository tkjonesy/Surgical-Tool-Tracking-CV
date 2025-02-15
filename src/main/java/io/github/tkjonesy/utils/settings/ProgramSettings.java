package io.github.tkjonesy.utils.settings;

import ai.onnxruntime.OrtSession;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.tkjonesy.utils.annotations.SettingsLabel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;

@AllArgsConstructor
@Getter
public class ProgramSettings {

    private static ProgramSettings currentSettings;

    private final String FILE_DIRECTORY = System.getProperty("user.home") + "/SurgicalToolTrackingFiles";

    // Camera variables
    @SettingsLabel(value = "cameraDeviceId", type = Integer.class)
    private int cameraDeviceId;
    @SettingsLabel(value = "cameraFps", type = Integer.class)
    private int cameraFps;

    // Storage variables
    @SettingsLabel(value = "fileDirectory", type = String.class)
    private String fileDirectory;

    // AI settings
    @SettingsLabel(value = "modelPath", type = String.class)
    private String modelPath;
    @SettingsLabel(value = "labelPath", type = Integer.class)
    private int processEveryNthFrame;
    @SettingsLabel(value = "showBoundingBoxes", type = Boolean.class)
    private boolean showBoundingBoxes;
    @SettingsLabel(value = "confThreshold", type = Float.class)
    private float confThreshold;

    // Advanced AI settings
    @SettingsLabel(value = "nmsThreshold", type = Float.class)
    private float nmsThreshold;
    @SettingsLabel(value = "optimizationLevelString", type = String.class) // all, extended, basic, none
    private String optimizationLevel;
    private OrtSession.SessionOptions.OptLevel optimizationLevelEnum;
    @SettingsLabel(value = "gpuDeviceId", type = Integer.class)
    private int gpuDeviceId;

    public void setSettings(String label, Object value) {
        Field[] fields = this.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(SettingsLabel.class)) {
                SettingsLabel annotation = field.getAnnotation(SettingsLabel.class);
                if (annotation.value().equals(label)) {
                    field.setAccessible(true);
                    try {
                        // Check if the value is of the correct type
                        if (annotation.type().isInstance(value)) {
                            field.set(this, value);

                            if(label.equals("optimizationLevelString")) {
                                this.optimizationLevelEnum = getSessionOptions((String) value);
                            }


                        } else {
                            System.out.println("Type mismatch: Cannot assign " +
                                    value.getClass().getSimpleName() + " to " +
                                    annotation.type().getSimpleName());
                        }
                    } catch (IllegalAccessException e) {
                        System.err.println("Failed to set value for " + label + ": " + e.getMessage());
                    }
                    return;
                }
            }
        }
        System.err.println("No setting found with label: " + label);
    }

    private OrtSession.SessionOptions.OptLevel getSessionOptions(String optimizationLevel) {
        return switch (optimizationLevel.toLowerCase()) {
            case "all" -> OrtSession.SessionOptions.OptLevel.ALL_OPT;
            case "extended" -> OrtSession.SessionOptions.OptLevel.EXTENDED_OPT;
            case "basic" -> OrtSession.SessionOptions.OptLevel.BASIC_OPT;
            case "none" -> OrtSession.SessionOptions.OptLevel.NO_OPT;
            default -> throw new IllegalArgumentException("Invalid optimization level: " + optimizationLevel);
        };
    }

    public static ProgramSettings getCurrentSettings() throws IOException {
        // Grab settings.json from directory
        // If it doesn't exist, create it with default values

        ObjectMapper objectMapper = new ObjectMapper();
        ProgramSettings programSettings = objectMapper.readValue(new File("settings.json"), ProgramSettings.class);


    }

}
