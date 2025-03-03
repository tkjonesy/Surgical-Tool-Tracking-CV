package io.github.tkjonesy.frontend.settingsGUI;

import io.github.tkjonesy.ONNX.Yolo;
import io.github.tkjonesy.ONNX.YoloV8;
import io.github.tkjonesy.utils.settings.ProgramSettings;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class AdvancedSettingsPanel extends JPanel {

    private final HashMap<String, Integer> gpuDevices = new HashMap<>();
    private final JCheckBox useGPUCheckBox;
    private final JComboBox<String> gpuDeviceSelector;
    private final JSlider nmsThresholdSlider;
    private final JTextField nmsThresholdTextField;
    private final JComboBox<String> optimizationLevelComboBox;
    private final JSpinner numInputElementsSpinner;
    private final JSpinner inputSizeSpinner;
    private final JTextField inputShapeTextField;

    public AdvancedSettingsPanel() {
        final ProgramSettings settings = ProgramSettings.getCurrentSettings();

        // Notice label
        JLabel noticeLabel = new JLabel("<html><b>Only modify these settings if you truly understand their impact.</b></html>");
        noticeLabel.setForeground(Color.RED);

        // Use GPU (Checkbox)
        JLabel useGPULabel = new JLabel("Use GPU:");
        useGPUCheckBox = new JCheckBox("");
        useGPUCheckBox.setSelected(settings.isUseGPU() && YoloV8.isCudaAvailable());
        useGPUCheckBox.setToolTipText("Enable GPU for inferencing.");
        if (!YoloV8.isCudaAvailable()) {
            useGPUCheckBox.setEnabled(false);
            useGPUCheckBox.setSelected(false);
            useGPUCheckBox.setToolTipText("GPU is not available.");
        }

        // GPU Device Selector (Dropdown)
        List<String> availableGPUs = detectAvailableGPUs();
        gpuDeviceSelector = new JComboBox<>(availableGPUs.toArray(new String[0]));
        gpuDeviceSelector.setSelectedIndex(settings.getGpuDeviceId());
        gpuDeviceSelector.setToolTipText("Select the GPU device to use (0 for default).");
        gpuDeviceSelector.setVisible(useGPUCheckBox.isSelected());

        useGPUCheckBox.addItemListener(e -> {
            gpuDeviceSelector.setVisible(useGPUCheckBox.isSelected());
            revalidate();
            repaint();
        });

        // NMS Threshold (Slider + TextField)
        JLabel nmsThresholdLabel = new JLabel("NMS Threshold:");
        nmsThresholdSlider = new JSlider(0, 100, (int) (settings.getNmsThreshold() * 100));
        nmsThresholdSlider.setMajorTickSpacing(10);
        nmsThresholdSlider.setMinorTickSpacing(5);
        nmsThresholdSlider.setPaintTicks(true);
        nmsThresholdSlider.setPaintLabels(true);

        nmsThresholdTextField = new JTextField(String.format("%.2f", settings.getNmsThreshold()), 4);
        nmsThresholdTextField.setHorizontalAlignment(JTextField.CENTER);
        nmsThresholdTextField.setToolTipText("Non-Maximum Suppression threshold (0.01 - 1.00)");

        // Sync Slider & TextField
        nmsThresholdSlider.addChangeListener(e ->
                nmsThresholdTextField.setText(String.format("%.2f", nmsThresholdSlider.getValue() / 100.0))
        );
        nmsThresholdTextField.addActionListener(e -> {
            try {
                float value = Float.parseFloat(nmsThresholdTextField.getText());
                if (value < 0.01f) value = 0.01f;
                else if (value > 1.0f) value = 1.0f;
                nmsThresholdSlider.setValue((int) (value * 100));
            } catch (NumberFormatException ex) {
                nmsThresholdTextField.setText(String.format("%.2f", nmsThresholdSlider.getValue() / 100.0));
            }
        });

        // Optimization Level (Dropdown)
        JLabel optimizationLabel = new JLabel("Optimization Level:");
        optimizationLevelComboBox = new JComboBox<>(new String[]{"ALL_OPT", "EXTENDED_OPT", "BASIC_OPT", "NO_OPT"});
        optimizationLevelComboBox.setSelectedItem(settings.getOptimizationLevel().name());
        optimizationLevelComboBox.setToolTipText("Choose the level of ONNX Runtime optimizations.");

        // Num Input Elements (Spinner)
        JLabel numInputElementsLabel = new JLabel("Num Input Elements:");
        numInputElementsSpinner = new JSpinner(new SpinnerNumberModel(settings.getNumInputElements(), 1, Integer.MAX_VALUE, 1));
        numInputElementsSpinner.setToolTipText("Total number of input elements.");

        // Input Size (Spinner)
        JLabel inputSizeLabel = new JLabel("Input Size:");
        inputSizeSpinner = new JSpinner(new SpinnerNumberModel(settings.getInputSize(), 1, Integer.MAX_VALUE, 1));
        inputSizeSpinner.setToolTipText("The input image size (e.g., 640 for YOLO).");

        // Input Shape (TextField)
        JLabel inputShapeLabel = new JLabel("Input Shape:");
        inputShapeTextField = new JTextField(
                Arrays.stream(settings.getInputShape()).mapToObj(String::valueOf).collect(Collectors.joining(", ")),
                20
        );
        inputShapeTextField.setToolTipText("Comma-separated input shape (e.g., 1,3,640,640)");

        // Layout using GroupLayout
        GroupLayout layout = new GroupLayout(this);
        layout.setAutoCreateContainerGaps(true);
        layout.setAutoCreateGaps(true);

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(noticeLabel)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(useGPULabel)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(useGPUCheckBox)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(gpuDeviceSelector, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(nmsThresholdLabel)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(nmsThresholdSlider, GroupLayout.PREFERRED_SIZE, 150, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(nmsThresholdTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(optimizationLabel)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(optimizationLevelComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(numInputElementsLabel)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(numInputElementsSpinner, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(inputSizeLabel)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(inputSizeSpinner, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(inputShapeLabel)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(inputShapeTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
        );

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addComponent(noticeLabel)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(useGPULabel)
                                .addComponent(useGPUCheckBox)
                                .addComponent(gpuDeviceSelector))
                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(nmsThresholdLabel)
                                .addComponent(nmsThresholdSlider)
                                .addComponent(nmsThresholdTextField))
                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(optimizationLabel)
                                .addComponent(optimizationLevelComboBox))
                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(numInputElementsLabel)
                                .addComponent(numInputElementsSpinner))
                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(inputSizeLabel)
                                .addComponent(inputSizeSpinner))
                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(inputShapeLabel)
                                .addComponent(inputShapeTextField))
        );

        this.setLayout(layout);
    }

    // Detect available GPUs using nvidia-smi
    private List<String> detectAvailableGPUs() {
        List<String> gpuList = new ArrayList<>();

        try {
            ProcessBuilder processBuilder = new ProcessBuilder("nvidia-smi", "--query-gpu=name", "--format=csv,noheader");
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            int index = 0;

            while ((line = reader.readLine()) != null) {
                gpuList.add("GPU " + index + ": " + line.trim());
                index++;
            }
            reader.close();
        } catch (IOException e) {
            System.err.println("Error detecting GPUs: " + e.getMessage());
            gpuList.add("No GPU detected");
        }

        return gpuList;
    }
}
