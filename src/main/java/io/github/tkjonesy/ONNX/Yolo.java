package io.github.tkjonesy.ONNX;

import ai.onnxruntime.*;

import io.github.tkjonesy.frontend.App;
import io.github.tkjonesy.utils.settings.ProgramSettings;
import lombok.Getter;
import org.bytedeco.opencv.opencv_core.Mat;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public abstract class Yolo {

    @Getter
    private static boolean isCudaAvailable = false;

    public OnnxJavaType inputType;
    protected final OrtEnvironment env;
    protected OrtSession session;
    protected final String inputName;
    public ArrayList<String> labelNames;

    OnnxTensor inputTensor;

    // Yolo constructor, taking in the modelPath, file with labels, confidence threshold, non-maximum suppression threshold, and GPU device ID
    // gpuDevice omitted from this example
    public Yolo(String modelPath, String labelPath) throws OrtException, IOException {

        // Create the Onnx Runtime Environment and Session
        this.env = OrtEnvironment.getEnvironment();

        EnumSet<OrtProvider> availableProviders = OrtEnvironment.getAvailableProviders();

        System.out.println("Available providers: " + availableProviders);

        var sessionOptions = new OrtSession.SessionOptions();

        boolean useGPU = availableProviders.contains(OrtProvider.CUDA) && ProgramSettings.getCurrentSettings().isUseGPU();
        if (useGPU) {
            System.out.println("CUDA is available and useGPU is true, attempting to use GPU for inference");
            sessionOptions = createSessionOptions(true);
        } else {
            System.out.println("Using CPU for inference");
            sessionOptions = createSessionOptions(false);
        }

        try {
            // Set the session (this is where failure will occur if CUDA is not working)
            this.session = this.env.createSession(modelPath, sessionOptions);
            isCudaAvailable = true;
        } catch (OrtException e) {
            JOptionPane.showMessageDialog(App.getInstance(), "Failed to create session with GPU, falling back to CPU: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            sessionOptions = createSessionOptions(false);
            this.session = this.env.createSession(modelPath, sessionOptions);
            isCudaAvailable = false;
        }

        if (System.getProperty("os.name").toLowerCase().contains("mac")) {
            isCudaAvailable = false;
        }

        // Get the input information
        Map<String, NodeInfo> inputMetaMap = this.session.getInputInfo();
        this.inputName = this.session.getInputNames().iterator().next();
        NodeInfo inputMeta = inputMetaMap.get(this.inputName);
        this.inputType = ((TensorInfo) inputMeta.getInfo()).type;


        // Use a buffered reader to read the labels from the file
        BufferedReader br = new BufferedReader(new FileReader(labelPath));
        String line;
        this.labelNames = new ArrayList<>();
        while ((line = br.readLine()) != null) {
            this.labelNames.add(line);
        }
    }

    private OrtSession.SessionOptions createSessionOptions(boolean useGPU) throws OrtException {
        OrtSession.SessionOptions sessionOptions = new OrtSession.SessionOptions();
        sessionOptions.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT);
        if (useGPU) {
            sessionOptions.addCUDA(ProgramSettings.getCurrentSettings().getGpuDeviceId());
        } else {
            sessionOptions.addCPU(true);
            sessionOptions.setInterOpNumThreads(1);
            sessionOptions.setIntraOpNumThreads(1);
        }
        return sessionOptions;
    }

    public abstract List<Detection> run(Mat img) throws OrtException;

    // Compute the Intersection over Union (IoU) of two bounding boxes
    private float computeIOU(float[] box1, float[] box2) {

        float area1 = (box1[2] - box1[0]) * (box1[3] - box1[1]);
        float area2 = (box2[2] - box2[0]) * (box2[3] - box2[1]);

        float left = Math.max(box1[0], box2[0]);
        float top = Math.max(box1[1], box2[1]);
        float right = Math.min(box1[2], box2[2]);
        float bottom = Math.min(box1[3], box2[3]);

        float interArea = Math.max(right - left, 0) * Math.max(bottom - top, 0);
        float unionArea = area1 + area2 - interArea;
        return Math.max(interArea / unionArea, 1e-8f);
    }

    protected List<float[]> nonMaxSuppression(List<float[]> bboxes) {

        // output boxes
        List<float[]> bestBboxes = new ArrayList<>();

        // confidence
        bboxes.sort(Comparator.comparing(a -> a[4]));

        // standard nms
        while (!bboxes.isEmpty()) {
//            float[] bestBbox = bboxes.removeLast();
            float [] bestBbox = bboxes.remove(bboxes.size() - 1);
            bestBboxes.add(bestBbox);
            bboxes = bboxes.stream().filter(a -> computeIOU(a, bestBbox) < ProgramSettings.getCurrentSettings().getNmsThreshold()).collect(Collectors.toList());
        }

        return bestBboxes;
    }

    protected void xywh2xyxy(float[] bbox) {
        float x = bbox[0];
        float y = bbox[1];
        float w = bbox[2];
        float h = bbox[3];

        bbox[0] = x - w * 0.5f;
        bbox[1] = y - h * 0.5f;
        bbox[2] = x + w * 0.5f;
        bbox[3] = y + h * 0.5f;
    }

    // Scale the coordinates of the bounding box
    protected void scaleCoords(float[] bbox, float orgW, float orgH, float padW, float padH, float gain) {
        // xmin, ymin, xmax, ymax -> (xmin_org, ymin_org, xmax_org, ymax_org)
        bbox[0] = Math.max(0, Math.min(orgW - 1, (bbox[0] - padW) / gain));
        bbox[1] = Math.max(0, Math.min(orgH - 1, (bbox[1] - padH) / gain));
        bbox[2] = Math.max(0, Math.min(orgW - 1, (bbox[2] - padW) / gain));
        bbox[3] = Math.max(0, Math.min(orgH - 1, (bbox[3] - padH) / gain));
    }

    // Find the index of the maximum value in an array
    static int argmax(float[] a) {
        float re = -Float.MAX_VALUE;
        int arg = -1;
        for (int i = 0; i < a.length; i++) {
            if (a[i] >= re) {
                re = a[i];
                arg = i;
            }
        }
        return arg;
    }
}