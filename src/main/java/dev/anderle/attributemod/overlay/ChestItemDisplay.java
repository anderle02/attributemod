package dev.anderle.attributemod.overlay;

import dev.anderle.attributemod.AttributeMod;
import dev.anderle.attributemod.Config;
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
import java.util.stream.IntStream;

public class ChestItemDisplay extends ChestOverlay {
    /** Stores which item is where, used to locate it when user interacts with the item list. */
    private final Map<Integer, Slot> itemSlotMapping = new HashMap<>();
    /** The strings to render with next render tick. Split in columns to be properly aligned. */
    private List<List<String>> content = new ArrayList<>();
    /** Sums um max widths of each column, to know at which x to render the column. */
    private List<Integer> xOffsets = new ArrayList<>();

    private long totalValue = 0; // Current total value of the chest.
    private int hoveredItem = -1; // Index of `itemSlotMapping`, which item is focused.
    private int scrollOffset = 0; // Scroll offset in `content`.
    private boolean copyButtonFocused = false; // Whether the copy to clipboard button is focused.

    public ChestItemDisplay() {
        super("Chest Overlay", 1000, Color.blue);
    }

    /** When opening a chest, reset hovered item, scroll offset and immediately update the data. */
    public @Override void onGuiOpen(GuiChest chest) {
        hoveredItem = -1;
        scrollOffset = 0;
        scheduleImmediateUpdate();
    }

    /** When clicking, either move cursor to `hoveredItem` or copy data to clipboard. */
    public @Override void onClickOverlay(GuiScreenEvent.MouseInputEvent e, double mouseX, double mouseY) {
        if(copyButtonFocused) { copyToClipboard(); return; }
        if(hoveredItem == -1) return;

        Point slotPos = getSlotPos(itemSlotMapping.get(hoveredItem), (GuiChest) e.gui);
        Mouse.setCursorPosition(
                (slotPos.x + SLOT_SIZE / 2) * e.gui.mc.displayWidth / e.gui.width,
                (e.gui.height - (slotPos.y + SLOT_SIZE / 2 - 1)) * e.gui.mc.displayHeight / e.gui.height
        );

        if(!Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) && e.isCancelable()) e.setCanceled(true);
        else scheduleImmediateUpdate(); // Since onClickSlot does not trigger here.

