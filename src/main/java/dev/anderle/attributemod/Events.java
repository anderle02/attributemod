package dev.anderle.attributemod;

import dev.anderle.attributemod.features.ContainerValue;
import dev.anderle.attributemod.features.KuudraProfit;
import dev.anderle.attributemod.features.OneTimeMessage;
import dev.anderle.attributemod.features.TooltipPriceDisplay;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.inventory.ContainerChest;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class Events {
    // Classes that need events are initialized here, to avoid duplicate event handlers.
    ContainerValue containerValue = new ContainerValue();
    OneTimeMessage oneTimeMessage = new OneTimeMessage();
    KuudraProfit kuudraProfit = new KuudraProfit();
    TooltipPriceDisplay tooltipPriceDisplay = new TooltipPriceDisplay();

    // Helper variables.
    private boolean currentGuiIsChest = false;
    private boolean currentGuiIsKuudraPaidChest = false;

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent e) {
        if(!(e.gui instanceof GuiChest)) {
            currentGuiIsChest = false;
        } else if(!(((GuiChest) e.gui).inventorySlots instanceof ContainerChest)) {
            currentGuiIsChest = false;
        } else {
            currentGuiIsChest = true;
            currentGuiIsKuudraPaidChest = ((ContainerChest) ((GuiChest) e.gui).inventorySlots)
                    .getLowerChestInventory().getDisplayName().getUnformattedText()
                    .contains("(Slot #6)");

            if(currentGuiIsKuudraPaidChest) {
                kuudraProfit.onGuiOpen((ContainerChest) ((GuiChest) e.gui).inventorySlots);
            } else {
                containerValue.onGuiOpen();
            }

            System.out.println(((ContainerChest) ((GuiChest) e.gui).inventorySlots)
                    .getLowerChestInventory().getDisplayName().getUnformattedText());
        }
    }

    @SubscribeEvent
    public void onDrawGuiBackground(GuiScreenEvent.BackgroundDrawnEvent e) {
        if(currentGuiIsKuudraPaidChest) kuudraProfit.onDrawGuiBackground(e);
        else if(currentGuiIsChest) containerValue.onDrawGuiBackground(e);
    }

    @SubscribeEvent
    public void onDrawGuiForeground(GuiScreenEvent.DrawScreenEvent e) {
        if(currentGuiIsChest && !currentGuiIsKuudraPaidChest) containerValue.onDrawGuiForeground(e);
    }

    @SubscribeEvent
    public void onKeyboardInput(GuiScreenEvent.KeyboardInputEvent.Post e) {
        if(currentGuiIsChest && !currentGuiIsKuudraPaidChest) containerValue.onKeyboardInput();
    }

    @SubscribeEvent
    public void onMouseInput(GuiScreenEvent.MouseInputEvent e) {
        if(currentGuiIsChest && !currentGuiIsKuudraPaidChest) containerValue.onMouseInput(e);
    }

    @SubscribeEvent
    public void onWorldJoin(EntityJoinWorldEvent e) {
        oneTimeMessage.onWorldJoin(e);
    }

    @SubscribeEvent
    public void onRenderToolTip(ItemTooltipEvent e) {
        tooltipPriceDisplay.onRenderToolTip(e);
    }
}