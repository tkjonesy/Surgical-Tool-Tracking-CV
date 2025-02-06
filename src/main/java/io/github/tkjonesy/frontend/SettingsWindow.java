package io.github.tkjonesy.frontend;

import javax.swing.*;
import javax.swing.LayoutStyle.ComponentPlacement;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

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

        /*----------------+
        | CAMERA SETTINGS |
        +----------------*/
        // Components
        JPanel cameraPanel = new JPanel();
        JLabel cameraSelectorLabel = new JLabel("Camera Selection");
        JComboBox<Integer> cameraSelector = new JComboBox<>();
        for(int cameraIndex : AVAILABLE_CAMERAS)
            cameraSelector.addItem(cameraIndex);
        JCheckBox boundingBoxCheck = new JCheckBox("Bounding Boxes",true);
        boundingBoxCheck.setHorizontalTextPosition(SwingConstants.LEFT);
        boundingBoxCheck.setToolTipText("When this is on, the bounding boxes will be drawn in the viewing window");

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
        );
        cameraPanel.setLayout(cameraSettingsLayout);

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
    }
    private void cancelChanges() {
        System.out.println("Cancelling changed settings");
    }
}
