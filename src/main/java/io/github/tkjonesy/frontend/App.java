package io.github.tkjonesy.frontend;

import javax.swing.*;
import javax.swing.LayoutStyle.ComponentPlacement;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import io.github.tkjonesy.ONNX.models.OnnxRunner;
import io.github.tkjonesy.frontend.models.*;
import io.github.tkjonesy.frontend.models.cameraGrabber.CameraGrabber;
import io.github.tkjonesy.frontend.models.cameraGrabber.MacOSCameraGrabber;
import io.github.tkjonesy.frontend.models.cameraGrabber.WindowsCameraGrabber;
import lombok.Getter;
import lombok.Setter;

import org.bytedeco.opencv.opencv_videoio.VideoCapture;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.javacpp.Loader;

import static io.github.tkjonesy.ONNX.settings.Settings.*;

public class App extends JFrame {
    private final SessionHandler sessionHandler;

    public static final HashMap<String, Integer> AVAILABLE_CAMERAS;
    static {
        // Load OpenCV
        Loader.load(opencv_core.class);

        CameraGrabber grabber;

        if(System.getProperty("os.name").toLowerCase().contains("mac")) {
            grabber = new MacOSCameraGrabber();
        } else if(System.getProperty("os.name").toLowerCase().contains("windows")) {
            grabber = new WindowsCameraGrabber();
        }

        AVAILABLE_CAMERAS = grabber.getCameraNames();

        //print cameras
        System.out.println("Available Cameras:");
        for (String cameraName : AVAILABLE_CAMERAS.keySet()) {
            System.out.println(cameraName);
        }


        // Check first 10 device ports for any connected cameras, add them to available cameras
//        AVAILABLE_CAMERAS = new ArrayList<>();
//        final int MAX_PORTS_TO_CHECK = 10;
//        for(int i = 0; i < MAX_PORTS_TO_CHECK; i++)
//        {
//            try(VideoCapture camera = new VideoCapture(i)) {
//                if (camera.isOpened()) {
//                    AVAILABLE_CAMERAS.add(i);
//                    camera.release();
//                }
//            }
//        }
    }

    private final OnnxRunner onnxRunner;

    @Getter
    private final VideoCapture camera;
    private final Thread cameraFetcherThread;
    @Getter
    private JLabel cameraFeed;
    private JToggleButton startSessionButton;
    private JButton settingsButton;
    @Getter
    @Setter
    private JTextPane logTextPane;

    private static final Color SUNSET = new Color(255, 40, 79);
    private static final Color OCEAN = new Color(55, 90, 129);
    private static final Color CHARCOAL = new Color(30, 31, 34);

    public App() {
        initComponents();
        initListeners();
        this.setVisible(true);
        this.camera = new VideoCapture(VIDEO_CAPTURE_DEVICE_ID);
        if (!camera.isOpened()) {
            System.err.println("Error: Camera could not be opened. Exiting...");
            System.exit(-1);
        }


        LogHandler logHandler = new LogHandler(logTextPane);
        this.sessionHandler = new SessionHandler(logHandler);

        this.onnxRunner = new OnnxRunner(logHandler.getLogQueue());

        // Camera fetcher thread task
        CameraFetcher cameraFetcher = new CameraFetcher(this.cameraFeed, this.camera, onnxRunner, sessionHandler);
        cameraFetcherThread = new Thread(cameraFetcher);
        cameraFetcherThread.start();
    }

