package dev.anderle.attributemod.features;

import dev.anderle.attributemod.AttributeMod;
import dev.anderle.attributemod.overlay.MoveOverlayGui;
import dev.anderle.attributemod.overlay.OverlayElement;
import gg.essential.vigilance.Vigilant;
import gg.essential.vigilance.data.*;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.List;

public class Config extends Vigilant {

    public Config() {
        super(new File("./config/attributemod.toml"), AttributeMod.NAME, new JVMAnnotationPropertyCollector(), new Sorting());
        initialize();
        configureDependencies();
    }

    /** ----------------------------------------------- ACTIVATION ----------------------------------------------- */

    @Property(type = PropertyType.SWITCH, name = "Enable this Mod", category = "Activation",
            description = "Toggles all features of this mod. You can completely disable this mod here." +
                    "\nNote: /ap and /au will still work!")
    public boolean modEnabled = true;

    @Property(type = PropertyType.TEXT, name = "Mod Key", category = "Activation", protectedText = true,
            description = "This mod requires an active subscription to work. To get your key,\n" +
                    "\n- subscribe to https://patreon.com/kuudragang" +
                    "\n- join Kuudra Gang: https://kuudragang.anderle.dev" +
                    "\n- run /mod in #commands" +
                    "\n- enter your key here")
    public String modkey = "";

    /* ------------------------------------------------ OVERLAYS ------------------------------------------------ */

    @SuppressWarnings("unused")
    @Property(type = PropertyType.BUTTON, name = "Edit Overlay Positions", category = "Overlays", placeholder = "Edit",
            description = "Make sure you have at least one overlay enabled.")
    public void openMoveGui() {
        if(OverlayElement.ALL.stream().anyMatch(OverlayElement::isEnabled)) {
            AttributeMod.mc.displayGuiScreen(new MoveOverlayGui());
        }
    }

    /** Chest Item Display */

    @Property(type = PropertyType.SWITCH, name = "Chest Overlay", category = "Overlays",
            description = "Show an overlay for chests that contain items with attributes.")
    public boolean chestOverlayEnabled = true;

    @Property(type = PropertyType.NUMBER, name = "Items to Show", category = "Overlays", min = 1, max = 54,
            description = "Max amount of how many items to show in the chest overlay." +
                    "\nIf there are more items in the chest, you can scroll to view them!")
    public int chestOverlayItemsToShow = 20;

    @Property(type = PropertyType.NUMBER, name = "Chest Overlay X", category = "Overlays", hidden = true)
    public int chestOverlayX = 0;

    @Property(type = PropertyType.NUMBER, name = "Chest Overlay Y", category = "Overlays", hidden = true)
    public int chestOverlayY = 0;

    @Property(type = PropertyType.NUMBER, name = "Chest Overlay Scale in %", category = "Overlays", hidden = true)
    public int chestOverlayScale = 100;

    /** Kuudra Reward Chest Display */

    @Property(type = PropertyType.SWITCH, name = "Kuudra Reward Chest Overlay", category = "Overlays",
            description = "Show Kuudra reward chest profits and re-roll indicator.")
    public boolean kuudraChestOverlayEnabled = true;

    @Property(type = PropertyType.NUMBER, name = "Kuudra Chest Overlay X", category = "Overlays", hidden = true)
    public int kuudraChestOverlayX = 0;

    @Property(type = PropertyType.NUMBER, name = "Kuudra Chest Overlay Y", category = "Overlays", hidden = true)
    public int kuudraChestOverlayY = 0;

    @Property(type = PropertyType.NUMBER, name = "Kuudra Chest Overlay Scale in %", category = "Overlays", hidden = true)
    public int kuudraChestOverlayScale = 100;

    /** Tooltip Attribute Price */

    @Property(type = PropertyType.SWITCH, name = "Tooltip Attribute Price Display", category = "Overlays",
            description = "Show attribute prices directly next to the attributes on item tooltips.")
    public boolean tooltipAttributePriceEnabled = true;

    /* ---------------------------------------------- HELP / BUGS ---------------------------------------------- */

    @SuppressWarnings("unused")
    @Property(type = PropertyType.BUTTON, name = "Help, Bug Reports and Feature Suggestions", category = "Links", placeholder = "Join",
            description = "Can't find something or something doesn't work as expected?" +
                    "\nYou would like a feature that doesn't exist yet?" +
                    "\nJoin Kuudra Gang, go to #donator-chat and just ask!")
    public void openDiscordLink() throws URISyntaxException, IOException {
        if (Desktop.isDesktopSupported()) { // Use my own page so no mod update is needed if the invite link changes.
            Desktop.getDesktop().browse(new URI("https://kuudragang.anderle.dev/"));
        }
    }

    @SuppressWarnings("unused")
    @Property(type = PropertyType.BUTTON, name = "Like this Mod?", category = "Links", placeholder = "Open Github",
            description = "Give it a star on Github, if you have a free minute!")
    public void openGithubLink() throws URISyntaxException, IOException {
        if (Desktop.isDesktopSupported()) { // Use my own page so no mod update is needed if the invite link changes.
            Desktop.getDesktop().browse(new URI("https://attributemod.anderle.dev/"));
        }
    }

    @SuppressWarnings("unused")
    @Property(type = PropertyType.BUTTON, name = "Update AttributeMod", category = "Links", placeholder = "Update",
            description = "Download the latest version of this Mod. Your version is " + AttributeMod.VERSION + "!")
    public void openDownloadLink() throws URISyntaxException, IOException {
        if (Desktop.isDesktopSupported()) { // Use my own page so no mod update is needed if the invite link changes.
            Desktop.getDesktop().browse(new URI("https://kuudragang.anderle.dev/releases/latest"));
        }
    }

    /* --------------------------------------------------------------------------------------------------------- */

    /** Sorting */

    public static class Sorting extends SortingBehavior {
        private final List<String> categories = Arrays.asList("Activation", "Overlays", "Links");
        private final List<String> settings = Arrays.asList( // Yeah, I actually listed all settings in the correct order...
                "Enable this Mod", "Mod Key", "Edit Overlay Positions", "Chest Overlay", "Items to Show",
                "Kuudra Reward Chest Overlay", "Tooltip Attribute Price Display",
                "Help, Bug Reports and Feature Suggestions", "Like this Mod?", "Update AttributeMod");

        @Override
        public @NotNull Comparator<Category> getCategoryComparator() {
            return Comparator.comparingInt(o -> categories.indexOf(o.getName()));
        }

        @Override
        public @NotNull Comparator<PropertyData> getPropertyComparator() {
            return Comparator.comparingInt(o -> settings.indexOf(o.getAttributesExt().getName()));
        }
    }

    /** Dependencies */

    private final List<String> dependOnModEnabled = Arrays.asList(
            "openMoveGui", "chestOverlayEnabled", "chestOverlayItemsToShow", "kuudraChestOverlayEnabled",
            "openDiscordLink", "openGithubLink", "openDownloadLink", "tooltipAttributePriceEnabled");

    private void configureDependencies() {
        addDependency("chestOverlayItemsToShow", "chestOverlayEnabled");
        dependOnModEnabled.forEach(setting -> addDependency(setting, "modEnabled"));
    }

    /* --------------------------------------------------------------------------------------------------------- */
}