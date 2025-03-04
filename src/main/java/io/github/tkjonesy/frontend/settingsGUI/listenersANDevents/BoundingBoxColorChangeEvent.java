package io.github.tkjonesy.frontend.settingsGUI.listenersANDevents;

/**
 * @param newColor [r, g, b]
 */
public record BoundingBoxColorChangeEvent(int[] newColor) {}