package dev.anderle.attributemod;

import dev.anderle.attributemod.features.*;
import dev.anderle.attributemod.overlay.ChestOverlayElement;
import dev.anderle.attributemod.overlay.HudOverlayElement;
import dev.anderle.attributemod.overlay.OverlayElement;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Mouse;

public class Events {
    // Some objects that respond to events are initialized here, to avoid duplicate event handlers.
    public static OneTimeMessage oneTimeMessage;
    public static TooltipPriceDisplay tooltipPriceDisplay;

    // Helper variables.
    public static boolean showVigilanceGuiWithNextTick = false;

    public static void initializeFeatures() {
        oneTimeMessage = new OneTimeMessage();
        tooltipPriceDisplay = new TooltipPriceDisplay();
    }

    @SubscribeEvent @SuppressWarnings("unused")
    public void onGuiOpen(GuiOpenEvent e) {
        OverlayElement.ALL.stream()
                .filter(element -> element instanceof ChestOverlayElement && element.shouldRender(e.gui))
                .map(element -> (ChestOverlayElement) element)
                .forEach(element -> element.onGuiOpen((GuiChest) e.gui));
    }

    @SubscribeEvent @SuppressWarnings("unused")
    public void onRenderGameOverlay(RenderGameOverlayEvent e) {
        OverlayElement.ALL.stream()
                .filter(element -> element instanceof HudOverlayElement && element.shouldRender(null))
                .map(element -> (HudOverlayElement) element)
                .forEach(element -> {
                    element.updateIfNeeded(null);
                    element.renderOverlay(null);
                });
    }

    @SubscribeEvent @SuppressWarnings("unused")
    public void onDrawGuiBackground(GuiScreenEvent.BackgroundDrawnEvent e) {
        OverlayElement.ALL.stream()
                .filter(element -> element instanceof ChestOverlayElement && element.shouldRender(e.gui))
                .map(element -> (ChestOverlayElement) element)
                .forEach(element -> {
                    element.updateIfNeeded(e.gui);
                    element.renderOverlay(e.gui);
                });
    }

    @SubscribeEvent @SuppressWarnings("unused")
    public void onDrawGuiForeground(GuiScreenEvent.DrawScreenEvent e) {
        OverlayElement.ALL.stream()
                .filter(element -> element instanceof ChestOverlayElement && element.shouldRender(e.gui))
                .map(element -> (ChestOverlayElement) element)
                .forEach(element -> element.onDrawForeground((GuiChest) e.gui));
    }

    @SubscribeEvent @SuppressWarnings("unused")
    public void onMouseInput(GuiScreenEvent.MouseInputEvent.Pre e) {
        double mousePosX = (double) Mouse.getEventX() * e.gui.width / e.gui.mc.displayWidth;
        double mousePosY = (e.gui.height - (double) Mouse.getEventY() * e.gui.height / e.gui.mc.displayHeight);

        OverlayElement.ALL.stream()
                .filter(element -> element instanceof ChestOverlayElement && element.shouldRender(e.gui) && element.isInside(mousePosX, mousePosY))
                .map(element -> (ChestOverlayElement) element)
                .forEach(element -> {
                    if(Mouse.getEventButton() == 0) {
                        element.onClick(e, mousePosX, mousePosY);
                    } else if(Mouse.getEventDWheel() != 0) {
                        element.onScroll((GuiChest) e.gui, Mouse.getEventDWheel() < 0);
                        element.onHover((GuiChest) e.gui, mousePosX, mousePosY);
                    } else {
                        element.onHover((GuiChest) e.gui, mousePosX, mousePosY);
                    }
                });
    }

    @SubscribeEvent @SuppressWarnings("unused")
    public void onWorldJoin(EntityJoinWorldEvent e) {
        oneTimeMessage.onWorldJoin(e);
    }

    @SubscribeEvent @SuppressWarnings("unused")
    public void onRenderToolTip(ItemTooltipEvent e) {
        tooltipPriceDisplay.onRenderToolTip(e);
    }

    @SubscribeEvent @SuppressWarnings("unused")
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
        // Checks for due tasks and executes them on a different thread.
        AttributeMod.scheduler.executeTasksIfNeeded();
    }
}