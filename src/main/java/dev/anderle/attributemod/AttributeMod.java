package dev.anderle.attributemod;

import dev.anderle.attributemod.api.Backend;
import dev.anderle.attributemod.commands.*;
import dev.anderle.attributemod.overlay.Overlay;
import dev.anderle.attributemod.utils.Scheduler;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.Arrays;

@Mod(modid = AttributeMod.MODID, useMetadata = true, clientSideOnly = true, acceptedMinecraftVersions = "[1.8.9]")
public class AttributeMod {
    public static final String NAME = "Attribute Mod";
    public static final String MODID = "attributemod";
    public static final String VERSION = "1.2.1";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    public static Scheduler scheduler;
    public static Backend backend;
    public static File modFolder;
    public static Config config;
    public static Minecraft mc;

    @Mod.EventHandler @SuppressWarnings("unused")
    public void preInit(FMLPreInitializationEvent event) {
        deleteLegacyConfigFile(event.getModConfigurationDirectory());
        modFolder = event.getSourceFile().getParentFile();
    }

    @Mod.EventHandler @SuppressWarnings("unused")
    public void init(FMLInitializationEvent event) {
        mc = Minecraft.getMinecraft();
        scheduler = new Scheduler();
        backend = new Backend();
        config = new Config();

        MinecraftForge.EVENT_BUS.register(new Events());

        Arrays.asList(
                new ApCommand(),
                new AuCommand(),
                new SettingsCommand(),
                new ProfitTrackerCommand(),
                new KuudraStatsCommand()
        ).forEach(ClientCommandHandler.instance::registerCommand);

        Events.initializeFeatures();
        Overlay.initAll();
        scheduler.registerTasks();

        LOGGER.log(Level.INFO, "Attribute Mod Loaded!");
    }

    private void deleteLegacyConfigFile(File configDir) {
        try {
            boolean deleted = new File(configDir, "attributemod.cfg").delete();
            if(deleted) LOGGER.info("Successfully deleted the old config file.");
        } catch(Exception ignored) {}
    }
}
