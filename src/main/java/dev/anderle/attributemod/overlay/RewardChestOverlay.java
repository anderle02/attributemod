package dev.anderle.attributemod.overlay;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.anderle.attributemod.AttributeMod;
import dev.anderle.attributemod.api.Backend;
import dev.anderle.attributemod.utils.Helper;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.Slot;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraftforge.client.event.GuiScreenEvent;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.Collection;
import java.util.List;

public class RewardChestOverlay extends ChestOverlayElement {
    public static final int MAX_WIDTH = AttributeMod.mc.fontRendererObj.getStringWidth(
            "+ 888.8M Crimson Chestplate [Arachno Resistance 5] [Blazing Resistance 4]");

    public RewardChestOverlay() {
        super("Kuudra Reward Chest Overlay", 0, Color.orange);
    }

    @Override
    public void onGuiOpen(GuiChest chest) {
        content.clear();
        new Thread(() -> {
            try {
                Thread.sleep(200);
                getProfitData((ContainerChest) chest.inventorySlots);
            } catch (InterruptedException ignored) {}
        }).start();
    }

    @Override
    public void onClick(GuiScreenEvent.MouseInputEvent e, double mouseX, double mouseY) {

    }

    @Override
    public void onHover(GuiChest chest, double mouseX, double mouseY) {

    }

    @Override
    public void onScroll(GuiChest chest, boolean direction) {

    }

    @Override
    public void onDrawForeground(GuiChest chest) {

    }

    @Override
    public void renderOverlay(GuiScreen screen) {
        if(content.isEmpty()) return;

        GL11.glPushMatrix();
        GL11.glScaled(scale, scale, 1);

        for(int i = 0; i < content.size(); i++) {
            screen.drawString(
                    screen.mc.fontRendererObj, content.get(i),
                    position.x, position.y + (i + 1) * (screen.mc.fontRendererObj.FONT_HEIGHT + 1),
                    0xffffffff
            );
        }

        GL11.glPopMatrix();
    }

    @Override
    protected void updateData(GuiScreen screen) {

    }

    @Override
    public void saveScaleAndLocation() {
        AttributeMod.config.kuudraChestOverlayScale = (int) scale;
        AttributeMod.config.kuudraChestOverlayX = position.x;
        AttributeMod.config.kuudraChestOverlayY = position.y;

        AttributeMod.config.markDirty();
    }

    @Override
    public void readScaleAndLocation() {
        scale = AttributeMod.config.kuudraChestOverlayScale;
        position = new Point(AttributeMod.config.kuudraChestOverlayX, AttributeMod.config.kuudraChestOverlayY);
        size = getSize();
    }

    @Override
    public boolean isEnabled() {
        return AttributeMod.config.kuudraChestOverlayEnabled;
    }

    @Override
    public boolean shouldRender() {
        if(!this.isEnabled()) return false;

        GuiScreen screen = AttributeMod.mc.currentScreen;
        if(!(screen instanceof GuiChest)) return false;

        return ((ContainerChest) ((GuiChest) screen).inventorySlots)
                .getLowerChestInventory().getDisplayName().getUnformattedText()
                .contains("Paid Chest");
    }

    @Override
    public Dimension getSize() {
        return new Dimension(
                (int) (MAX_WIDTH * scale / 100),
                (int) (11 * (AttributeMod.mc.fontRendererObj.FONT_HEIGHT + 1) * scale / 100)
        );
    }

    private void getProfitData(ContainerChest chest) {
        List<Slot> slots = chest.inventorySlots.subList(0, chest.inventorySlots.size() - 36);
        JsonArray chestItems = new JsonArray();

        for(Slot slot : slots) {
            if(!slot.getHasStack()) continue;

            JsonObject nbt = Helper.convertNBTToJson(slot.getStack().writeToNBT(new NBTTagCompound()));
            String id = nbt.get("id").getAsString();

            if(id.equals("minecraft:stained_glass_pane") || id.equals("minecraft:chest") || id.equals("minecraft:barrier")) continue;

            chestItems.add(nbt);
        }

        AttributeMod.backend.sendPostRequest("/kuudrachest", "&tier=" + getKuudraTier(), chestItems.toString(), new Backend.ResponseCallback() {
            @Override
            public void onResponse(String a) {
                for(JsonElement line : new JsonParser().parse(a.replaceAll("§", "§")).getAsJsonArray()) {
                    content.add(line.getAsString());
                }
            }

            @Override
            public void onError(Exception e) {
                AttributeMod.LOGGER.error("Failed fetching from /kuudrachest." , e);
            }
        });
    }

    private char getKuudraTier() {
        try { // I have no idea how the scoreboard works so I just try catch everything.
            Scoreboard scoreboard = AttributeMod.mc.theWorld.getScoreboard();
            ScoreObjective sidebarObjective = scoreboard.getObjectiveInDisplaySlot(1);
            Collection<Score> scores = scoreboard.getSortedScores(sidebarObjective);
            for(Score score : scores) {
                ScorePlayerTeam team = scoreboard.getPlayersTeam(score.getPlayerName());
                String scoreboardLine = ScorePlayerTeam.formatPlayerName(team, score.getPlayerName()).trim();
                if(scoreboardLine.contains("Kuudra's ")) {
                    return scoreboardLine.charAt(scoreboardLine.length() - 2);
                }
            }
        } catch(Exception e) {
            AttributeMod.LOGGER.error("Error getting Kuudra Tier from Scoreboard.", e);
        }
        // Should not happen because those chests should always be in Kuudra.
        // Will result in "400 Bad Request".
        return '0';
    }
}
