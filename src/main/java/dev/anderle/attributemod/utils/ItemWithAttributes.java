package dev.anderle.attributemod.utils;

import dev.anderle.attributemod.Main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemWithAttributes {
    private final HashMap<String, Integer> attributes = new HashMap<String, Integer>();
    private final String id;
    private final String displayName;
    private Evaluation price;

    public ItemWithAttributes(String itemId) {
        this.id = itemId;
        this.displayName = Helper.itemIdToName(itemId);
    }

    public void addAttribute(String name, int level) {
        this.attributes.put(name, level);
    }

    public String[] getAttributeNames() {
        return this.attributes.keySet().toArray(new String[0]);
    }

    public Integer[] getAttributeLevels() {
        return this.attributes.values().toArray(new Integer[0]);
    }

    public Evaluation getDetailedPrice() {
        if(this.price != null) return this.price; // so it doesn't calculate more than 1 time

        int lbin = Main.api.getLbin(this.id);
        int estimate = lbin;

        HashMap<String, Integer> singlePrices = new HashMap<String, Integer>();
        for(Map.Entry<String, Integer> attribute : this.attributes.entrySet()) {
            double singlePrice = Math.pow(2, attribute.getValue() - 1)
                    * Main.api.getAttributePrice(this.id, attribute.getKey());
            singlePrices.put(attribute.getKey(), (int) singlePrice);
            estimate += singlePrice - lbin;
        }

        int combinationPrice = 0;
        if(this.attributes.size() == 2) {
            List<String> targetList = new ArrayList<String>(this.attributes.keySet());
            combinationPrice = Main.api.getCombinationPrice(this.id, targetList.get(0), targetList.get(1));
            estimate += combinationPrice - lbin;
        }

        if(estimate < lbin) estimate = lbin;

        this.price = new Evaluation(singlePrices, combinationPrice, estimate);
        return this.price;
    }

    public String getDisplayName() {
        return this.displayName;
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
}