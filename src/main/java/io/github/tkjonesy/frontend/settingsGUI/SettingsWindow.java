package io.github.tkjonesy.frontend.settingsGUI;


import ai.onnxruntime.OrtSession;
import io.github.tkjonesy.utils.Paths;
import io.github.tkjonesy.utils.settings.ProgramSettings;
import io.github.tkjonesy.utils.settings.SettingsLoader;

import javax.swing.*;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.event.ChangeListener;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.*;

import static io.github.tkjonesy.utils.Paths.AIMS_MODELS_DIRECTORY;

import static io.github.tkjonesy.frontend.App.AVAILABLE_CAMERAS;

public class SettingsWindow extends JDialog {

    private ProgramSettings settings = ProgramSettings.getCurrentSettings();
    private final HashMap<String, Object> settingsUpdates = new HashMap<>();

    private JButton confirmButton, cancelButton, applyButton;

    private JComboBox<String> cameraSelector;
    private JSpinner cameraFpsSpinner;
    private JSlider cameraRotationSlider;
    private JCheckBox mirrorCameraCheckbox;
    private JCheckBox preserveAspectRatioCheckbox;

    private JButton folderSelectorButton;
    private JLabel selectedFolderLabel;
    private File[] selectedFolder;
    private JRadioButtonMenuItem defaultSaveOption;
    private JRadioButtonMenuItem customSaveOption;
    private JCheckBox saveVideoCheckbox;
    private JCheckBox saveLogsTextCheckbox;
    private JCheckBox saveLogsCSVCheckbox;

    private JSpinner processEveryNthFrameSpinner;
    private JSpinner bufferThresholdSpinner;
    private JSlider confThresholdSlider;
    private JTextField rInputTextField;
    private JTextField gInputTextField;
    private JTextField bInputTextField;
    private int[] boundingBoxColor;
    private JCheckBox boundingBoxCheckbox;
    private JCheckBox showLabelsCheckbox;
    private JCheckBox showConfidencesCheckbox;

    private JComboBox<String> modelSelector, labelSelector;

    private JCheckBox useGPUCheckbox;
    private JComboBox<String> gpuDeviceSelector;
    private JSlider nmsThresholdSlider;
    private JComboBox<String> optimizationLevelComboBox;
    private JSpinner numInputElementsSpinner;
    private JSpinner inputSizeSpinner;
    private JTextField inputShapeTextField;

    private static final Color OCEAN = new Color(55, 90, 129);

    public SettingsWindow(JFrame parent) {
        super(parent, "AIM Settings", true);
        initComponents();
        initListeners();
        this.setVisible(true);
    }

