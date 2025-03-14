package dev.anderle.attributemod.features;

import dev.anderle.attributemod.overlay.Overlay;
import dev.anderle.attributemod.overlay.ProfitPerHourOverlay;
import dev.anderle.attributemod.utils.Constants;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ProfitTrackerCommand extends CommandBase {
    private ProfitPerHourOverlay tracker;

    @Override
    public String getCommandName() {
        return "profittracker";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return Constants.prefix + EnumChatFormatting.DARK_RED
                + "Correct Usage: " + EnumChatFormatting.RED
                + "/" + getCommandName() + " <start|stop|reset>";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if(tracker == null) getOverlayObject();

        switch(args[0].toLowerCase()) {
            case "start": tracker.start(); break;
            case "stop": tracker.stop(); break;
            case "reset": tracker.reset(); break;
            default: sender.addChatMessage(new ChatComponentText(getCommandUsage(sender)));
        }
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        if(args.length == 1) {
            return new ArrayList<>(Arrays.asList("start", "stop", "reset"));
        } else {
            return Collections.emptyList();
        }
    }

    private void getOverlayObject() {
        tracker = (ProfitPerHourOverlay) Overlay.ALL.stream().filter(o -> o instanceof ProfitPerHourOverlay).findFirst().orElse(null);
    }
}
