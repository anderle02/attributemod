package dev.anderle.attributemod.overlay;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.inventory.Slot;
import net.minecraftforge.client.event.GuiScreenEvent;

import java.awt.*;

/**
 * A base class for all overlays that work inside a ChestContainer.
 * Implement abstract methods to define functionality.
 */
public abstract class ChestOverlay extends Overlay {
    public static final int CHEST_GUI_WIDTH = 176;
    public static final int CHEST_GUI_HEIGHT = 220;
    public static final int SLOT_SIZE = 16;

    /** @see Overlay */
    public ChestOverlay(String name, long updateInterval, Color color) {
        super(name, updateInterval, color);
    }

    public abstract void onGuiOpen(GuiChest chest);
    /** Called when the user clicks inside the overlay. */
    public abstract void onClickOverlay(GuiScreenEvent.MouseInputEvent e, double mouseX, double mouseY);
    /** Called when the user clicks a container or inventory slot. */
    public abstract void onClickSlot(GuiScreenEvent.MouseInputEvent e, Slot slot);
    /** Called when the user hovers inside the overlay. */
    public abstract void onHover(GuiChest chest, double mouseX, double mouseY);
    /** Called when the user scrolls inside the overlay. Direction = false for up, true for down. */
    public abstract void onScroll(GuiChest chest, boolean direction);
    /** Called every GuiScreenEvent.DrawScreenEvent. Used for highlighting chest slots (so it doesn't show above the item). */
    public abstract void onDrawForeground(GuiChest chest);

    public void highlightSlot(Slot slot, GuiScreen screen) {
        Point slotPos = new Point(
                (screen.width - CHEST_GUI_WIDTH) / 2 + slot.xDisplayPosition,
                (screen.height - CHEST_GUI_HEIGHT) / 2 + slot.yDisplayPosition - 1);
        GuiChest.drawRect(slotPos.x, slotPos.y,
                slotPos.x + SLOT_SIZE, slotPos.y + SLOT_SIZE,
                0xff00aa00);
    }
}
