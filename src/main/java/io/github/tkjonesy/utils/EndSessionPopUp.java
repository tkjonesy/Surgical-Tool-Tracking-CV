package io.github.tkjonesy.utils;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;

/**
 * Utility class to display a popup at the end of a session,
 * prompting the user to open the sessions directory.
 */
public class EndSessionPopUp {
    /**
     * Displays a confirmation popup asking the user if they want to open the specific session directory.
     * @param sessionTitle The title of the session, used to locate the session folder.
     */
    public static void showSessionEndDialog(String sessionTitle) {
        SwingUtilities.invokeLater(() -> {
            Object[] options = {"Cancel",
                    "Open session folder"};;
            int choice = JOptionPane.showOptionDialog(null,
                    "Would you like to open the session folder?",
                    "Session Ended",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[1]
            );

            if (choice == JOptionPane.NO_OPTION) {
                openSessionDirectory(sessionTitle);
            }
        });
    }

    /**
     * Opens the specific session directory based on the session title.
     * @param sessionTitle The title of the session.
     */
    private static void openSessionDirectory(String sessionTitle) {
        File directory = new File(sessionTitle);
        System.out.println("Attempting to open: " + directory.getAbsolutePath());

        if (!directory.exists() || !directory.isDirectory()) {
            JOptionPane.showMessageDialog(null, "Error: The session folder does not exist: " + directory.getAbsolutePath(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            Desktop.getDesktop().open(directory);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Error: Failed to open the session folder: " + directory.getAbsolutePath(), "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
}
