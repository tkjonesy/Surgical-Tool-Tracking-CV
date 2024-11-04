package io.github.tkjonesy.ONNX;

public record Detection(String label, float[] bbox, float confidence) {}