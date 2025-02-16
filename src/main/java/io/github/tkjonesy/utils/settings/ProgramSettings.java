package io.github.tkjonesy.utils.settings;

import ai.onnxruntime.OrtSession;
import io.github.tkjonesy.utils.annotations.SettingsLabel;
import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Field;
import java.util.HashMap;

@Getter
public class ProgramSettings {

    @Setter
    @Getter
    private static ProgramSettings currentSettings;

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
    @SettingsLabel(value = "gpuDeviceId", type = Integer.class)
    private int gpuDeviceId;
    @SettingsLabel(value = "nmsThreshold", type = Float.class)
    private float nmsThreshold;
    @SettingsLabel(value = "optimizationLevel", type = OrtSession.SessionOptions.OptLevel.class) // all, extended, basic, no
    private OrtSession.SessionOptions.OptLevel optimizationLevel;
    @SettingsLabel(value = "numInputElements", type = Integer.class)
    private int numInputElements;
    @SettingsLabel(value = "inputSize", type = Integer.class)
    private int inputSize;
    @SettingsLabel(value = "inputShape", type = long[].class)
    private long[] inputShape;

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

    @Override
    public String toString(){
        return "ProgramSettings{" +
                "cameraDeviceId=" + cameraDeviceId +
                ", cameraFps=" + cameraFps +
                ", fileDirectory='" + fileDirectory + '\'' +
                ", modelPath='" + modelPath + '\'' +
                ", labelPath='" + labelPath + '\'' +
                ", processEveryNthFrame=" + processEveryNthFrame +
                ", showBoundingBoxes=" + showBoundingBoxes +
                ", confThreshold=" + confThreshold +
                ", gpuDeviceId=" + gpuDeviceId +
                ", nmsThreshold=" + nmsThreshold +
                ", optimizationLevel=" + optimizationLevel +
                ", numInputElements=" + numInputElements +
                ", inputSize=" + inputSize +
                '}';
    }

}
