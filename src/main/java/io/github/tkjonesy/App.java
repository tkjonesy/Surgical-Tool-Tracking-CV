package io.github.tkjonesy;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.LayoutStyle.ComponentPlacement;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import lombok.Getter;
import org.opencv.core.Core;
import org.opencv.videoio.VideoCapture;

import static io.github.tkjonesy.ONNX.settings.Settings.*;

public class App extends JFrame {

    // Compulsory OpenCV loading
    static{ System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }

    @Getter
    private final VideoCapture camera;
    private final Thread cameraFetcherThread;
    @Getter
    private JLabel cameraFeed;
    private JToggleButton recCameraButton, recAllButton, recLogButton;
    private JButton settingsButton;

    public App() {
        initComponents();
        initListeners();
        this.setVisible(true);

        this.camera = new VideoCapture(VIDEO_CAPTURE_DEVICE_ID);
        if(!camera.isOpened()) {
            System.err.println("Error: Camera could not be opened. Exiting...");
            System.exit(-1);
        }

        // Camera fetcher thread task
        CameraFetcher cameraFetcher = new CameraFetcher(this.cameraFeed, this.camera);
        cameraFetcherThread = new Thread(cameraFetcher);
        cameraFetcherThread.start();
    }

    private void initComponents() {

        // Titling, sizing, and exit actions
        this.setTitle("Surgical Tool Tracker");
        this.setMinimumSize(new Dimension(746, 401));
        this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        // Camera Panel
        JPanel cameraPanel = new JPanel();
        cameraPanel.setBorder(BorderFactory.createTitledBorder("Camera"));

        cameraFeed = new JLabel("");
        cameraFeed.setMinimumSize(new Dimension(320, 240));

        GroupLayout cameraPanelLayout = new GroupLayout(cameraPanel);
        cameraPanelLayout.setAutoCreateContainerGaps(true);
        cameraPanelLayout.setHorizontalGroup(
                cameraPanelLayout.createSequentialGroup()
                        .addComponent(cameraFeed)
        );
        cameraPanelLayout.setVerticalGroup(
                cameraPanelLayout.createSequentialGroup()
                        .addComponent(cameraFeed)
        );
        cameraPanel.setLayout(cameraPanelLayout);
//        cameraPanel.setLayout(new GridBagLayout());
//        cameraPanel.add(cameraFeed, createConstraints(0,0,0.5,0.5));

        // Tracking Panel
        JPanel trackingPanel = new JPanel();
        trackingPanel.setBorder(BorderFactory.createTitledBorder("Log"));

        JTextPane logTextPane = new JTextPane();
        logTextPane.setMinimumSize(new Dimension(320, 240));

        GroupLayout trackingPanelLayout = new GroupLayout(trackingPanel);
        trackingPanelLayout.setAutoCreateContainerGaps(true);
        trackingPanelLayout.setHorizontalGroup(
                trackingPanelLayout.createSequentialGroup()
                        .addComponent(logTextPane)
        );
        trackingPanelLayout.setVerticalGroup(
                trackingPanelLayout.createSequentialGroup()
                        .addComponent(logTextPane)
        );
        trackingPanel.setLayout(trackingPanelLayout);

        // Bottom Button Panel
        JPanel bottomPanel = new JPanel();

        recCameraButton = new JToggleButton("Start Camera");
        recAllButton = new JToggleButton("Start All");
        recLogButton = new JToggleButton("Start Log");
        settingsButton = new JButton("Settings");

        GroupLayout bottomPanelLayout = new GroupLayout(bottomPanel);
        bottomPanelLayout.setAutoCreateContainerGaps(true);
        bottomPanelLayout.setHorizontalGroup(
                bottomPanelLayout.createSequentialGroup()
                        .addComponent(recCameraButton)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(recAllButton)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(recLogButton)
                        .addPreferredGap(ComponentPlacement.UNRELATED)
                        .addComponent(settingsButton)
        );
        bottomPanelLayout.setVerticalGroup(
                bottomPanelLayout.createSequentialGroup()
                        .addGroup(
                                bottomPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                        .addComponent(recCameraButton)
                                        .addComponent(recAllButton)
                                        .addComponent(recLogButton)
                                        .addComponent(settingsButton)
                        )
        );
        bottomPanel.setLayout(bottomPanelLayout);

        // Window Layout
        this.setLayout(new GridBagLayout());
        this.add(cameraPanel, createConstraints(0, 0, 0.5, 1));
        this.add(trackingPanel, createConstraints(1, 0, 0.5, 0.5));
        GridBagConstraints bottomPanelConstraints = createConstraints(0, 1, 1, 0.05);
        bottomPanelConstraints.gridwidth = 2;
        bottomPanelConstraints.fill = GridBagConstraints.VERTICAL;
        this.add(bottomPanel, bottomPanelConstraints);
        this.pack();
    }

