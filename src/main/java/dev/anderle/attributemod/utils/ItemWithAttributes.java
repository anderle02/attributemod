package dev.anderle.attributemod.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import dev.anderle.attributemod.Main;
import net.minecraft.inventory.Slot;
import net.minecraft.nbt.*;
import net.minecraft.util.JsonUtils;

import java.io.ByteArrayOutputStream;
import java.util.*;

public class ItemWithAttributes {
    private final HashMap<String, Integer> attributes = new HashMap<>();
    private final Slot slot;
    private final String id;
    private final String displayName;
    private Evaluation price;

    public ItemWithAttributes(String itemId, Slot slot) {
        this.id = itemId;
        this.slot = slot;
        this.displayName = Helper.itemIdToName(itemId, true);
    }

    public void addAttribute(String name, int level) {
        attributes.put(name, level);
    }

    public String[] getAttributeNames() {
        return attributes.keySet().toArray(new String[0]);
    }

    public Integer[] getAttributeLevels() {
        return attributes.values().toArray(new Integer[0]);
    }

    public Evaluation getDetailedPrice() {
        if(price != null) return price; // so it doesn't calculate more than 1 time

        // lbin
        int lbin = Main.api.getLbin(id);

        // single
        HashMap<String, Integer> singlePricesMap = new HashMap<>();
        int[] singlePricesArray = attributes.entrySet().stream().map(attribute -> {
            int price = (int) Math.pow(2, attribute.getValue() - 1) * Main.api.getAttributePrice(id, attribute.getKey());
            singlePricesMap.put(attribute.getKey(), price);
            return price;
        }).mapToInt(Integer::intValue).toArray();

        // attribute shards
        if(attributes.size() == 1) return new Evaluation(singlePricesMap, 0, singlePricesArray[0] + lbin);

        // not attribute shards
        Integer[] attributeLevels = getAttributeLevels();
        int singlePrice = attributeLevels[0] < 6 && attributeLevels[1] < 6
            ? Math.max(singlePricesArray[0], singlePricesArray[1]) - lbin
            : singlePricesArray[0] + singlePricesArray[1] - 2 * lbin;

        // hellfire rods
        if(id.equals("HELLFIRE_ROD")) singlePrice += 2 * lbin;

        // combination
        String[] attributeNames = getAttributeNames();
        int combinationPrice = Main.api.getCombinationPrice(id, attributeNames[0], attributeNames[1]);

        // estimate
        int estimate = singlePrice + combinationPrice;
        if(estimate < lbin) estimate = lbin;
        if(estimate < singlePrice + lbin) estimate = singlePrice + lbin;

        price = new Evaluation(singlePricesMap, combinationPrice, estimate);
        return price;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Slot getSlot() {
        return slot;
    }

    public static class Evaluation {
        HashMap<String, Integer> singlePrices;
        int combinationPrice;
        int estimate;

        public Evaluation(HashMap<String, Integer> singlePrices, int combinationPrice, int estimate) {
            this.combinationPrice = combinationPrice;
            this.singlePrices = singlePrices;
            this.estimate = estimate;
        }

        public HashMap<String, Integer> getSinglePrices() {
            return singlePrices;
        }

        public int getCombinationPrice() {
            return combinationPrice;
        }

        public int getEstimate() {
            return estimate;
        }
    }

    public static ItemWithAttributes[] getValidItems(List<Slot> slotsToCheck) {
        List<ItemWithAttributes> items = new ArrayList<>();

        for(Slot slot : slotsToCheck) {
            if(!slot.getHasStack()) continue;
            NBTTagCompound extra = slot.getStack().serializeNBT()
                    .getCompoundTag("tag").getCompoundTag("ExtraAttributes");
            NBTTagCompound attributeCompound = extra.getCompoundTag("attributes");
            String itemId = extra.getString("id");

            if(itemId == null || attributeCompound.getKeySet().isEmpty()) continue;
            else itemId = Helper.removeExtraItemIdParts(itemId);

            ItemWithAttributes item = new ItemWithAttributes(itemId, slot);
            for(String attribute : attributeCompound.getKeySet()) {
                item.addAttribute(Helper.formatAttribute(attribute), attributeCompound.getInteger(attribute));
            }
            items.add(item);
        }

        items.sort(Comparator.comparingInt(i -> - i.getDetailedPrice().getEstimate()));
        return items.toArray(new ItemWithAttributes[0]);
    }
}