package dev.anderle.attributemod.features;

import com.google.gson.JsonParser;
import dev.anderle.attributemod.AttributeMod;
import dev.anderle.attributemod.utils.Constants;
import dev.anderle.attributemod.utils.Helper;
import dev.anderle.attributemod.utils.ChatUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.http.client.HttpResponseException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AuCommand extends CommandBase {
    /**
     * GuiNewChat used to reflect and get all chat lines to check for recent responses and delete them.
     * Initialized when the command is executed the first time.
     */
    private GuiNewChat chat;

    @Override
    public String getCommandName() {
        return "attributeupgrade";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return Constants.prefix + EnumChatFormatting.DARK_RED
            + "Correct Usage: " + EnumChatFormatting.RED
            + "/" + getCommandName() + " <attribute> <item> <from> <to>\n"
            + EnumChatFormatting.RED + "Make sure to enter attributes and items without spaces.";
    }

    @Override
    public List<String> getCommandAliases() {
        List<String> aliases = new ArrayList<>();
        aliases.add("au");
        return aliases;
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if(this.chat == null) this.chat = Minecraft.getMinecraft().ingameGUI.getChatGUI();

        if(args.length != 4 || !NumberUtils.isNumber(args[2]) || !NumberUtils.isNumber(args[3])) {
            wrongUsage(sender);
            return;
        }
        int from = parseInt(args[2]);
        int to = parseInt(args[3]);
        if(from < 0 || from > 10 || to < 0 || to > 10) {
            ChatUtils.badNumber(sender, 0, 10);
            return;
        }
        if(to <= from) {
            sender.addChatMessage(ChatUtils.errorMessage(
                "The first level has to be lower than the second one.",
                false
            ));
        }
        String attribute = Helper.getAttribute(args[0]);
        String item = Helper.getBestMatch(itemNameToId(args[1]), Constants.supportedItems);
        showResult(sender, attribute, item, from, to);
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        List<String> options = new ArrayList<>();
        switch(args.length) {
            case 1: {
                for(String attribute : Constants.supportedAttributes) {
                    if(attribute.toLowerCase().startsWith(args[args.length - 1].toLowerCase())) {
                        options.add(attribute.replaceAll(" ", ""));
                    }
                }
                break;
            }
            case 2: {
                for(String item : Constants.supportedItems) {
                    String formattedItemId = Helper.itemIdToName(item, false);
                    if(formattedItemId.toLowerCase().startsWith(args[1].toLowerCase())) {
                        options.add(formattedItemId);
                    }
                }
                break;
            }
            default: break;
        }
        return options;
    }

    /**
     * Get result from api and send it to the chat.
     */
    private void showResult(final ICommandSender sender, String attribute, String item, int from, int to) {
        AttributeMod.backend.sendGetRequest(
                "/attributeupgrade",
                "&attribute=" + Helper.urlEncodeAttribute(attribute) + "&item=" + item + "&from=" + from + "&to=" + to,
                (String response) -> ChatUtils.resendChatMessage(this.chat, ChatUtils.decodeToFancyChatMessage(
                        new JsonParser().parse(response).getAsJsonObject().get("text").getAsString()
                )),
                (IOException error) -> {
                    int statusCode = error instanceof HttpResponseException
                            ? ((HttpResponseException) error).getStatusCode() : 0;
                    sender.addChatMessage(ChatUtils.errorMessage(error.getMessage(),
                            statusCode == 0 || statusCode == 400));
                });
    }

    /**
     * Send a chat message to the sender that the command usage is wrong.
     */
    private void wrongUsage(ICommandSender sender) {
        sender.addChatMessage(new ChatComponentText(getCommandUsage(sender)));
    }

    /**
     * Convert an item ID to Hypixel's ID format.
     */
    private String itemNameToId(String string) {
        return string.replaceAll("(.)([A-Z])", "$1_$2").toUpperCase();
    }
}