package io.github.tkjonesy.frontend.mainGUI;

import io.github.tkjonesy.frontend.App;
import io.github.tkjonesy.frontend.models.SessionInputDialog;
import io.github.tkjonesy.frontend.settingsGUI.SettingsWindow;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;

@Getter
public class ButtonPanel extends JPanel {
    private final App appInstance;
    private JToggleButton sessionButton;
    private JButton settingsButton;
    private static final Color OCEAN = new Color(55, 90, 129);
    private static final Color SUNSET = new Color(255, 40, 79);

    public ButtonPanel(App appInstance) {
        this.appInstance = appInstance;
        initializeComponents();
        initializeListeners();
        setupLayout();
    }

    private void initializeComponents() {
        sessionButton = new JToggleButton("Start Session");
        sessionButton.setBackground(OCEAN);
        settingsButton = new JButton("Settings");
    }

    private void initializeListeners() {

        sessionButton.addActionListener(
                e -> {
                    if (sessionButton.getText().equals("Start Session")) {
                        // Open Session input dialog
                        SessionInputDialog dialog = new SessionInputDialog(appInstance);
                        dialog.pack();
                        dialog.setVisible(true);

                        // Check if dialog was confirmed
                        if (dialog.isConfirmed()) {
                            String sessionTitle = dialog.getSessionTitle();
                            String sessionDescription = dialog.getSessionDescription();

                            // Ensure title and description are not empty
                            if (sessionTitle.isEmpty()) {
                                JOptionPane.showMessageDialog(appInstance,
                                        "Please fill in both fields.",
                                        "Invalid Input", JOptionPane.ERROR_MESSAGE);
                                return;
                            }

                            // Start new session
                            boolean sessionStarted = appInstance.getSessionHandler().startNewSession(sessionTitle, sessionDescription, App.getOnnxRunner());

                            // If session started successfully, update UI and begin logging
                            if (sessionStarted) {
                                sessionButton.setText("Stop Session");
                                sessionButton.setBackground(SUNSET);
                                settingsButton.setEnabled(false);
                            } else {
                                JOptionPane.showMessageDialog(appInstance,
                                        "Failed to start session. Please check the console for more information.",
                                        "Session Start Failed", JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    } else {
                        sessionButton.setText("Start Session");
                        sessionButton.setBackground(OCEAN);
                        settingsButton.setEnabled(true);
                        appInstance.getSessionHandler().endSession();
                    }
                }
        );

        settingsButton.addActionListener(e -> SwingUtilities.invokeLater(() -> new SettingsWindow(appInstance)));
    }

    private void setupLayout() {
        GroupLayout layout = new GroupLayout(this);
        layout.setAutoCreateContainerGaps(true);
        layout.setHorizontalGroup(
                layout.createSequentialGroup()
                        .addComponent(sessionButton)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(settingsButton)
        );
        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addGroup(
                                layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                        .addComponent(sessionButton)
                                        .addComponent(settingsButton)
                        )
        );
        setLayout(layout);
    }
}
