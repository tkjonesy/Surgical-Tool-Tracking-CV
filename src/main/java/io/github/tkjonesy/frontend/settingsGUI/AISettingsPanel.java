package io.github.tkjonesy.frontend.settingsGUI;

import io.github.tkjonesy.frontend.settingsGUI.listenersANDevents.AISettingsListener;
import io.github.tkjonesy.frontend.settingsGUI.listenersANDevents.BoundingBoxColorChangeEvent;
import io.github.tkjonesy.utils.settings.ProgramSettings;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static io.github.tkjonesy.utils.Paths.AIMS_MODELS_DIRECTORY;


@Getter
public class AISettingsPanel extends JPanel {

    private final JComboBox<String> modelSelector;
    private final JComboBox<String> labelSelector;

    private int[] boundingBoxColor = new int[3];
    private final JTextField rInputTextField;
    private final JTextField gInputTextField;
    private final JTextField bInputTextField;
    private final JButton colorPreviewButton;

    private final JCheckBox boundingBoxCheckbox;
    private final JCheckBox showLabelsCheckbox;
    private final JCheckBox showConfidencesCheckbox;
    private final JSpinner processEveryNthFrameSpinner;
    private final JSpinner bufferThresholdSpinner;
    private final JSlider confThresholdSlider;
    private final JTextField confThresholdTextField;

    private final List<AISettingsListener> listeners = new ArrayList<>();

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
        modelSelector = new JComboBox<>(getFilesWithExtension(".onnx"));
        modelSelector.setSelectedItem(new File(settings.getModelPath()).getName());

        // Label Selector
        JLabel labelLabel = new JLabel("Label File:");
        labelSelector = new JComboBox<>(getFilesWithExtension(".names"));
        labelSelector.setSelectedItem(new File(settings.getLabelPath()).getName());

        // Color Chooser
        JLabel colorLabel = new JLabel("Bounding box color (RGB):");

        int r = settings.getBoundingBoxColor()[0];
        int g = settings.getBoundingBoxColor()[1];
        int b = settings.getBoundingBoxColor()[2];

        this.rInputTextField = new JTextField(String.valueOf(r), 3);
        this.gInputTextField = new JTextField(String.valueOf(g), 3);
        this.bInputTextField = new JTextField(String.valueOf(b), 3);

        rInputTextField.addActionListener(e -> updateBoundingBoxColor());
        gInputTextField.addActionListener(e -> updateBoundingBoxColor());
        bInputTextField.addActionListener(e -> updateBoundingBoxColor());

        // Small color preview button
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


        // Open Color Picker Popup
        colorPreviewButton.addActionListener(e -> openColorChooser());

        // Bounding box details
        this.boundingBoxCheckbox = new JCheckBox("Bounding Boxes:", settings.isShowBoundingBoxes());
        boundingBoxCheckbox.setHorizontalTextPosition(SwingConstants.LEFT);
        boundingBoxCheckbox.setToolTipText("When this is on, the bounding boxes will be drawn in the viewing window");

        // Show labels
        this.showLabelsCheckbox = new JCheckBox("Show Labels:", settings.isShowLabels());
        showLabelsCheckbox.setHorizontalTextPosition(SwingConstants.LEFT);
        showLabelsCheckbox.setToolTipText("When this is on, the labels will be drawn in the viewing window");

        // Show confidences
        this.showConfidencesCheckbox = new JCheckBox("Show Confidences:", settings.isShowConfidences());
        showConfidencesCheckbox.setHorizontalTextPosition(SwingConstants.LEFT);
        showConfidencesCheckbox.setToolTipText("When this is on, the confidences will be drawn in the viewing window");

        // Process Every Nth Frame (Spinner)
        JLabel processNthLabel = new JLabel("Process Every Nth Frame:");
        processEveryNthFrameSpinner = new JSpinner(new SpinnerNumberModel(settings.getProcessEveryNthFrame(), 15, 1000, 1));
        processEveryNthFrameSpinner.setToolTipText("Controls how often the AI processes frames. Higher values improve performance.");

        // Buffer threshold (Spinner)
        JLabel bufferThresholdLabel = new JLabel("Buffer Threshold:");
        this.bufferThresholdSpinner = new JSpinner(new SpinnerNumberModel(settings.getBufferThreshold(), 0, 100, 1));
        bufferThresholdSpinner.setToolTipText("Controls the number of inferences in a row before a label is considered valid.");

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
                        .addGroup(modelLayout.createSequentialGroup()
                                .addComponent(colorLabel)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(rInputTextField)
                                .addComponent(gInputTextField)
                                .addComponent(bInputTextField)
                                .addComponent(colorPreviewButton))
                        .addGroup(
                                modelLayout.createSequentialGroup()
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(boundingBoxCheckbox)
                        )
                        .addGroup(
                                modelLayout.createSequentialGroup()
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(showLabelsCheckbox)
                        )
                        .addGroup(
                                modelLayout.createSequentialGroup()
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(showConfidencesCheckbox)
                        )
                        .addGroup(
                                modelLayout.createSequentialGroup()
                                        .addComponent(processNthLabel)
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(processEveryNthFrameSpinner, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        )
                        .addGroup(
                                modelLayout.createSequentialGroup()
                                        .addComponent(bufferThresholdLabel)
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(bufferThresholdSpinner, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        )
                        .addGroup(
                                modelLayout.createSequentialGroup()
                                        .addComponent(confThresholdLabel)
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(confThresholdSlider, GroupLayout.PREFERRED_SIZE, 150, GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(confThresholdTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
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
                        .addGroup(modelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(colorLabel)
                                .addComponent(rInputTextField)
                                .addComponent(gInputTextField)
                                .addComponent(bInputTextField)
                                .addComponent(colorPreviewButton))
                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(
                                modelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(boundingBoxCheckbox)
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(
                                modelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(showLabelsCheckbox)
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(
                                modelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(showConfidencesCheckbox)
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
                                        .addComponent(bufferThresholdLabel)
                                        .addComponent(bufferThresholdSpinner)
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(
                                modelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(confThresholdLabel)
                                        .addComponent(confThresholdSlider)
                                        .addComponent(confThresholdTextField)
                        )

        );
        this.setLayout(modelLayout);

    }

    // Get files with specific extension from a directory
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


    // Open AI Models Directory in File Explorer
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

    /**
     * Registers a listener for AI settings changes.
     */
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
