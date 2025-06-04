package dev.anderle.attributemod.utils;

import net.minecraft.util.Formatting;

public class Constants {
    // Note: Attributes moved to `Attribute` enum.

    public static final String prefix = Formatting.GOLD + "[" + Formatting.YELLOW + "AttributeMod" + Formatting.GOLD + "] " + Formatting.RESET;
    public static final String[] itemIdPartsToIgnore = {
            "HOT_", "BURNING_", "FIERY_", "INFERNAL_"
    };

    public static final String[] supportedItems = {
            "ATTRIBUTE_SHARD",
            // NORMAL ARMOR
            "CRIMSON_HELMET", "TERROR_HELMET", "AURORA_HELMET", "FERVOR_HELMET", "HOLLOW_HELMET",
            "CRIMSON_CHESTPLATE", "TERROR_CHESTPLATE", "AURORA_CHESTPLATE", "FERVOR_CHESTPLATE", "HOLLOW_CHESTPLATE",
            "CRIMSON_LEGGINGS", "TERROR_LEGGINGS", "AURORA_LEGGINGS", "FERVOR_LEGGINGS", "HOLLOW_LEGGINGS",
            "CRIMSON_BOOTS", "TERROR_BOOTS", "AURORA_BOOTS", "FERVOR_BOOTS", "HOLLOW_BOOTS",
            // FISHING ARMOR
            "MAGMA_LORD_HELMET", "THUNDER_HELMET", "TAURUS_HELMET",
            "MAGMA_LORD_CHESTPLATE", "THUNDER_CHESTPLATE", "FLAMING_CHESTPLATE",
            "MAGMA_LORD_LEGGINGS", "THUNDER_LEGGINGS", "MOOGMA_LEGGINGS",
            "MAGMA_LORD_BOOTS", "THUNDER_BOOTS", "SLUG_BOOTS",
            // EQUIPMENT
            "THUNDERBOLT_NECKLACE", "DELIRIUM_NECKLACE", "LAVA_SHELL_NECKLACE", "VANQUISHED_MAGMA_NECKLACE", "MAGMA_NECKLACE", "MOLTEN_NECKLACE",
            "SCOURGE_CLOAK", "VANQUISHED_GHAST_CLOAK", "GHAST_CLOAK", "MOLTEN_CLOAK",
            "SCOVILLE_BELT", "VANQUISHED_BLAZE_BELT", "BLAZE_BELT", "IMPLOSION_BELT", "MOLTEN_BELT",
            "VANQUISHED_GLOWSTONE_GAUNTLET", "GLOWSTONE_GAUNTLET", "GAUNTLET_OF_CONTAGION", "FLAMING_FIST", "MOLTEN_BRACELET",
            // SWORDS / WANDS
            "SWORD_OF_BAD_HEALTH", "BLADE_OF_THE_VOLCANO", "RAGNAROCK_AXE", "ENRAGER",
            "STAFF_OF_THE_VOLCANO", "FIRE_VEIL_WAND", "FIRE_FREEZE_STAFF", "FIRE_FURY_STAFF", "WAND_OF_STRENGTH", "HOLLOW_WAND",
            // BOWS
            "SULPHUR_BOW",
            // FISHING RODS
            "MAGMA_ROD", "INFERNO_ROD", "HELLFIRE_ROD"
    };
}
