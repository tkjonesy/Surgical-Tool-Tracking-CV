package io.github.tkjonesy.frontend.models;

import lombok.Getter;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

public class SessionInputDialog extends JDialog {
    private final JTextField titleField;
    private final JTextArea descriptionArea;
    @Getter
    private boolean confirmed;

    public SessionInputDialog(Frame parent) {
        super(parent, "New Session", true);
        setSize(400, 300);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 10, 5, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 1.0;

        // Title Label
        gbc.gridx = 0;
        gbc.gridy = 0;
        inputPanel.add(new JLabel("Title"), gbc);

        // Title Input Box with Placeholder
        titleField = new JTextField();
        titleField.setPreferredSize(new Dimension(350, 30));
        setPlaceholder(titleField, "Enter session title...");
        gbc.gridy = 1;
        inputPanel.add(titleField, gbc);

        // Description Label
        gbc.gridy = 2;
        inputPanel.add(new JLabel("Description"), gbc);

        // Description Input Box with Placeholder
        descriptionArea = new JTextArea(5, 30);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setLineWrap(true);
        JScrollPane scrollPane = new JScrollPane(descriptionArea);
        scrollPane.setPreferredSize(new Dimension(350, 100));
        setPlaceholder(descriptionArea, "Describe your session here...");
        gbc.gridy = 3;
        inputPanel.add(scrollPane, gbc);

        add(inputPanel, BorderLayout.CENTER);

        // Buttons Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton confirmButton = new JButton("Start");
        JButton cancelButton = new JButton("Cancel");

        confirmButton.addActionListener(e -> {
            confirmed = true;
            setVisible(false);
        });

        cancelButton.addActionListener(e -> {
            confirmed = false;
            setVisible(false);
        });

        buttonPanel.add(cancelButton);
        buttonPanel.add(confirmButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    public String getSessionTitle() {
        if(titleField.getForeground().equals(Color.GRAY)){
            return "";
        }
        return titleField.getText().trim();
    }

    public String getSessionDescription() {
        if(descriptionArea.getForeground().equals(Color.GRAY)){
            return "";
        }
        String desc = descriptionArea.getText().trim();
        return !desc.isEmpty() ? desc : "No description provided.";
    }

    private void setPlaceholder(JTextComponent component, String placeholder) {
        component.setForeground(Color.GRAY);
        component.setText(placeholder);

        component.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                if (component.getText().equals(placeholder)) {
                    component.setText("");
                    component.setForeground(Color.WHITE);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (component.getText().trim().isEmpty()) {
                    component.setText(placeholder);
                    component.setForeground(Color.GRAY);
                }
            }
        });
    }
}
