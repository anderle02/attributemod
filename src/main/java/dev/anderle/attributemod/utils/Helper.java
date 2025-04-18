package dev.anderle.attributemod.utils;

import com.google.gson.*;
import dev.anderle.attributemod.AttributeMod;
import net.minecraft.nbt.*;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import org.apache.commons.lang3.StringUtils;

import java.text.DecimalFormat;
import java.util.Collection;

public class Helper {
    /** Nicely format any number. */
    public static String formatNumber(double number) {
        DecimalFormat df = new DecimalFormat("###.#");

        if (number < 1000) return df.format(number);
        else if (number < 1e6) return df.format(number / 1e3) + "k";
        else if (number < 1e9) return df.format(number / 1e6) + "M";
        else return df.format(number / 1e9) + "B";
    }

    /** Get the most similar string to a given input. Uses Levenshtein Distance score. */
    public static String getBestMatch(String input, String[] toCompare) {
        if(toCompare.length == 0) return input;
        else input = input.toLowerCase();
        int lowestDistance = Integer.MAX_VALUE;
        String bestMatch = toCompare[0];
        for(String string : toCompare) {
            int distance = StringUtils.getLevenshteinDistance(string.toLowerCase(), input);
            if(distance < lowestDistance) {
                lowestDistance = distance;
                bestMatch = string;
            }
        }
        return bestMatch;
    }

    /** Replaces all underscores with spaces. Then capitalizes every word. */
    public static String itemIdToName(String id, boolean withSpaces) {
        StringBuilder result = new StringBuilder();
        for(String part : id.split("_")) {
            if(withSpaces) result.append(" ");
            result.append(part.charAt(0)).append(part.substring(1).toLowerCase());
        }
        return result.toString().trim();
    }

    /** Replaces all spaces with underscores. Then makes the string uppercase. */
    public static String itemNameToId(String string) {
        return string.replaceAll("(.)([A-Z])", "$1_$2").toUpperCase();
    }

    /** Add a url encoded space (%20) between words of an attribute. For example, "ManaPool" becomes "Mana%20Pool". */
    public static String urlEncodeAttribute(String string) {
        return string.replaceAll("(.)([A-Z])", "$1%20$2");
    }

    /** Remove parts of the item ID, that don't affect the prices of the item's attribute. */
    public static String removeExtraItemIdParts(String itemId) {
        for(String part : Constants.itemIdPartsToIgnore) {
            if(itemId.contains(part)) return itemId.replace(part, "");
        }
        return itemId;
    }

    /** Get player UUID dynamically. */
    public static String getPlayerUUID() {
        return AttributeMod.mc.thePlayer == null
                ? AttributeMod.mc.getSession().getPlayerID()
                : AttributeMod.mc.thePlayer.getUniqueID().toString().replaceAll("-", "");
    }

    /** Convert NBT tag to JSON format. */
    public static JsonObject convertNBTToJson(NBTTagCompound nbtTag) {
        JsonObject jsonObject = new JsonObject();

        for (String key : nbtTag.getKeySet()) {
            JsonElement jsonElement = convertTagToJsonElement(nbtTag.getTag(key));
            jsonObject.add(key, jsonElement);
        }

        return jsonObject;
    }

    /** Recursively convert any NBT data to JSON. */
    private static JsonElement convertTagToJsonElement(NBTBase nbtTag) {
        if (nbtTag instanceof NBTTagCompound) {
            return convertNBTToJson((NBTTagCompound) nbtTag);
        } else if (nbtTag instanceof NBTTagList) {
            JsonArray jsonArray = new JsonArray();
            NBTTagList nbtList = (NBTTagList) nbtTag;
            for (int i = 0; i < nbtList.tagCount(); i++) {
                jsonArray.add(convertTagToJsonElement(nbtList.get(i)));
            }
            return jsonArray;
        } else if (nbtTag instanceof NBTTagString) {
            return new JsonPrimitive(((NBTTagString) nbtTag).getString());
        } else if (nbtTag instanceof NBTTagInt) {
            return new JsonPrimitive(((NBTTagInt) nbtTag).getInt());
        } else {
            // Handle other NBT types similarly
            return JsonNull.INSTANCE;  // Default return for unsupported types
        }
    }

    /** Ensures the value x is inside max and min. */
    public static double withLimits(double value, double max, double min) {
        return Math.max(Math.min(value, max), min);
    }
    /** Ensures the value x is inside max and min. */
    public static int withLimits(int value, int max, int min) {
        return Math.max(Math.min(value, max), min);
    }

    /** Returns the current Kuudra tier as a char from '1' to '5'. '0' if not in Kuudra. */
    public static char getKuudraTier(Scoreboard scoreboard) {
        try { // I have no idea how the scoreboard works so I just try catch everything.
            ScoreObjective sidebarObjective = scoreboard.getObjectiveInDisplaySlot(1);
            Collection<Score> scores = scoreboard.getSortedScores(sidebarObjective);
            System.out.println(scores.size());
            for(Score score : scores) {
                ScorePlayerTeam team = scoreboard.getPlayersTeam(score.getPlayerName());
                String scoreboardLine = ScorePlayerTeam.formatPlayerName(team, score.getPlayerName()).trim();
                if(scoreboardLine.contains("Kuudra's ")) {
                    return scoreboardLine.charAt(scoreboardLine.length() - 2);
                }
            }
        } catch(Exception e) {
            AttributeMod.LOGGER.error("Error getting Kuudra Tier from Scoreboard.", e);
        }
        return '0';
    }
}
