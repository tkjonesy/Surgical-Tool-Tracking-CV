package io.github.tkjonesy;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class Main {
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
    }
}
