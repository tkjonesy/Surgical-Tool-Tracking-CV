package io.github.tkjonesy.frontend.settingsGUI.panels;

import io.github.tkjonesy.frontend.settingsGUI.SettingsUI;
import io.github.tkjonesy.frontend.settingsGUI.SettingsWindow;
import io.github.tkjonesy.frontend.settingsGUI.listenersANDevents.AISettingsListener;
import io.github.tkjonesy.frontend.settingsGUI.listenersANDevents.BoundingBoxColorChangeEvent;
import io.github.tkjonesy.utils.settings.ProgramSettings;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

import static io.github.tkjonesy.frontend.settingsGUI.SettingsWindow.updateApplyButtonState;
import static io.github.tkjonesy.utils.Paths.AIMS_MODELS_DIRECTORY;
import static io.github.tkjonesy.frontend.settingsGUI.SettingsWindow.addSettingChangeListener;


public class AISettingsPanel extends JPanel implements SettingsUI {
    private static final ProgramSettings settings = ProgramSettings.getCurrentSettings();
    private static final HashMap<String, Object> settingsUpdates = SettingsWindow.getSettingsUpdates();

    private final JLabel modelLabel;
    private final JLabel labelLabel;
    private final JLabel colorLabel;
    private final JLabel processNthLabel;
    private final JLabel bufferThresholdLabel;
    private final JLabel confThresholdLabel;
    private final JLabel noticeLabel;
    private final JButton openFolderButton;

    private int[] boundingBoxColor = new int[3];
    private final JTextField rInputTextField;
    private final JTextField gInputTextField;
    private final JTextField bInputTextField;
    private final JButton colorPreviewButton;

    private final JComboBox<String> modelSelector;
    private final JComboBox<String> labelSelector;
    private final JCheckBox boundingBoxCheckbox;
    private final JCheckBox showLabelsCheckbox;
    private final JCheckBox showConfidencesCheckbox;
    private final JSpinner processEveryNthFrameSpinner;
    private final JSpinner bufferThresholdSpinner;
    private final JSlider confThresholdSlider;
    private final JTextField confThresholdTextField;

    private final List<AISettingsListener> listeners = new ArrayList<>();

    public AISettingsPanel() {
        this.noticeLabel = new JLabel("<html><b>Only YOLOv8+ models in .onnx format are supported.</b></html>");
        this.noticeLabel.setForeground(Color.GRAY);

        this.openFolderButton = new JButton("Open AI Models Folder");
        openFolderButton.addActionListener(e -> openAIDirectory());

        this.modelLabel = new JLabel("AI Model:");
        this.modelSelector = new JComboBox<>(getFilesWithExtension(".onnx"));
        modelSelector.setSelectedItem(new File(settings.getModelPath()).getName());

        this.labelLabel = new JLabel("Label File:");
        this.labelSelector = new JComboBox<>(getFilesWithExtension(".names"));
        labelSelector.setSelectedItem(new File(settings.getLabelPath()).getName());

        this.colorLabel = new JLabel("Bounding box color (RGB):");

        int r = settings.getBoundingBoxColor()[0];
        int g = settings.getBoundingBoxColor()[1];
        int b = settings.getBoundingBoxColor()[2];

        this.rInputTextField = new JTextField(String.valueOf(r), 3);
        this.gInputTextField = new JTextField(String.valueOf(g), 3);
        this.bInputTextField = new JTextField(String.valueOf(b), 3);

        rInputTextField.addActionListener(e -> updateBoundingBoxColor());
        gInputTextField.addActionListener(e -> updateBoundingBoxColor());
        bInputTextField.addActionListener(e -> updateBoundingBoxColor());

        this.colorPreviewButton = new JButton() {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(30, 30);
            }
        };
        colorPreviewButton.setBackground(new Color(r, g, b));
        colorPreviewButton.setMinimumSize(new Dimension(30, 30));
        colorPreviewButton.setMaximumSize(new Dimension(30, 30));
        colorPreviewButton.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        colorPreviewButton.addActionListener(e -> openColorChooser());

        this.boundingBoxCheckbox = new JCheckBox("Bounding Boxes:", settings.isShowBoundingBoxes());
        this.showLabelsCheckbox = new JCheckBox("Show Labels:", settings.isShowLabels());
        this.showConfidencesCheckbox = new JCheckBox("Show Confidences:", settings.isShowConfidences());

