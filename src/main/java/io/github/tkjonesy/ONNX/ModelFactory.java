package io.github.tkjonesy.ONNX;

import ai.onnxruntime.OrtException;
import io.github.tkjonesy.ONNX.settings.Settings;

import java.io.IOException;

public class ModelFactory {

    public Yolo getModel() throws IOException, OrtException, NotImplementedException {
        return new YoloV8(Settings.modelPath, Settings.labelPath);
    }
}