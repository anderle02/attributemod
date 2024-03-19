package dev.anderle.attributemod.utils;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;

import java.text.DecimalFormat;

public class Helper {
    public static String formatNumber(double number) {
        DecimalFormat df = new DecimalFormat("###.#");

        if (number < 1000) return df.format(number);
        else if (number < 1e6) return df.format(number / 1e3) + "k";
        else if (number < 1e9) return df.format(number / 1e6) + "M";
        else return df.format(number / 1e9) + "B";
    }

    /**
     * Convert attribute names from nbt data to the format that I use.
     */
    public static String formatAttribute(String attribute) {
        String result = WordUtils.capitalize(attribute.replaceAll("_", " "));
        if(result.equals("Mending")) result = "Vitality";
        return result;
    }

    /**
     * Get the most similar string to a given input. Uses Levenshtein Distance score.
     */
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

    /**
     * Get the most similar attribute from the list of supported attributes.
     */
    public static String getAttribute(String input) {
        return getBestMatch(input, Constants.supportedAttributes);
    }

    public static String itemIdToName(String id, boolean withSpaces) {
        StringBuilder result = new StringBuilder();
        for(String part : id.split("_")) {
            if(withSpaces) result.append(" ");
            result.append(part.charAt(0)).append(part.substring(1).toLowerCase());
        }
        return result.toString().trim();
    }

    /**
     * Add a url encoded space (%20) between words of an attribute.
     * For example, "ManaPool" becomes "Mana%20Pool".
     */
    public static String urlEncodeAttribute(String string) {
        return string.replaceAll("(.)([A-Z])", "$1%20$2");
    }

    /**
     * Remove parts of the item ID, that don't affect the prices of the item's attribute.
     */
    public static String removeExtraItemIdParts(String itemId) {
        for(String part : Constants.itemIdPartsToIgnore) {
            if(itemId.contains(part)) return itemId.replace(part, "");
        }
        return itemId;
    }
}