        hoveredItem = -1;
    }

    @Override
    public void onClickSlot(GuiScreenEvent.MouseInputEvent e, Slot slot) {
        if(!itemSlotMapping.isEmpty()) scheduleImmediateUpdate();
    }

    /** When the mouse is moved, check if any item string or the copy button is focused. */
    public @Override void onHover(GuiChest chest, double mouseX, double mouseY) {
        if(itemSlotMapping.isEmpty()) return; // Don't try to calculate hovered item if there are no items.

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

    /** When scrolling, update `scrollOffset` if allowed. */
    public @Override void onScroll(GuiChest chest, boolean direction) {
        if(itemSlotMapping.isEmpty()) return; // Don't try to calculate scroll offset if there are no items.

        if(direction && scrollOffset + AttributeMod.config.chestOverlayItemsToShow < itemSlotMapping.size()) {
            scrollOffset++;
        } else if(!direction && scrollOffset > 0) {
            scrollOffset--;
        }
    }

    /** When the foreground layer is drawn, highlight the slot of `hoveredItem`, if needed. */
    public @Override void onDrawForeground(GuiChest chest) {
        if(hoveredItem != -1) {
            highlightSlot(getSlotPos(itemSlotMapping.get(hoveredItem), chest));
        }
    }

    /** Update item prices and locations. */
    public @Override void updateData(GuiScreen screen) {
        List<Slot> allSlots = ((GuiChest) screen).inventorySlots.inventorySlots;
        List<ItemWithAttributes> items = ItemWithAttributes.fromContainer(allSlots.subList(0, allSlots.size() - 36));

        itemSlotMapping.clear();
        for(int i = 0; i < items.size(); i++) {
            itemSlotMapping.put(i, items.get(i).getSlot());
        }

        content = getItemStrings(items);
        xOffsets = getColumnOffsets(content);
        totalValue = items.stream()
                .map(item -> (long) item.getDetailedPrice().getEstimate())
                .reduce(Long::sum).orElse(0L);

        if(scrollOffset > 0 && scrollOffset > itemSlotMapping.size() - AttributeMod.config.chestOverlayItemsToShow) {
            scrollOffset--; // To avoid showing "-1" at the end when an item was removed.
        }
    }

    /** Save current overlay scale and position to config. */
    public @Override void saveScaleAndLocation() {
        AttributeMod.config.chestOverlayScale = (int) scale;
        AttributeMod.config.chestOverlayX = position.x;
        AttributeMod.config.chestOverlayY = position.y;

        AttributeMod.config.markDirty();
    }

    /** Read scale and location from config and update the overlay size. */
    public @Override void readScaleAndLocation() {
        scale = AttributeMod.config.chestOverlayScale;
        position = new Point(AttributeMod.config.chestOverlayX, AttributeMod.config.chestOverlayY);
        size = getSize();
    }

    /** Show this overlay in all GuiChest except Kuudra reward chests. */
    public @Override boolean shouldRender(GuiScreen screen) {
        if(!this.isEnabled() || !(screen instanceof GuiChest)) return false;

        return !((ContainerChest) ((GuiChest) screen).inventorySlots)
                .getLowerChestInventory().getDisplayName().getUnformattedText()
                .contains("Paid Chest");
    }

    /** Get current overlay width and height depending on its scale and hardcoded "longest possible" item strings. */
    public @Override Dimension getSize() {
        // Height = (items + top line + items remaining line) * (line height + 1 space)
        int height = (AttributeMod.config.chestOverlayItemsToShow + 2) * (AttributeMod.mc.fontRendererObj.FONT_HEIGHT + 1);
        String longestText;

        switch(Config.OverlayStyle.values()[AttributeMod.config.overlayStyle]) { // Pretty sure these are the longest possible strings for each overlay style
            case ITEM_NAMES_ONLY: longestText = "888.8M  Vanquished Glowstone Gauntlet"; break;
            case SHORT_ATTRIBUTES_POPULAR:
            case SHORT_ATTRIBUTES_ALL: longestText = "888.8M  DOM 10, VET 10 Vanquished Glowstone Gauntlet"; break;
            default: longestText = "888.8M  Vanquished Glowstone Gauntlet - [Arachno Resistance 10] [Blazing Resistance 10]";
        }

        return new Dimension(
                (int) (AttributeMod.mc.fontRendererObj.getStringWidth(longestText) * scale / 100),
                (int) (height * scale / 100));
    }

    /** Returns whether the chest overlay is enabled in config. */
    public @Override boolean isEnabled() {
        return AttributeMod.config.chestOverlayEnabled;
    }

    /** Renders all lines and columns. Adds a top line and the scroll offset display. */
    public @Override void renderOverlay(GuiScreen screen) {
        if (itemSlotMapping.isEmpty()) return;

        int itemsBelow = content.size() > AttributeMod.config.chestOverlayItemsToShow
                ? content.size() - scrollOffset - AttributeMod.config.chestOverlayItemsToShow
                : 0;

        GL11.glPushMatrix();
        GL11.glScaled(scale / 100, scale / 100, 1);

        screen.drawString(screen.mc.fontRendererObj, getFirstLine(), (int) (position.x * 100 / scale), (int) (position.y * 100 / scale), 0xffffffff);

        int i = 1;
        for (; i <= AttributeMod.config.chestOverlayItemsToShow && i + scrollOffset <= itemSlotMapping.size(); i++) {
            List<String> parts = i + scrollOffset == hoveredItem + 1
                    ? highlightItemStrings(content.get(i + scrollOffset - 1))
                    : content.get(i + scrollOffset - 1);

            for(int j = 0; j < parts.size(); j++) {
                screen.drawString(
                        screen.mc.fontRendererObj, parts.get(j),
                        (int) (position.x * 100 / scale) + xOffsets.get(j),
                        (int) (position.y * 100 / scale) + i * (screen.mc.fontRendererObj.FONT_HEIGHT + 1),
                        0xffffffff
                );
            }
        }

        if(itemsBelow != 0) screen.drawString(screen.mc.fontRendererObj,
                EnumChatFormatting.YELLOW + "v " + itemsBelow,
                (int) (position.x * 100 / scale), (int) (position.y * 100 / scale) + i * (screen.mc.fontRendererObj.FONT_HEIGHT + 1) + 1,
                0xffffffff);

        GL11.glPopMatrix();
    }

    /* ---------------------------------------------- Helper Functions ---------------------------------------------- */

    /** Returns a list of item strings with EXACTLY 4 columns. */
    private List<List<String>> getItemStrings(List<ItemWithAttributes> items) {
        if(AttributeMod.config.overlayStyle == 1 || AttributeMod.config.overlayStyle == 2) {
            // When showing short attribute names, show attributes left of the item name.
            return items.stream().map(i -> Arrays.asList(
                    getItemPriceString(i.getDetailedPrice().getEstimate()),
                    getAttributeString(i.getAttributes().get(0), i.getAttributeLevels().get(0)),
                    i.hasTwoAttributes() ? getAttributeString(i.getAttributes().get(1), i.getAttributeLevels().get(1)) : "",
                    getItemNameString(i.getDisplayName())
            )).collect(Collectors.toList());
        } else {
            // When showing long attribute names, show attributes right of the item name.
            return items.stream().map(i -> Arrays.asList(
                    getItemPriceString(i.getDetailedPrice().getEstimate()),
                    getItemNameString(i.getDisplayName()),
                    getAttributeString(i.getAttributes().get(0), i.getAttributeLevels().get(0)),
                    i.hasTwoAttributes() ? getAttributeString(i.getAttributes().get(1), i.getAttributeLevels().get(1)) : ""
            )).collect(Collectors.toList());
        }
    }

    private String getItemPriceString(int price) {
        return EnumChatFormatting.GOLD + Helper.formatNumber(price);
    }

    private String getItemNameString(String itemName) {
        return "  " + EnumChatFormatting.YELLOW + itemName;
    }

    private String getAttributeString(Attribute attribute, int level) {
        switch(Config.OverlayStyle.values()[AttributeMod.config.overlayStyle]) {
            case SHORT_ATTRIBUTES_POPULAR: return attribute.isPopular()
                ? " " + EnumChatFormatting.AQUA + attribute.getShortName() + " " + EnumChatFormatting.GREEN + level
                : "";
            case SHORT_ATTRIBUTES_ALL: return " " + EnumChatFormatting.AQUA + attribute.getShortName() + " " + EnumChatFormatting.GREEN + level;
            case LONG_ATTRIBUTES_POPULAR: return attribute.isPopular()
                    ? EnumChatFormatting.GREEN + " [" + EnumChatFormatting.AQUA +attribute.getName() + " " + level + EnumChatFormatting.GREEN + "]"
                    : "";
            case LONG_ATTRIBUTES_ALL: return EnumChatFormatting.GREEN + " [" + EnumChatFormatting.AQUA +attribute.getName() + " " + level + EnumChatFormatting.GREEN + "]";
            default: return "";
        }
    }

    private List<Integer> getColumnOffsets(List<List<String>> strings) {
        List<Integer> result = new ArrayList<>();
        List<Integer> stringWidths = IntStream.range(0, 4)
                .mapToObj(i -> strings.stream()
                        .map(parts -> AttributeMod.mc.fontRendererObj.getStringWidth(parts.get(i)))
                        .max(Integer::compareTo)
                        .orElse(0))
                .collect(Collectors.toList());

        int sum = 0;
        for (int value : stringWidths) {
            result.add(sum);
            sum += value;
        }
        return result;
    }

    private String getFirstLine() {
        return (scrollOffset != 0 ? EnumChatFormatting.YELLOW + "^ " + scrollOffset + " "  : "     ")
                + EnumChatFormatting.DARK_RED + "Total Value " + EnumChatFormatting.YELLOW + "-"
                + EnumChatFormatting.GOLD + " " + Helper.formatNumber(totalValue)
                + (copyButtonFocused ? EnumChatFormatting.YELLOW : EnumChatFormatting.GRAY) + " [Copy]";
    }

    private List<String> highlightItemStrings(List<String> strings) {
        return Arrays.asList(
                strings.get(0).replace(EnumChatFormatting.GOLD.toString(), EnumChatFormatting.RED.toString()),
                strings.get(1).replace(EnumChatFormatting.GREEN.toString(), EnumChatFormatting.DARK_GREEN.toString())
                        .replace(EnumChatFormatting.AQUA.toString(), EnumChatFormatting.BLUE.toString())
                        .replace(EnumChatFormatting.YELLOW.toString(), EnumChatFormatting.GOLD.toString()),
                strings.get(2).replace(EnumChatFormatting.GREEN.toString(), EnumChatFormatting.DARK_GREEN.toString())
                        .replace(EnumChatFormatting.AQUA.toString(), EnumChatFormatting.BLUE.toString())
                        .replace(EnumChatFormatting.YELLOW.toString(), EnumChatFormatting.GOLD.toString()),
                strings.get(3).replace(EnumChatFormatting.GREEN.toString(), EnumChatFormatting.DARK_GREEN.toString())
                        .replace(EnumChatFormatting.AQUA.toString(), EnumChatFormatting.BLUE.toString())
                        .replace(EnumChatFormatting.YELLOW.toString(), EnumChatFormatting.GOLD.toString())
        );
    }

    private boolean isHoveringOverItemString(int index, FontRenderer fontRenderer, double mouseX) {
        if(index >= AttributeMod.config.chestOverlayItemsToShow || index == - 1) return false;
        if(index + scrollOffset >= itemSlotMapping.size()) return false;

        List<String> itemStrings = content.get(index + scrollOffset);
        int lastColumn = itemStrings.get(3).isEmpty() ? (itemStrings.get(2).isEmpty() ? 1 : 2) : 3;

        return mouseX < position.x + (xOffsets.get(lastColumn)
                + fontRenderer.getStringWidth(itemStrings.get(lastColumn))) * scale / 100;
    }

    private void checkIfCopyButtonFocused(double mousePosX, int focusedLine) {
        int copyButtonPosX = position.x + (int) (AttributeMod.mc.fontRendererObj.getStringWidth(getFirstLine().split("\\[Copy")[0]) * scale / 100);

        copyButtonFocused = focusedLine == 0
                && mousePosX >= copyButtonPosX
                && mousePosX <= copyButtonPosX + AttributeMod.mc.fontRendererObj.getStringWidth("[Copy]") * scale / 100;
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
            String item = content.get(i).get(0) + " " + content.get(i).get(1) + " " + content.get(i).get(2) + " " + content.get(i).get(3);
            if(content.get(i).get(1).equals("Attribute Shard")) { // make attribute shards look nicer in the message
                item = item.replace("Attribute Shard ", "").replace("[", "").replace("]", "");
            }
            Integer count = items.get(item);
            items.put(item, count == null ? 1 : count + 1);
        }
        return items;
    }

    public void resetHoveredItem() {
        hoveredItem = -1;
    }
}
