package io.github.tkjonesy.ONNX.enums;

// Log enum for errors, info, and success using Colors
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.awt.Color;

@Getter
@AllArgsConstructor
public enum LogEnum {
    ERROR(Color.RED),
    REMOVE(Color.RED),
    INFO(Color.YELLOW),
    SUCCESS(Color.GREEN),
    ADD(Color.GREEN),
    DEFAULT(Color.DARK_GRAY);

    private final Color color;
}
