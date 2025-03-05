package dev.anderle.attributemod.overlay;

import dev.anderle.attributemod.AttributeMod;
import dev.anderle.attributemod.utils.Helper;
import dev.anderle.attributemod.utils.ItemWithAttributes;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.Slot;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.GuiScreenEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.*;
import java.util.List;

public class ChestItemDisplay extends ChestOverlayElement {
    /** Pretty sure this is the longest possible item name. */
    public static final int MAX_WIDTH = AttributeMod.mc.fontRendererObj.getStringWidth(
            "888.8M  Vanquished Glowstone Gauntlet - [Arachno Resistance 10] [Blazing Resistance 10]");

    /** Stores which item is where, used to locate it when user interacts with the item list. */
    private final Map<Integer, Slot> itemSlotMapping = new HashMap<>();
    /** Index of the item string the user is currently hovering over. */
    private int hoveredItem = -1;
    private int scrollOffset = 0;

    public ChestItemDisplay() {
        super("Chest Overlay", 1000, Color.blue);
    }

    @Override
    public void onGuiOpen(GuiChest chest) {
        hoveredItem = -1;
        scrollOffset = 0;
        scheduleImmediateUpdate();
    }

    @Override
    public void onClick(GuiScreenEvent.MouseInputEvent e, double mouseX, double mouseY) {
        if(hoveredItem == -1) return;

        Slot slot = itemSlotMapping.get(hoveredItem);
        Mouse.setCursorPosition(
                ((e.gui.width - CHEST_GUI_WIDTH) / 2 + slot.xDisplayPosition + SLOT_SIZE / 2) * e.gui.mc.displayWidth / e.gui.width,
                (e.gui.height - ((e.gui.height - CHEST_GUI_HEIGHT) / 2 + slot.yDisplayPosition + SLOT_SIZE / 2 - 1)) * e.gui.mc.displayHeight / e.gui.height
        );

        if(!Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) && e.isCancelable()) e.setCanceled(true);
        else scheduleImmediateUpdate();

