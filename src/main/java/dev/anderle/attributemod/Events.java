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
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class Events {
    // Classes that need events are initialized here, to avoid duplicate event handlers.
    ContainerValue containerValue = new ContainerValue();
    OneTimeMessage oneTimeMessage = new OneTimeMessage();
    KuudraProfit kuudraProfit = new KuudraProfit(containerValue);
    TooltipPriceDisplay tooltipPriceDisplay = new TooltipPriceDisplay();

    // Helper variables.
    public static boolean showVigilanceGuiWithNextTick = false;
    private boolean currentGuiIsKuudraPaidChest = false;

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent e) {
        if(!(e.gui instanceof GuiChest) || !(((GuiChest) e.gui).inventorySlots instanceof ContainerChest)) {
            currentGuiIsKuudraPaidChest = false;
            return;
        }

        currentGuiIsKuudraPaidChest = ((ContainerChest) ((GuiChest) e.gui).inventorySlots)
                .getLowerChestInventory().getDisplayName().getUnformattedText()
                .contains("Paid Chest");

        if(currentGuiIsKuudraPaidChest) kuudraProfit.onGuiOpen((ContainerChest) ((GuiChest) e.gui).inventorySlots);
        else containerValue.onGuiOpen();
    }

    @SubscribeEvent
    public void onDrawGuiBackground(GuiScreenEvent.BackgroundDrawnEvent e) {
        if(!(e.gui instanceof GuiChest) || !(((GuiChest) e.gui).inventorySlots instanceof ContainerChest)) return;
        if(currentGuiIsKuudraPaidChest) kuudraProfit.onDrawGuiBackground((GuiChest) e.gui);
        else containerValue.onDrawGuiBackground(e);
    }

    @SubscribeEvent
    public void onDrawGuiForeground(GuiScreenEvent.DrawScreenEvent e) {
        if(!(e.gui instanceof GuiChest) || !(((GuiChest) e.gui).inventorySlots instanceof ContainerChest)) return;
        if(!currentGuiIsKuudraPaidChest) containerValue.onDrawGuiForeground(e);
    }

    @SubscribeEvent
    public void onKeyboardInput(GuiScreenEvent.KeyboardInputEvent.Post e) {
        if(!(e.gui instanceof GuiChest) || !(((GuiChest) e.gui).inventorySlots instanceof ContainerChest)) return;
        if(!currentGuiIsKuudraPaidChest) containerValue.onKeyboardInput();
    }

    @SubscribeEvent
    public void onMouseInput(GuiScreenEvent.MouseInputEvent e) {
        if(!(e.gui instanceof GuiChest) || !(((GuiChest) e.gui).inventorySlots instanceof ContainerChest)) return;
        if(!currentGuiIsKuudraPaidChest) containerValue.onMouseInput(e);
    }

    @SubscribeEvent
    public void onWorldJoin(EntityJoinWorldEvent e) {
        oneTimeMessage.onWorldJoin(e);
    }

    @SubscribeEvent
    public void onRenderToolTip(ItemTooltipEvent e) {
        tooltipPriceDisplay.onRenderToolTip(e);
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            return;
        }
        // This is needed to open the vigilance config gui with next tick, after running the settings command.
        // Minecraft closes the ChatGui after opening the config gui, closing the config gui too (we love Minecraft).
        if (showVigilanceGuiWithNextTick) {
            showVigilanceGuiWithNextTick = false;
            AttributeMod.mc.displayGuiScreen(AttributeMod.config.gui());
        }
    }
}