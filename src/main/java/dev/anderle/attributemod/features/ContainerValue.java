package dev.anderle.attributemod.features;

import dev.anderle.attributemod.utils.Helper;
import dev.anderle.attributemod.utils.ItemWithAttributes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.inventory.Slot;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class ContainerValue {
    // These values are hardcoded in Minecraft, so I'll just hardcode them here too.
    public static final int CHEST_GUI_WIDTH = 176;
    public static final int CHEST_GUI_HEIGHT = 220;

    @SubscribeEvent
    public void onDrawGuiBackground(GuiScreenEvent.BackgroundDrawnEvent e) {
        // Guis like main menu, inventory and anything else that's not a chest.
        if(!(e.gui instanceof GuiChest)) return;

        // Check all chest slots for items with attributes and organize them.
        List<Slot> allSlots = ((GuiChest) e.gui).inventorySlots.inventorySlots;
        ItemWithAttributes[] items = this.getValidItems(allSlots.subList(0, allSlots.size() - 36));
        if(items.length == 0) return;

        // Sort items by estimated price.
        this.sortItems(items);

        // Render overlay.
        this.renderItemInformation((GuiChest) e.gui, items);
    }

    private ItemWithAttributes[] getValidItems(List<Slot> slotsToCheck) {
        List<ItemWithAttributes> items = new ArrayList<ItemWithAttributes>();

        for(Slot slot : slotsToCheck) {
            if(!slot.getHasStack()) continue;
            NBTTagCompound extra = slot.getStack().serializeNBT()
                    .getCompoundTag("tag").getCompoundTag("ExtraAttributes");
            NBTTagCompound attributeCompound = extra.getCompoundTag("attributes");
            String itemId = extra.getString("id");

            if(itemId == null || attributeCompound.getKeySet().size() == 0) continue;
            else itemId = Helper.removeExtraItemIdParts(itemId);

            ItemWithAttributes item = new ItemWithAttributes(itemId);
            for(String attribute : attributeCompound.getKeySet()) {
                item.addAttribute(Helper.formatAttribute(attribute), attributeCompound.getInteger(attribute));
            }
            items.add(item);
        }

        return items.toArray(new ItemWithAttributes[0]);
    }

    private void sortItems(ItemWithAttributes[] items) {
        Arrays.sort(items, new Comparator<ItemWithAttributes>() {
            @Override
            public int compare(ItemWithAttributes i1, ItemWithAttributes i2) {
                int price1 = i1.getDetailedPrice().getEstimate();
                int price2 = i2.getDetailedPrice().getEstimate();
                if(price1 < price2) return 1;
                if(price1 > price2) return -1;
                return 0;
            }
        });
    }

    private void renderItemInformation(GuiChest gui, ItemWithAttributes[] items) {
        FontRenderer renderer = Minecraft.getMinecraft().fontRendererObj;
        Point overlayPos = new Point(
                (gui.width + CHEST_GUI_WIDTH) / 2,
                (gui.height - CHEST_GUI_HEIGHT) / 2
        );
        int maxNameLength = this.getMaxItemNameLength(items);
        for(int i = 0; i < items.length; i++) {
            ItemWithAttributes item = items[i];
            ItemWithAttributes.Evaluation price = item.getDetailedPrice();
            String[] attributeNames = item.getAttributeNames();
            Integer[] attributeLevels = item.getAttributeLevels();
            String itemName = item.getDisplayName();

            StringBuilder alignmentString = new StringBuilder(" ");
            for(int j = 0; j < maxNameLength - itemName.length(); j++) alignmentString.append(" ");

            StringBuilder displayString = new StringBuilder()
            .append(EnumChatFormatting.GOLD).append(Helper.format(price.getEstimate()))
            .append(" ").append(EnumChatFormatting.YELLOW).append(itemName)
            .append(alignmentString).append(EnumChatFormatting.AQUA).append(attributeNames[0])
            .append(" ").append(attributeLevels[0]);

            if(attributeNames.length == 2) displayString
                .append(EnumChatFormatting.YELLOW)
                .append(", ").append(EnumChatFormatting.AQUA).append(attributeNames[1])
                .append(" ").append(attributeLevels[1]);

            gui.drawString(
                    renderer, displayString.toString(),
                    overlayPos.x, overlayPos.y + i * (renderer.FONT_HEIGHT + 1),
                    0xffffffff
            );
        }
    }

    private int getMaxItemNameLength(ItemWithAttributes[] items) {
        int max = 0;
        for(ItemWithAttributes item : items) {
            int length = item.getDisplayName().length();
            if(length > max) max = length;
        }
        return max;
    }
}
