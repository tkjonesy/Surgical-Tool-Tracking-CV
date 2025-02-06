package io.github.tkjonesy.ONNX.models;

import io.github.tkjonesy.ONNX.Detection;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * The {@code OnnxOutput} class represents the output of the ONNX model,
 * containing a list of detected objects.
 */
@Getter
@AllArgsConstructor
public class OnnxOutput {

    /** The list of detections produced by the ONNX model. Each detection represents
     * an identified object along with its associated data (e.g., label, confidence).
     */
    private List<Detection> detectionList;
}