package io.github.tkjonesy.frontend.settingsGUI.panels;

import io.github.tkjonesy.frontend.settingsGUI.SettingsUI;
import io.github.tkjonesy.frontend.settingsGUI.SettingsWindow;
import io.github.tkjonesy.utils.ErrorDialogManager;
import io.github.tkjonesy.utils.settings.ProgramSettings;
import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Hashtable;

import static io.github.tkjonesy.frontend.App.AVAILABLE_CAMERAS;
import static io.github.tkjonesy.frontend.settingsGUI.SettingsWindow.addSettingChangeListener;

public class CameraSettingsPanel extends JPanel implements SettingsUI {

    private static final ProgramSettings settings = ProgramSettings.getCurrentSettings();
    private static final HashMap<String, Object> settingsUpdates = SettingsWindow.getSettingsUpdates();


    private final JLabel cameraSelectorLabel;
    private final JLabel cameraFpsLabel;
    private final JLabel cameraRotationLabel;
    private final JLabel mirrorCameraLabel;
    private final JLabel preserveAspectRatioLabel;

    private final JComboBox<String> cameraSelector;
    private final JSpinner cameraFpsSpinner;
    private final JSlider cameraRotationSlider;
    private final JCheckBox mirrorCameraCheckbox;
    private final JCheckBox preserveAspectRatioCheckbox;
    private final JLabel cameraFpsWarningLabel;

    public CameraSettingsPanel(ProgramSettings settings, HashMap<String, Integer> availableCameras) {
        // Components
        this.cameraSelectorLabel = new JLabel("Camera Selection");
        this.cameraSelector = new JComboBox<>();
        this.cameraFpsLabel = new JLabel("Camera Frames Per Second");
        this.cameraFpsSpinner = new JSpinner(new SpinnerNumberModel(settings.getCameraFps(), 0, 60, 1));
        this.cameraFpsWarningLabel = new JLabel("");

        // Populate camera selection menu with available cameras
        int itemIndex = 0;
        for(String cameraName : availableCameras.keySet()) {
            cameraSelector.addItem(cameraName);
            if(availableCameras.get(cameraName) == settings.getCameraDeviceId()) {
                cameraSelector.setSelectedIndex(itemIndex);
            }
            itemIndex++;
        }

        // Camera Rotation
        this.cameraRotationLabel = new JLabel("Camera Rotation:");
        this.cameraRotationSlider = new JSlider(0, 270, settings.getCameraRotation());
        cameraRotationSlider.setMajorTickSpacing(90);
        cameraRotationSlider.setSnapToTicks(true);
        cameraRotationSlider.setPaintTicks(true);
        cameraRotationSlider.setPaintLabels(true);
        Hashtable<Integer, JLabel> labelTable = new Hashtable<>();
        labelTable.put(0, new JLabel("0"));
        labelTable.put(90, new JLabel("90"));
        labelTable.put(180, new JLabel("180"));
        labelTable.put(270, new JLabel("270"));
        cameraRotationSlider.setLabelTable(labelTable);

        // Mirror & Aspect Ratio
        this.mirrorCameraLabel = new JLabel("Mirror Camera");
        this.mirrorCameraCheckbox = new JCheckBox();
        this.mirrorCameraCheckbox.setSelected(settings.isMirrorCamera());

        this.preserveAspectRatioLabel = new JLabel("Preserve Aspect Ratio");
        this.preserveAspectRatioCheckbox = new JCheckBox();
        this.preserveAspectRatioCheckbox.setSelected(settings.isPreserveAspectRatio());

        setLayout();
        initListeners();
    }

