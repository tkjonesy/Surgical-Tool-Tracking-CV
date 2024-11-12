package io.github.tkjonesy;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.LayoutStyle.ComponentPlacement;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

import java.util.Timer;
import java.util.TimerTask;
import java.util.ArrayList;
import java.util.List;

import io.github.tkjonesy.ONNX.Detection;
import io.github.tkjonesy.ONNX.ImageUtil;
import lombok.Getter;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import io.github.tkjonesy.ONNX.models.OnnxOutput;
import io.github.tkjonesy.ONNX.models.OnnxRunner;

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

    public static void main(String[] args) {

        // TODO remove Jake's initial UI code
        //Initialize Frame
        JFrame frame = new JFrame("Surgical Tool Tracker");
        frame.setSize(960,540);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setBackground(new Color(52,52,52));
        frame.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        //Panel for Camera View
        JPanel cameraPanel = new JPanel();
        cameraPanel.setBackground(new Color(0,0,0));
        TitledBorder cameraBorder = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(255,255,255)), "Camera View");
        cameraBorder.setTitleColor(new Color(255,255,255));
        cameraPanel.setBorder(cameraBorder);
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0.5;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        frame.add(cameraPanel, c);

        //Panel for Tracking Log
        JPanel trackingPanel = new JPanel();
        trackingPanel.setBackground(new Color(0,0,0));
        TitledBorder trackingBorder = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(255,255,255)), "Tracking Log");
        trackingBorder.setTitleColor(new Color(255,255,255));
        trackingPanel.setBorder(trackingBorder);
        c.gridx = 1;
        c.gridy = 0;
        c.weightx = 0.5;
        c.weighty = 0.5;
        c.fill = GridBagConstraints.BOTH;
        frame.add(trackingPanel, c);

        //Panel for Bottom Bar
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new FlowLayout());
        bottomPanel.setBackground(new Color(27,27,27));

        //Initialize Buttons
        JButton settingsButton = new JButton();
        JButton recCameraButton = new JButton();
        JButton recAllButton = new JButton();
        JButton recLogButton = new JButton();

        //Settings Button
        settingsButton.setBounds(0, 0, 200, 50);
        settingsButton.setText("Settings");
        settingsButton.setFocusable(false);
        settingsButton.setForeground(new Color(255,255,255));
        settingsButton.setBackground(new Color(52,52,52));
        settingsButton.setBorder(BorderFactory.createMatteBorder(5, 10, 5, 10, new Color(52,52,52)));
        settingsButton.addActionListener(e -> System.out.println("Settings Clicked"));

        //Record Camera Button
        recCameraButton.setBounds(0, 0, 200, 50);
        recCameraButton.setText("Start Camera");
        recCameraButton.setFocusable(false);
        recCameraButton.setForeground(new Color(255,255,255));
        recCameraButton.setBackground(new Color(52,52,52));
        recCameraButton.setBorder(BorderFactory.createMatteBorder(5, 10, 5, 10, new Color(52,52,52)));
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

        //Record All Button
        recAllButton.setBounds(0, 0, 200, 50);
        recAllButton.setText("Start All");
        recAllButton.setFocusable(false);
        recAllButton.setForeground(new Color(255,255,255));
        recAllButton.setBackground(new Color(52,52,52));
        recAllButton.setBorder(BorderFactory.createMatteBorder(5, 10, 5, 10, new Color(52,52,52)));
        recAllButton.addActionListener(e -> {
            if(recAllButton.getText().equals("Start All")){
                System.out.println("Start All Clicked");
                recAllButton.setText("Stop All");
                //Change Camera Border Title
                cameraBorder.setTitle("Camera View : Recording");
                cameraBorder.setTitleColor(new Color(255,0,0));
                cameraPanel.repaint();
                //Change Tracking Border Title
                trackingBorder.setTitle("Tracking View : Recording");
                trackingBorder.setTitleColor(new Color(255,0,0));
                trackingPanel.repaint();
                //Disable Buttons
                recCameraButton.setEnabled(false);
                recLogButton.setEnabled(false);
            }
            else {
                System.out.println("Stop All Clicked");

                int answer = JOptionPane.showConfirmDialog(recLogButton, "Save Recording?", "Save Data", JOptionPane.YES_NO_CANCEL_OPTION);

                //Yes: Save Data
                if(answer == 0) {
                    System.out.println("Save All Recording Clicked\n");
                    recAllButton.setText("Start All");
                    //Change Camera Border Title
                    cameraBorder.setTitle("Camera View");
                    cameraBorder.setTitleColor(new Color(255,255,255));
                    cameraPanel.repaint();
                    //Change Tracking Border Title
                    trackingBorder.setTitle("Tracking View");
                    trackingBorder.setTitleColor(new Color(255,255,255));
                    trackingPanel.repaint();
                    //Enable Buttons
                    recCameraButton.setEnabled(true);
                    recLogButton.setEnabled(true);
                }

                //No: Don't Save Data
                else if(answer == 1) {
                    System.out.println("Don't Save All Recording Clicked\n");
                    recAllButton.setText("Start All");
                    //Change Camera Border Title
                    cameraBorder.setTitle("Camera View");
                    cameraBorder.setTitleColor(new Color(255,255,255));
                    cameraPanel.repaint();
                    //Change Tracking Border Title
                    trackingBorder.setTitle("Tracking View");
                    trackingBorder.setTitleColor(new Color(255,255,255));
                    trackingPanel.repaint();
                    //Enable Buttons
                    recCameraButton.setEnabled(true);
                    recLogButton.setEnabled(true);
                }

                //Cancel
                else if(answer == 2) {
                    System.out.println("Cancel Clicked");
                }
            }
        });

        //Record Log Button
        recLogButton.setBounds(0, 0, 200, 50);
        recLogButton.setText("Start Log");
        recLogButton.setFocusable(false);
        recLogButton.setForeground(new Color(255,255,255));
        recLogButton.setBackground(new Color(52,52,52));
        recLogButton.setBorder(BorderFactory.createMatteBorder(5, 10, 5, 10, new Color(52,52,52)));
        recLogButton.addActionListener(e -> {
            if(recLogButton.getText().equals("Start Log")){
                System.out.println("Start Log Clicked");
                recLogButton.setText("Stop Log");
                //Change Tracking Border Title
                trackingBorder.setTitle("Tracking View : Recording");
                trackingBorder.setTitleColor(new Color(255,0,0));
                trackingPanel.repaint();
                //Disable Buttons
                recCameraButton.setEnabled(false);
                recAllButton.setEnabled(false);
            }
            else {
                System.out.println("Stop Log Clicked");

                int answer = JOptionPane.showConfirmDialog(recLogButton, "Save Recording?", "Save Data", JOptionPane.YES_NO_CANCEL_OPTION);

                //Yes: Save Data
                if(answer == 0) {
                    System.out.println("Save Log Recording Clicked\n");
                    recLogButton.setText("Start Log");
                    //Change Tracking Border Title
                    trackingBorder.setTitle("Tracking View");
                    trackingBorder.setTitleColor(new Color(255,255,255));
                    trackingPanel.repaint();
                    //Enable buttons
                    recCameraButton.setEnabled(true);
                    recAllButton.setEnabled(true);
                }

                //No: Don't Save Data
                else if(answer == 1) {
                    System.out.println("Don't Save Log Recording Clicked\n");
                    recLogButton.setText("Start Log");
                    //Change Tracking Border Title
                    trackingBorder.setTitle("Tracking View");
                    trackingBorder.setTitleColor(new Color(255,255,255));
                    trackingPanel.repaint();
                    //Enable buttons
                    recCameraButton.setEnabled(true);
                    recAllButton.setEnabled(true);
                }

                //Cancel
                else if(answer == 2) {
                    System.out.println("Cancel Clicked");
                }
            }

        });

        //Add buttons to bottom panel
        bottomPanel.add(settingsButton);
        bottomPanel.add(recCameraButton);
        bottomPanel.add(recAllButton);
        bottomPanel.add(recLogButton);

        //Add bottom panel
        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 1;
        c.weighty = .05;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.BOTH;
        frame.add(bottomPanel, c);

        //Display Frame
        frame.setVisible(true);

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


    private static class CameraFetcher implements Runnable {

        private final JLabel cameraFeed;
        private final VideoCapture camera;
        private final Timer timer;

        public CameraFetcher(JLabel cameraFeed, VideoCapture camera) {
            this.cameraFeed = cameraFeed;
            this.camera = camera;
            this.timer = new Timer();
        }

        private static BufferedImage cvt2bi(Mat frame) {
            // Select grayscale or color based on incoming frame
            int type = BufferedImage.TYPE_BYTE_GRAY;
            if (frame.channels() > 1) {
                type = BufferedImage.TYPE_3BYTE_BGR;
            }

            // Create buffer to store bytes
            int bufferSize = frame.channels() * frame.cols() * frame.rows();
            byte[] b = new byte[bufferSize];

            // Copy data from Mat into the BufferedImage
            frame.get(0, 0, b);
            BufferedImage image = new BufferedImage(frame.cols(), frame.rows(), type);
            final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
            System.arraycopy(b, 0, targetPixels, 0, b.length);
            return image;
        }

        @Override
        public void run() {
            // New task to run at frame rate specified in Settings file

            camera.set(Videoio.CAP_PROP_FRAME_WIDTH, cameraFeed.getWidth());
            camera.set(Videoio.CAP_PROP_FRAME_HEIGHT, cameraFeed.getHeight());

            TimerTask task = new TimerTask() {

                // Required objects for detections & display
                private static final Mat frame = new Mat();
                private static int currentFrame = 0;
                private static final OnnxRunner onnxRunner = new OnnxRunner();
                private static OnnxOutput onnxOutput;
                private static List<Detection> detections = new ArrayList<>();
                @Override
                public void run() {
                    // Run if the thread hasn't been interrupted, otherwise purge the timer's schedule
                    if(!Thread.currentThread().isInterrupted()) {

                        // Resize camera size to whatever the current feed window size is
//                        camera.set(Videoio.CAP_PROP_FRAME_WIDTH, cameraFeed.getWidth());
//                        camera.set(Videoio.CAP_PROP_FRAME_HEIGHT, cameraFeed.getHeight());
                        // Pull frame and run through Onnx
                        camera.read(frame);


                        // Every Nth frame, we run the object detection on it
                        if (++currentFrame % PROCESS_EVERY_NTH_FRAME == 0) {
                            new Thread(() -> {
                                onnxOutput = onnxRunner.runInference(frame);
                                detections = onnxOutput.getDetectionList();
                            }).start();
                            currentFrame = 0;
                        }

                        // Overlay the predictions, and display the frame on the label
                        ImageUtil.drawPredictions(frame, detections);
                        Imgproc.resize(frame, frame, new Size(cameraFeed.getWidth(), cameraFeed.getHeight()));
                        BufferedImage biFrame = cvt2bi(frame);
                        cameraFeed.setIcon(new ImageIcon(biFrame));
                    }
                    else {
                        timer.cancel();
                        timer.purge();
                    }
                }
            };
            // Set the task to execute every CAMERA_FRAME_RATE frames per second
            timer.schedule(task, 0, 1000 / CAMERA_FRAME_RATE);
        }
    }
}
