package io.github.tkjonesy.frontend.settingsGUI;


import ai.onnxruntime.OrtSession;
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
import java.util.EventListener;
import java.util.HashMap;
import java.util.Set;

import static io.github.tkjonesy.frontend.App.AVAILABLE_CAMERAS;

public class SettingsWindow extends JDialog {

    private ProgramSettings settings = ProgramSettings.getCurrentSettings();
    private final HashMap<String, Object> settingsUpdates = new HashMap<>();

    private JButton confirmButton, cancelButton, applyButton;

    private JComboBox<String> cameraSelector;
    private JSpinner cameraFpsSpinner;

    private JSpinner processEveryNthFrameSpinner;
    private JSlider confThresholdSlider;
    private JCheckBox boundingBoxCheckbox;

    private JComboBox<String> modelSelector, labelSelector;

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
            ImageIcon appIcon = new ImageIcon("src/main/resources/logo32.png");
            this.setIconImage(appIcon.getImage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        /* TODO add a panel for each settings group (look at Rachel's Figma mockup for reference)
         * Display (Camera) ✅
         * Storage
         * Model
         * Advanced
         */
        // TODO When a session is running, disable the settings screen from opening. Just disable the settings button while a session is active. The comment is in here bc I don't want to make another commit to App.java

        /*----------------+
        | CAMERA SETTINGS |
        +----------------*/
        // Components
        JPanel cameraPanel = new JPanel();
        JLabel cameraSelectorLabel = new JLabel("Camera Selection");
        this.cameraSelector = new JComboBox<>();
        this.boundingBoxCheckbox = new JCheckBox("Bounding Boxes", settings.isShowBoundingBoxes());
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

        // Bounding box details
        boundingBoxCheckbox.setHorizontalTextPosition(SwingConstants.LEFT);
        boundingBoxCheckbox.setToolTipText("When this is on, the bounding boxes will be drawn in the viewing window");

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

        // Layout
        GroupLayout cameraSettingsLayout = new GroupLayout(cameraPanel);
        cameraSettingsLayout.setAutoCreateContainerGaps(true);
        cameraSettingsLayout.setHorizontalGroup(
                cameraSettingsLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(
                                cameraSettingsLayout.createSequentialGroup()
                                        .addComponent(cameraSelectorLabel)
                                        .addPreferredGap(ComponentPlacement.RELATED)
                                        .addComponent(cameraSelector, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        )
                        .addComponent(boundingBoxCheckbox)
                        .addGroup(
                                cameraSettingsLayout.createSequentialGroup()
                                        .addComponent(cameraFpsLabel)
                                        .addPreferredGap(ComponentPlacement.RELATED)
                                        .addComponent(cameraFpsSpinner, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(ComponentPlacement.RELATED)
                                        .addComponent(cameraFpsWarningLabel)
                        )
        );
        cameraSettingsLayout.setVerticalGroup(
                cameraSettingsLayout.createSequentialGroup()
                        .addGroup(
                                cameraSettingsLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(cameraSelectorLabel)
                                        .addComponent(cameraSelector)
                        )
                        .addPreferredGap(ComponentPlacement.UNRELATED)
                        .addComponent(boundingBoxCheckbox)
                        .addPreferredGap(ComponentPlacement.UNRELATED)
                        .addGroup(
                                cameraSettingsLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(cameraFpsLabel)
                                        .addComponent(cameraFpsSpinner)
                                        .addComponent(cameraFpsWarningLabel)
                        )
        );
        cameraPanel.setLayout(cameraSettingsLayout);

        /*-----------------+
        | STORAGE SETTINGS |
        +-----------------*/

        JPanel storagePanel = new JPanel();
        JLabel storageSelectorLabel = new JLabel("File Save Location");
        ButtonGroup storageSelectorGroup = new ButtonGroup();
        JRadioButtonMenuItem defaultSaveOption = new JRadioButtonMenuItem("Default");
        JRadioButtonMenuItem customSaveOption = new JRadioButtonMenuItem("Custom");
        storageSelectorGroup.add(defaultSaveOption);
        storageSelectorGroup.add(customSaveOption);
        defaultSaveOption.setSelected(true); // TODO this needs to pull from a custom setting later such that it sets the correct selection

        // TODO This needs to integrate and save the selected custom directory in the settings file. If custom storage is saved as the selection in settings, this needs to retain the last picked folder
        // Logic for folder selector
        final File[] selectedFolder = new File[1]; // This is so jank, I don't want to talk about it holy cow. This is the work-around for keeping this final to make the linter stfu but still make the value re-assignable
        JButton folderSelectorButton = new JButton("Choose Folder...");
        JLabel selectedFolderLabel = new JLabel(""); // TODO this needs to pull the custom directory from settings
        folderSelectorButton.setEnabled(!defaultSaveOption.isSelected());
        folderSelectorButton.addActionListener(e -> {
            JFileChooser folderChooser = new JFileChooser();
            folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            folderChooser.setAcceptAllFileFilterUsed(false);

            int returnVal = folderChooser.showOpenDialog(SettingsWindow.this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                selectedFolder[0] = folderChooser.getSelectedFile();
                selectedFolderLabel.setText(selectedFolder[0].getAbsolutePath());
                System.out.println("Selected Folder: " + selectedFolder[0].getAbsolutePath());
            }
        });

        // TODO consider moving this to the initListeners() function instead of keeping it in block
        // ! Keeping it in block would keep code cleaner, but removal makes it more consistent.
        //Event Listeners for buttons
        defaultSaveOption.addActionListener(
                e -> {
                    folderSelectorButton.setEnabled(false);
                    System.out.println("default saving selected");
                }
        );

        customSaveOption.addActionListener(
                e -> {
                    folderSelectorButton.setEnabled(true);
                    System.out.println("custom saving selected");
                }
        );

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
        this.processEveryNthFrameSpinner = modelPanel.getProcessEveryNthFrameSpinner();
        this.confThresholdSlider = modelPanel.getConfThresholdSlider();


        /*------------------+
        | ADVANCED SETTINGS |
        +------------------*/

        AdvancedSettingsPanel advancedPanel = new AdvancedSettingsPanel();
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

        addSettingChangeListener(boundingBoxCheckbox, (ActionListener)
                e -> {
                    boolean value = boundingBoxCheckbox.isSelected();
                    System.out.println("Bounding boxes: " + boundingBoxCheckbox.isSelected());
                    settingsUpdates.put("showBoundingBoxes", value);
                    if(settings.isShowBoundingBoxes() == value)
                        settingsUpdates.remove("showBoundingBoxes");
                }
        );

        addSettingChangeListener(modelSelector, (ActionListener)
                e -> {
                    String value = (String) modelSelector.getSelectedItem();
                    String path = SettingsLoader.getAIMS_Directory() + "/ai_models/" + value;
                    System.out.println("Model: " + modelSelector.getSelectedItem());
                    settingsUpdates.put("modelPath", path);
                    if(settings.getModelPath().equals(path))
                        settingsUpdates.remove("modelPath");
                }
        );

        addSettingChangeListener(labelSelector, (ActionListener)
                e -> {
                    String value = (String) labelSelector.getSelectedItem();
                    String path = SettingsLoader.getAIMS_Directory() + "/ai_models/" + value;
                    System.out.println("Labels: " + labelSelector.getSelectedItem());
                    settingsUpdates.put("labelPath", path);
                    if(settings.getLabelPath().equals(path))
                        settingsUpdates.remove("labelPath");
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

        addSettingChangeListener(confThresholdSlider, (ChangeListener)
                e -> {
                    float value = confThresholdSlider.getValue() / 100f;
                    System.out.println("Confidence threshold: " + value);
                    settingsUpdates.put("confThreshold", value);
                    if(settings.getConfThreshold() == value)
                        settingsUpdates.remove("confThreshold");
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
                    String value = inputShapeTextField.getText();
                    System.out.println("Input shape: " + value);
                    settingsUpdates.put("inputShape", value);
                    if(settings.getInputShape().toString().equals(value))
                        settingsUpdates.remove("inputShape");
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