    private void initComponents() {

        // Titling, sizing, and exit actions
        this.setTitle("AIM: Surgical");
        this.setMinimumSize(new Dimension(746, 401));
        this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        // Icon
        try {
            ImageIcon appIcon = new ImageIcon("src/main/resources/logo32.png");
            this.setIconImage(appIcon.getImage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

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

        // Log tracker Panel
        JPanel trackerPanel = new JPanel();
        trackerPanel.setBorder(BorderFactory.createTitledBorder("Tracking Log"));
        this.logTextPane = new JTextPane();
        this.logTextPane.setEditable(false);
        this.logTextPane.setContentType("text/html");
        this.logTextPane.setBackground(CHARCOAL);

        // Log tracker scroll pane for text area
        JScrollPane scrollPane = new JScrollPane(logTextPane);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        // Set the layout for tracking panel using GroupLayout
        GroupLayout trackingPanelLayout = new GroupLayout(trackerPanel);
        trackingPanelLayout.setAutoCreateContainerGaps(true);
        trackingPanelLayout.setHorizontalGroup(
                trackingPanelLayout.createSequentialGroup()
                        .addComponent(scrollPane)
        );
        trackingPanelLayout.setVerticalGroup(
                trackingPanelLayout.createSequentialGroup()
                        .addComponent(scrollPane)
        );
        trackerPanel.setLayout(trackingPanelLayout);

        // Bottom Button Panel
        JPanel bottomPanel = new JPanel();

        startSessionButton = new JToggleButton("Start Session");
        startSessionButton.setBackground(OCEAN);
        settingsButton = new JButton("Settings");

        GroupLayout bottomPanelLayout = new GroupLayout(bottomPanel);
        bottomPanelLayout.setAutoCreateContainerGaps(true);
        bottomPanelLayout.setHorizontalGroup(
                bottomPanelLayout.createSequentialGroup()
                        .addComponent(startSessionButton)
                        .addPreferredGap(ComponentPlacement.UNRELATED)
                        .addComponent(settingsButton)
        );
        bottomPanelLayout.setVerticalGroup(
                bottomPanelLayout.createSequentialGroup()
                        .addGroup(
                                bottomPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                        .addComponent(startSessionButton)
                                        .addComponent(settingsButton)
                        )
        );
        bottomPanel.setLayout(bottomPanelLayout);

        // Window Layout
        this.setLayout(new GridBagLayout());
        this.add(cameraPanel, createConstraints(0, 0, 0.5, 1));
        this.add(trackerPanel, createConstraints(1, 0, 0.5, 0.5));
        GridBagConstraints bottomPanelConstraints = createConstraints(0, 1, 1, 0.05);
        bottomPanelConstraints.gridwidth = 2;
        bottomPanelConstraints.fill = GridBagConstraints.VERTICAL;
        this.add(bottomPanel, bottomPanelConstraints);
        this.pack();
        this.setLocationRelativeTo(null); // Center application
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

        // Session Button Action Listener
        startSessionButton.addActionListener(
                e -> {
                    if (startSessionButton.getText().equals("Start Session")) {

                        // Open session input dialog
                        SessionInputDialog dialog = new SessionInputDialog(this);
                        dialog.setVisible(true);

                        // Check if the user confirmed the dialog
                        if (dialog.isConfirmed()) {
                            String sessionTitle = dialog.getSessionTitle();
                            String sessionDescription = dialog.getSessionDescription();

                            // Ensure title and description are not empty
                            if (sessionTitle.isEmpty()) {
                                JOptionPane.showMessageDialog(App.this,
                                        "Please fill in both fields.",
                                        "Invalid Input", JOptionPane.ERROR_MESSAGE);
                                return;
                            }

                            // Start new session
                            boolean sessionStarted = sessionHandler.startNewSession(sessionTitle, sessionDescription, this.onnxRunner);

                            // If session started successfully, update UI and begin logging
                            if (sessionStarted) {
                                startSessionButton.setText("Stop Session");
                                startSessionButton.setBackground(SUNSET);
                            } else {
                                JOptionPane.showMessageDialog(App.this,
                                        "Failed to start session. Please check the console for more information.",
                                        "Session Start Failed", JOptionPane.ERROR_MESSAGE);
                            }
                        }

                    } else if (startSessionButton.getText().equals("Stop Session")) {
                        startSessionButton.setText("Start Session");
                        startSessionButton.setBackground(OCEAN);
                        sessionHandler.endSession();
                    }
                }
        );

        // Settings Listener
        settingsButton.addActionListener(e -> SwingUtilities.invokeLater(() -> new SettingsWindow(this)));

        // Window Event Listener
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                int confirmation = JOptionPane.showConfirmDialog(App.this,
                        "Are you sure you want to quit?",
                        "Confirm Exit", JOptionPane.YES_NO_OPTION);

                if (confirmation == JOptionPane.YES_OPTION) {
                    System.out.println("Beginning cleanup Process...");
                    System.out.println("Stopping camera feed thread...");
                    if(cameraFetcherThread != null) cameraFetcherThread.interrupt();
                    System.out.println("Closing camera access...");
                    if (camera != null && camera.isOpened())
                        camera.release();
                    System.out.println("Done cleanup process.");

                    App.this.dispose();
                    System.exit(0);
                } else {
                    System.out.println("Exit cancelled.");
                }
            }
        });
    }

    public static void main(String[] args) {
        try {
//            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            UIManager.setLookAndFeel("com.formdev.flatlaf.FlatDarkLaf");
        } catch (Exception e) {
            System.out.println("Unable to set Look and Feel to system default.");
        }

        SwingUtilities.invokeLater(App::new);
    }
}