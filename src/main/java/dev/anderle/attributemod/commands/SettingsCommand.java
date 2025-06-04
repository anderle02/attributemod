package dev.anderle.attributemod.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.anderle.attributemod.AttributeMod;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;

public class SettingsCommand {

    public SettingsCommand(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        LiteralCommandNode<FabricClientCommandSource> node = dispatcher.register(
            ClientCommandManager.literal("attributemod").executes(context -> {
                MinecraftClient client = context.getSource().getClient();
                client.send(() -> client.setScreen(AttributeMod.config.gui()));
                return 1;
            }));
        dispatcher.register(ClientCommandManager.literal("am").redirect(node));
    }
}
