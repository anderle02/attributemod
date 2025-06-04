package dev.anderle.attributemod.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.anderle.attributemod.AttributeMod;
import dev.anderle.attributemod.utils.Constants;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Display a message from api the first time the player joins a world.
 * Used for update checks and
 */
public class OneTimeMessage {

    private boolean sent = false;

    public void onWorldLoad() {
        if(sent) return;
        sent = true;

        AttributeMod.backend.sendGetRequest("/onetimemessage", "",
            (String response) -> {
                JsonObject responseObject = JsonParser.parseString(response).getAsJsonObject();
                boolean isWhitelisted = responseObject.get("whitelisted").getAsBoolean();
                String latestVersion = responseObject.get("latest").getAsString();
                String downloadLink = responseObject.get("download").getAsString();
                ChatHud chat = AttributeMod.mc.inGameHud.getChatHud();

                if(isWhitelisted) {
                    if(latestVersion.equals(AttributeMod.VERSION)) return;
                    try {
                        chat.addMessage(getUpdateAvailableMessage(latestVersion, downloadLink));
                    } catch (IOException | URISyntaxException error) {
                        AttributeMod.LOGGER.error("Failed to send One Time Message.", error);
                    }
                } else {
                    chat.addMessage(getNotWhitelistedMessage());
                }
            },
            (Exception error) -> AttributeMod.LOGGER.error("Failed to fetch data from /onetimemessage", error));
    }

    private Text getUpdateAvailableMessage(String newVersion, String downloadLink) throws IOException, URISyntaxException {
        return Text.literal(Constants.prefix)
            .append(Text.literal("New Update Available!\n").formatted(Formatting.YELLOW))
            .append(Text.literal(Constants.prefix))
            .append(Text.literal("Current: ").formatted(Formatting.YELLOW))
            .append(Text.literal(AttributeMod.VERSION).formatted(Formatting.AQUA))
            .append(Text.literal(", Latest: ").formatted(Formatting.YELLOW))
            .append(Text.literal(newVersion).formatted(Formatting.AQUA))
            .append(Text.literal(".\n\n").formatted(Formatting.YELLOW))
            .append(Text.literal(Constants.prefix))
            .append(Text.literal("[ ").formatted(Formatting.GREEN))

            .append(Text.literal("Download").setStyle(Style.EMPTY
                .withClickEvent(new ClickEvent.OpenUrl(new URI(downloadLink)))
                .withUnderline(true)
                .withColor(Formatting.GREEN)))

            .append(Text.literal(" ] ").formatted(Formatting.GREEN))
            .append(Text.literal("[ ").formatted(Formatting.BLUE))

            .append(Text.literal("Open Mod Folder").setStyle(Style.EMPTY
                .withClickEvent(new ClickEvent.OpenFile(AttributeMod.MOD_FOLDER))
                .withUnderline(true)
                .withColor(Formatting.BLUE)))

            .append(Text.literal(" ]").formatted(Formatting.BLUE));
    }

    private Text getNotWhitelistedMessage() {
        return Text.literal(Constants.prefix)
                .append(Text.literal("Looks like you're not whitelisted to use this mod. Please use ").formatted(Formatting.RED))
                .append(Text.literal("/attributemod").formatted(Formatting.GOLD))
                .append(Text.literal(", to set or update your key.").formatted(Formatting.RED));
    }
}
