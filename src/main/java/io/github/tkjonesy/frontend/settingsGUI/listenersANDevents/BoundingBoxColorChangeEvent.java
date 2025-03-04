package io.github.tkjonesy.frontend.settingsGUI.listenersANDevents;

import lombok.Getter;

/**
 * @param newColor [r, g, b]
 */
public record BoundingBoxColorChangeEvent(int[] newColor) {

}