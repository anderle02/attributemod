package dev.anderle.attributemod.overlay;

import dev.anderle.attributemod.AttributeMod;
import dev.anderle.attributemod.utils.Helper;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.EnumChatFormatting;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.time.Duration;
import java.util.ArrayList;

public class ProfitPerHourOverlay extends HudOverlay {
    public static final int MAX_WIDTH = AttributeMod.mc.fontRendererObj.getStringWidth(
            "Time Played: 88 days, 00:00:00 [Stopped]");

    private long totalProfit;
    private long totalTrackedTime;
    private int openedChests;

    private long lastUpdate = 0;

    private boolean isStopped = true;
    private boolean updatedOnce = false;

    private final ArrayList<String> content = new ArrayList<>();

    public ProfitPerHourOverlay() {
        super("Profit per Hour", 1000, Color.green);
        this.totalProfit = AttributeMod.config.totalProfitK * 1000L;
        this.totalTrackedTime = AttributeMod.config.totalProfitTime;
        this.openedChests = AttributeMod.config.totalOpenedChests;
    }

    @Override
    public boolean isEnabled() {
        return AttributeMod.config.kuudraChestOverlayEnabled && AttributeMod.config.profitPerHourEnabled;
    }

    @Override
    public boolean shouldRender(GuiScreen screen) {
        return isEnabled();
    }

    @Override
    public Dimension getSize() {
        return new Dimension(
                (int) (MAX_WIDTH * scale / 100),
                (int) (4 * (AttributeMod.mc.fontRendererObj.FONT_HEIGHT + 1) * scale / 100)
        );
    }

    @Override
    protected void updateData(GuiScreen screen) {
        if(isStopped && updatedOnce) return;

        long now = System.currentTimeMillis() / 1000L;
        totalTrackedTime += lastUpdate == 0 ? 0 : now - lastUpdate;
        lastUpdate = now;

        long profitPerHour = totalTrackedTime == 0 ? 0 : totalProfit * 3600L / totalTrackedTime;
        double chestsPerHour = totalTrackedTime == 0 ? 0 : (double) (openedChests * 3600) / totalTrackedTime;
        double profitPerChest = openedChests == 0 ? 0 : (double) totalProfit / openedChests;

        content.clear();
        content.add(EnumChatFormatting.GOLD + Helper.formatNumber(profitPerHour) + EnumChatFormatting.YELLOW + " Profit / Hour");
        content.add(EnumChatFormatting.YELLOW + "Total Profit: " + EnumChatFormatting.GOLD + Helper.formatNumber(totalProfit));
        content.add(EnumChatFormatting.YELLOW + "Chests Opened: " + EnumChatFormatting.DARK_GREEN + Helper.formatNumber(openedChests)
                + EnumChatFormatting.YELLOW + " (" + EnumChatFormatting.DARK_GREEN + Helper.formatNumber(chestsPerHour)
                + EnumChatFormatting.YELLOW + " per hour)");
        content.add(EnumChatFormatting.YELLOW + "Average: " + EnumChatFormatting.GOLD + Helper.formatNumber(profitPerChest)
                + EnumChatFormatting.YELLOW + " / Chest");
        content.add(EnumChatFormatting.YELLOW + "Time Played: " + EnumChatFormatting.RED + formatSeconds(totalTrackedTime)
                + (isStopped ? EnumChatFormatting.YELLOW + " [" + EnumChatFormatting.RED + "Stopped" + EnumChatFormatting.YELLOW + "]" : ""));

        updatedOnce = true;
        saveProfitToConfig();
    }

    @Override
    public void renderOverlay(GuiScreen screen) {
        if(content.isEmpty()) return;

        FontRenderer fontRenderer = AttributeMod.mc.fontRendererObj;

        GL11.glPushMatrix();
        GL11.glScaled(scale / 100, scale / 100, 1);

        for(int i = 0; i < content.size(); i++) {
            fontRenderer.drawString(
                    content.get(i),
                    (int) (position.x * 100 / scale),
                    (int) (position.y * 100 / scale) + i * (fontRenderer.FONT_HEIGHT + 1),
                    0xffffffff
            );
        }

        GL11.glPopMatrix();
    }

    @Override
    public void saveScaleAndLocation() {
        AttributeMod.config.profitPerHourScale = (int) scale;

        AttributeMod.config.profitPerHourX = position.x;
        AttributeMod.config.profitPerHourY = position.y;

        AttributeMod.config.markDirty();
    }

    @Override
    public void readScaleAndLocation() {
        scale = AttributeMod.config.profitPerHourScale;
        position = new Point(AttributeMod.config.profitPerHourX, AttributeMod.config.profitPerHourY);
        size = getSize();
    }

    public void onKuudraPaidChestClaimed(int profit) {
        if(isStopped) return;
        totalProfit += profit;
        openedChests++;
    }

    public void reset() {
        totalProfit = 0;
        totalTrackedTime = 0;
        openedChests = 0;
        lastUpdate = 0;
        updatedOnce = false;
    }

    public void start() {
        if(!isStopped) return;
        isStopped = false;
        lastUpdate = 0;
    }

    public void stop() {
        if(isStopped) return;
        isStopped = true;
        updatedOnce = false;
    }

    private void saveProfitToConfig() {
        AttributeMod.config.totalOpenedChests = openedChests;
        AttributeMod.config.totalProfitK = (int) (totalProfit / 1000L);
        AttributeMod.config.totalProfitTime = (int) (totalTrackedTime);

        AttributeMod.config.markDirty();
    }

    private String formatSeconds(long seconds) {
        Duration duration = Duration.ofSeconds(seconds);

        long days = duration.toDays();
        long hours = duration.toHours() % 24;
        long minutes = duration.toMinutes() % 60;
        long secs = duration.getSeconds() % 60;

        if (days > 0) {
            return String.format("%d days, %02d:%02d:%02d", days, hours, minutes, secs);
        } else if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, secs);
        } else {
            return String.format("%02d:%02d", minutes, secs); // Show only minutes and seconds
        }
    }
}
