package io.github.tkjonesy.frontend.models.cameraGrabber;

import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.OpenCVFrameGrabber;
import org.bytedeco.opencv.opencv_videoio.VideoCapture;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class CameraGrabber {

    public HashMap<String, Integer> getCameraNames() {
        List<Integer> cameraIndices = new ArrayList<>(getAvailableCameraIndices(10).values());
        return getCameraNamesFromIndices(cameraIndices);
    }

    private HashMap<String, Integer> getCameraNamesFromIndices(List<Integer> cameraIndices) {
        List<String> cameraNames = getSystemProfilerCameraNames();
        HashMap<String, Integer> cameraMap = new HashMap<>();

        // Map OpenCV indices to camera names
        for (int i = 0; i < cameraIndices.size(); i++) {
            int index = cameraIndices.get(i);
            String cameraName = (i < cameraNames.size()) ? cameraNames.get(i) : "Unknown Camera " + index;
            cameraMap.put(cameraName, index);
        }
        return cameraMap;
    }

    /**
     * Returns a HashMap mapping camera names to their corresponding OpenCV indices.
     */
    private HashMap<String, Integer> getAvailableCameraIndices(int maxIndex) {
        HashMap<String, Integer> validIndexes = new HashMap<>();
        System.out.println("Searching for cameras...");

        for (int i = 0; i < maxIndex; i++) {

            if (!isCameraPresentQuickly(i)) {
                continue;
            }

            try (FrameGrabber grabber = new OpenCVFrameGrabber(i)) {
                grabber.setTimeout(1000);
                grabber.start();
                grabber.stop();
                validIndexes.put("Camera " + i, i);
            } catch (FrameGrabber.Exception e) {
                System.out.println("Camera index " + i + " failed to initialize.");
            }
        }
        return validIndexes;
    }

    private boolean isCameraPresentQuickly(int index) {
        VideoCapture capture = new VideoCapture(index);
        boolean isOpened = capture.isOpened();
        capture.release();
        return isOpened;
    }


    abstract List<String> getSystemProfilerCameraNames();
}
