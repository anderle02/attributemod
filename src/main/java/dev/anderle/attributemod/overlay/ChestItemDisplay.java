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
import java.util.stream.IntStream;

public class ChestItemDisplay extends ChestOverlayElement {
    /** Stores which item is where, used to locate it when user interacts with the item list. */
    private final Map<Integer, Slot> itemSlotMapping = new HashMap<>();

    /** The strings to render with next render tick. Has to be split in columns to be able to be properly aligned. */
    private List<List<String>> content = new ArrayList<>();
    /** Sums um max widths of each column, to know at which x to render the column. */
    private List<Integer> xOffsets = new ArrayList<>();
    /** Current total value of the chest items. */
    private long totalValue = 0;

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

    @Override
    public void onScroll(GuiChest chest, boolean direction) {
        if(itemSlotMapping.isEmpty()) return; // Don't try to calculate scroll offset if there are no items.

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

        content = getItemStrings(items);
        xOffsets = getColumnOffsets(content);
        totalValue = items.stream()
                .map(item -> (long) item.getDetailedPrice().getEstimate())
                .reduce(Long::sum).orElse(0L);

        if(scrollOffset > 0 && scrollOffset > itemSlotMapping.size() - AttributeMod.config.chestOverlayItemsToShow) {
            scrollOffset--; // To avoid showing "-1" at the end when an item was removed.
        }
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

    public void resetHoveredItem() {
        hoveredItem = -1;
    }

    /** Returns a list of item strings with EXACTLY 4 parts. */
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
        switch(AttributeMod.config.overlayStyle) {
            case 1: return attribute.isPopular()
                ? " " + EnumChatFormatting.AQUA + attribute.getShortName() + " " + EnumChatFormatting.GREEN + level
                : "";
            case 2: return " " + EnumChatFormatting.AQUA + attribute.getShortName() + " " + EnumChatFormatting.GREEN + level;
            case 3: return attribute.isPopular()
                    ? EnumChatFormatting.GREEN + " [" + EnumChatFormatting.AQUA +attribute.getName() + " " + level + EnumChatFormatting.GREEN + "]"
                    : "";
            case 4: return EnumChatFormatting.GREEN + " [" + EnumChatFormatting.AQUA +attribute.getName() + " " + level + EnumChatFormatting.GREEN + "]";
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

        // TODO: fix for shards (somehow need a 2 in here if shard)

        return mouseX < position.x + (xOffsets.get(3)
                + fontRenderer.getStringWidth(content.get(index + scrollOffset).get(3))) * scale / 100;
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
}
