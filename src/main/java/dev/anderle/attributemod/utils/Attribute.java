package dev.anderle.attributemod.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum Attribute {
    ARACHNO("Arachno", "arachno", "ARA", false, Collections.emptyList()),
    ATTACK_SPEED("Attack Speed", "attack_speed", "ATS", false, Collections.emptyList()),
    COMBO("Combo", "combo", "CMB", false, Collections.emptyList()),
    ELITE("Elite", "elite", "ELT", false, Collections.emptyList()),
    IGNITION("Ignition", "ignition", "IGN", false, Collections.emptyList()),
    LIFE_RECOVERY("Life Recovery", "life_recovery", "LRC", false, Collections.emptyList()),
    MIDAS_TOUCH("Midas Touch", "midas_touch", "MDS", false, Collections.emptyList()),
    UNDEAD("Undead", "undead", "UND", false, Collections.emptyList()),
    MANA_STEAL("Mana Steal", "mana_steal", "MST", false, Collections.emptyList()),
    ENDER("Ender", "ender", "END", false, Collections.emptyList()),
    ARACHNO_RESISTANCE("Arachno Resistance", "arachno_resistance", "ARR", false, Collections.emptyList()),
    BLAZING_RESISTANCE("Blazing Resistance", "blazing_resistance", "BLR", false, Collections.emptyList()),
    EXPERIENCE("Experience", "experience", "EXP", false, Collections.singletonList("exp")),
    SPEED("Speed", "speed", "SPD", true, Arrays.asList("sp", "spd")),
    UNDEAD_RESISTANCE("Undead Resistance", "undead_resistance", "UR", true, Collections.singletonList("ur")),
    BREEZE("Breeze", "breeze", "BR", true, Collections.singletonList("br")),
    LIFELINE("Lifeline", "lifeline", "LL", true, Collections.singletonList("ll")),
    LIFE_REGENERATION("Life Regeneration", "life_regeneration", "LR", false, Collections.singletonList("lr")),
    MANA_POOL("Mana Pool", "mana_pool", "MP", true, Collections.singletonList("mp")),
    DOMINANCE("Dominance", "dominance", "DOM", true, Collections.singletonList("dom")),
    ENDER_RESISTANCE("Ender Resistance", "ender_resistance", "ENR", false, Collections.singletonList("er")),
    VITALITY("Vitality", "mending", "VIT", true, Collections.singletonList("vit")),
    MANA_REGENERATION("Mana Regeneration", "mana_regeneration", "MR", true, Collections.singletonList("mr")),
    VETERAN("Veteran", "veteran", "VET", true, Collections.singletonList("vet")),
    BLAZING_FORTUNE("Blazing Fortune", "blazing_fortune", "BF", true, Collections.singletonList("bf")),
    BLAZING("Blazing", "blazing", "BLZ", false, Collections.emptyList()),
    FISHING_EXPERIENCE("Fishing Experience", "fishing_experience", "FE", true, Collections.singletonList("fe")),
    DOUBLE_HOOK("Double Hook", "double_hook", "DH", true, Collections.singletonList("dh")),
    FISHERMAN("Fisherman", "fisherman", "FM", false, Collections.emptyList()),
    FISHING_SPEED("Fishing Speed", "fishing_speed", "FS", true, Collections.singletonList("fs")),
    HUNTER("Hunter", "hunter", "HNT", false, Collections.emptyList()),
    TROPHY_HUNTER("Trophy Hunter", "trophy_hunter", "TH", true, Collections.singletonList("th")),
    INFECTION("Infection", "infection", "INF", false, Collections.emptyList()),
    MAGIC_FIND("Magic Find", "magic_find", "MF", true, Collections.singletonList("mf")),
    FORTITUDE("Fortitude", "fortitude", "FOR", false, Collections.emptyList()),
    WARRIOR("Warrior", "warrior", "WAR", false, Collections.emptyList()),
    DEADEYE("Deadeye", "deadeye", "DED", false, Collections.emptyList());

    private final String name;
    private final String hypixelName;
    private final String shortName;
    private final boolean isPopular;
    private final List<String> aliases;

    Attribute(String name, String hypixelName, String shortName, boolean isPopular, List<String> aliases) {
        this.name = name;
        this.hypixelName = hypixelName;
        this.shortName = shortName;
        this.isPopular = isPopular;
        this.aliases = aliases;
    }

    public String getName() {
        return name;
    }

    public String getHypixelName() {
        return hypixelName;
    }

    public String getNameWithoutSpace() {
        return name.replaceAll(" ", "");
    }

    public String getShortName() {
        return shortName;
    }

    public boolean isPopular() {
        return isPopular;
    }

    public List<String> getAliases() {
        return aliases;
    }

    public static String getBestMatchWithoutSpace(String input) {
        for(Attribute attribute : Attribute.values()) {
            if(attribute.getAliases().contains(input)) return attribute.getNameWithoutSpace();
        }

        String[] names = Arrays.stream(Attribute.values()).map(Attribute::getNameWithoutSpace).toArray(String[]::new);
        return Helper.getBestMatch(input, names);
    }
}
