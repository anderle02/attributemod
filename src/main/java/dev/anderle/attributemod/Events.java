package dev.anderle.attributemod;

import dev.anderle.attributemod.api.OneTimeMessage;
import dev.anderle.attributemod.commands.AuCommand;
import dev.anderle.attributemod.commands.KuudraStatsCommand;
import dev.anderle.attributemod.overlay.*;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.inventory.Slot;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Mouse;

import java.util.regex.Matcher;

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
        if(!AttributeMod.config.modEnabled) return;

        Overlay.ALL.stream()
                .filter(element -> element instanceof ChestOverlay && element.shouldRender(e.gui))
                .map(element -> (ChestOverlay) element)
                .forEach(element -> element.onGuiOpen((GuiChest) e.gui));
    }

    @SubscribeEvent @SuppressWarnings("unused")
    public void onRenderGameOverlayText(RenderGameOverlayEvent.Text e) {
        if(!AttributeMod.config.modEnabled) return;

        Overlay.ALL.stream()
                .filter(element -> element instanceof HudOverlay && element.shouldRender(AttributeMod.mc.currentScreen))
                .map(element -> (HudOverlay) element)
                .forEach(element -> {
                    element.updateIfNeeded(null);
                    element.renderOverlay(null);
                });
    }

    @SubscribeEvent @SuppressWarnings("unused")
    public void onDrawGuiBackground(GuiScreenEvent.BackgroundDrawnEvent e) {
        if(!AttributeMod.config.modEnabled) return;

        Overlay.ALL.stream()
                .filter(element -> element instanceof ChestOverlay && element.shouldRender(e.gui))
                .map(element -> (ChestOverlay) element)
                .forEach(element -> {
                    element.updateIfNeeded(e.gui);
                    element.renderOverlay(e.gui);
                });
    }

    @SubscribeEvent @SuppressWarnings("unused")
    public void onDrawGuiForeground(GuiScreenEvent.DrawScreenEvent e) {
        if(!AttributeMod.config.modEnabled) return;

        Overlay.ALL.stream()
                .filter(element -> element instanceof ChestOverlay && element.shouldRender(e.gui))
                .map(element -> (ChestOverlay) element)
                .forEach(element -> element.onDrawForeground((GuiChest) e.gui));
    }

    @SubscribeEvent @SuppressWarnings("unused")
    public void onMouseInput(GuiScreenEvent.MouseInputEvent.Pre e) {
        if(!AttributeMod.config.modEnabled) return;

        double mousePosX = (double) Mouse.getEventX() * e.gui.width / e.gui.mc.displayWidth;
        double mousePosY = (e.gui.height - (double) Mouse.getEventY() * e.gui.height / e.gui.mc.displayHeight);

        Overlay.ALL.stream()
                .filter(element -> element instanceof ChestOverlay && element.shouldRender(e.gui))
                .map(element -> (ChestOverlay) element)
                .forEach(element -> {
                    if(element.isInside(mousePosX, mousePosY)) {
                        if(Mouse.getEventButton() == 0 && Mouse.isButtonDown(0)) {
                            element.onClickOverlay(e, mousePosX, mousePosY);
                        } else if(Mouse.getEventDWheel() != 0) {
                            element.onScroll((GuiChest) e.gui, Mouse.getEventDWheel() < 0);
                            element.onHover((GuiChest) e.gui, mousePosX, mousePosY);
                        } else {
                            element.onHover((GuiChest) e.gui, mousePosX, mousePosY);
                        }
                    } else {
                        if(Mouse.getEventButton() == 0 && Mouse.isButtonDown(0)) {
                            Slot slot = ((GuiChest) e.gui).getSlotUnderMouse();
                            if(slot != null) element.onClickSlot(e, slot);
                        }
                        if(element instanceof ChestItemDisplay) {
                            ((ChestItemDisplay) element).resetHoveredItem();
                        }
                    }
                });
    }

    @SubscribeEvent @SuppressWarnings("unused")
    public void onWorldLoad(WorldEvent.Load e) {
        if(AttributeMod.config.modEnabled) {
            oneTimeMessage.onWorldLoad();

            Overlay.ALL.stream()
                    .filter(o -> o instanceof ProfitPerHourOverlay).findFirst()
                    .ifPresent(o -> ((ProfitPerHourOverlay) o).onWorldLoad(e.world));
        }
    }

    @SubscribeEvent @SuppressWarnings("unused")
    public void onRenderToolTip(ItemTooltipEvent e) {
        if(AttributeMod.config.modEnabled && AttributeMod.config.tooltipAttributePriceEnabled) {
            tooltipPriceDisplay.onRenderToolTip(e);
        }
    }

    @SubscribeEvent @SuppressWarnings("unused")
    public void onChat(ClientChatReceivedEvent e) {
        if(!AttributeMod.config.modEnabled || !AttributeMod.config.partyFinderShowStats) return;

        Matcher matcher = KuudraStatsCommand.PARTY_JOIN_PATTERN.matcher(e.message.getUnformattedText());
        if(matcher.matches()) {
            String playerName = matcher.group(1);
            if(playerName.equals(AttributeMod.mc.thePlayer.getName())) return;
            ClientCommandHandler.instance.executeCommand(AttributeMod.mc.thePlayer, "/kuudra " + playerName);
        }
    }

    @SubscribeEvent @SuppressWarnings("unused")
    public void onKeyPress(InputEvent.KeyInputEvent e) {
        if(!AttributeMod.config.modEnabled) return;
        if(AuCommand.auBuyNextKey.isKeyDown() && AttributeMod.mc.thePlayer != null) {
            AttributeMod.commands.stream().filter(c -> c instanceof AuCommand).findAny()
                    .ifPresent(c -> ((AuCommand) c).openNextAuction());
        }
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
        if(AttributeMod.config.modEnabled) {
            AttributeMod.scheduler.executeTasksIfNeeded();
        }
    }
}