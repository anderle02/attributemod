package dev.anderle.attributemod.overlay;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.anderle.attributemod.AttributeMod;
import dev.anderle.attributemod.utils.Helper;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.Slot;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraftforge.client.event.GuiScreenEvent;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class KuudraProfitDisplay extends ChestOverlay {
    // Didn't take the longest possible width so it's not that wide.
    // Doesn't really matter for this overlay because it's not clickable anyway.
    public static final int MAX_WIDTH = AttributeMod.mc.fontRendererObj.getStringWidth(
            "+ 888.8M Crimson Chestplate [Veteran 5] [Dominance 4]");

    public static final String CLAIM_ITEM_NAME = "Open Reward Chest";

    /** The strings to render with next render tick. */
    private final ArrayList<String> content = new ArrayList<>();
    /** The profit of the last chest (needed for profit/h overlay). */
    private int lastChestProfit = 0;

    public KuudraProfitDisplay() {
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
    public void onClickOverlay(GuiScreenEvent.MouseInputEvent e, double mouseX, double mouseY) {

    }

    @Override
    public void onClickSlot(GuiScreenEvent.MouseInputEvent e, Slot slot) {
        if(!slot.getHasStack()) return;
        if(!slot.getStack().getDisplayName().contains(CLAIM_ITEM_NAME)) return;

        Overlay overlay = Overlay.ALL.stream().filter(o -> o instanceof ProfitPerHourOverlay).findFirst().orElse(null);
        if(overlay == null || !overlay.isEnabled()) return;

        ((ProfitPerHourOverlay) overlay).onKuudraPaidChestClaimed(lastChestProfit);
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
        GL11.glScaled(scale / 100, scale / 100, 1);

        for(int i = 0; i < content.size(); i++) {
            screen.drawString(
                    screen.mc.fontRendererObj, content.get(i),
                    (int) (position.x * 100 / scale), (int) (position.y * 100 / scale) + i * (screen.mc.fontRendererObj.FONT_HEIGHT + 1),
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
    public boolean shouldRender(GuiScreen screen) {
        if(!this.isEnabled() || !(screen instanceof GuiChest)) return false;

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

        Scoreboard scoreboard = AttributeMod.mc.theWorld.getScoreboard();

        AttributeMod.backend.sendPostRequest(
                "/kuudrachest",
                "&tier=" + Helper.getKuudraTier(scoreboard) + "&bonusessence=" + AttributeMod.config.essenceBonus,
                chestItems.toString(),
                (String response) -> {
                    content.clear();
                    for(JsonElement line : new JsonParser().parse(response).getAsJsonArray()) {
                        // For compatibility with older mod versions, the backend now sends the raw total profit as last line.
                        if(line.getAsString().startsWith("%")) {
                            lastChestProfit = (int) Double.parseDouble(line.getAsString().replace("%", ""));
                        } else {
                            content.add(line.getAsString());
                        }
                    }
                }, (IOException error) -> AttributeMod.LOGGER.error("Failed fetching from /kuudrachest." , error));
    }
}
