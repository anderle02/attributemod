package dev.anderle.attributemod.features;

import dev.anderle.attributemod.Main;
import dev.anderle.attributemod.utils.Constants;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import java.util.*;

public class SettingsCommand extends CommandBase {

    /**
     * Contains all available settings with all values a setting accepts.
     */
    private final Map<String, List<String>> availableSettings = new HashMap<String, List<String>>();

    public SettingsCommand() {
        availableSettings.put("setkey", Arrays.asList("any"));
    }

    @Override
    public String getCommandName() {
        return "attributemod";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return Constants.prefix + EnumChatFormatting.DARK_RED
                + "Correct Usage: " + EnumChatFormatting.RED
                + "/" + getCommandName() + " <setting> <value>\n"
                + EnumChatFormatting.RED + "Use tab completion to change settings, should all be self explanatory.";
    }

    @Override
    public List<String> getCommandAliases() {
        List<String> aliases = new ArrayList<String>();
        aliases.add("am");
        return aliases;
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if(args.length != 2 || !availableSettings.containsKey(args[0])
        || (!availableSettings.get(args[0]).contains("any") && !availableSettings.get(args[0]).contains(args[1]))) {
            wrongUsage(sender);
            return;
        }
        if(args[0].equals("setkey")) {
            Main.config.set("key", args[1], "");
            sender.addChatMessage(new ChatComponentText(
                Constants.prefix + EnumChatFormatting.GREEN
                + "Successfully " + EnumChatFormatting.YELLOW
                + "changed your API key."
            ));
        }
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        List<String> options = new ArrayList<String>();
        if(args.length == 1) {
            options.addAll(availableSettings.keySet());
        } else if(args.length == 2) {
            if(!availableSettings.containsKey(args[0])) return options;
            List<String> acceptedValues = availableSettings.get(args[0]);
            if(!acceptedValues.contains("any")) {
                options.addAll(acceptedValues);
            }
        }
        return options;
    }

    private void wrongUsage(ICommandSender sender) {
        sender.addChatMessage(new ChatComponentText(getCommandUsage(sender)));
    }
}