        hoveredItem = -1;
    }

    @Override
    public void onHover(GuiChest chest, double mouseX, double mouseY) {
        // Font height and width need to be scaled by the scale used for rendering.
        double scaledLineHeight = (chest.mc.fontRendererObj.FONT_HEIGHT + 1) * scale / 100;
        int itemIndex = (int) Math.floor((mouseY - position.y) / scaledLineHeight) - 1;

        if(isHoveringOverItemString(itemIndex, chest.mc.fontRendererObj, mouseX)) {
            hoveredItem = itemIndex + scrollOffset;
        } else {
            hoveredItem = - 1;
        }
    }

    @Override
    public void onScroll(GuiChest chest, boolean direction) {
        if(direction && scrollOffset + AttributeMod.config.chestOverlayItemsToShow < itemSlotMapping.size()) {
            scrollOffset++;
        } else if(!direction && scrollOffset > 0) {
            scrollOffset--;
        }
    }

    @Override
    public void onDrawForeground(GuiChest chest) {
        if(hoveredItem != -1) {
            highlightSlot(itemSlotMapping.get(hoveredItem), chest);
        }
    }

    @Override
    public void updateData(GuiScreen screen) {
        List<Slot> allSlots = ((GuiChest) screen).inventorySlots.inventorySlots;
        ItemWithAttributes[] items = ItemWithAttributes.getValidItems(allSlots.subList(0, allSlots.size() - 36));

        itemSlotMapping.clear();
        for(int i = 0; i < items.length; i++) {
            itemSlotMapping.put(i, items[i].getSlot());
        }

        long totalValue = Arrays.stream(items).map((ItemWithAttributes item) ->
                (long) item.getDetailedPrice().getEstimate()
        ).reduce(Long::sum).orElse(0L);

        ArrayList<String> newContent = getItemStrings(items);

        newContent.add(0, EnumChatFormatting.DARK_RED + "Total Value " + EnumChatFormatting.YELLOW +
                "-" + EnumChatFormatting.GOLD + " " + Helper.formatNumber(totalValue) + EnumChatFormatting.GRAY +
                " [Copy to Clipboard]");

        content = newContent;
    }

    @Override
    public void saveScaleAndLocation() {
        AttributeMod.config.chestOverlayScale = (int) scale;
        AttributeMod.config.chestOverlayX = position.x;
        AttributeMod.config.chestOverlayY = position.y;

        AttributeMod.config.markDirty();
    }

    @Override
    public void readScaleAndLocation() {
        scale = AttributeMod.config.chestOverlayScale;
        position = new Point(AttributeMod.config.chestOverlayX, AttributeMod.config.chestOverlayY);
        size = getSize();
    }

    @Override // Show this overlay in all GuiChest except Kuudra reward chests.
    public boolean shouldRender() {
        if(!this.isEnabled()) return false;

        GuiScreen screen = AttributeMod.mc.currentScreen;
        if(!(screen instanceof GuiChest)) return false;

        return !((ContainerChest) ((GuiChest) screen).inventorySlots)
                .getLowerChestInventory().getDisplayName().getUnformattedText()
                .contains("Paid Chest");
    }

    @Override
    public Dimension getSize() {
        // Height = (items + top line + items remaining line) * (line height + 1 space)
        int height = (AttributeMod.config.chestOverlayItemsToShow + 2) * (AttributeMod.mc.fontRendererObj.FONT_HEIGHT + 1);

        return new Dimension((int) (MAX_WIDTH * scale / 100), (int) (height * scale / 100));
    }

    @Override
    public boolean isEnabled() {
        return AttributeMod.config.chestOverlayEnabled;
    }

    @Override
    public void renderOverlay(GuiScreen screen) {
        if (itemSlotMapping.isEmpty()) return;

        int itemsAbove = scrollOffset;
        int itemsBelow = content.size() > AttributeMod.config.chestOverlayItemsToShow
                ? content.size() - 1 - scrollOffset - AttributeMod.config.chestOverlayItemsToShow
                : 0;

        GL11.glPushMatrix();
        GL11.glScaled(scale / 100, scale / 100, 1);

        screen.drawString(screen.mc.fontRendererObj,
                (itemsAbove != 0 ? EnumChatFormatting.YELLOW + "^ " + itemsAbove + " more " : "") + content.get(0),
                (itemsAbove == 0 ? screen.mc.fontRendererObj.getStringWidth("^ 1 more ") : 0) + (int) (position.x * 100 / scale), (int) (position.y * 100 / scale),
                0xffffffff);

        int i = 1;
        for (; i <= AttributeMod.config.chestOverlayItemsToShow && i + scrollOffset <= itemSlotMapping.size(); i++) {
            screen.drawString(
                    screen.mc.fontRendererObj,
                    i + scrollOffset == (hoveredItem + 1) ? highlightItemString(content.get(i + scrollOffset)) : content.get(i + scrollOffset),
                    (int) (position.x * 100 / scale), (int) (position.y * 100 / scale) + i * (screen.mc.fontRendererObj.FONT_HEIGHT + 1),
                    0xffffffff
            );
        }

        if(itemsBelow != 0) screen.drawString(screen.mc.fontRendererObj,
                EnumChatFormatting.YELLOW + "v " + itemsBelow + " more",
                (int) (position.x * 100 / scale), (int) (position.y * 100 / scale) + i * (screen.mc.fontRendererObj.FONT_HEIGHT + 1) + 1,
                0xffffffff);

        GL11.glPopMatrix();
    }

    private ArrayList<String> getItemStrings(ItemWithAttributes[] items) {
        ArrayList<String> toDisplay = new ArrayList<>();

        for(ItemWithAttributes item : items) {
            ItemWithAttributes.Evaluation price = item.getDetailedPrice();
            String[] attributeNames = item.getAttributeNames();
            Integer[] attributeLevels = item.getAttributeLevels();
            String itemName = item.getDisplayName();

            String displayString = EnumChatFormatting.GOLD + Helper.formatNumber(price.getEstimate()) + " "
                    + EnumChatFormatting.YELLOW + " " + itemName + " - " + EnumChatFormatting.GREEN + "["
                    + EnumChatFormatting.AQUA + attributeNames[0] + " " + attributeLevels[0]
                    + EnumChatFormatting.GREEN + "]";

            if(attributeNames.length == 2) displayString += " [" + EnumChatFormatting.AQUA + attributeNames[1]
                    + " " + attributeLevels[1] + EnumChatFormatting.GREEN + "]";

            toDisplay.add(displayString);
        }
        return toDisplay;
    }

    private String highlightItemString(String str) {
        return str.replace("" + EnumChatFormatting.GOLD, "" + EnumChatFormatting.RED)
                .replace("" + EnumChatFormatting.GREEN, "" + EnumChatFormatting.DARK_GREEN)
                .replace("" + EnumChatFormatting.YELLOW, "" + EnumChatFormatting.GOLD)
                .replace("" + EnumChatFormatting.AQUA, "" + EnumChatFormatting.BLUE);
    }

    private boolean isHoveringOverItemString(int index, FontRenderer fontRenderer, double mouseX) {
        return index < AttributeMod.config.chestOverlayItemsToShow
                && index + scrollOffset < itemSlotMapping.size()
                && index != - 1
                && mouseX - position.x < fontRenderer.getStringWidth(content.get(index + scrollOffset + 1)) * scale / 100;
    }
}
