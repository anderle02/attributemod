package dev.anderle.attributemod.features;

import dev.anderle.attributemod.Events;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;

import java.util.Collections;
import java.util.List;

public class SettingsCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "attributemod";
    }

    @Override
    public List<String> getCommandAliases() {
        return Collections.singletonList("am");
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "There is no wrong usage because the command only opens the settings gui. No one will ever see this... :/";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        Events.showVigilanceGuiWithNextTick = true;
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }
}
