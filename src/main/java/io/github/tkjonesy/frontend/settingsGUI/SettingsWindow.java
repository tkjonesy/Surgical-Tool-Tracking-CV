package io.github.tkjonesy.frontend.settingsGUI;


import ai.onnxruntime.OrtSession;
import io.github.tkjonesy.utils.Paths;
import io.github.tkjonesy.utils.settings.ProgramSettings;
import io.github.tkjonesy.utils.settings.SettingsLoader;
import lombok.Getter;

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
    @Getter
    private static final HashMap<String, Object> settingsUpdates = new HashMap<>();

    private JButton confirmButton;
    private JButton cancelButton;
    private static JButton applyButton;

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
        CameraSettingsPanel cameraPanel = new CameraSettingsPanel(settings, AVAILABLE_CAMERAS);

        /*-----------------+
        | STORAGE SETTINGS |
        +-----------------*/

        StorageSettingsPanel storagePanel = new StorageSettingsPanel();

        /*---------------+
        | MODEL SETTINGS |
        +---------------*/
        AISettingsPanel modelPanel = new AISettingsPanel();

        /*------------------+
        | ADVANCED SETTINGS |
        +------------------*/

        AdvancedSettingsPanel advancedPanel = new AdvancedSettingsPanel();
        this.useGPUCheckbox = advancedPanel.getUseGPUCheckBox();
        this.gpuDeviceSelector = advancedPanel.getGpuDeviceSelector();
        this.nmsThresholdSlider = advancedPanel.getNmsThresholdSlider();
        this.optimizationLevelComboBox = advancedPanel.getOptimizationLevelComboBox();
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
    public static void updateApplyButtonState() {
        applyButton.setEnabled(!settingsUpdates.isEmpty());
    }

    private void initListeners() {

        // CAMERA LISTENERS -----------------------------------------------

        // STORAGE LISTENERS -----------------------------------------------

        // MODEL LISTENERS -----------------------------------------------

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

        addSettingChangeListener(inputSizeSpinner, (ChangeListener)
                e -> {
                    int value = (int) inputSizeSpinner.getValue();
                    System.out.println("Input size: " + value);
                    settingsUpdates.put("inputSize", value);
                    if(settings.getInputSize() == value)
                        settingsUpdates.remove("inputSize");
                }
        );

        inputShapeTextField.addActionListener(
                e -> {
                    String newValue = inputShapeTextField.getText().trim().replaceAll(" ", "");
                    String currentValue = Arrays.toString(settings.getInputShape()).replaceAll("[\\[\\] ]", " ").trim().replaceAll(" ", "");

                    System.out.println("Current value: " + currentValue);
                    System.out.println("New value: " + newValue);

                    if (!newValue.equals(currentValue)) {

                        long[] inputShape = Arrays.stream(newValue.split(","))
                                .mapToLong(Long::parseLong)
                                .toArray();

                        long numInputElements = Arrays.stream(inputShape).reduce(1, (a, b) -> a * b);

                        settingsUpdates.put("inputShape", inputShape);
                        settingsUpdates.put("numInputElements", (int) numInputElements);

                        System.out.println("Input shape changed: " + newValue);
                        System.out.println("Number of input elements: " + numInputElements);

                        updateApplyButtonState();
                    } else {
                        settingsUpdates.remove("inputShape");
                        settingsUpdates.remove("numInputElements");

                        updateApplyButtonState();
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

    public static <T extends EventListener> void addSettingChangeListener(JComponent component, T listener) {
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
