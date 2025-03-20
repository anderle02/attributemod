package dev.anderle.attributemod.commands;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.anderle.attributemod.AttributeMod;
import dev.anderle.attributemod.utils.ChatUtils;
import dev.anderle.attributemod.utils.Constants;
import gg.essential.lib.caffeine.cache.Cache;
import gg.essential.lib.caffeine.cache.Caffeine;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import org.apache.http.client.HttpResponseException;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class KuudraStatsCommand extends CommandBase {
    public static final Pattern PARTY_JOIN_PATTERN = Pattern.compile(
            "Party Finder > (.+) joined the group! \\(.*\\)");
    public static final Cache<String, IChatComponent> CACHE = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .maximumSize(100)
            .build();

    private int currentStyle = AttributeMod.config.statsMessageStyle;

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
        if(args.length > 1) {
            wrongUsage(sender);
            return;
        }

        String ign = args.length == 0 ? AttributeMod.mc.thePlayer.getName() : args[0];
        IChatComponent cached = CACHE.getIfPresent(ign.toLowerCase());

        // Get from cache if: Cached and message style wasn't changed.
        if(cached != null && currentStyle == AttributeMod.config.statsMessageStyle) {
            sender.addChatMessage(cached);
            return;
        }

        currentStyle = AttributeMod.config.statsMessageStyle;

        AttributeMod.backend.sendGetRequest(
        "/kuudrastats",
        "&ign=" + ign.toLowerCase() + "&style=" + currentStyle,
            (String result) -> {
                JsonObject resultObj = new JsonParser().parse(result).getAsJsonObject();

                String playerName = resultObj.getAsJsonPrimitive("ign").getAsString();
                String text = resultObj.getAsJsonPrimitive("text").getAsString();

                IChatComponent chatComponent = getStatsMessage(text, playerName);
                sender.addChatMessage(chatComponent);
                CACHE.put(playerName.toLowerCase(), chatComponent);
            }, (IOException error) -> {
                int statusCode = error instanceof HttpResponseException
                        ? ((HttpResponseException) error).getStatusCode() : 0;
                sender.addChatMessage(ChatUtils.errorMessage(error.getMessage(),
                        statusCode == 0 || statusCode == 400));
            });
    }

    private IChatComponent getStatsMessage(String statsText, String playerString) {
        ChatComponentText component = getChatLineSeparator(EnumChatFormatting.YELLOW + "Kuudra Stats for " + EnumChatFormatting.RED + playerString + EnumChatFormatting.YELLOW);
        ChatUtils.decodeToFancyChatMessage(component, statsText);
        return component.appendSibling(getChatLineSeparator(""));
    }

    public ChatComponentText getChatLineSeparator(String title) {
        FontRenderer fontRenderer = AttributeMod.mc.fontRendererObj;

        int chatWidth = AttributeMod.mc.ingameGUI.getChatGUI().getChatWidth();
        int dashWidth = fontRenderer.getStringWidth("-");

        if(title.isEmpty()) {
            String dashes = new String(new char[chatWidth / dashWidth]).replace("\0", "-");
            return new ChatComponentText(EnumChatFormatting.YELLOW + dashes);
        }

        int titleWidth = fontRenderer.getStringWidth(title) + 2 * fontRenderer.getCharWidth(' ');
        int dashCount = (chatWidth - titleWidth) / dashWidth;

        if(dashCount < 1) {
            return new ChatComponentText(title);
        }

        int rightDashCount = dashCount / 2;
        int leftDashCount = dashCount - rightDashCount;

        String leftDashes = new String(new char[leftDashCount]).replace("\0", "-");
        String rightDashes = new String(new char[rightDashCount]).replace("\0", "-");

        return new ChatComponentText(EnumChatFormatting.YELLOW + leftDashes + " " + title + " " + EnumChatFormatting.YELLOW + rightDashes);
    }

    private void wrongUsage(ICommandSender sender) {
        sender.addChatMessage(new ChatComponentText(getCommandUsage(sender)));
    }
}
