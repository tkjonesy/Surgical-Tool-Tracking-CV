package io.github.tkjonesy.ONNX;

import ai.onnxruntime.OnnxJavaType;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.FloatPointer;
import org.bytedeco.opencv.opencv_core.Mat;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.github.tkjonesy.ONNX.settings.Settings.INPUT_SHAPE;
import static io.github.tkjonesy.ONNX.settings.Settings.INPUT_SIZE;
import static io.github.tkjonesy.ONNX.settings.Settings.NUM_INPUT_ELEMENTS;
import static io.github.tkjonesy.ONNX.settings.Settings.confThreshold;

import static org.bytedeco.opencv.global.opencv_imgproc.cvtColor;
import static org.bytedeco.opencv.global.opencv_imgproc.COLOR_BGR2RGB;
import static org.bytedeco.opencv.global.opencv_core.CV_32F;

public class YoloV8 extends Yolo {

    public YoloV8(String modelPath, String labelPath) throws OrtException, IOException {
        super(modelPath, labelPath);
    }

    // Run inference on the image
    public List<Detection> run(Mat img) throws OrtException {

        float orgW = (float) img.size().width();
        float orgH = (float) img.size().height();

        float gain = Math.min((float) INPUT_SIZE / orgW, (float) INPUT_SIZE / orgH);
        float padW = (INPUT_SIZE - orgW * gain) * 0.5f;
        float padH = (INPUT_SIZE - orgH * gain) * 0.5f;

        // preprocessing
        Map<String, OnnxTensor> inputContainer = this.preprocess(img);

        // Run inference
        float[][] predictions;

        OrtSession.Result results = this.session.run(inputContainer);
        predictions = ((float[][][]) results.get(0).getValue())[0];

        // postprocessing
        return postprocess(predictions, orgW, orgH, padW, padH, gain);
    }

    // Preprocess the image. Returns a map of the input tensor name to the input tensor
    public Map<String, OnnxTensor> preprocess(Mat img) throws OrtException {

        // Resizing with padding
        Mat resizedImg = new Mat();
        ImageUtil.resizeWithPadding(img, resizedImg, INPUT_SIZE, INPUT_SIZE);

        // BGR -> RGB
        cvtColor(resizedImg, resizedImg, COLOR_BGR2RGB);

        // Create input tensor container
        Map<String, OnnxTensor> container = new HashMap<>();

        if (this.inputType.equals(OnnxJavaType.UINT8)) {
            byte[] whc = new byte[NUM_INPUT_ELEMENTS];
            BytePointer bp = resizedImg.data();
            bp.get(whc);

            // Reorder W-H-C to C-W-H
            byte[] chw = ImageUtil.whc2cwh(whc);
            ByteBuffer inputBuffer = ByteBuffer.wrap(chw);
            inputTensor = OnnxTensor.createTensor(this.env, inputBuffer, INPUT_SHAPE, this.inputType);

        } else {
            resizedImg.convertTo(resizedImg, CV_32F, 1.0 / 255.0 , 0);
            float[] whc = new float[NUM_INPUT_ELEMENTS];
            FloatPointer fp = new FloatPointer(resizedImg.data());
            fp.get(whc);

            // Reorder W-H-C to C-W-H
            float[] chw = ImageUtil.whc2cwh(whc);

            // Wrap in FloatBuffer for ONNX
            FloatBuffer inputBuffer = FloatBuffer.wrap(chw);
            inputTensor = OnnxTensor.createTensor(this.env, inputBuffer, INPUT_SHAPE);
        }

        // Add the tensor to the container
        container.put(this.inputName, inputTensor);

        return container;
    }

    public List<Detection> postprocess(float[][] outputs, float orgW, float orgH, float padW, float padH, float gain) {

        // predictions
        outputs = transposeMatrix(outputs);
        Map<Integer, List<float[]>> class2Bbox = new HashMap<>();

        for (float[] bbox : outputs) {


            float[] conditionalProbabilities = Arrays.copyOfRange(bbox, 4, 84);
            int label = argmax(conditionalProbabilities);
            float conf = conditionalProbabilities[label];
            if (conf < confThreshold) continue;

            bbox[4] = conf;

            // xywh to (x1, y1, x2, y2)
            xywh2xyxy(bbox);

            // skip invalid predictions
            if (bbox[0] >= bbox[2] || bbox[1] >= bbox[3]) continue;

            // xmin, ymin, xmax, ymax -> (xmin_org, ymin_org, xmax_org, ymax_org)
            scaleCoords(bbox, orgW, orgH, padW, padH, gain);
            class2Bbox.putIfAbsent(label, new ArrayList<>());
            class2Bbox.get(label).add(bbox);
        }

        // Apply Non-max suppression for each class
        List<Detection> detections = new ArrayList<>();
        for (Map.Entry<Integer, List<float[]>> entry : class2Bbox.entrySet()) {
            int label = entry.getKey();
            List<float[]> bboxes = entry.getValue();
            bboxes = nonMaxSuppression(bboxes);
            for (float[] bbox : bboxes) {
                String labelString = this.labelNames.get(label);
                detections.add(new Detection(labelString, Arrays.copyOfRange(bbox, 0, 4), bbox[4]));
            }
        }

        return detections;
    }

    public static float[][] transposeMatrix(float [][] m){
        float[][] temp = new float[m[0].length][m.length];
        for (int i = 0; i < m.length; i++)
            for (int j = 0; j < m[0].length; j++)
                temp[j][i] = m[i][j];
        return temp;
    }
}