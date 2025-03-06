package io.github.tkjonesy.frontend;

import javax.imageio.ImageIO;
import javax.swing.*;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.io.*;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.bytedeco.javacv.VideoInputFrameGrabber;
import org.bytedeco.opencv.opencv_videoio.VideoCapture;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.javacpp.Loader;
import org.opencv.core.CvException;

import org.bytedeco.ffmpeg.ffmpeg;

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
    public static Map<String, Integer> AVAILABLE_CAMERAS;
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

        AVAILABLE_CAMERAS = getCameraDevices();
        for(String key : AVAILABLE_CAMERAS.keySet()){
            System.out.println(key + " : " + AVAILABLE_CAMERAS.get(key));
        }

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

        updateCamera(settings.getCameraName());

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

    public void updateCamera(String cameraName) {
        System.out.println("Updating camera to: " + cameraName);
        int cameraId = 0;
        if(AVAILABLE_CAMERAS.containsKey(cameraName)){
            System.out.println("Camera name found in available cameras.");
            cameraId = AVAILABLE_CAMERAS.get(cameraName);
        }else{
            System.out.println("Camera name not found in available cameras. Using first available camera.");
            for(String key : AVAILABLE_CAMERAS.keySet()){
                if(AVAILABLE_CAMERAS.get(key) == 0){
                    System.out.println("Using camera: " + key);
                    settings.setCameraName(key);
                    SettingsLoader.saveSettings(settings);
                    break;
                }
            }
        }
        try{
            cameraId=1;
            System.out.println("----- Opening camera with ID: " + cameraId);
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

    public static Map<String, Integer> getCameraDevices() {
        Map<String, Integer> cameraMap = new LinkedHashMap<>(); // Preserve order from FFmpeg output
        String os = System.getProperty("os.name").toLowerCase();
        String command;

        try {
            String ffmpegPath = Loader.load(ffmpeg.class);

            if (os.contains("win")) {
                command = "cmd.exe /c " + ffmpegPath + " -list_devices true -f dshow -i dummy";
            } else if (os.contains("mac")) {
                command = "bash -c \"" + ffmpegPath + " -f avfoundation -list_devices true -i \\\"\\\"\"";
            } else {
                command = "bash -c \"" + ffmpegPath + " v4l2-ctl --list-devices\"";
            }

            System.out.println("Executing: " + command);

            ProcessBuilder processBuilder;
            if (os.contains("win")) {
                processBuilder = new ProcessBuilder("cmd.exe", "/c", ffmpegPath, "-list_devices", "true", "-f", "dshow", "-i", "dummy");
            } else {
                processBuilder = new ProcessBuilder("bash", "-c", command);
            }
            processBuilder.redirectErrorStream(true); // Merge stderr and stdout

            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            boolean isMacVideoSection = false;
            Pattern macPattern = Pattern.compile("\\[(\\d+)] (.+)"); // Matches [index] Camera Name

            while ((line = reader.readLine()) != null) {
                System.out.println("FFmpeg Output: " + line); // Debugging: Compare with terminal output
                if (os.contains("mac")) {
                    if (line.contains("AVFoundation video devices:")) {
                        isMacVideoSection = true;
                        continue;
                    }
                    if (line.contains("AVFoundation audio devices:")) {
                        break; // Stop at audio section
                    }
                    if (isMacVideoSection) {
                        Matcher matcher = macPattern.matcher(line.trim()); // Ensures proper spacing
                        if (matcher.find()) {
                            int deviceIndex = Integer.parseInt(matcher.group(1));
                            String deviceName = matcher.group(2).trim();

                            // Ignore non-camera video devices
                            if (!deviceName.toLowerCase().contains("capture screen") &&
                                    !deviceName.toLowerCase().contains("microphone")) {

                                // Store the camera name while ensuring index ordering is correct
                                if (!cameraMap.containsKey(deviceName)) {
                                    cameraMap.put(deviceName, deviceIndex);
                                    System.out.println("Found camera: " + deviceName + " at index: " + deviceIndex);
                                }
                            }
                        }
                    }
                } else {
                    // Windows parsing: Ensure only video devices are captured
                    if (line.toLowerCase().contains("(video)")) {
                        String deviceName = line.replaceAll("\"", "").replace(" (video)", "").trim();
                        if (!cameraMap.containsKey(deviceName)) {
                            cameraMap.put(deviceName, cameraMap.size());
                            System.out.println("Found camera: " + deviceName);
                        }
                    }
                }
            }

            process.waitFor();
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return cameraMap;
    }
}