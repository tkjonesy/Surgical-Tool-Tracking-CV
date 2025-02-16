package io.github.tkjonesy.frontend.settingsGUI;

import io.github.tkjonesy.utils.settings.ProgramSettings;
import io.github.tkjonesy.utils.settings.SettingsLoader;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AISettingsPanel extends JPanel {

    @Getter
    private final JComboBox<String> modelSelector;
    @Getter
    private final JComboBox<String> labelSelector;
    private final String aiModelsDirectory = SettingsLoader.getAIMS_Directory() + "/ai_models";

    @Getter
    private final JSpinner processEveryNthFrameSpinner;
    @Getter
    private final JSlider confThresholdSlider;
    private final JTextField confThresholdTextField;

    public AISettingsPanel() {
        final ProgramSettings settings = ProgramSettings.getCurrentSettings();

        // Notice label
        JLabel noticeLabel = new JLabel("<html><b>Only YOLOv8+ models in .onnx format are supported.</b></html>");
        noticeLabel.setForeground(Color.GRAY);

        // Open Directory Button
        JButton openFolderButton = new JButton("Open AI Models Folder");
        openFolderButton.addActionListener(e -> openAIDirectory());

        // Model selector
        JLabel modelLabel = new JLabel("AI Model:");
        modelSelector = new JComboBox<>(getFilesWithExtension(aiModelsDirectory, ".onnx"));
        modelSelector.setSelectedItem(new File(settings.getModelPath()).getName());

        // Label Selector
        JLabel labelLabel = new JLabel("Label File:");
        labelSelector = new JComboBox<>(getFilesWithExtension(aiModelsDirectory, ".names"));
        labelSelector.setSelectedItem(new File(settings.getLabelPath()).getName());

        // Process Every Nth Frame (Spinner)
        JLabel processNthLabel = new JLabel("Process Every Nth Frame:");
        processEveryNthFrameSpinner = new JSpinner(new SpinnerNumberModel(settings.getProcessEveryNthFrame(), 1, 60, 1));
        processEveryNthFrameSpinner.setToolTipText("Controls how often the AI processes frames. Higher values improve performance.");

        // Confidence Threshold (Slider + Editable TextField)
        JLabel confThresholdLabel = new JLabel("Confidence Threshold:");

        this.confThresholdSlider = new JSlider(0, 100, (int) (settings.getConfThreshold() * 100));
        confThresholdSlider.setMajorTickSpacing(10);
        confThresholdSlider.setMinorTickSpacing(5);
        confThresholdSlider.setPaintTicks(true);
        confThresholdSlider.setPaintLabels(true);

        // Editable text field for threshold value
        this.confThresholdTextField = new JTextField(String.format("%.2f", settings.getConfThreshold()), 4);
        confThresholdTextField.setHorizontalAlignment(JTextField.CENTER);
        confThresholdTextField.setToolTipText("Enter a value between 0.01 and 1.00");

        // Sync slider to text field
        confThresholdSlider.addChangeListener(e -> {
            double value = confThresholdSlider.getValue() / 100.0;
            confThresholdTextField.setText(String.format("%.2f", value));
        });

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


        // Layout using GroupLayout
        GroupLayout modelLayout = new GroupLayout(this);
        modelLayout.setAutoCreateContainerGaps(true);
        modelLayout.setAutoCreateGaps(true);

        modelLayout.setHorizontalGroup(
                modelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(noticeLabel)
                        .addComponent(openFolderButton)
                        .addGroup(
                                modelLayout.createSequentialGroup()
                                        .addComponent(modelLabel)
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(modelSelector, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        )
                        .addGroup(
                                modelLayout.createSequentialGroup()
                                        .addComponent(labelLabel)
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(labelSelector, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        )
                        .addGroup(
                                modelLayout.createSequentialGroup()
                                        .addComponent(processNthLabel)
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(processEveryNthFrameSpinner, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        )
                        .addGroup(
                                modelLayout.createSequentialGroup()
                                        .addComponent(confThresholdLabel)
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(confThresholdSlider, GroupLayout.PREFERRED_SIZE, 150, GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(confThresholdTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE) // Updated to use TextField instead of Label
                        )
        );

        modelLayout.setVerticalGroup(
                modelLayout.createSequentialGroup()
                        .addComponent(noticeLabel)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(openFolderButton)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(
                                modelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(modelLabel)
                                        .addComponent(modelSelector)
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(
                                modelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(labelLabel)
                                        .addComponent(labelSelector)
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(
                                modelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(processNthLabel)
                                        .addComponent(processEveryNthFrameSpinner)
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(
                                modelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(confThresholdLabel)
                                        .addComponent(confThresholdSlider)
                                        .addComponent(confThresholdTextField) // Updated for proper alignment
                        )
        );
        this.setLayout(modelLayout);

    }

    // Get files with specific extension from a directory
    private String[] getFilesWithExtension(String directory, String extension) {
        File dir = new File(directory);
        if (!dir.exists() || !dir.isDirectory()) return new String[]{};

        List<String> files = new ArrayList<>();
        for (File file : Objects.requireNonNull(dir.listFiles())) {
            if (file.isFile() && file.getName().endsWith(extension)) {
                files.add(file.getName());
            }
        }
        return files.toArray(new String[0]);
    }

    // Open AI Models Directory in File Explorer
    private void openAIDirectory() {
        File dir = new File(aiModelsDirectory);
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
}
