package io.github.tkjonesy.utils.settings;

import ai.onnxruntime.OrtSession;
import io.github.tkjonesy.utils.annotations.SettingsLabel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Field;
import java.util.HashMap;

import static io.github.tkjonesy.utils.settings.SettingsLoader.loadSettings;

@AllArgsConstructor
@Getter
public class ProgramSettings {

    private static final ProgramSettings currentSettings;
    static {currentSettings = loadSettings();}

    private static final String FILE_DIRECTORY = System.getProperty("user.home") + "/AIMs";

    // Camera variables
    @SettingsLabel(value = "cameraDeviceId", type = Integer.class)
    private int cameraDeviceId;
    @SettingsLabel(value = "cameraFps", type = Integer.class)
    private int cameraFps;

    // Storage variables
    @SettingsLabel(value = "fileDirectory", type = String.class)
    private String fileDirectory;

    // AI settings
    @Setter
    @SettingsLabel(value = "modelPath", type = String.class)
    private String modelPath;
    @Setter
    @SettingsLabel(value = "labelPath", type = String.class)
    private String labelPath;
    @SettingsLabel(value = "processEveryNthFrame", type = Integer.class)
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

    // -------------------------------------------------------------------------

    public void updateSettings(HashMap<String, Object> newSettings) {
        for (String key : newSettings.keySet()) {
            setSettings(key, newSettings.get(key));
        }
    }

    private void setSettings(String label, Object value) {
        Field[] fields = this.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(SettingsLabel.class)) {
                SettingsLabel annotation = field.getAnnotation(SettingsLabel.class);
                if (annotation.value().equals(label)) {
                    field.setAccessible(true);
                    try {
                        if (annotation.type().isInstance(value)) {
                            field.set(this, value);

                            if(label.equals("optimizationLevelString")) {
                                this.optimizationLevelEnum = getSessionOptimizations((String) value);
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

    private OrtSession.SessionOptions.OptLevel getSessionOptimizations(String optimizationLevel) {
        return switch (optimizationLevel.toLowerCase()) {
            case "all" -> OrtSession.SessionOptions.OptLevel.ALL_OPT;
            case "extended" -> OrtSession.SessionOptions.OptLevel.EXTENDED_OPT;
            case "basic" -> OrtSession.SessionOptions.OptLevel.BASIC_OPT;
            case "none" -> OrtSession.SessionOptions.OptLevel.NO_OPT;
            default -> throw new IllegalArgumentException("Invalid optimization level: " + optimizationLevel);
        };
    }


}
