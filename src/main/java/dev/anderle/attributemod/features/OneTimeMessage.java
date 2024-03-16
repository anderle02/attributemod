package dev.anderle.attributemod.features;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.anderle.attributemod.Main;
import dev.anderle.attributemod.api.PriceApi;
import dev.anderle.attributemod.utils.ChatUtils;
import dev.anderle.attributemod.utils.Constants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.event.ClickEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.io.IOException;

/**
 * Display a message from api the first time the player joins a world.
 * Used for update checks and
 */
public class OneTimeMessage {

    private boolean sent = false;

    @SubscribeEvent
    public void onWorldJoin(EntityJoinWorldEvent e) {
        if(this.sent) return;
        if(!e.entity.equals(Minecraft.getMinecraft().thePlayer)) return;
        final EntityPlayerSP player = (EntityPlayerSP) e.entity;
        this.sent = true;

        Main.api.request("/onetimemessage", "", new PriceApi.ResponseCallback() {
            @Override
            public void onResponse(String a) {
                JsonObject response = new JsonParser().parse(a).getAsJsonObject();
                boolean isWhitelisted = response.get("whitelisted").getAsBoolean();
                String latestVersion = response.get("latest").getAsString();
                String downloadLink = response.get("download").getAsString();

                if(isWhitelisted) {
                    if(latestVersion.equals(Main.VERSION)) return;
                    try {
                        player.addChatMessage(getUpdateAvailableMessage(latestVersion, downloadLink));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    player.addChatMessage(getNotWhitelistedMessage());
                }
            }

            @Override
            public void onError(Exception ignored) {}
        });
    }

    private IChatComponent getUpdateAvailableMessage(String newVersion, String downloadLink) throws IOException {
        return new ChatComponentText(
        Constants.prefix + EnumChatFormatting.YELLOW + "New Update Available!\n" +
            Constants.prefix + EnumChatFormatting.YELLOW + "Current: " +
                EnumChatFormatting.AQUA + Main.VERSION + EnumChatFormatting.YELLOW +
                ", Latest: " + EnumChatFormatting.AQUA + newVersion + EnumChatFormatting.YELLOW +
                ".\n\n" +
            Constants.prefix + EnumChatFormatting.GREEN + "[ ")
        .appendSibling(ChatUtils.chatLink("Download", downloadLink, EnumChatFormatting.GREEN))
        .appendSibling(new ChatComponentText(EnumChatFormatting.GREEN + " ] " + EnumChatFormatting.BLUE + "[ "))
        .appendSibling(new ChatComponentText(
        "Open Mod Folder").setChatStyle(new ChatStyle()
            .setChatClickEvent(new ClickEvent(
                    ClickEvent.Action.OPEN_FILE, Main.modFolder.getCanonicalPath()
            )).setUnderlined(true).setColor(EnumChatFormatting.BLUE)))
        .appendSibling(new ChatComponentText(EnumChatFormatting.BLUE + " ]"));
    }

    private IChatComponent getNotWhitelistedMessage() {
        return new ChatComponentText(
        Constants.prefix + EnumChatFormatting.RED +
                "Looks like you're not whitelisted to use this mod. Please use "
                + EnumChatFormatting.GOLD + "/attributemod setkey <key> " + EnumChatFormatting.RED +
                "to set or update your key. " + EnumChatFormatting.RED + "Please visit ")
        .appendSibling(ChatUtils.chatLink("Kuudra Gang", "https://discord.gg/kuudra", EnumChatFormatting.RED))
        .appendSibling(new ChatComponentText(
        EnumChatFormatting.RED + ", and run " + EnumChatFormatting.GOLD + "/mod" +
            EnumChatFormatting.RED + " there for instructions on how to obtain a key."
        ));
    }
}
