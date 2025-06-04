package dev.anderle.attributemod;

import gg.essential.vigilance.Vigilant;
import gg.essential.vigilance.data.*;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class Config extends Vigilant {

    public Config() {
        super(new File("./config/attributemod.toml"), AttributeMod.NAME, new JVMAnnotationPropertyCollector(), new Sorting());
        initialize();
        configureDependencies();
    }

    /* ----------------------------------------------- ACTIVATION ----------------------------------------------- */

    @Property(type = PropertyType.SWITCH, name = "Enable this Mod", category = "Activation",
            description = "Toggles §call features§r of this mod. You can completely disable this mod here." +
                    "\nNote: §8/ap§r and §8/au§r will still work!")
    public boolean modEnabled = true;

    @Property(type = PropertyType.TEXT, name = "Mod Key", category = "Activation", protectedText = true,
            description = "This mod requires an §cactive subscription§r to work. To get your key," +
                    "\n\n- subscribe to §bhttps://patreon.com/kuudragang§r" +
                    "\n\n- join Kuudra Gang: §bhttps://kuudragang.anderle.dev§r" +
                    "\n\n- run §6/mod§r in §8#commands§r")
    public String modkey = "";

    /* ------------------------------------------------ OVERLAYS ------------------------------------------------ */

    @SuppressWarnings("unused")
    /*@Property(type = PropertyType.BUTTON, name = "Edit Overlay Positions", category = "Overlays", placeholder = "Edit",
            description = "Make sure you have at least one overlay enabled.")
    public void openMoveGui() {
        if(Overlay.ALL.stream().anyMatch(Overlay::isEnabled)) {
            AttributeMod.mc.setScreen(new MoveOverlayGui());
        }
    }*/

    /** Chest Item Display */

    @Property(type = PropertyType.SWITCH, name = "Chest Overlay", category = "Overlays",
            description = "Show an overlay for chests that contain items with attributes.")
    public boolean chestOverlayEnabled = true;

    public enum OverlayStyle { ITEM_NAMES_ONLY, SHORT_ATTRIBUTES_POPULAR,
        SHORT_ATTRIBUTES_ALL, LONG_ATTRIBUTES_POPULAR, LONG_ATTRIBUTES_ALL }

    @Property(
            type = PropertyType.SELECTOR, name = "Overlay Style", category = "Overlays",
            description = "Choose the §cstyle of your chest overlay§r, so it fits better on your screen.",
            options = {
                    "item names only §c(SMALLEST)§r",
                    "short attribute names, only popular attributes",
                    "short attribute names, all attributes",
                    "long attribute names, only popular attributes",
                    "long attribute names, all attributes §c(LARGEST)§r"
            }
    )
    public int overlayStyle = OverlayStyle.SHORT_ATTRIBUTES_ALL.ordinal(); // Would use the enum but Vigilance doesn't like that.

    @Property(type = PropertyType.SLIDER, name = "Items to Show", category = "Overlays", min = 1, max = 54,
            description = "Max amount of §chow many items to show§r in the chest overlay." +
                    "\nIf there are more items in the chest, you can §cscroll to view them§r!")
    public int chestOverlayItemsToShow = 20;

    @Property(type = PropertyType.NUMBER, name = "Chest Overlay X", category = "Overlays", hidden = true)
    public int chestOverlayX = 0;

    @Property(type = PropertyType.NUMBER, name = "Chest Overlay Y", category = "Overlays", hidden = true)
    public int chestOverlayY = 0;

    @Property(type = PropertyType.NUMBER, name = "Chest Overlay Scale in %", category = "Overlays", hidden = true)
    public int chestOverlayScale = 100;

    /** Kuudra Reward Chest Display */

    @Property(type = PropertyType.SWITCH, name = "Kuudra Profit Overlay", category = "Overlays",
            description = "Show §cKuudra chest profits§r and re-roll indicator.")
    public boolean kuudraChestOverlayEnabled = true;

    @Property(type = PropertyType.NUMBER, name = "Kuudra Chest Overlay X", category = "Overlays", hidden = true)
    public int kuudraChestOverlayX = 0;

    @Property(type = PropertyType.NUMBER, name = "Kuudra Chest Overlay Y", category = "Overlays", hidden = true)
    public int kuudraChestOverlayY = 0;

    @Property(type = PropertyType.NUMBER, name = "Kuudra Chest Overlay Scale in %", category = "Overlays", hidden = true)
    public int kuudraChestOverlayScale = 100;

    @Property(type = PropertyType.DECIMAL_SLIDER, name = "Bonus Essence", category = "Overlays",
            maxF = 20.0F,
            description = "Your §cKuudra Pet§r may give you §6extra essence§r!\nEnter the percentage here for a §ccorrect Kuudra Profit value§r.")
    public float essenceBonus = 0;

    /** Profit Per Hour Display */

    @Property(type = PropertyType.SWITCH, name = "Profit per Hour Overlay", category = "Overlays",
            description = "Show §cKuudra profit per hour§r on your screen. Requires the Kuudra Profit Overlay to be enabled. Use §r/profittracker§c to manage.")
    public boolean profitPerHourEnabled = true;

    @Property(type = PropertyType.SWITCH, name = "Automatic Profit Tracker", category = "Overlays",
            description = "§cAutomatically start and stop the profit tracker§r when joining or leaving Kuudra.")
    public boolean autoProfitTracker = false;

    @Property(type = PropertyType.NUMBER, name = "Profit per Hour Overlay X", category = "Overlays", hidden = true)
    public int profitPerHourX = 0;

    @Property(type = PropertyType.NUMBER, name = "Profit per Hour Overlay Y", category = "Overlays", hidden = true)
    public int profitPerHourY = 0;

    @Property(type = PropertyType.NUMBER, name = "Profit per Hour Overlay Scale in %", category = "Overlays", hidden = true)
    public int profitPerHourScale = 100;

    @Property(type = PropertyType.NUMBER, name = "Total Tracked Kuudra Profit in K", category = "Overlays", hidden = true)
    public int totalProfitK = 0;

    @Property(type = PropertyType.NUMBER, name = "Total Tracked Kuudra Chests", category = "Overlays", hidden = true)
    public int totalOpenedChests = 0;

    @Property(type = PropertyType.NUMBER, name = "Total Tracked Kuudra Profit Time", category = "Overlays", hidden = true)
    public int totalProfitTime = 0;

    /** Tooltip Attribute Price */

    @Property(type = PropertyType.SWITCH, name = "Tooltip Attribute Price Display", category = "Overlays",
            description = "Show attribute prices directly §cnext to the attributes§r on item tooltips.")
    public boolean tooltipAttributePriceEnabled = true;

    /* ---------------------------------------------- KUUDRA STATS --------------------------------------------- */

    @Property(type = PropertyType.SWITCH, name = "Party Finder Stats", category = "Kuudra Stats",
            description = "When someone joins your party, §cautomatically run /kuudra§r on them.")
    public boolean partyFinderShowStats = true;

    @Property(type = PropertyType.SELECTOR, name = "Style", category = "Kuudra Stats",
            description = "The style of the §c/kuudra§r command.",
            options = { "Normal", "Compact" })
    public int statsMessageStyle = 1;

    /* ---------------------------------------------- HELP / BUGS ---------------------------------------------- */

    @SuppressWarnings("unused")
    @Property(type = PropertyType.BUTTON, name = "Help, Bug Reports and Feature Suggestions", category = "Links", placeholder = "Join",
            description = "Can't find something or something doesn't work as expected?" +
                    "\nYou would like a feature that doesn't exist yet?" +
                    "\n\nJoin §cKuudra Gang§r, go to §8#donator-chat§r and just ask!")
    public void openDiscordLink() throws URISyntaxException, IOException {
        if (Desktop.isDesktopSupported()) { // Use my own page so no mod update is needed if the invite link changes.
            Desktop.getDesktop().browse(new URI("https://kuudragang.anderle.dev/"));
        }
    }

    @SuppressWarnings("unused")
    @Property(type = PropertyType.BUTTON, name = "Like this Mod?", category = "Links", placeholder = "Open Github",
            description = "Give it a §6star on Github§r, if you have a free minute!")
    public void openGithubLink() throws URISyntaxException, IOException {
        if (Desktop.isDesktopSupported()) { // Use my own page so no mod update is needed if the invite link changes.
            Desktop.getDesktop().browse(new URI("https://attributemod.anderle.dev/"));
        }
    }

    //TODO: find a way to display the version

    @SuppressWarnings("unused")
    @Property(type = PropertyType.BUTTON, name = "Update AttributeMod", category = "Links", placeholder = "Update",
            description = "Download the §clatest version§r of this Mod.")
    public void openDownloadLink() throws URISyntaxException, IOException {
        if (Desktop.isDesktopSupported()) { // Use my own page so no mod update is needed if the invite link changes.
            Desktop.getDesktop().browse(new URI("https://attributemod.anderle.dev/releases/latest"));
        }
    }

    /* --------------------------------------------------------------------------------------------------------- */

    /** Sorting */

    public static class Sorting extends SortingBehavior {
        private final List<String> categories = Arrays.asList("Overlays", "Kuudra Stats", "Activation", "Links");
        private final List<String> settings = Arrays.asList( // Yeah, I actually listed all settings in the correct order...
                "Enable this Mod", "Mod Key", "Edit Overlay Positions", "Chest Overlay", "Overlay Style",
                "Items to Show", "Kuudra Profit Overlay", "Profit per Hour Overlay",
                "Automatic Profit Tracker", "Bonus Essence", "Tooltip Attribute Price Display", "Party Finder Stats", "Style",
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

    private void configureDependencies() {
        addDependency("chestOverlayItemsToShow", "chestOverlayEnabled");
        addDependency("overlayStyle", "chestOverlayEnabled");
        addDependency("profitPerHourEnabled", "kuudraChestOverlayEnabled");
        addDependency("essenceBonus", "kuudraChestOverlayEnabled");
        addDependency("autoProfitTracker", "kuudraChestOverlayEnabled");
    }

    /* --------------------------------------------------------------------------------------------------------- */
}