package dev.anderle.attributemod.overlay;

import java.awt.*;

public abstract class HudOverlayElement extends OverlayElement {

    public HudOverlayElement(String name, long updateInterval, Color color) {
        super(name, updateInterval, color);
    }
}
