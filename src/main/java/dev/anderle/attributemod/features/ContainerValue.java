package dev.anderle.attributemod.features;

import dev.anderle.attributemod.Main;
import dev.anderle.attributemod.utils.Helper;
import dev.anderle.attributemod.utils.ItemWithAttributes;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.inventory.Slot;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.*;
import java.util.List;

public class ContainerValue {
    // These values are hardcoded in Minecraft, so I'll just hardcode them here too.
    public static final int CHEST_GUI_WIDTH = 176;
    public static final int CHEST_GUI_HEIGHT = 220;
    public static final int SLOT_SIZE = 16;
    // Don't update item prices with every BackgroundDrawnEvent, use the interval specified below instead.
    public static final int UPDATE_INTERVAL = 1000;

    // the current position of the overlay
    private final Point overlayPos;
    // the current scale (size) of the overlay, default is 1
    private double overlayScale;
    // Whether the overlay is enabled, toggle with TAB key.
    private boolean enabled;

    // Stores the last time when items and their prices have been updated.
    private long lastItemUpdate = 0;
    // Stores the overlay to be rendered with next BackgroundDrawnEvent.
    private ArrayList<String> toRender = new ArrayList<>();
    // Stores the first line of the overlay including total value and the copy button.
    private String firstLine;
    // Which overlay control button is focused: 0 - none, 1 - copy to clipboard, 2 - [+], 3 - [-]
    private ControlButton focusedControlButton = ControlButton.NONE;
    // Fix for NEU's storage overlay to disable my overlay and stop weird behavior of the cursor.
    private boolean backgroundDrawnEventFiredOnce = false;
    // Stores which item the player is currently hovering over, as an index of toRender.
    private int currentItem = -1;
    // Stores which item is where, used to locate it when user interacts with the item list.
    private final Map<Integer, Slot> itemSlotMapping = new HashMap<>();

    // Helper variables for moving the overlay.
    private boolean moved = false;
    private Point lastMousePos;

    public ContainerValue() {
        this.overlayPos = getPositionFromConfig();
        this.overlayScale = getOverlayScaleFromConfig();
        this.enabled = getOverlayEnabledFromConfig();
    }

    @SubscribeEvent // Reset when a new Gui was opened.
    public void onGuiOpen(GuiOpenEvent e) {
        backgroundDrawnEventFiredOnce = false;
        lastItemUpdate = 0;
        currentItem = -1;
    }

    @SubscribeEvent // When Minecraft draws the background, render the overlay, so tooltips are displayed above.
    public void onDrawGuiBackground(GuiScreenEvent.BackgroundDrawnEvent e) {
        if(!enabled || Main.api.data == null) return; // don't show or update the overlay if there is no data or if disabled
        if(!(e.gui instanceof GuiChest)) return; // skip if gui is not a chest

        backgroundDrawnEventFiredOnce = true;

        if(System.currentTimeMillis() - UPDATE_INTERVAL > lastItemUpdate) {
            List<Slot> allSlots = ((GuiChest) e.gui).inventorySlots.inventorySlots;
            ItemWithAttributes[] items = getValidItems(allSlots.subList(0, allSlots.size() - 36));

            itemSlotMapping.clear();
            for(int i = 0; i < items.length; i++) itemSlotMapping.put(i, items[i].getSlot());

            long totalValue = Arrays.stream(items).map((ItemWithAttributes item) ->
                    (long) item.getDetailedPrice().getEstimate()
            ).reduce(Long::sum).orElse(0L);

            firstLine = EnumChatFormatting.DARK_RED + "Total Value " + EnumChatFormatting.YELLOW +
                    "-" + EnumChatFormatting.GOLD + " " + Helper.formatNumber(totalValue) + EnumChatFormatting.GRAY +
                    " [Copy to Clipboard] [+] [-]";
            toRender = getOverlay(items);

            lastItemUpdate = System.currentTimeMillis();
        }

        if(toRender.isEmpty()) return;

        if(this.overlayPos.x == 0 && this.overlayPos.y == 0) { // set location to the default value
            this.overlayPos.setLocation((e.gui.width + CHEST_GUI_WIDTH) / 2 + 5, (e.gui.height - CHEST_GUI_HEIGHT) / 2);
        }

        renderOverlay(((GuiChest) e.gui), toRender);
    }

