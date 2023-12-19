package dev.anderle.attributemod.utils;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;

import java.util.*;

public class Helper {
    private static final NavigableMap<Long, String> suffixes = new TreeMap<Long, String>();
    static {
        suffixes.put(1000L, "k");
        suffixes.put(1000000L, "M");
        suffixes.put(1000000000L, "G");
        suffixes.put(1000000000000L, "T");
        suffixes.put(1000000000000000L, "P");
        suffixes.put(1000000000000000000L, "E");
    }

    /**
     * Converts any number to a nice format.
     * @see <a href="https://stackoverflow.com/questions/4753251/how-to-go-about-formatting-1200-to-1-2k-in-java">Source</a>
     */
    public String format(long value) {
        if (value == Long.MIN_VALUE) return format(Long.MIN_VALUE + 1);
        if (value < 0) return "-" + format(-value);
        if (value < 1000) return Long.toString(value);

        Map.Entry<Long, String> e = suffixes.floorEntry(value);
        Long divideBy = e.getKey();
        String suffix = e.getValue();

        long truncated = value / (divideBy / 10); //the number part of the output times 10
        boolean hasDecimal = truncated < 100 && (truncated / 10d) != (truncated / 10d);
        return hasDecimal ? (truncated / 10d) + suffix : (truncated / 10) + suffix;
    }

    /**
     * Convert attribute names from nbt data to the format that I use.
     */
    public String formatAttribute(String attribute) {
        String result = WordUtils.capitalize(attribute.replaceAll("_", " "));
        if(result.equals("Mending")) result = "Vitality";
        return result;
    }

    /**
     * Get the most similar string to a given input. Uses Levenshtein Distance score.
     */
    public String getBestMatch(String input, String[] toCompare) {
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
    public String getAttribute(String input) {
        return this.getBestMatch(input, Constants.supportedAttributes);
    }

    public String itemIdToName(String id) {
        String result = "";
        for(String part : id.split("_")) {
            result += part.substring(0, 1) + part.substring(1).toLowerCase();
        }
        return result.trim();
    }

    /**
     * Add a url encoded space (%20) between words of an attribute.
     * For example, "ManaPool" becomes "Mana%20Pool".
     */
    public String urlEncodeAttribute(String string) {
        return string.replaceAll("(.)([A-Z])", "$1%20$2");
    }
}
