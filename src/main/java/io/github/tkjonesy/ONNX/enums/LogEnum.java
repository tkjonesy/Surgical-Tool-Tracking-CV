package io.github.tkjonesy.ONNX.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.awt.Color;

/**
 * The {@code LogEnum} enum represents different log levels, each associated with a specific color.
 * It is used to classify log entries by type, such as errors, informational messages, and successes.
 */
@Getter
@AllArgsConstructor
public enum LogEnum {

    /** Log type for error messages, displayed in red. */
    ERROR(Color.RED),

    /** Log type for informational messages, displayed in yellow. */
    INFO(Color.YELLOW),

    /** Log type for success messages, displayed in green. */
    SUCCESS(Color.GREEN),

    /** Default log type, displayed in dark gray. */
    DEFAULT(Color.DARK_GRAY);

    /** The color associated with the log type. */
    private final Color color;
}
