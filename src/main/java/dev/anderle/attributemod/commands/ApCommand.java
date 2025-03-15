package dev.anderle.attributemod.commands;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.anderle.attributemod.utils.Attribute;
import dev.anderle.attributemod.utils.Constants;
import dev.anderle.attributemod.AttributeMod;
import dev.anderle.attributemod.utils.Helper;
import dev.anderle.attributemod.utils.ChatUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.event.ClickEvent;
import net.minecraft.util.*;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.http.client.HttpResponseException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ApCommand extends CommandBase {

    /**
     * GuiNewChat used to reflect and get all chat lines to check for recent responses and delete them.
     * Initialized when the command is executed the first time.
     */
    private GuiNewChat chat;

    @Override
    public String getCommandName() {
        return "attributeprice";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return Constants.prefix + EnumChatFormatting.DARK_RED
                + "Correct Usage: " + EnumChatFormatting.RED
                + "/" + getCommandName() + " <attribute> [level]\n"
                + EnumChatFormatting.DARK_RED + "OR " + EnumChatFormatting.RED
                + "/" + getCommandName() + "<first> [level] <second> [level]\n"
                + EnumChatFormatting.RED + "Make sure to enter attributes without spaces.";
    }

    @Override
    public List<String> getCommandAliases() {
        List<String> aliases = new ArrayList<>();
        aliases.add("ap");
        return aliases;
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if(this.chat == null) this.chat = Minecraft.getMinecraft().ingameGUI.getChatGUI();

        // raw input, only meant to work for follow-up commands
        if(args.length == 1 && args[0].startsWith("%%data%%")) {
            JsonObject data = new JsonParser().parse(args[0].split("%%data%%")[1]).getAsJsonObject();
            if(data.get("a2") == null) showSinglePrices(
                sender, data.get("a1").getAsString(), data.get("l1").getAsInt(),
                data.get("appearance").getAsString().replaceAll("_", "%20")
            ); else showCombinationPrices(sender,
                data.get("a1").getAsString(), data.get("l1").getAsInt(),
                data.get("a2").getAsString(), data.get("l2").getAsInt(),
                data.get("appearance").getAsString().replaceAll("_", "%20")
            );
            return;
        }
        // display help page if nothing entered
        if(args.length == 0 || NumberUtils.isNumber(args[0])) {
            wrongUsage(sender);
            return;
        }
        // extract attributes with levels and show the response
        String firstAttribute = Attribute.getBestMatchWithoutSpace(args[0]);
        switch(args.length) {
            case 1: {
                showSinglePrices(sender, firstAttribute, 1, null);
                return;
            }
            case 2: {
                if(NumberUtils.isNumber(args[1])) showSinglePrices(sender, firstAttribute, parseInt(args[1]), null); // a1 l1
                else showCombinationPrices(sender, firstAttribute, 0, Attribute.getBestMatchWithoutSpace(args[1]), 0, null); // a1 a2
                return;
            }
            case 3: {
                if(NumberUtils.isNumber(args[2])) {
                    if(NumberUtils.isNumber(args[1])) wrongUsage(sender);
                    else showCombinationPrices(sender, firstAttribute, 0, Attribute.getBestMatchWithoutSpace(args[1]), parseInt(args[2]), null); // a1 a2 l2
                } else {
                    if(!NumberUtils.isNumber(args[1])) wrongUsage(sender);
                    else showCombinationPrices(sender, firstAttribute, parseInt(args[1]), Attribute.getBestMatchWithoutSpace(args[2]), 0, null); // a1 l1 a2
                }
                return;
            }
            case 4: {
                if(!NumberUtils.isNumber(args[1]) || NumberUtils.isNumber(args[2]) || !NumberUtils.isNumber(args[3])) {
                    wrongUsage(sender);
                } else {
                    showCombinationPrices(sender, firstAttribute, parseInt(args[1]), Attribute.getBestMatchWithoutSpace(args[2]), parseInt(args[3]), null); // a1 l1 a2 l2
                }
                return;
            }
            default: wrongUsage(sender);
        }
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        if(args.length >= 5) return new ArrayList<>();

        return Arrays.stream(Attribute.values())
                .map(Attribute::getNameWithoutSpace)
                .filter(a -> a.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }

    /** Handle processing if one attribute was entered. If no level was entered, pass "1" as argument. */
    private void showSinglePrices(ICommandSender sender, String attribute, int level, String appearance) {
        float level_ = Helper.withLimits(level, 10, 1);

        AttributeMod.backend.sendGetRequest(
                "/attributeprice",
                "&a1=" + Helper.urlEncodeAttribute(attribute) + "&l1=" + level_ + (appearance == null ? "" : "&appearance=" + appearance),
                (String response) -> decodeResponseAndSendText(
                        new JsonParser().parse(response).getAsJsonObject(),
                        "/ap %%data%%{\"a1\":\"" + attribute + "\",\"l1\":" + level_ + ",\"appearance\":\"%%appearance%%\"}"),
                (IOException error) -> {
                    int statusCode = error instanceof HttpResponseException
                            ? ((HttpResponseException) error).getStatusCode() : 0;
                    sender.addChatMessage(ChatUtils.errorMessage(error.getMessage(),
                            statusCode == 0 || statusCode == 400));
                });
    }

    /** Handle processing if two attributes were entered (with or without level). */
    private void showCombinationPrices(ICommandSender sender, String first, int firstLevel, String second, int secondLevel, String appearance) {
        int firstLevel_ = Helper.withLimits(firstLevel, 10, 0);
        int secondLevel_ = Helper.withLimits(secondLevel, 10, 0);

        AttributeMod.backend.sendGetRequest("/attributeprice",
        "&a1=" + Helper.urlEncodeAttribute(first) + "&l1=" + firstLevel_
            + "&a2=" + Helper.urlEncodeAttribute(second) + "&l2=" + secondLevel_
            + (appearance == null ? "" : "&appearance=" + appearance),

        (String response) -> decodeResponseAndSendText(
                new JsonParser().parse(response).getAsJsonObject(),
                "/ap %%data%%{\"a1\":\"" + first + "\",\"l1\":" + firstLevel_ + ",\"a2\":\"" + second + "\",\"l2\":" + secondLevel_ + ",\"appearance\":\"%%appearance%%\"}"
        ), (IOException error) -> {
                int statusCode = error instanceof HttpResponseException
                        ? ((HttpResponseException) error).getStatusCode() : 0;
                sender.addChatMessage(ChatUtils.errorMessage(error.getMessage(),
                        statusCode == 0 || statusCode == 400));
        });
    }

    /** Send a chat message to the sender that the command usage is wrong. */
    private void wrongUsage(ICommandSender sender) {
        sender.addChatMessage(new ChatComponentText(getCommandUsage(sender)));
    }

    /**
     * Get the text to display and all available appearances from the api response.
     * Add simple menu to switch between appearances, to the message.
     * Send the message.
     * @param response The response from the api. Must contain a "text", "appearances" and "currentAppearance" field.
     * @param newCommand The command to be executed when the user clicks on an appearance.
     *                   "%%appearance%%" will be replaced with the clicked one.
     */
    private void decodeResponseAndSendText(JsonObject response, String newCommand) {
        ChatComponentText comp = new ChatComponentText(Constants.prefix);
        ChatUtils.decodeToFancyChatMessage(comp, response.get("text").getAsString());
        JsonElement appearances = response.get("appearances");
        JsonElement currentAppearance = response.get("currentAppearance");

        if(appearances != null && currentAppearance != null) {
            comp.appendText("\n\n");
            for(JsonElement element : appearances.getAsJsonArray()) {
                String appearance = element.getAsString();
                comp.appendSibling(new ChatComponentText(
                    (appearance.equals(currentAppearance.getAsString())
                    ? EnumChatFormatting.GREEN : EnumChatFormatting.YELLOW)
                    + " [" + appearance + "]"
                ).setChatStyle(new ChatStyle().setChatClickEvent(new ClickEvent(
                    ClickEvent.Action.RUN_COMMAND,
                    newCommand.replace("%%appearance%%", appearance.replaceAll(" ", "_"))
                ))));
            }
        }
        ChatUtils.resendChatMessage(this.chat, comp);
    }
}
