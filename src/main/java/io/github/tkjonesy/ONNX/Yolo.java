package io.github.tkjonesy.ONNX;

import ai.onnxruntime.NodeInfo;
import ai.onnxruntime.OnnxJavaType;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.TensorInfo;
import org.opencv.core.Mat;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.github.tkjonesy.ONNX.settings.Settings.nmsThreshold;

public abstract class Yolo {

    public OnnxJavaType inputType;
    protected final OrtEnvironment env;
    protected final OrtSession session;
    protected final String inputName;
    public ArrayList<String> labelNames;

    OnnxTensor inputTensor;

    // Yolo constructor, taking in the modelPath, file with labels, confidence threshold, non-maximum suppression threshold, and GPU device ID
    // gpuDevice omitted from this example
    public Yolo(String modelPath, String labelPath) throws OrtException, IOException {

        // Create the Onnx Runtime Environment and Session
        this.env = OrtEnvironment.getEnvironment();
        var sessionOptions = new OrtSession.SessionOptions();
        sessionOptions.addCPU(false);
        sessionOptions.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT);

        // Set the session
        this.session = this.env.createSession(modelPath, sessionOptions);

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
            float[] bestBbox = bboxes.removeLast();
            bestBboxes.add(bestBbox);
            bboxes = bboxes.stream().filter(a -> computeIOU(a, bestBbox) < nmsThreshold).collect(Collectors.toList());
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