    @SubscribeEvent // When Minecraft draws the foreground, highlight the slot if the user is hovering over an item.
    public void onDrawGuiForeground(GuiScreenEvent.DrawScreenEvent e) {
        if(!backgroundDrawnEventFiredOnce) return;
        if(enabled && currentItem != -1 && currentItem < toRender.size()) {
            highlightSlot(itemSlotMapping.get(currentItem), e.gui);
        }
    }

    @SubscribeEvent // Enable or disable the overlay when TAB key is pressed.
    public void onKeyboardInput(GuiScreenEvent.KeyboardInputEvent.Post e) {
        if(!backgroundDrawnEventFiredOnce) return;
        if(!(e.gui instanceof GuiChest) || toRender.isEmpty()) return;
        if(Keyboard.getEventKey() != Keyboard.KEY_TAB || !Keyboard.isKeyDown(Keyboard.KEY_TAB)) return;
        if(Keyboard.getEventKeyState()) toggleOverlay();
    }

    @SubscribeEvent // On mouse input (cursor moved), calculate its position and which item the user is hovering over.
    public void onMouseInput(GuiScreenEvent.MouseInputEvent e) {
        if(!backgroundDrawnEventFiredOnce) return;
        // Return if not enabled or no data.
        if(!enabled || !(e.gui instanceof GuiChest) || toRender.isEmpty()) { currentItem = -1; return; }

        // Copy items to clipboard if button was clicked.
        if(focusedControlButton == ControlButton.CLIPBOARD && Mouse.isButtonDown(0)) {
            copyToClipboard();
            focusedControlButton = ControlButton.NONE;
            return;
        }

        // Change scale of the overlay if button was clicked.
        if((focusedControlButton == ControlButton.LARGER || focusedControlButton == ControlButton.SMALLER) && Mouse.isButtonDown(0)) {
            changeOverlayScale(focusedControlButton == ControlButton.SMALLER);
            focusedControlButton = ControlButton.NONE;
            return;
        }

        // The mouse position inverse scaled by overlayScale
        double mousePosX = (double) Mouse.getEventX() * e.gui.width / e.gui.mc.displayWidth / overlayScale;
        double mousePosY = (e.gui.height - (double) Mouse.getEventY() * e.gui.height / e.gui.mc.displayHeight) / overlayScale;

        // Check if mouse is inside the overlay bounds.
        if(mousePosX < overlayPos.x) { currentItem = -1; return; }
        int itemIndex = (int) Math.floor((mousePosY - overlayPos.y) / (e.gui.mc.fontRendererObj.FONT_HEIGHT + 1)) - 1; // - 1 because of the top value line which is not an item

        if(itemIndex == -1 // Check if and which overlay control button is focused.
                && !Mouse.isButtonDown(0)
                && mousePosX >= overlayPos.x + e.gui.mc.fontRendererObj.getStringWidth(firstLine.split("\\[Copy")[0])
                && mousePosX <= overlayPos.x + e.gui.mc.fontRendererObj.getStringWidth(firstLine)
        ) {
            if(mousePosX <= overlayPos.x + e.gui.mc.fontRendererObj.getStringWidth(firstLine.split(" \\[\\+]")[0])) {
                focusedControlButton = ControlButton.CLIPBOARD;
            } else if(mousePosX <= overlayPos.x + e.gui.mc.fontRendererObj.getStringWidth(firstLine.split(" \\[-]")[0])) {
                focusedControlButton = ControlButton.LARGER;
            } else {
                focusedControlButton = ControlButton.SMALLER;
            }
        } else focusedControlButton = ControlButton.NONE;

        // Check if the current item is focused.
        if(itemIndex >= toRender.size() || itemIndex < 0) { currentItem = -1; return; }
        if(mousePosX >= overlayPos.x + e.gui.mc.fontRendererObj.getStringWidth(toRender.get(itemIndex))) { currentItem = -1; return; }

        currentItem = itemIndex;

        if(Mouse.getEventButton() == 0) moveCursorToItem(e, itemIndex);
        if(Mouse.isButtonDown(2)) { // move the overlay with the mouse position
            moveOverlay(new Point((int) Math.round(mousePosX), (int) Math.round(mousePosY)), new Dimension(e.gui.width, e.gui.height));
            moved = true;
        }
        if(moved && !Mouse.isButtonDown(2)) { // Middle button was released, save new location to config.
            savePosToConfig();
            lastMousePos = null;
            moved = false;
        }
    }

