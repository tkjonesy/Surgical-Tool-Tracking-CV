package io.github.tkjonesy;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.TimerTask;
import javax.swing.LayoutStyle.ComponentPlacement;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;


public class App extends JFrame {

    // Compulsory OpenCV loading
    static{ System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }

    private JPanel cameraPanel, trackingPanel, bottomPanel;
    private JToggleButton recCameraButton, recAllButton, recLogButton;
    private JButton settingsButton;
    private JLabel cameraFeed;

    private JTextPane logTextPane;
    private GroupLayout layout, cameraPanelLayout, trackingPanelLayout, bottomPanelLayout;


    public App() {
        initComponents();
        initListeners();
        this.setVisible(true);
    }

    private void initComponents() {

        // Titling, sizing, and exit actions
        this.setTitle("Surgical Tool Tracker");
        this.setMinimumSize(new Dimension(746, 401));
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);

        // Camera Panel
        cameraPanel = new JPanel();
        cameraPanel.setBorder(BorderFactory.createTitledBorder("Camera"));

        cameraFeed = new JLabel("");
        cameraFeed.setMinimumSize(new Dimension(320, 240));

        cameraPanelLayout = new GroupLayout(cameraPanel);
        cameraPanelLayout.setHorizontalGroup(
                cameraPanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(cameraFeed)
                        .addContainerGap()
        );
        cameraPanelLayout.setVerticalGroup(
                cameraPanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(cameraFeed)
                        .addContainerGap()
        );
        cameraPanel.setLayout(cameraPanelLayout);

        // Tracking Panel
        trackingPanel = new JPanel();
        trackingPanel.setBorder(BorderFactory.createTitledBorder("Log"));
        trackingPanel.setMaximumSize(new Dimension(1920 / 2, 1080)); //TODO possibly poll for system screen width

        logTextPane = new JTextPane();
        logTextPane.setMinimumSize(new Dimension(320, 240));

        trackingPanelLayout = new GroupLayout(trackingPanel);
        trackingPanelLayout.setHorizontalGroup(
                trackingPanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(logTextPane)
                        .addContainerGap()
        );
        trackingPanelLayout.setVerticalGroup(
                trackingPanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(logTextPane)
                        .addContainerGap()
        );
        trackingPanel.setLayout(trackingPanelLayout);

        // Bottom Button Panel
        bottomPanel = new JPanel();

        recCameraButton = new JToggleButton("Start Camera");
        recAllButton = new JToggleButton("Start All");
        recLogButton = new JToggleButton("Start Log");
        settingsButton = new JButton("Settings");

        bottomPanelLayout = new GroupLayout(bottomPanel);
        bottomPanelLayout.setHorizontalGroup(
                bottomPanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(recCameraButton)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(recAllButton)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(recLogButton)
                        .addPreferredGap(ComponentPlacement.UNRELATED)
                        .addComponent(settingsButton)
                        .addContainerGap()
        );
        bottomPanelLayout.setVerticalGroup(
                bottomPanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(
                                bottomPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                        .addComponent(recCameraButton)
                                        .addComponent(recAllButton)
                                        .addComponent(recLogButton)
                                        .addComponent(settingsButton)
                        )
                        .addContainerGap()
        );
        bottomPanel.setLayout(bottomPanelLayout);

        // Window Layout
        // TODO MAKE THE TRACKING PANEL STICKY TO THE RIGHT SIDE
        layout = new GroupLayout(this.getContentPane());
        layout.setHorizontalGroup(
                layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(
                                layout.createParallelGroup()
                                        .addGroup(
                                                layout.createSequentialGroup()
                                                        .addComponent(cameraPanel)
                                                        .addPreferredGap(ComponentPlacement.RELATED)
                                                        .addComponent(trackingPanel)
                                        )
                                        .addComponent(bottomPanel)
                        )
                        .addContainerGap()
        );
        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(
                                layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                        .addComponent(cameraPanel)
                                        .addComponent(trackingPanel)
                        )
                        .addPreferredGap(ComponentPlacement.UNRELATED)
                        .addComponent(bottomPanel)
                        .addContainerGap()
        );
        this.setLayout(layout);
        this.pack();
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
    }

    private void setRecButtons(boolean camEnable, boolean allEnable, boolean logEnable) {
        recCameraButton.setEnabled(camEnable);
        recAllButton.setEnabled(allEnable);
        recLogButton.setEnabled(logEnable);
    }

    private JLabel getCameraFeed() {
        return this.cameraFeed;
    }

    public static void main(String[] args) {

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

                //No: Dont Save Data
                else if(answer == 1) {
                    System.out.println("Dont Save Camera Recording Clicked\n");
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

                //No: Dont Save Data
                else if(answer == 1) {
                    System.out.println("Dont Save All Recording Clicked\n");
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

                //No: Dont Save Data
                else if(answer == 1) {
                    System.out.println("Dont Save Log Recording Clicked\n");
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
            e.printStackTrace();
        }

        // Create new instant of STT app
        App sttApp = new App();

        CameraFetcher camFetch = new CameraFetcher(sttApp.getCameraFeed());
        new Thread(camFetch).start();


        // OpenCV create camera

//        SwingUtilities.invokeLater(
//                () -> {
//                    // Invoke new instance of STT App
//                    App sttApp = new App();
//                }
//        );
    }


    private static class CameraFetcher implements Runnable {

        private final JLabel cameraFeed;

        public CameraFetcher(JLabel cameraFeed) {
            this.cameraFeed = cameraFeed;
        }

        private BufferedImage cvt2bi(Mat frame)
        {
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

        public void run() {
            // TODO this needs to eventually be a websocket receiver, or just get the camera frame from elsewhere

            final long FRAMES_PER_SECOND = 60;

            VideoCapture camera = new VideoCapture(0);
            if(!camera.isOpened()) {
                System.err.println("Error: Camera could not be opened. Exiting...");
                System.exit(0);
            }

            Mat frame = new Mat();
            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    // Get the frame and write to BufferedImage, write to label
                    camera.read(frame);
                    Dimension cfSize = cameraFeed.getSize();
                    Imgproc.resize(frame, frame, new Size(cfSize.getWidth(), cfSize.getHeight()));
                    BufferedImage biFrame = cvt2bi(frame);
                    cameraFeed.setIcon(new ImageIcon(biFrame));
                }
            };

            java.util.Timer t = new java.util.Timer();
            t.schedule(task, 0, 1000 / FRAMES_PER_SECOND);
        }
    }
}