    @Override
    public void initListeners() {
        // FPS Warning Label
        cameraFpsWarningLabel.setForeground(Color.RED);
        cameraFpsSpinner.addChangeListener(
                e -> {
                    if((int) cameraFpsSpinner.getValue() > 30)
                        ErrorDialogManager.displayWarningDialog("Values over 30 may not be supported by all cameras. Setting this value higher than 30 will not make the recording smoother if the camera does not have a refresh rate this high. Additionally, values over 60 may cause extreme performance issues.");
                    }
        );


        addSettingChangeListener(cameraSelector, (ActionListener)
                e -> {
                    String value = (String) cameraSelector.getSelectedItem();
                    System.out.println("Camera: " + cameraSelector.getSelectedItem());
                    settingsUpdates.put("cameraDeviceId", AVAILABLE_CAMERAS.get(value));
                    if(settings.getCameraDeviceId() == AVAILABLE_CAMERAS.get(value))
                        settingsUpdates.remove("cameraDeviceId");
                }
        );

        addSettingChangeListener(cameraFpsSpinner, (ChangeListener)
                e -> {
                    int value = (int) cameraFpsSpinner.getValue();
                    System.out.println("Camera FPS: " + cameraFpsSpinner.getValue());
                    settingsUpdates.put("cameraFps", value);
                    if(settings.getCameraFps() == value)
                        settingsUpdates.remove("cameraFps");
                }
        );

        addSettingChangeListener(cameraRotationSlider, (ChangeListener)
                e -> {
                    int value = cameraRotationSlider.getValue();
                    System.out.println("Camera rotation: " + cameraRotationSlider.getValue());
                    settingsUpdates.put("cameraRotation", value);
                    if(settings.getCameraRotation() == value)
                        settingsUpdates.remove("cameraRotation");
                }
        );

        addSettingChangeListener(mirrorCameraCheckbox, (ActionListener)
                e -> {
                    boolean value = mirrorCameraCheckbox.isSelected();
                    System.out.println("Mirror camera: " + mirrorCameraCheckbox.isSelected());
                    settingsUpdates.put("mirrorCamera", value);
                    if(settings.isMirrorCamera() == value)
                        settingsUpdates.remove("mirrorCamera");
                }
        );

        addSettingChangeListener(preserveAspectRatioCheckbox, (ActionListener)
                e -> {
                    boolean value = preserveAspectRatioCheckbox.isSelected();
                    System.out.println("Preserve aspect ratio: " + preserveAspectRatioCheckbox.isSelected());
                    settingsUpdates.put("preserveAspectRatio", value);
                    if(settings.isPreserveAspectRatio() == value)
                        settingsUpdates.remove("preserveAspectRatio");
                }
        );
    }

    @Override
    public void setLayout() {
        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        layout.setAutoCreateContainerGaps(true);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(
                                layout.createSequentialGroup()
                                        .addComponent(cameraSelectorLabel)
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(cameraSelector, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        )
                        .addGroup(
                                layout.createSequentialGroup()
                                        .addComponent(cameraFpsLabel)
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(cameraFpsSpinner, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(cameraFpsWarningLabel)
                        )
                        .addGroup(
                                layout.createSequentialGroup()
                                        .addComponent(cameraRotationLabel)
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(cameraRotationSlider, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE) // Slider
                        )
                        .addGroup(
                                layout.createSequentialGroup()
                                        .addComponent(mirrorCameraLabel)
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(mirrorCameraCheckbox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        )
                        .addGroup(
                                layout.createSequentialGroup()
                                        .addComponent(preserveAspectRatioLabel)
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(preserveAspectRatioCheckbox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        )
        );

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addGroup(
                                layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(cameraSelectorLabel)
                                        .addComponent(cameraSelector)
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(
                                layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(cameraFpsLabel)
                                        .addComponent(cameraFpsSpinner)
                                        .addComponent(cameraFpsWarningLabel)
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(
                                layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(cameraRotationLabel)
                                        .addComponent(cameraRotationSlider)
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(
                                layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(mirrorCameraLabel)
                                        .addComponent(mirrorCameraCheckbox)
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(
                                layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(preserveAspectRatioLabel)
                                        .addComponent(preserveAspectRatioCheckbox)
                        )
        );
    }
}