package dev.anderle.attributemod;

import dev.anderle.attributemod.api.Backend;
import dev.anderle.attributemod.utils.Scheduler;
import net.fabricmc.api.ClientModInitializer;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class AttributeMod implements ClientModInitializer {
	public static final String MODID = "attributemod";
	public static final ModContainer ATTRIBUTE_MOD = FabricLoader.getInstance().getModContainer(MODID).orElseThrow();

	public static final Logger LOGGER = LoggerFactory.getLogger(MODID);
	public static final String NAME = ATTRIBUTE_MOD.getMetadata().getName();
	public static final String VERSION = ATTRIBUTE_MOD.getMetadata().getVersion().getFriendlyString();
	public static final Path MOD_FOLDER = ATTRIBUTE_MOD.getRootPaths().getFirst();

	public static Config config;
	public static Backend backend;
	public static Scheduler scheduler;
	public static MinecraftClient mc;

	@Override
	public void onInitializeClient() {
		config = new Config();
		backend = new Backend();
		scheduler = new Scheduler();
		mc = MinecraftClient.getInstance();

		// Commands
		// Events

		scheduler.registerTasks();

		LOGGER.info("Attribute Mod Loaded!");
	}
}