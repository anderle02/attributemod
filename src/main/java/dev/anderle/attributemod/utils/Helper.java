package dev.anderle.attributemod.utils;

import com.google.gson.*;
import dev.anderle.attributemod.AttributeMod;
import net.minecraft.nbt.*;
import net.minecraft.scoreboard.*;
import net.minecraft.text.Text;

import java.text.DecimalFormat;

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
            int distance = getLevenshteinDistance(string.toLowerCase(), input);
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
        return AttributeMod.mc.player == null
                ? AttributeMod.mc.getSession().getUuidOrNull().toString()
                : AttributeMod.mc.player.getUuidAsString().replaceAll("-", "");
    }

    /** Convert NBT tag to JSON format. */
    public static JsonObject convertNBTToJson(NbtCompound compound) {
        JsonObject jsonObject = new JsonObject();

        for (String key : compound.getKeys()) {
            JsonElement jsonElement = convertTagToJsonElement(compound.get(key));
            jsonObject.add(key, jsonElement);
        }

        return jsonObject;
    }

    /** Recursively convert any NBT data to JSON. */
    public static JsonElement convertTagToJsonElement(NbtElement nbt) {
        switch (nbt) {
            case NbtCompound nbtCompound -> {
                return convertNBTToJson(nbtCompound);
            }
            case NbtList nbtList -> {
                JsonArray jsonArray = new JsonArray();
                for (NbtElement nbtElement : nbtList) {
                    jsonArray.add(convertTagToJsonElement(nbtElement));
                }
                return jsonArray;
            }
            case NbtString nbtString -> {
                return new JsonPrimitive(nbtString.toString());
            }
            case NbtInt nbtInt -> {
                return new JsonPrimitive(nbtInt.intValue());
            }
            case null, default -> {
                return JsonNull.INSTANCE;
            }
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
        try {
            ScoreboardObjective sidebarObjective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
            if (sidebarObjective == null) return '0';

            for (ScoreboardEntry scoreEntry : scoreboard.getScoreboardEntries(sidebarObjective)) {
                String name = scoreEntry.owner();
                Team team = scoreboard.getScoreHolderTeam(name);
                String line = Team.decorateName(team, Text.literal(name)).getString().trim();

                if (line.contains("Kuudra's ")) {
                    return line.charAt(line.length() - 2); // Same logic as before
                }
            }
        } catch (Exception e) {
            AttributeMod.LOGGER.error("Error getting Kuudra Tier from Scoreboard.", e);
        }
        return '0';
    }

    /** Copied from org.apache.commons.lang3.StringUtils (deprecated), I don't wanna depend on a whole new module. */
    public static int getLevenshteinDistance(CharSequence s, CharSequence t) {
        if (s == null || t == null) {
            throw new IllegalArgumentException("Strings must not be null");
        }

        int n = s.length();
        int m = t.length();

        if (n == 0) {
            return m;
        }
        if (m == 0) {
            return n;
        }

        if (n > m) {
            // swap the input strings to consume less memory
            final CharSequence tmp = s;
            s = t;
            t = tmp;
            n = m;
            m = t.length();
        }

        final int[] p = new int[n + 1];
        // indexes into strings s and t
        int i; // iterates through s
        int j; // iterates through t
        int upperleft;
        int upper;

        char jOfT; // jth character of t
        int cost;

        for (i = 0; i <= n; i++) {
            p[i] = i;
        }

        for (j = 1; j <= m; j++) {
            upperleft = p[0];
            jOfT = t.charAt(j - 1);
            p[0] = j;

            for (i = 1; i <= n; i++) {
                upper = p[i];
                cost = s.charAt(i - 1) == jOfT ? 0 : 1;
                // minimum of cell to the left+1, to the top+1, diagonally left and up +cost
                p[i] = Math.min(Math.min(p[i - 1] + 1, p[i] + 1), upperleft + cost);
                upperleft = upper;
            }
        }

        return p[n];
    }
}
