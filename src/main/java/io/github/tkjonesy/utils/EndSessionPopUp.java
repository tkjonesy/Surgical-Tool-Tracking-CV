package io.github.tkjonesy.utils;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

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
            Object[] options = {"Open session folder",
                    "Cancel"};
            String miniAAR = generateMiniAAR(sessionTitle);
            int choice = JOptionPane.showOptionDialog(null,
                    miniAAR,
                    "Session Ended",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[1]
            );

            if (choice == JOptionPane.YES_OPTION) {
                openSessionDirectory(sessionTitle);
            }
        });
    }

    /**
     * Opens the specific session directory based on the session title.
     * @param sessionTitle The full path of the session directory.
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

    /**
     * Generates a mini AAR summary from the AAR.txt file inside the session directory.
     * @param sessionTitle The full path of the session directory.
     * @return A short summary of the AAR file.
     */
    private static String generateMiniAAR(String sessionTitle) {
        String aarPath = sessionTitle + "/AAR.txt";
        File aarFile = new File(aarPath);
        if (!aarFile.exists()) {
            return "(No AAR available)";
        }

        try {
            List<String> lines = Files.readAllLines(Paths.get(aarPath));
            StringBuilder miniAAR = new StringBuilder();
            boolean captureSection = false;

            for (String line : lines) {
                if (line.contains("Session Name") || line.contains("Recording Duration") || line.contains("Peak Objects Seen at Once")) {
                    miniAAR.append(line).append("\n");
                }

                // Capture sections dynamically and include their content
                if (line.contains("Objects Present at End") ||
                        line.contains("Total Instances of Each Tool Ever Added") ||
                        line.contains("Objects Removed During Session")) {
                    captureSection = true;
                    miniAAR.append("\n").append(line).append("\n");
                    continue;
                }

                if (captureSection) {
                    if (line.trim().isEmpty()) {
                        captureSection = false;
                    } else {
                        miniAAR.append(line).append("\n");
                    }
                }
            }

            if (miniAAR.toString().trim().isEmpty()) {
                return "(No relevant data found in AAR)";
            }
            return miniAAR.toString().trim();
        } catch (IOException e) {
            return "(Failed to read AAR)";
        }
    }
}
