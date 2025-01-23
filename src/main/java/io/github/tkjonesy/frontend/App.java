package io.github.tkjonesy.frontend;

import javax.swing.*;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.border.TitledBorder;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.atomic.AtomicBoolean;

import io.github.tkjonesy.ONNX.models.LogQueue;
import io.github.tkjonesy.frontend.models.CameraFetcher;
import io.github.tkjonesy.frontend.models.FileSession;
import io.github.tkjonesy.frontend.models.LogHandler;
import lombok.Getter;
import lombok.Setter;
import org.opencv.core.Core;
import org.opencv.videoio.VideoCapture;

import static io.github.tkjonesy.ONNX.settings.Settings.*;

public class App extends JFrame {

    // Compulsory OpenCV loading
    static { System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }

    private final LogHandler logHandler;
    private final FileSession fileSession;

    @Getter
    private final VideoCapture camera;
    private final Thread cameraFetcherThread;
    @Getter
    private JLabel cameraFeed;
    private JToggleButton recCameraButton, recAllButton, recLogButton;
    private JButton settingsButton;
    @Getter
    @Setter
    private JTextPane logTextPane;

    public App() {
        initComponents();
        initListeners();
        this.setVisible(true);

        this.fileSession = new FileSession();

        // Log Handler. Used to handle the presentation of logs
        this.logHandler = new LogHandler(logTextPane, fileSession);

        this.camera = new VideoCapture(VIDEO_CAPTURE_DEVICE_ID);
        if (!camera.isOpened()) {
            System.err.println("Error: Camera could not be opened. Exiting...");
            System.exit(-1);
        }

        // Camera fetcher thread task
        CameraFetcher cameraFetcher = new CameraFetcher(this.cameraFeed, this.camera, logHandler.getLogQueue(), fileSession);
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

        // Log tracker border
        TitledBorder logBorder = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.WHITE), "Tracking Log");
        logBorder.setTitleColor(Color.WHITE);

        // Log tracker Tracking Panel
        JPanel trackingPanel = new JPanel();
        trackingPanel.setBorder(logBorder);
        trackingPanel.setBackground(Color.BLACK);

        this.logTextPane = new JTextPane();
        this.logTextPane.setEditable(false);
        this.logTextPane.setContentType("text/html");
        this.logTextPane.setBackground(Color.BLACK);

        // Log tracker scroll pane for text area
        JScrollPane scrollPane = new JScrollPane(logTextPane);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        // Set the layout for tracking panel using GroupLayout
        GroupLayout trackingPanelLayout = new GroupLayout(trackingPanel);
        trackingPanelLayout.setAutoCreateContainerGaps(true);
        trackingPanelLayout.setHorizontalGroup(
                trackingPanelLayout.createSequentialGroup()
                        .addComponent(scrollPane)
        );
        trackingPanelLayout.setVerticalGroup(
                trackingPanelLayout.createSequentialGroup()
                        .addComponent(scrollPane)
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
                    if (recCameraButton.isSelected()) {
                        recCameraButton.setText("Stop Camera");
                        setRecButtons(true, false, false);
                        fileSession.startNewSession();

                        logHandler.startLogProcessing();
                    } else {
                        recCameraButton.setText("Start Camera");
                        setRecButtons(true, true, true);
                        fileSession.endSession();
                    }
                }
        );

        // Record All Listener
        recAllButton.addActionListener(
                e -> {
                    if (recAllButton.isSelected()) {
                        recAllButton.setText("Stop All");
                        setRecButtons(false, true, false);
                        fileSession.startNewSession();
                        logHandler.startLogProcessing();

                    } else {
                        recAllButton.setText("Start All");
                        setRecButtons(true, true, true);
                        fileSession.endSession();
                        logHandler.endLogProcessing();
                    }
                }
        );

        // Record Log Listener
        recLogButton.addActionListener(
                e -> {
                    if (recLogButton.isSelected()) {
                        recLogButton.setText("Stop Log");
                        setRecButtons(false, false, true);
                    } else {
                        recLogButton.setText("Start Log");
                        setRecButtons(true, true, true);
                    }
                }
        );

        // Settings Listener
        settingsButton.addActionListener(e -> {
            // Add settings logic here
        });

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
                    cameraFetcherThread.interrupt();
                    System.out.println("Closing camera access...");
                    if (camera.isOpened())
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

    private void setRecButtons(boolean camEnable, boolean allEnable, boolean logEnable) {
        recCameraButton.setEnabled(camEnable);
        recAllButton.setEnabled(allEnable);
        recLogButton.setEnabled(logEnable);
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.out.println("Unable to set Look and Feel to system default.");
        }

        SwingUtilities.invokeLater(App::new);
    }
}
