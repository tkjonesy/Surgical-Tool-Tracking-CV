package io.github.tkjonesy.frontend.mainGUI;

import io.github.tkjonesy.frontend.App;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;

@Getter
public class CameraPanel extends JPanel {
    private final App parent;
    private JLabel cameraFeed;

    public CameraPanel(LayoutManager layoutManager, App parent){
        super(layoutManager);
        this.parent = parent;
        initializeComponents();
        setupLayout();
    }

    private void initializeComponents() {
        cameraFeed = new JLabel("");
        cameraFeed.setMinimumSize(new Dimension(320, 240));
    }

    private void setupLayout() {
        this.add(cameraFeed, BorderLayout.CENTER);
    }
}
