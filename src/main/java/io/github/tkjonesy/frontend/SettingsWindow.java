package io.github.tkjonesy.frontend;

import io.github.tkjonesy.ONNX.settings.Settings;

import javax.swing.*;
import javax.swing.LayoutStyle.ComponentPlacement;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Set;

import static io.github.tkjonesy.frontend.App.AVAILABLE_CAMERAS;

public class SettingsWindow extends JDialog {

    private JButton confirmButton, cancelButton, applyButton;

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
        JComboBox<String> cameraSelector = new JComboBox<>();
        JCheckBox boundingBoxCheck = new JCheckBox("Bounding Boxes",Settings.DISPLAY_BOUNDING_BOXES);
        JLabel cameraFpsLabel = new JLabel("Camera Frames Per Second");
        JSpinner cameraFpsSpinner = new JSpinner(new SpinnerNumberModel(Settings.CAMERA_FRAME_RATE, 0, 60, 1));
        JLabel cameraFpsWarningLabel = new JLabel("");

        // Populate camera selection menu with list of available cameras.
        Set<String> cameraNames = AVAILABLE_CAMERAS.keySet();
        int itemIndex = 0;
        for(String cameraName: cameraNames) {
            cameraSelector.addItem(cameraName);
            // Automatically set the selected camera to whatever camera is selected in the settings file.
            if(AVAILABLE_CAMERAS.get(cameraName) == Settings.VIDEO_CAPTURE_DEVICE_ID)
                cameraSelector.setSelectedIndex(itemIndex);
            itemIndex++;
        }

        // Bounding box details
        boundingBoxCheck.setHorizontalTextPosition(SwingConstants.LEFT);
        boundingBoxCheck.setToolTipText("When this is on, the bounding boxes will be drawn in the viewing window");

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
                        .addComponent(boundingBoxCheck)
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
                        .addComponent(boundingBoxCheck)
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

        JPanel modelPanel = new JPanel();

        /*------------------+
        | ADVANCED SETTINGS |
        +------------------*/

        JPanel advancedPanel = new JPanel();

        /*--------------+
        | BUTTON LAYOUT |
        +--------------*/
        // Components
        JPanel buttonPanel = new JPanel();
        confirmButton = new JButton("OK");
        confirmButton.setBackground(OCEAN);
        cancelButton = new JButton("Cancel");
        applyButton = new JButton("Apply");

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
        settingSelector.addTab("Model", modelPanel);
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

    private void initListeners() {
        confirmButton.addActionListener(
                e -> {
                    System.out.println("Pressed ok");
                    applyChanges();
                    this.dispose();
                }
        );

        cancelButton.addActionListener(
                e -> {
                    System.out.println("Pressed cancel");
                    cancelChanges();
                    this.dispose();
                }
        );

        applyButton.addActionListener(
                e -> {
                    System.out.println("Pressed apply");
                    applyChanges();

                }
        );

        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.out.println("Settings window closing");
                cancelChanges();
                SettingsWindow.this.dispose();
            }
        });
    }

    private void applyChanges() {
        System.out.println("Applying changed settings");
        // TODO compare camera selection with what is already selected. If there has been a change, save the new camera index value.
    }
    private void cancelChanges() {
        System.out.println("Cancelling changed settings");
    }
}
