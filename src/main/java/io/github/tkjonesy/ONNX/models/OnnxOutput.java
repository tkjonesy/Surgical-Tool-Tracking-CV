package io.github.tkjonesy.ONNX.models;

import io.github.tkjonesy.ONNX.Detection;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class OnnxOutput {
    private List<Detection> detectionList;
}
