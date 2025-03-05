package io.github.tkjonesy.utils.settings;

import ai.onnxruntime.OrtSession;
import io.github.tkjonesy.frontend.App;
import io.github.tkjonesy.utils.annotations.SettingsLabel;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.util.HashMap;

@SuppressWarnings("unused")
@Getter
public class ProgramSettings {
    private static final Logger logger = LogManager.getLogger(ProgramSettings.class);


    @Setter
    @Getter
    private static ProgramSettings currentSettings;

    // Camera variables
    @SettingsLabel(value = "cameraDeviceId", type = Integer.class)
    private int cameraDeviceId;
    @SettingsLabel(value = "cameraFps", type = Integer.class)
    private int cameraFps;
    @SettingsLabel(value = "cameraRotation", type = Integer.class)
    private int cameraRotation;
    @SettingsLabel(value = "mirrorCamera", type = Boolean.class)
    private boolean mirrorCamera;
    @SettingsLabel(value = "preserveAspectRatio", type = Boolean.class)
    private boolean preserveAspectRatio;

    // Storage variables
    @Setter
    @SettingsLabel(value = "fileDirectory", type = String.class)
    private String fileDirectory;
    @SettingsLabel(value = "saveVideo", type = Boolean.class)
    private boolean saveVideo;
    @SettingsLabel(value = "saveLogsTEXT", type = Boolean.class)
    private boolean saveLogsTEXT;
    @SettingsLabel(value = "saveLogsCSV", type = Boolean.class)
    private boolean saveLogsCSV;

    // AI settings
    @Setter
    @SettingsLabel(value = "modelPath", type = String.class)
    private String modelPath;
    @Setter
    @SettingsLabel(value = "labelPath", type = String.class)
    private String labelPath;
    @SettingsLabel(value = "boundingBoxColor", type = int[].class)
    private int[] boundingBoxColor;
    @SettingsLabel(value = "showBoundingBoxes", type = Boolean.class)
    private boolean showBoundingBoxes;
    @SettingsLabel(value = "showLabels", type = Boolean.class)
    private boolean showLabels;
    @SettingsLabel(value = "showConfidences", type = Boolean.class)
    private boolean showConfidences;
    @SettingsLabel(value = "processEveryNthFrame", type = Integer.class)
    private int processEveryNthFrame;
    @SettingsLabel(value = "bufferThreshold", type = Integer.class)
    private int bufferThreshold;
    @SettingsLabel(value = "confThreshold", type = Float.class)
    private float confThreshold;

    // Advanced AI settings
    @Setter
    @SettingsLabel(value = "useGPU", type = Boolean.class)
    private boolean useGPU;
    @SettingsLabel(value = "gpuDeviceId", type = Integer.class)
    private int gpuDeviceId;
    @SettingsLabel(value = "nmsThreshold", type = Float.class)
    private float nmsThreshold;
    @SettingsLabel(value = "optimizationLevel", type = OrtSession.SessionOptions.OptLevel.class) // all, extended, basic, no
    private OrtSession.SessionOptions.OptLevel optimizationLevel;
    @SettingsLabel(value = "inputSize", type = Integer.class)
    private int inputSize;
    @SettingsLabel(value = "inputShape", type = long[].class)
    private long[] inputShape;
    @SettingsLabel(value = "numInputElements", type = Integer.class)
    private int numInputElements;

    // -------------------------------------------------------------------------

    public void updateSettings(HashMap<String, Object> newSettings) {
        boolean updateONNX = false, updateCamera = false, updateBuffer = false;
        for (String key : newSettings.keySet()) {
            setSettings(key, newSettings.get(key));
            if(key.equals("modelPath") || key.equals("labelPath") || key.equals("useGPU")){
                updateONNX = true;
            }
            if(key.equals("cameraDeviceId")){
                updateCamera = true;
            }
            if(key.equals("bufferThreshold")){
                updateBuffer = true;
            }
        }
        if(updateONNX){
            App.getOnnxRunner().updateInferenceSession(modelPath, labelPath);
        }
        if(updateBuffer){
            App.getOnnxRunner().setBufferThreshold(bufferThreshold);
        }
        if(updateCamera){
            App.getInstance().updateCamera((int)newSettings.get("cameraDeviceId"));
        }

        SettingsLoader.saveSettings(this);
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
    }

    @Override
    public String toString(){
        return "ProgramSettings{" +
                "cameraDeviceId=" + cameraDeviceId +
                ", cameraFps=" + cameraFps +
                ", cameraRotation=" + cameraRotation +
                ", mirrorCamera=" + mirrorCamera +
                ", preserveAspectRatio=" + preserveAspectRatio +
                ", fileDirectory='" + fileDirectory + '\'' +
                ", saveVideo=" + saveVideo +
                ", saveLogsTEXT=" + saveLogsTEXT +
                ", saveLogsCSV=" + saveLogsCSV +
                ", modelPath='" + modelPath + '\'' +
                ", labelPath='" + labelPath + '\'' +
                ", showBoundingBoxes=" + showBoundingBoxes +
                ", processEveryNthFrame=" + processEveryNthFrame +
                ", bufferThreshold=" + bufferThreshold +
                ", confThreshold=" + confThreshold +
                ", useGPU=" + useGPU +
                ", gpuDeviceId=" + gpuDeviceId +
                ", nmsThreshold=" + nmsThreshold +
                ", optimizationLevel=" + optimizationLevel +
                '}';
    }

}
