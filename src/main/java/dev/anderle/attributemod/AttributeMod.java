package dev.anderle.attributemod;

import dev.anderle.attributemod.api.Backend;
import dev.anderle.attributemod.features.*;
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

@Mod(modid = "attributemod", useMetadata = true, clientSideOnly = true)
public class AttributeMod {
    public static final String NAME = "Attribute Mod";
    public static final String MODID = "attributemod";
    public static final String VERSION = "1.1.1";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    public static Backend backend;
    public static File modFolder;
    public static Config config;
    public static Minecraft mc;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        modFolder = event.getSourceFile().getParentFile();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        backend = new Backend();
        config = new Config();
        mc = Minecraft.getMinecraft();

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new Events());

        ClientCommandHandler.instance.registerCommand(new ApCommand());
        ClientCommandHandler.instance.registerCommand(new AuCommand());
        ClientCommandHandler.instance.registerCommand(new SettingsCommand());

        //scheduler.registerTasks();
        //scheduler.startExecutingTasks("ah");

        LOGGER.log(Level.INFO, "Attribute Mod Loaded!");
    }
}
