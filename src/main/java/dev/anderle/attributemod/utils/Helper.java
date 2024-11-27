package dev.anderle.attributemod.utils;

import com.google.gson.*;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.*;
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
        return Constants.aliases.containsKey(input)
                ? Constants.aliases.get(input)
                : getBestMatch(input, Constants.supportedAttributes);
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

    /**
     *  Get player UUID dynamically.
     */
    public static String getPlayerUUID(Minecraft mc) {
        return mc.thePlayer == null
                ? mc.getSession().getPlayerID()
                : mc.thePlayer.getUniqueID().toString().replaceAll("-", "");
    }

    /**
     * Convert NBT tag to JSON format.
     */
    public static JsonObject convertNBTToJson(NBTTagCompound nbtTag) {
        JsonObject jsonObject = new JsonObject();

        for (String key : nbtTag.getKeySet()) {
            JsonElement jsonElement = convertTagToJsonElement(nbtTag.getTag(key));
            jsonObject.add(key, jsonElement);
        }

        return jsonObject;
    }

    /**
     * Convert NBT tag to JSON format.
     */
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

    /**
     * Get Hypixel's item ID from the NBT tag.
     */
    public static String getHypixelId(JsonObject nbt) {
        if(!nbt.has("tag")) return "NO_ID";
        JsonObject tag = nbt.getAsJsonObject("tag");
        if(!tag.has("ExtraAttributes")) return "NO_ID";
        JsonObject extra = tag.getAsJsonObject("ExtraAttributes");
        if(!extra.has("id")) return "NO_ID";
        return extra.getAsJsonPrimitive("id").getAsString();
    }
}