        this.processNthLabel = new JLabel("Process Every Nth Frame:");
        this.processEveryNthFrameSpinner = new JSpinner(new SpinnerNumberModel(settings.getProcessEveryNthFrame(), 15, 1000, 1));

        this.bufferThresholdLabel = new JLabel("Buffer Threshold:");
        this.bufferThresholdSpinner = new JSpinner(new SpinnerNumberModel(settings.getBufferThreshold(), 0, 100, 1));

        this.confThresholdLabel = new JLabel("Confidence Threshold:");
        this.confThresholdSlider = new JSlider(0, 100, (int) (settings.getConfThreshold() * 100));
        this.confThresholdSlider.setMajorTickSpacing(10);
        this.confThresholdSlider.setMinorTickSpacing(5);
        this.confThresholdSlider.setPaintTicks(true);
        this.confThresholdSlider.setPaintLabels(true);

        this.confThresholdTextField = new JTextField(String.format("%.2f", settings.getConfThreshold()), 4);
        this.confThresholdTextField.setHorizontalAlignment(JTextField.CENTER);

        setLayout();
        initListeners();
    }


    @Override
    public void initListeners() {
        this.addAISettingsListener(
                newColor -> {
                    System.out.println("Bounding box color changed: " + Arrays.toString(newColor));
                    settingsUpdates.put("boundingBoxColor", newColor);
                    if(Arrays.equals(settings.getBoundingBoxColor(), newColor))
                        settingsUpdates.remove("boundingBoxColor");
                    updateApplyButtonState();
                }
        );

        // Sync text field to slider (with validation)
        confThresholdTextField.addActionListener(e -> {
            try {
                float typedValue = Float.parseFloat(confThresholdTextField.getText());

                if (typedValue < 0.01f) {
                    typedValue = 0.01f;
                } else if (typedValue > 1.0f) {
                    typedValue = 1.0f;
                }

                confThresholdSlider.setValue((int) (typedValue * 100));
                confThresholdTextField.setText(String.format("%.2f", typedValue));
            } catch (NumberFormatException ex) {
                confThresholdTextField.setText(String.format("%.2f", confThresholdSlider.getValue() / 100.0));
            }
        });

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
    }

    @Override
    public void setLayout() {
        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        layout.setAutoCreateContainerGaps(true);
        layout.setAutoCreateGaps(true);

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(noticeLabel)
                        .addComponent(openFolderButton)
                        .addGroup(
                                layout.createSequentialGroup()
                                        .addComponent(modelLabel)
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(modelSelector, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        )
                        .addGroup(
                                layout.createSequentialGroup()
                                        .addComponent(labelLabel)
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(labelSelector, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        )
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(colorLabel)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(rInputTextField)
                                .addComponent(gInputTextField)
                                .addComponent(bInputTextField)
                                .addComponent(colorPreviewButton))
                        .addGroup(
                                layout.createSequentialGroup()
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(boundingBoxCheckbox)
                        )
                        .addGroup(
                                layout.createSequentialGroup()
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(showLabelsCheckbox)
                        )
                        .addGroup(
                                layout.createSequentialGroup()
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(showConfidencesCheckbox)
                        )
                        .addGroup(
                                layout.createSequentialGroup()
                                        .addComponent(processNthLabel)
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(processEveryNthFrameSpinner, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        )
                        .addGroup(
                                layout.createSequentialGroup()
                                        .addComponent(bufferThresholdLabel)
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(bufferThresholdSpinner, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        )
                        .addGroup(
                                layout.createSequentialGroup()
                                        .addComponent(confThresholdLabel)
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(confThresholdSlider, GroupLayout.PREFERRED_SIZE, 150, GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(confThresholdTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        )
        );

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addComponent(noticeLabel)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(openFolderButton)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(
                                layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(modelLabel)
                                        .addComponent(modelSelector)
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(
                                layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(labelLabel)
                                        .addComponent(labelSelector)
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(colorLabel)
                                .addComponent(rInputTextField)
                                .addComponent(gInputTextField)
                                .addComponent(bInputTextField)
                                .addComponent(colorPreviewButton))
                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(
                                layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(boundingBoxCheckbox)
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(
                                layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(showLabelsCheckbox)
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(
                                layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(showConfidencesCheckbox)
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(
                                layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(processNthLabel)
                                        .addComponent(processEveryNthFrameSpinner)
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(
                                layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(bufferThresholdLabel)
                                        .addComponent(bufferThresholdSpinner)
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(
                                layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(confThresholdLabel)
                                        .addComponent(confThresholdSlider)
                                        .addComponent(confThresholdTextField)
                        )

        );
    }

    private String[] getFilesWithExtension(String extension) {
        File dir = new File(AIMS_MODELS_DIRECTORY);
        if (!dir.exists() || !dir.isDirectory()) return new String[]{};
        List<String> files = new ArrayList<>();
        for (File file : Objects.requireNonNull(dir.listFiles())) {
            if (file.isFile() && file.getName().endsWith(extension)) {
                files.add(file.getName());
            }
        }
        return files.toArray(new String[0]);
    }

    private void openAIDirectory() {
        File dir = new File(AIMS_MODELS_DIRECTORY);
        if (!dir.exists()) {
            JOptionPane.showMessageDialog(this, "AI Models directory does not exist!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            Desktop.getDesktop().open(dir);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Failed to open AI Models directory!", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Open Color Chooser Dialog
    private void openColorChooser() {
        JColorChooser colorChooser = new JColorChooser(colorPreviewButton.getBackground());

        removeUnwantedTabs(colorChooser);

        // Create and show a custom color chooser dialog
        JDialog colorDialog = JColorChooser.createDialog(
                this,
                "Choose Bounding Box Color",
                true,
                colorChooser,
                e -> {
                    Color selectedColor = colorChooser.getColor();
                    if (selectedColor != null) {
                        colorPreviewButton.setBackground(selectedColor);
                        rInputTextField.setText(String.valueOf(selectedColor.getRed()));
                        gInputTextField.setText(String.valueOf(selectedColor.getGreen()));
                        bInputTextField.setText(String.valueOf(selectedColor.getBlue()));

                        this.boundingBoxColor = new int[]{selectedColor.getRed(), selectedColor.getGreen(), selectedColor.getBlue()};
                        fireBoundingBoxColorChangedEvent(boundingBoxColor);
                    }
                },
                null
        );

        colorDialog.setVisible(true);
    }

    // Ensure only the "Swatches" and "RGB" tabs are visible
    private void removeUnwantedTabs(JColorChooser colorChooser) {
        for (Component comp : colorChooser.getComponents()) {
            if (comp instanceof JTabbedPane tabbedPane) {
                for (int i = tabbedPane.getTabCount() - 1; i >= 0; i--) {
                    String title = tabbedPane.getTitleAt(i);
                    if (!title.equals("Swatches") && !title.equals("RGB")) {
                        tabbedPane.remove(i);
                    }
                }
            }
        }
    }

    private void updateBoundingBoxColor() {
        try {
            int r = Integer.parseInt(rInputTextField.getText());
            int g = Integer.parseInt(gInputTextField.getText());
            int b = Integer.parseInt(bInputTextField.getText());

            // Ensure values are within valid RGB range (0-255)
            r = Math.max(0, Math.min(255, r));
            g = Math.max(0, Math.min(255, g));
            b = Math.max(0, Math.min(255, b));

            // Update text fields
            rInputTextField.setText(String.valueOf(r));
            gInputTextField.setText(String.valueOf(g));
            bInputTextField.setText(String.valueOf(b));

            // Update boundingBoxColor array
            this.boundingBoxColor = new int[]{r, g, b};

            // Update color preview
            colorPreviewButton.setBackground(new Color(r, g, b));

            // Fire the event
            fireBoundingBoxColorChangedEvent(this.boundingBoxColor);

        } catch (NumberFormatException ex) {
            // Handle invalid input (non-numeric)
            System.err.println("Invalid RGB input. Must be a number between 0-255.");
        }
    }

    public void addAISettingsListener(AISettingsListener listener) {
        listeners.add(listener);
    }

    /**
     * Fires an event to all registered listeners when the bounding box color changes.
     */
    private void fireBoundingBoxColorChangedEvent(int[] newColor) {
        BoundingBoxColorChangeEvent event = new BoundingBoxColorChangeEvent(newColor);
        for (AISettingsListener listener : listeners) {
            listener.onBoundingBoxColorChanged(event.newColor());
        }
    }
}