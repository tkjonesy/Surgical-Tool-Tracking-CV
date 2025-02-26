package io.github.tkjonesy.frontend;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.LayoutStyle.ComponentPlacement;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import io.github.tkjonesy.ONNX.models.OnnxRunner;
import io.github.tkjonesy.frontend.models.*;
import io.github.tkjonesy.frontend.models.SplashScreen;
import io.github.tkjonesy.frontend.models.cameraGrabber.CameraGrabber;
import io.github.tkjonesy.frontend.models.cameraGrabber.MacOSCameraGrabber;
import io.github.tkjonesy.frontend.models.cameraGrabber.WindowsCameraGrabber;
import io.github.tkjonesy.frontend.settingsGUI.SettingsWindow;
import io.github.tkjonesy.utils.Paths;
import io.github.tkjonesy.utils.models.LogHandler;
import io.github.tkjonesy.utils.models.SessionHandler;
import io.github.tkjonesy.utils.settings.ProgramSettings;
import io.github.tkjonesy.utils.settings.SettingsLoader;
import lombok.Getter;
import lombok.Setter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bytedeco.opencv.opencv_videoio.VideoCapture;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.javacpp.Loader;
import org.opencv.core.CvException;

import static io.github.tkjonesy.utils.settings.SettingsLoader.initializeAIMsDirectories;

public class App extends JFrame {

    @Getter
    private static App instance;
    private static final Logger logger = LogManager.getLogger(App.class);

    public static final HashMap<String, Integer> AVAILABLE_CAMERAS;
    private static final SplashScreen splashScreen;
    static {

        // Display the splash screen
        splashScreen = new SplashScreen();
        splashScreen.showSplash();

        // Load OpenCV
        Loader.load(opencv_core.class);

        // Load the camera devices from the user's system
        CameraGrabber grabber;
        if(System.getProperty("os.name").toLowerCase().contains("mac")) {
            grabber = new MacOSCameraGrabber();
        } else if(System.getProperty("os.name").toLowerCase().contains("windows")) {
            grabber = new WindowsCameraGrabber();
        }else{
            throw new UnsupportedOperationException("Unsupported OS");
        }

        AVAILABLE_CAMERAS = grabber.getCameraNames();

        //print cameras
        System.out.println("Available Cameras:");
        for (String cameraName : AVAILABLE_CAMERAS.keySet()) {
            System.out.println(cameraName);
        }
    }

    private final SessionHandler sessionHandler;

    @Getter
    private static OnnxRunner onnxRunner = null;
    private final ProgramSettings settings;

    private CameraFetcher cameraFetcher;
    @Getter
    @Setter
    private static VideoCapture camera;
    private Thread cameraFetcherThread;
    @Getter
    private JLabel cameraFeed;
    private JToggleButton startSessionButton;
    private JButton settingsButton;
    @Getter
    @Setter
    private JTextPane logTextPane;
    private JPanel trackerPanel;

    private static final Color SUNSET = new Color(255, 40, 79);
    private static final Color OCEAN = new Color(55, 90, 129);
    private static final Color CHARCOAL = new Color(30, 31, 34);

    public App() {
        instance = this;

        // Initialize the directories for AIMs
        try{
            initializeAIMsDirectories();
        }catch (RuntimeException e){
            JOptionPane.showMessageDialog(this, "Failed to initialize AIMs directories. Exiting application", "Error", JOptionPane.ERROR_MESSAGE);
            logger.error("Failed to initialize AIMs directories. Exiting application.");
            System.exit(1);
        }

        // Load settings from file
        this.settings = SettingsLoader.loadSettings();

        if(settings == null) {
            JOptionPane.showMessageDialog(this, "An error occurred when trying to load program settings. Existing Application", "Error", JOptionPane.ERROR_MESSAGE);
            logger.error("Failed to load settings from file. Exiting application.");
            System.exit(1);
        }

        ProgramSettings.setCurrentSettings(settings);

        System.out.println(settings);

        // Initialize the GUI components and listeners
        initComponents();
        initListeners();

        // Initialize the session handler, log handler, and ONNX runner
        LogHandler logHandler = new LogHandler(logTextPane);
        this.sessionHandler = new SessionHandler(logHandler);
        onnxRunner = new OnnxRunner(logHandler.getLogQueue());

        updateCamera(settings.getCameraDeviceId());

        // Close the splash screen and display the application
        splashScreen.closeSplash();
        this.setVisible(true);
    }

    private void initComponents() {

        // Titling, sizing, and exit actions
        this.setTitle("AIM: Surgical");
        this.setMinimumSize(new Dimension(746, 401));
        this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        // Icon
        try(InputStream stream = getClass().getResourceAsStream(Paths.LOGO32_PATH)) {
            if(stream == null) {
                throw new IOException("Resource not found: " + Paths.LOGO32_PATH);
            }

            ImageIcon appIcon = new ImageIcon(ImageIO.read(stream));
            this.setIconImage(appIcon.getImage());
        } catch (Exception ignored) {}

        // Camera Panel
        JPanel cameraPanel = new JPanel(new BorderLayout());
        cameraPanel.setBorder(BorderFactory.createTitledBorder("Camera"));
        cameraFeed = new JLabel("");
        cameraFeed.setMinimumSize(new Dimension(320, 240));
        cameraPanel.add(cameraFeed, BorderLayout.CENTER);


        // Log tracker Panel
        trackerPanel = new JPanel();
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
                            boolean sessionStarted = sessionHandler.startNewSession(sessionTitle, sessionDescription, onnxRunner);

                            // If session started successfully, update UI and begin logging
                            if (sessionStarted) {
                                startSessionButton.setText("Stop Session");
                                startSessionButton.setBackground(SUNSET);
                                settingsButton.setEnabled(false);
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
                        settingsButton.setEnabled(true);
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

                    SettingsLoader.saveSettings(settings);

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

        this.addComponentListener(
                new ComponentAdapter() {
                    @Override
                    public void componentResized(ComponentEvent e) {
                        trackerPanel.setPreferredSize(new Dimension(App.this.getWidth() / 3, trackerPanel.getHeight()));
                    }
                }
        );
    }

    public void updateCamera(int cameraId) {
        try{
            camera = new VideoCapture(cameraId);
            if (!camera.isOpened()) {
                cameraId = 0;
                camera = new VideoCapture(cameraId);
                if (!camera.isOpened()) {
                    throw new CvException("Unable to open camera with ID: " + cameraId);
                }
            }

            // Camera fetcher thread task
            cameraFetcher = new CameraFetcher(this.cameraFeed, camera, onnxRunner, sessionHandler);
            cameraFetcherThread = new Thread(cameraFetcher);
            cameraFetcherThread.start();

        }catch (CvException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            logger.error("Failed to open camera with ID: {}", cameraId);
        }
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