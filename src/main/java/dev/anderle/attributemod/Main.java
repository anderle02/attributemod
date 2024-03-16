package dev.anderle.attributemod;

import dev.anderle.attributemod.api.PriceApi;
import dev.anderle.attributemod.features.*;
import dev.anderle.attributemod.utils.Scheduler;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

@Mod(
    modid = Main.MODID, version = Main.VERSION,
    useMetadata = true, clientSideOnly = true, acceptedMinecraftVersions = "[1.8.9]"
)
public class Main {
    public static final String MODID = "attributemod";
    public static final String VERSION = "@@VERSION@@"; // is replaced with the version specified in build.gradle
    public static final String UUID = Minecraft.getMinecraft().getSession().getPlayerID();
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    /**
     * Used for scheduling an ah refresh every minute.
     * Will probably use this for other features in the future I think.
     */
    public static final Scheduler scheduler = new Scheduler();
    /**
     * My private API that allows access to users with a valid key.
     */
    public static final PriceApi api = new PriceApi(UUID);
    /**
     * This manages the config file which saves settings when the game is closed.
     */
    public static Config config;
    /**
     * The mod folder, used for the update checker to allow the user to directly open it.
     */
    public static File modFolder;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        config = new Config(event.getSuggestedConfigurationFile());
        modFolder = event.getSourceFile().getParentFile();
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        FMLCommonHandler.instance().bus().register(this);

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new TooltipPriceDisplay());
        MinecraftForge.EVENT_BUS.register(new OneTimeMessage());
        ClientCommandHandler.instance.registerCommand(new ApCommand());
        ClientCommandHandler.instance.registerCommand(new AuCommand());
        ClientCommandHandler.instance.registerCommand(new SettingsCommand());

        scheduler.registerTasks();
        scheduler.startExecutingTasks("ah");

        LOGGER.log(Level.INFO, "Attribute Mod Loaded!");
    }
}