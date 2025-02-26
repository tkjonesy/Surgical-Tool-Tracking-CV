package io.github.tkjonesy.frontend.mainGUI;

import io.github.tkjonesy.frontend.App;
import io.github.tkjonesy.frontend.settingsGUI.SettingsWindow;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;

@Getter
public class ButtonPanel extends JPanel {
    private final App parent;
    private JToggleButton startSessionButton;
    private JButton settingsButton;
    private static final Color OCEAN = new Color(55, 90, 129);

    public ButtonPanel(App parent) {
        this.parent = parent;
        initializeComponents();
        initializeListeners();
        setupLayout();
    }

    private void initializeComponents() {
        startSessionButton = new JToggleButton("Start Session");
        startSessionButton.setBackground(OCEAN);
        settingsButton = new JButton("Settings");
    }

    private void initializeListeners() {
//        settingsButton.addActionListener(e -> SwingUtilities.invokeLater(() -> new SettingsWindow(parent)));
    }

    private void setupLayout() {
        GroupLayout layout = new GroupLayout(this);
        layout.setAutoCreateContainerGaps(true);
        layout.setHorizontalGroup(
                layout.createSequentialGroup()
                        .addComponent(startSessionButton)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(settingsButton)
        );
        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addGroup(
                                layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                        .addComponent(startSessionButton)
                                        .addComponent(settingsButton)
                        )
        );
        setLayout(layout);
    }
}