    private void initComponents() {

        // Sizing, and exit actions
        this.setMinimumSize(new Dimension(640, 480));
        this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        // Icon
        try {
            ImageIcon appIcon = new ImageIcon(Paths.LOGO16_PATH);
            this.setIconImage(appIcon.getImage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        /*----------------+
        | CAMERA SETTINGS |
        +----------------*/
        // Components
        JPanel cameraPanel = new JPanel();
        JLabel cameraSelectorLabel = new JLabel("Camera Selection");
        this.cameraSelector = new JComboBox<>();
        JLabel cameraFpsLabel = new JLabel("Camera Frames Per Second");
        this.cameraFpsSpinner = new JSpinner(new SpinnerNumberModel(settings.getCameraFps(), 0, 60, 1));
        JLabel cameraFpsWarningLabel = new JLabel("");

        // Populate camera selection menu with list of available cameras.
        Set<String> cameraNames = AVAILABLE_CAMERAS.keySet();
        int itemIndex = 0;
        for(String cameraName: cameraNames) {
            cameraSelector.addItem(cameraName);
            // Automatically set the selected camera to whatever camera is selected in the settings file.
            if(AVAILABLE_CAMERAS.get(cameraName) == settings.getCameraDeviceId())
                cameraSelector.setSelectedIndex(itemIndex);
            itemIndex++;
        }

        // Camera FPS details
        cameraFpsSpinner.setToolTipText("<html><body style='width:200px'>Set the frame rate—the number of times per second the camera image updates—for the selected camera. Higher values are smoother, but may reduce performance. Default is 30.</body></html>");
        cameraFpsLabel.setToolTipText("<html><body style='width:200px'>Set the frame rate—the number of times per second the camera image updates—for the selected camera. Higher values are smoother, but may reduce performance. Default is 30.</body></html>");
        cameraFpsWarningLabel.setForeground(Color.RED);
        cameraFpsSpinner.addChangeListener(
                e -> {
                    if((int) cameraFpsSpinner.getValue() <= 30)
                        cameraFpsWarningLabel.setText("");
                    else {
                        cameraFpsWarningLabel.setText("<html><body style='width:200px'><b>NOTE: Values over 30 may not be supported by all cameras. Setting this value higher than 30 will not make the recording smoother if the camera does not have a refresh rate this high. Additionally, values over 60 may cause extreme performance issues.</b></body></html>");
                    }
                }
        );

        // Camera Rotation (Slider)
        JLabel cameraRotationLabel = new JLabel("Camera Rotation:");
        this.cameraRotationSlider = new JSlider(0, 270, settings.getCameraRotation());
        cameraRotationSlider.setMajorTickSpacing(90);  // Only allow 0, 90, 180, 270
        cameraRotationSlider.setSnapToTicks(true);
        cameraRotationSlider.setPaintTicks(true);
        cameraRotationSlider.setPaintLabels(true);

        // Define custom labels for the tick positions
        Hashtable<Integer, JLabel> labelTable = new Hashtable<>();
        labelTable.put(0, new JLabel("0"));
        labelTable.put(90, new JLabel("90"));
        labelTable.put(180, new JLabel("180"));
        labelTable.put(270, new JLabel("270"));
        cameraRotationSlider.setLabelTable(labelTable);

        // Mirror camera checkbox
        JLabel mirrorCameraLabel = new JLabel("Mirror Camera");
        this.mirrorCameraCheckbox = new JCheckBox();
        this.mirrorCameraCheckbox.setSelected(settings.isMirrorCamera());

        // Preserve aspect ratio checkbox
        JLabel preserveAspectRatioLabel = new JLabel("Preserve Aspect Ratio");
        this.preserveAspectRatioCheckbox = new JCheckBox();
        this.preserveAspectRatioCheckbox.setSelected(settings.isPreserveAspectRatio());

        // Layout
        GroupLayout cameraSettingsLayout = new GroupLayout(cameraPanel);
        cameraSettingsLayout.setAutoCreateContainerGaps(true);

        cameraSettingsLayout.setHorizontalGroup(
                cameraSettingsLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(
                                cameraSettingsLayout.createSequentialGroup()
                                        .addComponent(cameraSelectorLabel)
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(cameraSelector, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        )
                        .addGroup(
                                cameraSettingsLayout.createSequentialGroup()
                                        .addComponent(cameraFpsLabel)
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(cameraFpsSpinner, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(cameraFpsWarningLabel)
                        )
                        .addGroup(
                                cameraSettingsLayout.createSequentialGroup()
                                        .addComponent(cameraRotationLabel)
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(cameraRotationSlider, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE) // Slider
                        )
                        .addGroup(
                                cameraSettingsLayout.createSequentialGroup()
                                        .addComponent(mirrorCameraLabel)
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(mirrorCameraCheckbox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        )
                        .addGroup(
                                cameraSettingsLayout.createSequentialGroup()
                                        .addComponent(preserveAspectRatioLabel)
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(preserveAspectRatioCheckbox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        )
        );

        cameraSettingsLayout.setVerticalGroup(
                cameraSettingsLayout.createSequentialGroup()
                        .addGroup(
                                cameraSettingsLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(cameraSelectorLabel)
                                        .addComponent(cameraSelector)
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(
                                cameraSettingsLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(cameraFpsLabel)
                                        .addComponent(cameraFpsSpinner)
                                        .addComponent(cameraFpsWarningLabel)
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(
                                cameraSettingsLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(cameraRotationLabel)
                                        .addComponent(cameraRotationSlider)
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(
                                cameraSettingsLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(mirrorCameraLabel)
                                        .addComponent(mirrorCameraCheckbox)
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(
                                cameraSettingsLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(preserveAspectRatioLabel)
                                        .addComponent(preserveAspectRatioCheckbox)
                        )
        );
        cameraPanel.setLayout(cameraSettingsLayout);

        /*-----------------+
        | STORAGE SETTINGS |
        +-----------------*/

        JPanel storagePanel = new JPanel();
        JLabel storageSelectorLabel = new JLabel("File Save Location");
        ButtonGroup storageSelectorGroup = new ButtonGroup();
        this.defaultSaveOption = new JRadioButtonMenuItem("Default");
        defaultSaveOption.setToolTipText("Save to default location: " + Paths.DEFAULT_AIMS_SESSIONS_DIRECTORY);
        this.customSaveOption = new JRadioButtonMenuItem("Custom");
        customSaveOption.setToolTipText("Save to custom location");
        storageSelectorGroup.add(defaultSaveOption);
        storageSelectorGroup.add(customSaveOption);
        String settingsFileDirectory = settings.getFileDirectory();

        if(settingsFileDirectory==null)
            settingsFileDirectory = Paths.DEFAULT_AIMS_SESSIONS_DIRECTORY;

        if(settingsFileDirectory.equals(Paths.DEFAULT_AIMS_SESSIONS_DIRECTORY))
            defaultSaveOption.setSelected(true);
        else
            customSaveOption.setSelected(true);

        // TODO: Do not let the directory enter the ROOT directory
        // Logic for folder selector
        selectedFolder = new File[1]; // This is so jank, I don't want to talk about it holy cow. This is the work-around for keeping this final to make the linter stfu but still make the value re-assignable
        this.folderSelectorButton = new JButton("Choose Folder...");
        this.folderSelectorButton.setToolTipText("Select a folder to save files to");
        this.selectedFolderLabel = new JLabel(settingsFileDirectory);

        folderSelectorButton.setEnabled(!defaultSaveOption.isSelected());

        // Record video checkbox
        JLabel saveVideoLabel = new JLabel("Record video");
        this.saveVideoCheckbox = new JCheckBox();
        this.saveVideoCheckbox.setSelected(settings.isSaveVideo());

        // Save logs to text checkbox
        JLabel saveLogsTextLabel = new JLabel("Save logs to text");
        this.saveLogsTextCheckbox = new JCheckBox();
        this.saveLogsTextCheckbox.setSelected(settings.isSaveLogsTEXT());

        // Save logs to csv checkbox
        JLabel saveLogsCSVLabel = new JLabel("Save logs to csv");
        this.saveLogsCSVCheckbox = new JCheckBox();
        this.saveLogsCSVCheckbox.setSelected(settings.isSaveLogsCSV());

        GroupLayout storageLayout = new GroupLayout(storagePanel);
        storageLayout.setAutoCreateContainerGaps(true);
        storageLayout.setHorizontalGroup(
                storageLayout.createSequentialGroup()
                        .addGroup(
                                storageLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(storageSelectorLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(defaultSaveOption, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(customSaveOption, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addGroup(
                                                storageLayout.createSequentialGroup()
                                                        .addComponent(folderSelectorButton, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(selectedFolderLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        )
                        )
        );
        storageLayout.setVerticalGroup(
                storageLayout.createSequentialGroup()
                        .addComponent(storageSelectorLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(defaultSaveOption, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(customSaveOption, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addGroup(
                                storageLayout.createParallelGroup()
                                        .addComponent(folderSelectorButton, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(selectedFolderLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        )
        );
        storagePanel.setLayout(storageLayout);

        /*---------------+
        | MODEL SETTINGS |
        +---------------*/

        AISettingsPanel modelPanel = new AISettingsPanel();
        this.modelSelector = modelPanel.getModelSelector();
        this.labelSelector = modelPanel.getLabelSelector();
        this.rInputTextField = modelPanel.getRInputTextField();
        this.gInputTextField = modelPanel.getGInputTextField();
        this.bInputTextField = modelPanel.getBInputTextField();
        this.boundingBoxColor = settings.getBoundingBoxColor();
        this.boundingBoxCheckbox = modelPanel.getBoundingBoxCheckbox();
        this.showLabelsCheckbox = modelPanel.getShowLabelsCheckbox();
        this.showConfidencesCheckbox = modelPanel.getShowConfidencesCheckbox();
        this.bufferThresholdSpinner = modelPanel.getBufferThresholdSpinner();
        this.processEveryNthFrameSpinner = modelPanel.getProcessEveryNthFrameSpinner();
        this.confThresholdSlider = modelPanel.getConfThresholdSlider();

        modelPanel.addAISettingsListener(
                newColor -> {
                    System.out.println("Bounding box color changed: " + Arrays.toString(newColor));
                    settingsUpdates.put("boundingBoxColor", newColor);
                    if(Arrays.equals(settings.getBoundingBoxColor(), newColor))
                        settingsUpdates.remove("boundingBoxColor");
                    updateApplyButtonState();
                }
        );


        /*------------------+
        | ADVANCED SETTINGS |
        +------------------*/

        AdvancedSettingsPanel advancedPanel = new AdvancedSettingsPanel();
        this.useGPUCheckbox = advancedPanel.getUseGPUCheckBox();
        this.gpuDeviceSelector = advancedPanel.getGpuDeviceSelector();
        this.nmsThresholdSlider = advancedPanel.getNmsThresholdSlider();
        this.optimizationLevelComboBox = advancedPanel.getOptimizationLevelComboBox();
        this.numInputElementsSpinner = advancedPanel.getNumInputElementsSpinner();
        this.inputSizeSpinner = advancedPanel.getInputSizeSpinner();
        this.inputShapeTextField = advancedPanel.getInputShapeTextField();

        /*--------------+
        | BUTTON LAYOUT |
        +--------------*/
        // Components
        JPanel buttonPanel = new JPanel();
        confirmButton = new JButton("OK");
        confirmButton.setBackground(OCEAN);
        cancelButton = new JButton("Cancel");
        applyButton = new JButton("Apply");
        applyButton.setEnabled(false);


        // Layout
        GroupLayout buttonPanelLayout = new GroupLayout(buttonPanel);
        buttonPanelLayout.setAutoCreateContainerGaps(true);
        buttonPanelLayout.setHorizontalGroup(
                buttonPanelLayout.createSequentialGroup()
                        .addPreferredGap(ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(confirmButton)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(cancelButton)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(applyButton)
        );
        buttonPanelLayout.setVerticalGroup(
                buttonPanelLayout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                        .addComponent(confirmButton)
                        .addComponent(cancelButton)
                        .addComponent(applyButton)
        );
        buttonPanel.setLayout(buttonPanelLayout);

        /*----------------------+
        | SETTINGS MENU SIDEBAR |
        +----------------------*/
        JTabbedPane settingSelector = new JTabbedPane(SwingConstants.LEFT);
        settingSelector.addTab("Camera", cameraPanel);
        settingSelector.addTab("Storage", storagePanel);
        settingSelector.addTab("AI Model", modelPanel);
        settingSelector.addTab("Advanced", advancedPanel);

        /*--------------+
        | WINDOW LAYOUT |
        +--------------*/
        GroupLayout layout = new GroupLayout(this.getContentPane());
        layout.setAutoCreateContainerGaps(true);
        layout.setHorizontalGroup(
                layout.createSequentialGroup()
                        .addGroup(
                                layout.createParallelGroup()
                                        .addComponent(settingSelector)
                                        .addComponent(buttonPanel)
                        )
        );
        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addComponent(settingSelector)
                        .addComponent(buttonPanel)
        );
        this.setLayout(layout);
        this.pack();
        this.setLocationRelativeTo(null); // Center application
    }

    // Method to enable/disable the Apply button
    private void updateApplyButtonState() {
        applyButton.setEnabled(!settingsUpdates.isEmpty());
    }

    private void initListeners() {

        // CAMERA LISTENERS -----------------------------------------------
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

        // STORAGE LISTENERS -----------------------------------------------
        addSettingChangeListener(customSaveOption, (ActionListener)
                e -> {
                    folderSelectorButton.setEnabled(true);
                    settingsUpdates.put("fileDirectory", selectedFolderLabel.getText());
                    System.out.println("File directory: " + selectedFolderLabel.getText());
                    if(settings.getFileDirectory().equals(selectedFolderLabel.getText()))
                        settingsUpdates.remove("fileDirectory");
                }
        );

        addSettingChangeListener(defaultSaveOption, (ActionListener)
                e -> {
                    folderSelectorButton.setEnabled(false);
                    selectedFolderLabel.setText(Paths.DEFAULT_AIMS_SESSIONS_DIRECTORY);
                    settingsUpdates.put("fileDirectory", Paths.DEFAULT_AIMS_SESSIONS_DIRECTORY);
                    if(settings.getFileDirectory().equals(Paths.DEFAULT_AIMS_SESSIONS_DIRECTORY))
                        settingsUpdates.remove("fileDirectory");
                }
        );

        addSettingChangeListener(folderSelectorButton, (ActionListener)
                e -> {
                    JFileChooser folderChooser = new JFileChooser();
                    folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                    folderChooser.setAcceptAllFileFilterUsed(false);

                    int returnVal = folderChooser.showOpenDialog(SettingsWindow.this);
                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        selectedFolder[0] = folderChooser.getSelectedFile();
                        selectedFolderLabel.setText(selectedFolder[0].getAbsolutePath());
                        System.out.println("Selected Folder: " + selectedFolder[0].getAbsolutePath());
                        settingsUpdates.put("fileDirectory", selectedFolder[0].getAbsolutePath());
                    }
                }
        );

        addSettingChangeListener(saveVideoCheckbox, (ActionListener)
                e -> {
                    boolean value = saveVideoCheckbox.isSelected();
                    System.out.println("Save video: " + saveVideoCheckbox.isSelected());
                    settingsUpdates.put("saveVideo", value);
                    if(settings.isSaveVideo() == value)
                        settingsUpdates.remove("saveVideo");
                }
        );
        addSettingChangeListener(saveLogsTextCheckbox, (ActionListener)
                e -> {
                    boolean value = saveLogsTextCheckbox.isSelected();
                    System.out.println("Save logs to text: " + saveLogsTextCheckbox.isSelected());
                    settingsUpdates.put("saveLogsTEXT", value);
                    if(settings.isSaveLogsTEXT() == value)
                        settingsUpdates.remove("saveLogsTEXT");
                }
        );
        addSettingChangeListener(saveLogsCSVCheckbox, (ActionListener)
                e -> {
                    boolean value = saveLogsCSVCheckbox.isSelected();
                    System.out.println("Save logs to csv: " + saveLogsCSVCheckbox.isSelected());
                    settingsUpdates.put("saveLogsCSV", value);
                    if(settings.isSaveLogsCSV() == value)
                        settingsUpdates.remove("saveLogsCSV");
                }
        );

        // MODEL LISTENERS -----------------------------------------------
        addSettingChangeListener(modelSelector, (ActionListener)
                e -> {
                    String value = (String) modelSelector.getSelectedItem();
                    String path = AIMS_MODELS_DIRECTORY + "/" + value;
                    System.out.println("Model: " + modelSelector.getSelectedItem());
                    settingsUpdates.put("modelPath", path);
                    if(settings.getModelPath().equals(path))
                        settingsUpdates.remove("modelPath");
                }
        );

        addSettingChangeListener(labelSelector, (ActionListener)
                e -> {
                    String value = (String) labelSelector.getSelectedItem();
                    String path = AIMS_MODELS_DIRECTORY + "/" + value;
                    System.out.println("Labels: " + labelSelector.getSelectedItem());
                    settingsUpdates.put("labelPath", path);
                    if(settings.getLabelPath().equals(path))
                        settingsUpdates.remove("labelPath");
                }
        );

        addSettingChangeListener(boundingBoxCheckbox, (ActionListener)
                e -> {
                    boolean value = boundingBoxCheckbox.isSelected();
                    System.out.println("Bounding boxes: " + boundingBoxCheckbox.isSelected());
                    settingsUpdates.put("showBoundingBoxes", value);
                    if(settings.isShowBoundingBoxes() == value)
                        settingsUpdates.remove("showBoundingBoxes");
                }
        );

        addSettingChangeListener(showLabelsCheckbox, (ActionListener)
                e -> {
                    boolean value = showLabelsCheckbox.isSelected();
                    System.out.println("Show labels: " + showLabelsCheckbox.isSelected());
                    settingsUpdates.put("showLabels", value);
                    if(settings.isShowLabels() == value)
                        settingsUpdates.remove("showLabels");
                }
        );

        addSettingChangeListener(showConfidencesCheckbox, (ActionListener)
                e -> {
                    boolean value = showConfidencesCheckbox.isSelected();
                    System.out.println("Show confidences: " + showConfidencesCheckbox.isSelected());
                    settingsUpdates.put("showConfidences", value);
                    if(settings.isShowConfidences() == value)
                        settingsUpdates.remove("showConfidences");
                }
        );

        addSettingChangeListener(processEveryNthFrameSpinner, (ChangeListener)
                e -> {
                    int value = (int) processEveryNthFrameSpinner.getValue();
                    System.out.println("Process every nth frame: " + processEveryNthFrameSpinner.getValue());
                    settingsUpdates.put("processEveryNthFrame", value);
                    if(settings.getProcessEveryNthFrame() == value)
                        settingsUpdates.remove("processEveryNthFrame");
                }
        );

        addSettingChangeListener(bufferThresholdSpinner, (ChangeListener)
                e -> {
                    int value = (int) bufferThresholdSpinner.getValue();
                    System.out.println("Buffer threshold: " + bufferThresholdSpinner.getValue());
                    settingsUpdates.put("bufferThreshold", value);
                    if(settings.getBufferThreshold() == value)
                        settingsUpdates.remove("bufferThreshold");
                }
        );

        addSettingChangeListener(confThresholdSlider, (ChangeListener)
                e -> {
                    float value = confThresholdSlider.getValue() / 100f;
                    System.out.println("Confidence threshold: " + value);
                    settingsUpdates.put("confThreshold", value);
                    if(settings.getConfThreshold() == value)
                        settingsUpdates.remove("confThreshold");
                }
        );

        // ADVANCED LISTENERS -----------------------------------------------
        addSettingChangeListener(useGPUCheckbox, (ActionListener)
                e -> {
                    boolean value = useGPUCheckbox.isSelected();
                    System.out.println("Use GPU: " + useGPUCheckbox.isSelected());
                    settingsUpdates.put("useGPU", value);
                    if(settings.isUseGPU() == value)
                        settingsUpdates.remove("useGPU");
                }
        );


        addSettingChangeListener(gpuDeviceSelector, (ActionListener)
                e -> {
                    String value = (String) gpuDeviceSelector.getSelectedItem();
                    System.out.println("GPU device: " + value);
                    assert value != null;
                    settingsUpdates.put("gpuDeviceId", Integer.parseInt(value));
                    if(settings.getGpuDeviceId() == Integer.parseInt(value))
                        settingsUpdates.remove("gpuDeviceId");
                }
        );

        addSettingChangeListener(nmsThresholdSlider, (ChangeListener)
                e -> {
                    float value = nmsThresholdSlider.getValue() / 100f;
                    System.out.println("NMS threshold: " + value);
                    settingsUpdates.put("nmsThreshold", value);
                    if(settings.getNmsThreshold() == value)
                        settingsUpdates.remove("nmsThreshold");
                }
        );

        addSettingChangeListener(optimizationLevelComboBox, (ActionListener)
                e -> {
                    String value = (String) optimizationLevelComboBox.getSelectedItem();
                    System.out.println("Optimization level: " + value);
                    settingsUpdates.put("optimizationLevel", OrtSession.SessionOptions.OptLevel.valueOf(value));
                    if(settings.getOptimizationLevel().toString().equals(value))
                        settingsUpdates.remove("optimizationLevel");
                }
        );

        addSettingChangeListener(numInputElementsSpinner, (ChangeListener)
                e -> {
                    int value = (int) numInputElementsSpinner.getValue();
                    System.out.println("Num input elements: " + value);
                    settingsUpdates.put("numInputElements", value);
                    if(settings.getNumInputElements() == value)
                        settingsUpdates.remove("numInputElements");
                }
        );

        addSettingChangeListener(inputSizeSpinner, (ChangeListener)
                e -> {
                    int value = (int) inputSizeSpinner.getValue();
                    System.out.println("Input size: " + value);
                    settingsUpdates.put("inputSize", value);
                    if(settings.getInputSize() == value)
                        settingsUpdates.remove("inputSize");
                }
        );

        addSettingChangeListener(inputShapeTextField, (PropertyChangeListener)
                e -> {
                    String newValue = inputShapeTextField.getText().trim();
                    String currentValue = Arrays.toString(settings.getInputShape()).replaceAll("[\\[\\] ]", " ").trim();

                    if (!newValue.equals(currentValue)) {
                        System.out.println("Input shape changed: " + newValue);
                        settingsUpdates.put("inputShape", newValue);
                    } else {
                        settingsUpdates.remove("inputShape");
                    }
                }
        );


        confirmButton.addActionListener(e -> {handleCloseAttempt();});

        cancelButton.addActionListener(e -> {handleCancelAttempt();});

        applyButton.addActionListener(e -> {applyChanges();});

        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleCloseAttempt();
            }
        });
    }

    private <T extends EventListener> void addSettingChangeListener(JComponent component, T listener) {
        if (component instanceof AbstractButton button && listener instanceof ActionListener actionListener) {
            button.addActionListener(e -> {
                actionListener.actionPerformed(e);
                updateApplyButtonState();
            });
        } else if (component instanceof JCheckBox checkBox && listener instanceof ItemListener itemListener) {
            checkBox.addItemListener(e -> {
                itemListener.itemStateChanged(e);
                updateApplyButtonState();
            });
        } else if (component instanceof JTextField textField && listener instanceof PropertyChangeListener propertyChangeListener) {
            textField.addPropertyChangeListener(evt -> {
                propertyChangeListener.propertyChange(evt);
                updateApplyButtonState();
            });
        } else if (component instanceof JComboBox<?> comboBox && listener instanceof ActionListener actionListener) {
            comboBox.addActionListener(e -> {
                actionListener.actionPerformed(e);
                updateApplyButtonState();
            });
        } else if (component instanceof JSlider slider && listener instanceof ChangeListener changeListener) {
            slider.addChangeListener(e -> {
                changeListener.stateChanged(e);
                updateApplyButtonState();
            });
        } else if (component instanceof JSpinner spinner && listener instanceof ChangeListener changeListener) {
            spinner.addChangeListener(e -> {
                changeListener.stateChanged(e);
                updateApplyButtonState();
            });
        } else {
            throw new IllegalArgumentException("Unsupported listener type for component: " + component.getClass().getName());
        }
    }

    // Show popup when closing window or confirm button with unsaved changes
    private void handleCloseAttempt() {
        if (!settingsUpdates.isEmpty()) {
            int choice = JOptionPane.showConfirmDialog(
                    this,
                    "You have unsaved changes. Do you want to save before exiting?",
                    "Unsaved Changes",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );

            if (choice == JOptionPane.YES_OPTION) {
                applyChanges();
                dispose();
            } else if (choice == JOptionPane.NO_OPTION) {
                settingsUpdates.clear();
                dispose();
            }

        } else {
            dispose();
        }
    }

    // Show popup when pressing cancel button with unsaved changes
    private void handleCancelAttempt() {
        if (!settingsUpdates.isEmpty()) {
            int choice = JOptionPane.showConfirmDialog(
                    this,
                    "Discard unsaved changes?",
                    "Cancel Changes",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );

            if (choice == JOptionPane.YES_OPTION) {
                cancelChanges();
                dispose();
            }
        } else {
            dispose();
        }
    }

    private void applyChanges() {
        System.out.println("Applying changed settings");
        settings.updateSettings(settingsUpdates);
        settingsUpdates.clear();
        updateApplyButtonState();
    }
    private void cancelChanges() {
        settingsUpdates.clear();
    }
}
