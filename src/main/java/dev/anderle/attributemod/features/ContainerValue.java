package dev.anderle.attributemod.features;

import dev.anderle.attributemod.utils.Helper;
import dev.anderle.attributemod.utils.ItemWithAttributes;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.inventory.Slot;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import java.util.List;

public class ContainerValue {

    /**
     * Save here when this ContainerValue overlay was rendered the last time.
     */
    private long overlayLastRenderedAt = 0;
    /**
     * Render the overlay only once every x milliseconds to reduce lag when in a GUI.
     */
    private final int interval = 1000;

    @SubscribeEvent // render overlay when a new gui was opened
    public void onOpenGui(GuiOpenEvent e) {
        this.overlayLastRenderedAt = 0;
    }

    @SubscribeEvent
    public void onDrawGuiBackground(GuiScreenEvent.BackgroundDrawnEvent e) {
        // Guis like main menu, inventory and anything else that's not a chest.
        if(!(e.gui instanceof GuiChest)) return;

        // Skip rendering this time if it was rendered shortly before.
        long currentTime = System.currentTimeMillis();
        if(currentTime < this.overlayLastRenderedAt + this.interval) return;
        this.overlayLastRenderedAt = currentTime;

        // Find and save prices of all items with attributes in that GUI.
        List<Slot> allSlots = ((GuiChest) e.gui).inventorySlots.inventorySlots;
        List<Slot> chestSlots = allSlots.subList(0, allSlots.size() - 36);
        for(Slot slot : chestSlots) {
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

            ItemWithAttributes.Evaluation price = item.getDetailedPrice();
            System.out.println(price.getEstimate());
        }

        // Render Overlay
    }
}
