package dev.anderle.attributemod.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.anderle.attributemod.AttributeMod;
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

import java.io.IOException;

/**
 * Display a message from api the first time the player joins a world.
 * Used for update checks and
 */
public class OneTimeMessage {

    private boolean sent = false;

    public void onWorldJoin(EntityJoinWorldEvent e) {
        if(sent) return;
        if(!e.entity.equals(Minecraft.getMinecraft().thePlayer)) return;
        EntityPlayerSP player = (EntityPlayerSP) e.entity;
        sent = true;

        AttributeMod.backend.sendGetRequest("/onetimemessage", "",
            (String response) -> {
                JsonObject responseObject = new JsonParser().parse(response).getAsJsonObject();
                boolean isWhitelisted = responseObject.get("whitelisted").getAsBoolean();
                String latestVersion = responseObject.get("latest").getAsString();
                String downloadLink = responseObject.get("download").getAsString();

                if(isWhitelisted) {
                    if(latestVersion.equals(AttributeMod.VERSION)) return;
                    try {
                        player.addChatMessage(getUpdateAvailableMessage(latestVersion, downloadLink));
                    } catch (IOException error) {
                        AttributeMod.LOGGER.error("Failed to send One Time Message.", error);
                    }
                } else {
                    player.addChatMessage(getNotWhitelistedMessage());
                }
            },
            (IOException error) -> AttributeMod.LOGGER.error("Failed to fetch data from /onetimemessage", error));
    }

    private IChatComponent getUpdateAvailableMessage(String newVersion, String downloadLink) throws IOException {
        return new ChatComponentText(
        Constants.prefix + EnumChatFormatting.YELLOW + "New Update Available!\n" +
            Constants.prefix + EnumChatFormatting.YELLOW + "Current: " +
                EnumChatFormatting.AQUA + AttributeMod.VERSION + EnumChatFormatting.YELLOW +
                ", Latest: " + EnumChatFormatting.AQUA + newVersion + EnumChatFormatting.YELLOW +
                ".\n\n" +
            Constants.prefix + EnumChatFormatting.GREEN + "[ ")
        .appendSibling(ChatUtils.chatLink("Download", downloadLink, EnumChatFormatting.GREEN))
        .appendSibling(new ChatComponentText(EnumChatFormatting.GREEN + " ] " + EnumChatFormatting.BLUE + "[ "))
        .appendSibling(new ChatComponentText(
        "Open Mod Folder").setChatStyle(new ChatStyle()
            .setChatClickEvent(new ClickEvent(
                    ClickEvent.Action.OPEN_FILE, AttributeMod.modFolder.getCanonicalPath()
            )).setUnderlined(true).setColor(EnumChatFormatting.BLUE)))
        .appendSibling(new ChatComponentText(EnumChatFormatting.BLUE + " ]"));
    }

    private IChatComponent getNotWhitelistedMessage() {
        return new ChatComponentText(Constants.prefix + EnumChatFormatting.RED +
                "Looks like you're not whitelisted to use this mod. Please use "
                + EnumChatFormatting.GOLD + "/attributemod" + EnumChatFormatting.RED +
                ", to set or update your key.");
    }
}
