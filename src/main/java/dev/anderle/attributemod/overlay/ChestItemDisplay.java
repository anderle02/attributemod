package dev.anderle.attributemod.overlay;

import dev.anderle.attributemod.AttributeMod;
import dev.anderle.attributemod.utils.Attribute;
import dev.anderle.attributemod.utils.Helper;
import dev.anderle.attributemod.utils.ItemWithAttributes;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.Slot;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiScreenEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class ChestItemDisplay extends ChestOverlayElement {
    /** Stores which item is where, used to locate it when user interacts with the item list. */
    private final Map<Integer, Slot> itemSlotMapping = new HashMap<>();
    /** Index of the item string the user is currently hovering over. */
    private int hoveredItem = -1;
    private int scrollOffset = 0;
    private boolean copyButtonFocused = false;

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
        if(copyButtonFocused) { copyToClipboard(); return; }
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
        int focusedLine = (int) Math.floor((mouseY - position.y) / scaledLineHeight);

        if(isHoveringOverItemString(focusedLine - 1, chest.mc.fontRendererObj, mouseX)) {
            hoveredItem = focusedLine - 1 + scrollOffset;
        } else {
            hoveredItem = - 1;
        }
        checkIfCopyButtonFocused(mouseX, focusedLine);
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
        List<ItemWithAttributes> items = ItemWithAttributes.fromContainer(allSlots.subList(0, allSlots.size() - 36));

        itemSlotMapping.clear();
        for(int i = 0; i < items.size(); i++) {
            itemSlotMapping.put(i, items.get(i).getSlot());
        }

        long totalValue = items.stream()
                .map(item -> (long) item.getDetailedPrice().getEstimate())
                .reduce(Long::sum).orElse(0L);

        ArrayList<String> newContent = new ArrayList<>(getItemStrings(items));

        newContent.add(0, EnumChatFormatting.DARK_RED + "Total Value " + EnumChatFormatting.YELLOW +
                "-" + EnumChatFormatting.GOLD + " " + Helper.formatNumber(totalValue) + EnumChatFormatting.GRAY +
                " [Copy]");

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
    public boolean shouldRender(GuiScreen screen) {
        if(!this.isEnabled() || !(screen instanceof GuiChest)) return false;

        return !((ContainerChest) ((GuiChest) screen).inventorySlots)
                .getLowerChestInventory().getDisplayName().getUnformattedText()
                .contains("Paid Chest");
    }

    @Override
    public Dimension getSize() {
        // Height = (items + top line + items remaining line) * (line height + 1 space)
        int height = (AttributeMod.config.chestOverlayItemsToShow + 2) * (AttributeMod.mc.fontRendererObj.FONT_HEIGHT + 1);
        String longestText;

        switch(AttributeMod.config.overlayStyle) { // Pretty sure these are the longest possible strings for each overlay style
            case 0: longestText = "888.8M  Vanquished Glowstone Gauntlet"; break;
            case 1:
            case 2: longestText = "888.8M  DOM 10, VET 10 Vanquished Glowstone Gauntlet"; break;
            default: longestText = "888.8M  Vanquished Glowstone Gauntlet - [Arachno Resistance 10] [Blazing Resistance 10]";
        }

        return new Dimension(
                (int) (AttributeMod.mc.fontRendererObj.getStringWidth(longestText) * scale / 100),
                (int) (height * scale / 100)
        );
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

        String firstLine = (itemsAbove != 0 ? EnumChatFormatting.YELLOW + "^ " + itemsAbove + " "  : "")
                + (copyButtonFocused ? content.get(0).replace("[Copy]", EnumChatFormatting.YELLOW + "[Copy]" + EnumChatFormatting.GRAY) : content.get(0));

        screen.drawString(screen.mc.fontRendererObj, firstLine,
                (itemsAbove == 0 ? screen.mc.fontRendererObj.getStringWidth("^ 0 ") : 0) + (int) (position.x * 100 / scale),
                (int) (position.y * 100 / scale),
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
                EnumChatFormatting.YELLOW + "v " + itemsBelow,
                (int) (position.x * 100 / scale), (int) (position.y * 100 / scale) + i * (screen.mc.fontRendererObj.FONT_HEIGHT + 1) + 1,
                0xffffffff);

        GL11.glPopMatrix();
    }

    private List<String> getItemStrings(List<ItemWithAttributes> items) {
        return items.stream().map(i -> getString(
                i.getAttributes(), i.getAttributeLevels(), i.getDisplayName(), i.getDetailedPrice().getEstimate()
        )).collect(Collectors.toList());
    }

    /** Get a nicely formatted string for every single overlay style. (took a while...) */
    private String getString(List<Attribute> attributes, List<Integer> attributeLevels, String itemName, int estimate) {
        String displayString = EnumChatFormatting.GOLD + Helper.formatNumber(estimate) + " ";
        switch(AttributeMod.config.overlayStyle) {
            case 1:
                if(attributes.get(0).isPopular()) displayString += " " + EnumChatFormatting.AQUA + attributes.get(0).getShortName() + " " + EnumChatFormatting.GREEN + attributeLevels.get(0);
                if(attributes.size() == 2) {
                    if(attributes.get(0).isPopular() && attributes.get(1).isPopular()) displayString += EnumChatFormatting.YELLOW + ",";
                    if(attributes.get(1).isPopular()) displayString += " " + EnumChatFormatting.AQUA + attributes.get(1).getShortName() + " " + EnumChatFormatting.GREEN + attributeLevels.get(1);
                }
                displayString += EnumChatFormatting.YELLOW + " " + itemName;
                break;
            case 2:
                displayString += " " + EnumChatFormatting.AQUA + attributes.get(0).getShortName() + " " + EnumChatFormatting.GREEN + attributeLevels.get(0);
                if(attributes.size() == 2) displayString += EnumChatFormatting.YELLOW + ", " + EnumChatFormatting.AQUA + attributes.get(1).getShortName() + " " + EnumChatFormatting.GREEN + attributeLevels.get(1);
                displayString += EnumChatFormatting.YELLOW + " " + itemName;
                break;
            case 3:
                displayString += EnumChatFormatting.YELLOW + " " + itemName;
                if(attributes.get(0).isPopular()) displayString += " - " + EnumChatFormatting.GREEN + "[" + EnumChatFormatting.AQUA + attributes.get(0).getName() + " " + attributeLevels.get(0) + EnumChatFormatting.GREEN + "]";
                if(attributes.size() == 2) {
                    if(!attributes.get(0).isPopular() && attributes.get(1).isPopular()) displayString += " - " + EnumChatFormatting.GREEN + "[" + EnumChatFormatting.AQUA + attributes.get(1).getName() + " " + attributeLevels.get(1) + EnumChatFormatting.GREEN + "]";
                    if(attributes.get(0).isPopular() && attributes.get(1).isPopular()) displayString += " [" + EnumChatFormatting.AQUA + attributes.get(1).getName() + " " + attributeLevels.get(1) + EnumChatFormatting.GREEN + "]";
                }
                break;
            case 4:
                displayString += EnumChatFormatting.YELLOW + " " + itemName;
                displayString += " - " + EnumChatFormatting.GREEN + "[" + EnumChatFormatting.AQUA + attributes.get(0).getName() + " " + attributeLevels.get(0) + EnumChatFormatting.GREEN + "]";
                if(attributes.size() == 2) displayString += " [" + EnumChatFormatting.AQUA + attributes.get(1).getName() + " " + attributeLevels.get(1) + EnumChatFormatting.GREEN + "]";
                break;
            default:
                displayString += EnumChatFormatting.YELLOW + " " + itemName;
        }
        return displayString;
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

    private void checkIfCopyButtonFocused(double mousePosX, int focusedLine) {
        int copyButtonPosX = position.x + AttributeMod.mc.fontRendererObj.getStringWidth(
                "^ " + scrollOffset + " " + content.get(0).split("\\[Copy")[0]);

        copyButtonFocused = focusedLine == 0
                && mousePosX >= copyButtonPosX
                && mousePosX <= copyButtonPosX + AttributeMod.mc.fontRendererObj.getStringWidth("[Copy]");
    }

    private void copyToClipboard() {
        StringBuilder toCopy = new StringBuilder("**Selling the following items**\n");
        for(Map.Entry<String, Integer> item : getItemStringsToCopy().entrySet()) {
            toCopy.append("\n").append(item.getKey());
            if(item.getValue() > 1) toCopy.append(" **x").append(item.getValue()).append("**");
        }
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(
                toCopy.toString().replaceAll(EnumChatFormatting.RED.toString().charAt(0) + ".", "")), null);

        AttributeMod.mc.getSoundHandler().playSound( // Play the button sound...
                PositionedSoundRecord.create(new ResourceLocation("gui.button.press"), 1.0F));
    }

    private LinkedHashMap<String, Integer> getItemStringsToCopy() {
        LinkedHashMap<String, Integer> items = new LinkedHashMap<>(); // summarize items that are the same
        for(int i = 1; i < content.size(); i++) {
            String item = content.get(i);
            if(item.contains("Attribute Shard")) { // make attribute shards look nicer in the message
                item = item.replace(" Attribute Shard -", "")
                        .replace("[", "").replace("]", "");
            }
            Integer count = items.get(item);
            items.put(item, count == null ? 1 : count + 1);
        }
        return items;
    }
}
