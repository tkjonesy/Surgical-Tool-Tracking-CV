package io.github.tkjonesy.frontend;

import javax.imageio.ImageIO;
import javax.swing.*;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import io.github.tkjonesy.ONNX.models.OnnxRunner;
import io.github.tkjonesy.frontend.mainGUI.ButtonPanel;
import io.github.tkjonesy.frontend.mainGUI.CameraPanel;
import io.github.tkjonesy.frontend.mainGUI.LoggingPanel;
import io.github.tkjonesy.frontend.models.*;
import io.github.tkjonesy.frontend.models.SplashScreen;
import io.github.tkjonesy.frontend.models.cameraGrabber.CameraGrabber;
import io.github.tkjonesy.frontend.models.cameraGrabber.MacOSCameraGrabber;
import io.github.tkjonesy.frontend.models.cameraGrabber.WindowsCameraGrabber;
import io.github.tkjonesy.frontend.settingsGUI.SettingsWindow;
import io.github.tkjonesy.utils.ErrorDialogManager;
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
    @Getter
    private static OnnxRunner onnxRunner = null;
    @Getter
    @Setter
    private static VideoCapture camera;

    private static final Logger logger = LogManager.getLogger(App.class);
    public static final HashMap<String, Integer> AVAILABLE_CAMERAS;
    private static final SplashScreen splashScreen;
    static {

        // Display the splash screen
        splashScreen = new SplashScreen();
        splashScreen.showSplash();

        // Load OpenCV
        Loader.load(opencv_core.class);

        System.getProperty("org.bytedeco.javacpp.maxphysicalbytes", "0");
        System.getProperty("org.bytedeco.javacpp.maxbytes", "0");
        opencv_core.setNumThreads(1);

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
    }

    private Thread cameraFetcherThread;
    private CameraPanel cameraPanel;
    private LoggingPanel loggingPanel;

    @Getter
    private final SessionHandler sessionHandler;
    private final ProgramSettings settings;

    private CameraFetcher cameraFetcher;
    @Getter
    private JLabel cameraFeed;
    @Getter
    @Setter
    private JTextPane logTextPane;
    private JPanel trackerPanel;

    private static final Color CHARCOAL = new Color(30, 31, 34);

    public App() {
        instance = this;

        // Initialize the directories for AIMs
        try{
            initializeAIMsDirectories();
        }catch (RuntimeException e){
            logger.error("Failed to initialize AIMs directories. Exiting application.");
            ErrorDialogManager.displayErrorDialogFatal("Failed to initialize AIMs directories. Exiting application.");
        }

        // Load settings from file
        this.settings = SettingsLoader.loadSettings();

        if(settings == null) {
            logger.error("Failed to load settings from file. Exiting application.");
            ErrorDialogManager.displayErrorDialogFatal("Failed to load settings from file. Exiting application.");
        }

        ProgramSettings.setCurrentSettings(settings);

        System.out.println(settings);

        // Initialize the GUI components and listeners
        initComponents();
        initListeners();

        // Initialize the session handler, log handler, and ONNX runner
        LogHandler logHandler = new LogHandler(loggingPanel.getLogTextPane());
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

        // GUI Panels
        cameraPanel = new CameraPanel(new BorderLayout(), instance);
        loggingPanel = new LoggingPanel();
        ButtonPanel buttonPanel = new ButtonPanel(instance);

        // Window Layout
        this.setLayout(new GridBagLayout());
        this.add(cameraPanel, createConstraints(0, 0, 0.5, 1));
        this.add(loggingPanel, createConstraints(1, 0, 0.5, 0.5));
        GridBagConstraints buttonPanelConstraints = createConstraints(0, 1, 1, 0.05);
        buttonPanelConstraints.gridwidth = 2;
        buttonPanelConstraints.fill = GridBagConstraints.VERTICAL;
        this.add(buttonPanel, buttonPanelConstraints);
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
                        loggingPanel.setPreferredSize(new Dimension(App.this.getWidth() / 3, loggingPanel.getHeight()));
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
            CameraFetcher cameraFetcher = new CameraFetcher(this.cameraPanel.getCameraFeed(), camera, onnxRunner, sessionHandler);
            cameraFetcherThread = new Thread(cameraFetcher);
            cameraFetcherThread.start();

        }catch (CvException e) {
            ErrorDialogManager.displayErrorDialog("Failed to open camera with ID: " + cameraId);
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