    private GridBagConstraints createConstraints(int gridX, int gridY, double weightX, double weightY) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = gridX;
        c.gridy = gridY;
        c.weightx = weightX;
        c.weighty = weightY;
        c.fill = GridBagConstraints.BOTH;
        return c;
    }

    private void initListeners() {

        // Record Camera Listener
        recCameraButton.addActionListener(
                e -> {
                    // Start recording
                    if(recCameraButton.isSelected()) {
                        recCameraButton.setText("Stop Camera");
                        setRecButtons(true, false, false);
                    }
                    // Stop recording
                    else {
                        recCameraButton.setText("Start Camera");
                        setRecButtons(true, true, true);
                    }
                }
        );

        // Record All Listener
        recAllButton.addActionListener(
                e -> {
                    // Start recording
                    if(recAllButton.isSelected()) {
                        recAllButton.setText("Stop All");
                        setRecButtons(false, true, false);
                    }
                    // Stop recording
                    else {
                        recAllButton.setText("Start All");
                        setRecButtons(true, true, true);
                    }
                }
        );

        // Record Log Listener
        recLogButton.addActionListener(
                e -> {
                    // Start recording
                    if(recLogButton.isSelected()) {
                        recLogButton.setText("Stop Log");
                        setRecButtons(false, false, true);
                    }
                    // Stop recording
                    else {
                        recLogButton.setText("Start Log");
                        setRecButtons(true, true, true);
                    }
                }
        );

        // Settings Listener
        settingsButton.addActionListener(
                e -> {

                }
        );

        // Window Event Listener
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // Ask for closing confirmation first
                int confirmation = JOptionPane.showConfirmDialog(App.this,
                        "Are you sure you want to quit?",
                        "Confirm Exit", JOptionPane.YES_NO_OPTION);

                // If "yes" is selected, clean up and dispose of windows
                if(confirmation == JOptionPane.YES_OPTION) {

                    // Clean up
                    System.out.println("Beginning cleanup Process...");
                    System.out.println("Stopping camera feed thread...");
                    cameraFetcherThread.interrupt(); // TODO fix crash when thread is interrupted, then the camera is closed (I think this has to do with the thread not actually stopping or smth like that, it's related to Imgproc.resize() in some manner? WTF?
                    System.out.println("Closing camera access...");
                    if (camera.isOpened())
                        camera.release();
                    System.out.println("Done cleanup process.");

                    // Disposal
                    App.this.dispose();
                    System.exit(0);
                }
                else
                    System.out.println("Exit cancelled.");
            }
        });
    }

    private void setRecButtons(boolean camEnable, boolean allEnable, boolean logEnable) {
        recCameraButton.setEnabled(camEnable);
        recAllButton.setEnabled(allEnable);
        recLogButton.setEnabled(logEnable);
    }

    private int saveConfirmationDialogue(JButton button) {
        return JOptionPane.showConfirmDialog(button, "Save Recording?", "Save Data", JOptionPane.YES_NO_CANCEL_OPTION);
    }

    public static void main(String[] args) {
        // Attempt to set L&F to system default for now
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch(Exception e) {
            System.out.println("Unable to set Look and Feel to system default.");
//            e.printStackTrace();
        }

        // Invoke instance of STT App
        SwingUtilities.invokeLater(App::new);
    }
}

/*
recCameraButton.addActionListener(e -> {
    if(recCameraButton.getText().equals("Start Camera")){
        System.out.println("Start Camera Clicked");
        recCameraButton.setText("Stop Camera");
        //Change Borders Title
        cameraBorder.setTitle("Camera View : Recording");
        cameraBorder.setTitleColor(new Color(255,0,0));
        cameraPanel.repaint();
        //Disable Buttons
        recAllButton.setEnabled(false);
        recLogButton.setEnabled(false);
    }
    else {
        System.out.println("Stop Camera Clicked");

        int answer = JOptionPane.showConfirmDialog(recLogButton, "Save Recording?", "Save Data", JOptionPane.YES_NO_CANCEL_OPTION);

        //Yes: Save Data
        if(answer == 0) {
            System.out.println("Save Camera Recording Clicked\n");
            recCameraButton.setText("Start Camera");
            //Change Borders Title
            cameraBorder.setTitle("Camera View");
            cameraBorder.setTitleColor(new Color(255,255,255));
            cameraPanel.repaint();
            //Enable Buttons
            recAllButton.setEnabled(true);
            recLogButton.setEnabled(true);
        }

        //No: Don't Save Data
        else if(answer == 1) {
            System.out.println("Don't Save Camera Recording Clicked\n");
            recCameraButton.setText("Start Camera");
            //Change Borders Title
            cameraBorder.setTitle("Camera View");
            cameraBorder.setTitleColor(new Color(255,255,255));
            cameraPanel.repaint();
            //Enable Buttons
            recAllButton.setEnabled(true);
            recLogButton.setEnabled(true);
        }

        //Cancel
        else if(answer == 2) {
            System.out.println("Cancel Clicked");
        }
    }
});
* */
