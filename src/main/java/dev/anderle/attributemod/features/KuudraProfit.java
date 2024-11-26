package dev.anderle.attributemod.features;

import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.Slot;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.client.event.GuiScreenEvent;

import java.util.List;

public class KuudraProfit {
    public KuudraProfit() {

    }

    public void onGuiOpen(ContainerChest chest) {
        System.out.println("test");
        List<Slot> slots = chest.inventorySlots.subList(0, chest.inventorySlots.size() - 36);
        for(Slot slot : slots) {
            System.out.println(slot.slotNumber);
            if(!slot.getHasStack()) continue;
            System.out.println(slot.getStack().getDisplayName());
            NBTTagCompound c = slot.getStack().serializeNBT();
            System.out.println(c);
        }
    }

    public void onDrawGuiBackground(GuiScreenEvent.BackgroundDrawnEvent e) {

    }
}
