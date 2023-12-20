package dev.anderle.attributemod.features;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.anderle.attributemod.utils.Constants;
import dev.anderle.attributemod.Main;
import dev.anderle.attributemod.api.PriceApi;
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

import java.util.ArrayList;
import java.util.List;

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
        List<String> aliases = new ArrayList<String>();
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
        String firstAttribute = Helper.getBestMatch(args[0], Constants.supportedAttributes);
        switch(args.length) {
            case 1: {
                showSinglePrices(sender, firstAttribute, 1, null);
                return;
            }
            case 2: {
                if(NumberUtils.isNumber(args[1])) showSinglePrices(sender, firstAttribute, parseInt(args[1]), null); // a1 l1
                else showCombinationPrices(sender, firstAttribute, 0, Helper.getAttribute(args[1]), 0, null); // a1 a2
                return;
            }
            case 3: {
                if(NumberUtils.isNumber(args[2])) {
                    if(NumberUtils.isNumber(args[1])) wrongUsage(sender);
                    else showCombinationPrices(sender, firstAttribute, 0, Helper.getAttribute(args[1]), parseInt(args[2]), null); // a1 a2 l2
                } else {
                    if(!NumberUtils.isNumber(args[1])) wrongUsage(sender);
                    else showCombinationPrices(sender, firstAttribute, parseInt(args[1]), Helper.getAttribute(args[2]), 0, null); // a1 l1 a2
                }
                return;
            }
            case 4: {
                if(!NumberUtils.isNumber(args[1]) || NumberUtils.isNumber(args[2]) || !NumberUtils.isNumber(args[3])) {
                    wrongUsage(sender);
                } else {
                    showCombinationPrices(sender, firstAttribute, parseInt(args[1]), Helper.getAttribute(args[2]), parseInt(args[3]), null); // a1 l1 a2 l2
                }
                return;
            }
            default: wrongUsage(sender);
        }
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        List<String> options = new ArrayList<String>();
        if(args.length < 5) for(String attribute : Constants.supportedAttributes) {
            if(attribute.toLowerCase().startsWith(args[args.length - 1].toLowerCase())) {
                options.add(attribute.replaceAll(" ", ""));
            }
        }
        return options;
    }

    /**
     * Handle processing if one attribute was entered.
     * If no level was entered, pass "1" as argument.
     */
    private void showSinglePrices(final ICommandSender sender, final String attribute, final int level, String appearance) {
        if(level < 1 || level > 10) {
            ChatUtils.badNumber(sender, 1, 10);
            return;
        }
        Main.api.request("/attributeprice",
            "&a1=" + Helper.urlEncodeAttribute(attribute) + "&l1=" + level + (appearance == null ? "" : "&appearance=" + appearance),
        new PriceApi.ResponseCallback() {
            @Override
            public void onResponse(String a) {
                decodeResponseAndSendText(
                    new JsonParser().parse(a).getAsJsonObject(),
                    "/ap %%data%%{\"a1\":\"" + attribute + "\",\"l1\":" + level + ",\"appearance\":\"%%appearance%%\"}"
                );
            }
            @Override
            public void onError(Exception e) {
                int statusCode = e instanceof HttpResponseException
                    ? ((HttpResponseException) e).getStatusCode() : 0;
                sender.addChatMessage(ChatUtils.errorMessage(e.getMessage(),
                    statusCode == 0 || statusCode == 400));
            }
        });
    }

    /**
     * Handle processing if two attributes were entered (with or without level).
     */
    private void showCombinationPrices(final ICommandSender sender,
                                       final String first, final int firstLevel,
                                       final String second, final int secondLevel,
                                       String appearance
    ) {
        if(firstLevel < 0 || firstLevel > 10 || secondLevel < 0 || secondLevel > 10) {
            ChatUtils.badNumber(sender, 1, 10);
            return;
        }
        Main.api.request("/attributeprice",
        "&a1=" + Helper.urlEncodeAttribute(first) + "&l1=" + firstLevel
            + "&a2=" + Helper.urlEncodeAttribute(second) + "&l2=" + secondLevel
            + (appearance == null ? "" : "&appearance=" + appearance),
        new PriceApi.ResponseCallback() {
            @Override
            public void onResponse(String a) {
                decodeResponseAndSendText(
                    new JsonParser().parse(a).getAsJsonObject(),
                    "/ap %%data%%{\"a1\":\"" + first + "\",\"l1\":" + firstLevel + ",\"a2\":\"" + second + "\",\"l2\":" + secondLevel + ",\"appearance\":\"%%appearance%%\"}"
                );
            }
            @Override
            public void onError(Exception e) {
                int statusCode = e instanceof HttpResponseException
                    ? ((HttpResponseException) e).getStatusCode() : 0;
                sender.addChatMessage(ChatUtils.errorMessage(
                    "Failed to fetch attribute prices from the API: " + e.getMessage() + ".",
                    statusCode == 0 || statusCode == 400));
            }
        });
    }

    /**
     * Send a chat message to the sender that the command usage is wrong.
     */
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
        IChatComponent comp = ChatUtils.decodeToFancyChatMessage(response.get("text").getAsString());
        JsonElement appearances = response.get("appearances");
        JsonElement currentAppearance = response.get("currentAppearance");
        if(!appearances.isJsonNull()) {
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
