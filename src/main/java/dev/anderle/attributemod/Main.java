package dev.anderle.attributemod;

import dev.anderle.attributemod.api.PriceApi;
import dev.anderle.attributemod.features.*;
import dev.anderle.attributemod.utils.ChatUtils;
import dev.anderle.attributemod.utils.Helper;
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

@Mod(
    modid = Main.MODID, version = Main.VERSION,
    useMetadata = true, clientSideOnly = true, acceptedMinecraftVersions = "[1.8.9]"
)
public class Main {
    public static final String MODID = "attributemod";
    public static final String VERSION = "1.0";
    public static final String UUID = Minecraft.getMinecraft().getSession().getPlayerID();
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    /**
     * Used for scheduling an ah refresh every minute.
     * Will probably use this for other features in the future I think.
     */
    public static final Scheduler scheduler = new Scheduler();
    /**
     * Mainly formatting of strings and numbers.
     */
    public static final Helper helper = new Helper();
    /**
     * Helper methods for the in game chat.
     */
    public static final ChatUtils chatUtils = new ChatUtils();
    /**
     * My private API that allows access to users with a valid key.
     */
    public static final PriceApi api = new PriceApi(UUID);
    /**
     * This manages the config file which saves settings when the game is closed.
     */
    public static Config config;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        config = new Config(event.getSuggestedConfigurationFile());
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        FMLCommonHandler.instance().bus().register(this);

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new TooltipPriceDisplay());
        ClientCommandHandler.instance.registerCommand(new ApCommand());
        ClientCommandHandler.instance.registerCommand(new AuCommand());
        ClientCommandHandler.instance.registerCommand(new SettingsCommand());

        api.makeJavaTrustMyApi();
        scheduler.registerTasks();
        scheduler.startExecutingTasks("ah");

        LOGGER.log(Level.INFO, "Attribute Mod Loaded!");
    }
}