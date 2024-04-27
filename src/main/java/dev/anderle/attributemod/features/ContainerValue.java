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
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import scala.swing.Dialog;

import javax.xml.transform.Result;
import java.awt.*;
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

    // Whether the overlay is enabled, toggle with TAB key.
    private boolean enabled = true;
    // Stores the last time when items and their prices have been updated.
    private long lastItemUpdate = 0;
    // Stores the overlay to be rendered with next BackgroundDrawnEvent.
    private ArrayList<String> toRender = new ArrayList<>();
    // Stores which item the player is currently hovering over, as an index of toRender.
    private int currentItem = -1;
    // Stores which item is where, used to locate it when user interacts with the item list.
    private final Map<Integer, Slot> itemSlotMapping = new HashMap<>();

    // helper variables for moving the overlay
    private boolean moved = false;
    private Point lastMousePos;

    public ContainerValue() {
        this.overlayPos = getPositionFromConfig();
    }

    @SubscribeEvent // Reset when a new Gui was opened.
    public void onGuiOpen(GuiOpenEvent e) {
        lastItemUpdate = 0;
        currentItem = -1;
    }

    @SubscribeEvent // When Minecraft draws the background, render the overlay, so tooltips are displayed above.
    public void onDrawGuiBackground(GuiScreenEvent.BackgroundDrawnEvent e) {
        if(!enabled || Main.api.data == null) return; // don't show or update the overlay if there is no data or if disabled
        if(!(e.gui instanceof GuiChest)) return; // skip if gui is not a chest

        if(System.currentTimeMillis() - UPDATE_INTERVAL > lastItemUpdate) {
            List<Slot> allSlots = ((GuiChest) e.gui).inventorySlots.inventorySlots;
            ItemWithAttributes[] items = getValidItems(allSlots.subList(0, allSlots.size() - 36));

            itemSlotMapping.clear();
            for(int i = 0; i < items.length; i++) itemSlotMapping.put(i, items[i].getSlot());

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
        if(enabled && currentItem != -1 && currentItem < toRender.size()) {
            highlightSlot(itemSlotMapping.get(currentItem), e.gui);
        }
    }

    @SubscribeEvent // Enable or disable the overlay when TAB key is pressed.
    public void onKeyboardInput(GuiScreenEvent.KeyboardInputEvent.Post e) {
        if(!(e.gui instanceof GuiChest) || toRender.isEmpty()) return;
        if(Keyboard.getEventKey() != Keyboard.KEY_TAB || !Keyboard.isKeyDown(Keyboard.KEY_TAB)) return;
        if(Keyboard.getEventKeyState()) enabled = !enabled;
    }

    @SubscribeEvent // On mouse input (cursor moved), calculate its position and which item the user is hovering over.
    public void onMouseInput(GuiScreenEvent.MouseInputEvent e) {
        // return if not enabled or no data
        if(!enabled || !(e.gui instanceof GuiChest) || toRender.isEmpty()) { currentItem = -1; return; }

        Point mousePos = new Point(
                Mouse.getEventX() * e.gui.width / e.gui.mc.displayWidth,
                e.gui.height - Mouse.getEventY() * e.gui.height / e.gui.mc.displayHeight
        );

        // check if mouse is inside the overlay bounds
        if(mousePos.x < overlayPos.x) { currentItem = -1; return; }
        int itemIndex = (mousePos.y - overlayPos.y) / (e.gui.mc.fontRendererObj.FONT_HEIGHT + 1);

        if(itemIndex >= toRender.size() || itemIndex < 0) { currentItem = -1; return; }
        if(mousePos.x >= overlayPos.x + e.gui.mc.fontRendererObj.getStringWidth(toRender.get(itemIndex))) { currentItem = -1; return; }

        currentItem = itemIndex;

        if(Mouse.getEventButton() == 0) moveCursorToItem(e, itemIndex);

        if(Mouse.isButtonDown(2)) { // move the overlay with the mouse position
            moveOverlay(mousePos, new Dimension(e.gui.width, e.gui.height));
            moved = true;
        }

        if(moved && !Mouse.isButtonDown(2)) { // middle button was released, save new location to config
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

    private void moveOverlay(Point mousePos, Dimension guiSize) {
        if(lastMousePos == null) lastMousePos = new Point(mousePos);

        if(lastMousePos.equals(mousePos)) return;

        Point newOverlayPos = new Point(
                overlayPos.x + mousePos.x - lastMousePos.x,
                overlayPos.y + mousePos.y - lastMousePos.y
        );

        if(newOverlayPos.x >= 1 && newOverlayPos.x <= guiSize.width - 200) {
            overlayPos.setLocation(newOverlayPos.x, overlayPos.y);
        }

        if(newOverlayPos.y >= 1 && newOverlayPos.y <= guiSize.height - 200) {
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

        if(!Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) e.setCanceled(true);
        else lastItemUpdate = 0; // Instantly schedule another update of the overlay because items were moved.

        currentItem = -1;
    }

    private void renderOverlay(GuiChest gui, ArrayList<String> strings) {
        for(int i = 0; i < strings.size(); i++) {
            gui.drawString(
                    gui.mc.fontRendererObj, i == currentItem ? highlightItemString(strings.get(i)) : strings.get(i),
                    this.overlayPos.x, this.overlayPos.y + i * (gui.mc.fontRendererObj.FONT_HEIGHT + 1),
                    0xffffffff
            );
        }
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

    private Point getPositionFromConfig() {
        return new Point(
                Main.config.get().get("Main Settings", "overlayX", "0").getInt(),
                Main.config.get().get("Main Settings", "overlayY", "0").getInt()
        );
    }
}