    private ItemWithAttributes[] getValidItems(List<Slot> slotsToCheck) {
        List<ItemWithAttributes> items = new ArrayList<>();

        for(Slot slot : slotsToCheck) {
            if(!slot.getHasStack()) continue;
            NBTTagCompound extra = slot.getStack().serializeNBT()
                    .getCompoundTag("tag").getCompoundTag("ExtraAttributes");
            NBTTagCompound attributeCompound = extra.getCompoundTag("attributes");
            String itemId = extra.getString("id");

            if(itemId == null || attributeCompound.getKeySet().isEmpty()) continue;
            else itemId = Helper.removeExtraItemIdParts(itemId);

            ItemWithAttributes item = new ItemWithAttributes(itemId, slot);
            for(String attribute : attributeCompound.getKeySet()) {
                item.addAttribute(Helper.formatAttribute(attribute), attributeCompound.getInteger(attribute));
            }
            items.add(item);
        }

        items.sort(Comparator.comparingInt(i -> - i.getDetailedPrice().getEstimate()));
        return items.toArray(new ItemWithAttributes[0]);
    }

    private ArrayList<String> getOverlay(ItemWithAttributes[] items) {
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

    private void savePosToConfig() {
        Main.config.set("overlayX", Integer.toString(overlayPos.x), "0");
        Main.config.set("overlayY", Integer.toString(overlayPos.y), "0");
    }

    private void changeOverlayScale(boolean smaller) {
        if(smaller) overlayScale *= 0.9;
        else overlayScale *= 1.1;
        if(overlayScale < 0.25) overlayScale = 0.25;
        if(overlayScale > 2.5) overlayScale = 2.5;

        Main.config.set("overlayScale", Double.toString(overlayScale), "1.0");
    }

    private double getOverlayScaleFromConfig() {
        return Main.config.get().get("Main Settings", "overlayScale", "1.0").getDouble();
    }

    private void toggleOverlay() {
        enabled = !enabled;
        Main.config.set("overlayEnabled", Boolean.toString(enabled), "true");
    }

    private boolean getOverlayEnabledFromConfig() {
        return Main.config.get().get("Main Settings", "overlayEnabled", "true").getBoolean();
    }

    private void copyToClipboard() {
        StringBuilder toCopy = new StringBuilder("**Selling the following items**\n");
        LinkedHashMap<String, Integer> items = new LinkedHashMap<>(); // summarize items that are the same
        for(String item : toRender) {
            if(item.contains("Attribute Shard")) { // make attribute shards look nicer in the message
                item = item.replace(" Attribute Shard -", "")
                .replace("[", "").replace("]", "");
            }
            Integer count = items.get(item);
            items.put(item, count == null ? 1 : count + 1);
        }
        for(Map.Entry<String, Integer> item : items.entrySet()) {
            toCopy.append("\n").append(item.getKey());
            if(item.getValue() > 1) toCopy.append(" **x").append(item.getValue()).append("**");
        }
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(
                toCopy.toString().replaceAll(EnumChatFormatting.RED.toString().charAt(0) + ".", "")), null
        );
    }

    private void moveOverlay(Point mousePos, Dimension guiSize) {
        if(lastMousePos == null) lastMousePos = new Point(mousePos);

        if(lastMousePos.equals(mousePos)) return;

        Point newOverlayPos = new Point(
                overlayPos.x + mousePos.x - lastMousePos.x,
                overlayPos.y + mousePos.y - lastMousePos.y
        );

        if(newOverlayPos.x >= 1 && newOverlayPos.x * overlayScale <= guiSize.width - 200) {
            overlayPos.setLocation(newOverlayPos.x, overlayPos.y);
        }

        if(newOverlayPos.y >= 1 && newOverlayPos.y * overlayScale <= guiSize.height - 200) {
            overlayPos.setLocation(overlayPos.x, newOverlayPos.y);
        }

        lastMousePos.setLocation(mousePos);
    }

