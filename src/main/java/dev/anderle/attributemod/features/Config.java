package dev.anderle.attributemod.features;

import dev.anderle.attributemod.AttributeMod;
import gg.essential.elementa.ElementaVersion;
import gg.essential.elementa.WindowScreen;
import gg.essential.vigilance.Vigilant;
import gg.essential.vigilance.data.Property;
import gg.essential.vigilance.data.PropertyType;

import java.io.File;

public class Config extends Vigilant {

    public Config() {
        super(new File("./config/attributemod.toml"), AttributeMod.NAME);
        initialize();
    }

    @Property(
            type = PropertyType.SWITCH,
            name = "Chest Overlay",
            description = "Show an overlay for inventories that contain items with attributes.",
            category = "Main Settings"
    )
    public boolean overlayEnabled = true;

    @Property(
            type = PropertyType.TEXT,
            name = "Mod Key",
            description = "This mod requires an active subscription to work. To get your key,\n- join https://discord.gg/kuudra\n- run /mod in #commands\n- enter your key here",
            category = "Main Settings",
            protectedText = true
    )
    public String modkey = "";
}
