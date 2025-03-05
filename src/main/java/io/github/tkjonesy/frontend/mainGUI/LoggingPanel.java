package io.github.tkjonesy.frontend.mainGUI;

import io.github.tkjonesy.frontend.App;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;

public class LoggingPanel extends JPanel {
    private static final Color CHARCOAL = new Color(30, 31, 34);

    private final App appInstance;

    @Getter
    private JTextPane logTextPane;

    public LoggingPanel(App appInstance) {
        this.appInstance = appInstance;
        this.setBorder(BorderFactory.createTitledBorder("Tracking Log"));
        this.logTextPane = new JTextPane();
        this.logTextPane.setEditable(false);
        this.logTextPane.setContentType("text/html");
        this.logTextPane.setBackground(CHARCOAL);

        JScrollPane scrollPane = new JScrollPane(this.logTextPane);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        GroupLayout layout = new GroupLayout(this);
        layout.setAutoCreateContainerGaps(true);
        layout.setHorizontalGroup(
                layout.createSequentialGroup()
                        .addComponent(scrollPane)
        );
        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addComponent(scrollPane)
        );
        this.setLayout(layout);
    }
}
