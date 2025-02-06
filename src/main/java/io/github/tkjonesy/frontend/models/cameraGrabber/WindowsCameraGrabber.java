package io.github.tkjonesy.frontend.models.cameraGrabber;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class WindowsCameraGrabber extends CameraGrabber {

    /**
     * Gets camera names from WMIC (Windows only).
     */
    @Override
    public List<String> getSystemProfilerCameraNames() {
        List<String> cameraNames = new ArrayList<>();
        try {
            Process process = new ProcessBuilder("wmic", "path", "Win32_PnPEntity", "get", "Name").start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                // Filters only camera-related devices
                if (line.toLowerCase().contains("camera") || line.toLowerCase().contains("webcam") || line.toLowerCase().contains("usb video device")) {
                    cameraNames.add(line);
                }
            }
            process.waitFor();
        } catch (Exception e) {
            System.err.println("Error retrieving camera names: " + e.getMessage());
        }
        return cameraNames;
    }
}
