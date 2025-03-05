package dev.anderle.attributemod.features;

import dev.anderle.attributemod.AttributeMod;
import dev.anderle.attributemod.overlay.MoveOverlayGui;
import dev.anderle.attributemod.overlay.OverlayElement;
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
            type = PropertyType.NUMBER,
            name = "Items to show",
            description = "Max amount of how many items to show in the chest overlay. " +
                    "You can scroll to view the rest or set this very high.",
            category = "Main Settings",
            min = 1,
            max = 54
    )
    public int chestOverlayItemsToShow = 10;

    @Property(
            type = PropertyType.SWITCH,
            name = "Chest Overlay",
            description = "Show an overlay for inventories that contain items with attributes.",
            category = "Main Settings"
    )
    public boolean chestOverlayEnabled = true;

    @Property(
            type = PropertyType.SWITCH,
            name = "Kuudra Reward Chest Overlay",
            description = "Show Kuudra reward chest profits and re-roll indicator.",
            category = "Main Settings"
    )
    public boolean kuudraChestOverlayEnabled = true;

    @Property(
            type = PropertyType.NUMBER,
            name = "Kuudra Reward Chest Overlay X",
            category = "Main Settings",
            hidden = true
    )
    public int kuudraChestOverlayX = 20;

    @Property(
            type = PropertyType.NUMBER,
            name = "Kuudra Reward Chest Overlay Y",
            category = "Main Settings",
            hidden = true
    )
    public int kuudraChestOverlayY = 20;

    @Property(
            type = PropertyType.NUMBER,
            name = "Kuudra Reward Chest Overlay Scale in %",
            category = "Main Settings",
            hidden = true
    )
    public int kuudraChestOverlayScale = 100;

    @Property(
            type = PropertyType.NUMBER,
            name = "Chest Overlay X",
            category = "Main Settings",
            hidden = true
    )
    public int chestOverlayX = 10;

    @Property(
            type = PropertyType.NUMBER,
            name = "Chest Overlay Y",
            category = "Main Settings",
            hidden = true
    )
    public int chestOverlayY = 10;

    @Property(
            type = PropertyType.NUMBER,
            name = "Chest Overlay Scale in %",
            category = "Main Settings",
            hidden = true
    )
    public int chestOverlayScale = 100;

    @Property(
            type = PropertyType.TEXT,
            name = "Mod Key",
            description = "This mod requires an active subscription to work. To get your key,\n- join https://discord.gg/kuudra\n- run /mod in #commands\n- enter your key here",
            category = "Main Settings",
            protectedText = true
    )
    public String modkey = "";

    @Property(
            type = PropertyType.BUTTON,
            name = "Edit Overlay Position",
            description = "Make sure you have at least one overlay enabled.",
            category = "Main Settings",
            placeholder = "Click!"
    )
    public void openMoveGui() {
        if(OverlayElement.ALL.stream().anyMatch(OverlayElement::isEnabled)) {
            AttributeMod.mc.displayGuiScreen(new MoveOverlayGui());
        }
    }
}
