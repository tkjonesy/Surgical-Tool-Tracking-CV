package io.github.tkjonesy.frontend.models.cameraGrabber;

import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.OpenCVFrameGrabber;
import org.bytedeco.opencv.opencv_videoio.VideoCapture;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MacOSCameraGrabber extends CameraGrabber {


    /**
     * Gets camera names from system_profiler (MacOS only).
     */
    @Override
    public List<String> getSystemProfilerCameraNames() {

        List<String> cameraNames = new ArrayList<>();
        try {
            Process process = new ProcessBuilder("system_profiler", "SPCameraDataType").start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("Model ID:")) {
                    cameraNames.add(line.replace("Model ID:", "").trim());
                }
            }
            process.waitFor();
        } catch (Exception e) {
            System.err.println("Error retrieving camera names: " + e.getMessage());
        }
        return cameraNames;
    }

}
