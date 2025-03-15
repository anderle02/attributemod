package dev.anderle.attributemod.features;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.anderle.attributemod.AttributeMod;
import dev.anderle.attributemod.utils.ChatUtils;
import dev.anderle.attributemod.utils.Constants;
import gg.essential.lib.caffeine.cache.Cache;
import gg.essential.lib.caffeine.cache.Caffeine;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.EnumChatFormatting;
import org.apache.http.client.HttpResponseException;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class KuudraStatsCommand extends CommandBase {
    public static final Pattern PARTY_JOIN_PATTERN = Pattern.compile(
            "Party Finder > (.+) joined the group! \\(.*\\)");
    public static final Cache<String, String> CACHE = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .maximumSize(100)
            .build();

    @Override
    public String getCommandName() {
        return "kuudra";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return Constants.prefix + EnumChatFormatting.DARK_RED
                + "Correct Usage: " + EnumChatFormatting.RED
                + "/" + getCommandName() + " [ign]";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        String ign = args.length == 0 ? AttributeMod.mc.thePlayer.getName() : args[0];
        String cached = CACHE.getIfPresent(ign.toLowerCase());

        if(cached != null) {
            sender.addChatMessage(ChatUtils.decodeToFancyChatMessage(cached));
            return;
        }

        AttributeMod.backend.sendGetRequest("/kuudrastats", "&ign=" + ign.toLowerCase(),
            (String result) -> {
                JsonObject resultObj = new JsonParser().parse(result).getAsJsonObject();
                String playerName = resultObj.getAsJsonPrimitive("ign").getAsString();
                String text = resultObj.getAsJsonPrimitive("text").getAsString();
                sender.addChatMessage(ChatUtils.decodeToFancyChatMessage(text));
                CACHE.put(playerName.toLowerCase(), text);
            }, (IOException error) -> {
                int statusCode = error instanceof HttpResponseException
                        ? ((HttpResponseException) error).getStatusCode() : 0;
                sender.addChatMessage(ChatUtils.errorMessage(error.getMessage(),
                        statusCode == 0 || statusCode == 400));
            });
    }
}
