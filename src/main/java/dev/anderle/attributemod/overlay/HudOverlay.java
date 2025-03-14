package dev.anderle.attributemod.overlay;

import java.awt.*;

public abstract class HudOverlay extends Overlay {

    public HudOverlay(String name, long updateInterval, Color color) {
        super(name, updateInterval, color);
    }
}