    private void moveCursorToItem(GuiScreenEvent.MouseInputEvent e, int itemIndex) {
        Slot slot = itemSlotMapping.get(itemIndex);
        Mouse.setCursorPosition(
                ((e.gui.width - CHEST_GUI_WIDTH) / 2 + slot.xDisplayPosition + SLOT_SIZE / 2) * e.gui.mc.displayWidth / e.gui.width,
                (e.gui.height - ((e.gui.height - CHEST_GUI_HEIGHT) / 2 + slot.yDisplayPosition + SLOT_SIZE / 2 - 1)) * e.gui.mc.displayHeight / e.gui.height
        );

        if(!Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) && e.isCancelable()) e.setCanceled(true);
        else lastItemUpdate = 0; // Instantly schedule another update of the overlay because items were moved.

        currentItem = -1;
    }

    private void renderOverlay(GuiChest gui, ArrayList<String> strings) {
        GL11.glPushMatrix();
        GL11.glScaled(overlayScale, overlayScale, 1);

        gui.drawString( // String containing total value and the "Copy to Clipboard" button.
            gui.mc.fontRendererObj,
            highlightControlButton(firstLine),
            overlayPos.x,
            overlayPos.y,
            0xffffffff
        );
        for(int i = 0; i < strings.size(); i++) {
            gui.drawString(
                    gui.mc.fontRendererObj, i == currentItem ? highlightItemString(strings.get(i)) : strings.get(i),
                    overlayPos.x, overlayPos.y + (i + 1) * (gui.mc.fontRendererObj.FONT_HEIGHT + 1),
                    0xffffffff
            );
        }
        gui.drawString( // Bottom line showing the overlay controls.
                gui.mc.fontRendererObj,
                EnumChatFormatting.GRAY + "[" + EnumChatFormatting.DARK_GRAY +
                "TAB " + EnumChatFormatting.GRAY + "to disable] [Hold " +
                EnumChatFormatting.DARK_GRAY + "middle button " + EnumChatFormatting.GRAY + "to move]",
                overlayPos.x, overlayPos.y + (strings.size() + 1) * (gui.mc.fontRendererObj.FONT_HEIGHT + 1),
                0xffffffff
        );
        GL11.glPopMatrix();
    }

    private void highlightSlot(Slot slot, GuiScreen gui) {
        Point slotPos = new Point(
                (gui.width - CHEST_GUI_WIDTH) / 2 + slot.xDisplayPosition,
                (gui.height - CHEST_GUI_HEIGHT) / 2 + slot.yDisplayPosition - 1
        );
        GuiChest.drawRect(slotPos.x, slotPos.y, slotPos.x + SLOT_SIZE, slotPos.y + SLOT_SIZE, 0xff00aa00);
    }

    private String highlightItemString(String str) {
        return str.replace("" + EnumChatFormatting.GOLD, "" + EnumChatFormatting.RED)
                .replace("" + EnumChatFormatting.GREEN, "" + EnumChatFormatting.DARK_GREEN)
                .replace("" + EnumChatFormatting.YELLOW, "" + EnumChatFormatting.GOLD)
                .replace("" + EnumChatFormatting.AQUA, "" + EnumChatFormatting.BLUE);
    }

    private String highlightControlButton(String str) {
        String toHighlight = "";
        switch(focusedControlButton) {
            case CLIPBOARD: toHighlight = "[Copy to Clipboard]"; break;
            case LARGER: toHighlight = "[+]"; break;
            case SMALLER: toHighlight = "[-]"; break;
            default: return str;
        }
        return str.replace(toHighlight, EnumChatFormatting.YELLOW + toHighlight + EnumChatFormatting.GRAY);
    }

    private Point getPositionFromConfig() {
        return new Point(
                Main.config.get().get("Main Settings", "overlayX", "0").getInt(),
                Main.config.get().get("Main Settings", "overlayY", "0").getInt()
        );
    }

    enum ControlButton {
        NONE, CLIPBOARD, LARGER, SMALLER
    }
}
