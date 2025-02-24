package dev.anderle.attributemod.features;

import com.google.gson.*;
import dev.anderle.attributemod.AttributeMod;
import dev.anderle.attributemod.api.Backend;
import dev.anderle.attributemod.utils.Helper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.Slot;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class KuudraProfit {
    // Stores the overlay to be rendered with next BackgroundDrawnEvent.
    private final ArrayList<String> toRender = new ArrayList<>();
    // Needs the ContainerValue object to infer position, scale and state from.
    private final ContainerValue containerValue;

    public KuudraProfit(ContainerValue containerValue) {
        this.containerValue = containerValue;
    }

    // I would get all items instantly, but I think Hypixel sends those later.
    public void onGuiOpen(ContainerChest chest) {
        toRender.clear();
        if(!containerValue.isEnabled()) return;
        new Thread(() -> {
            try {
                Thread.sleep(100);
                getProfitData(chest);
            } catch (InterruptedException ignored) {}
        }).start();
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
                for(JsonElement line : new JsonParser().parse(a.replaceAll("§", "\u00a7")).getAsJsonArray()) {
                    toRender.add(line.getAsString());
                }
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();
            }
        });
    }

    private char getKuudraTier() {
        try { // I have no idea how the scoreboard works so I just try catch everything.
            Scoreboard scoreboard = Minecraft.getMinecraft().theWorld.getScoreboard();
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
            e.printStackTrace();
        }
        // Should not happen because those chests should always be in Kuudra.
        // Will result in "400 Bad Request".
        return '0';
    }

    public void onDrawGuiBackground(GuiChest gui) {
        if(toRender.isEmpty()) return;

        double overlayScale = containerValue.getOverlayScale();
        Point overlayPos = containerValue.getOverlayPos();

        GL11.glPushMatrix();
        GL11.glScaled(overlayScale, overlayScale, 1);

        for(int i = 0; i < toRender.size(); i++) {
            gui.drawString(
                    gui.mc.fontRendererObj, toRender.get(i),
                    overlayPos.x, overlayPos.y + (i + 1) * (gui.mc.fontRendererObj.FONT_HEIGHT + 1),
                    0xffffffff
            );
        }

        GL11.glPopMatrix();
    }
}
