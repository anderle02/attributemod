package dev.anderle.attributemod.utils;

import dev.anderle.attributemod.Main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemWithAttributes {
    HashMap<String, Integer> attributes = new HashMap<String, Integer>();
    String id;

    public ItemWithAttributes(String itemId) {
        this.id = itemId;
    }

    public void addAttribute(String name, int level) {
        this.attributes.put(name, level);
    }

    public Evaluation getDetailedPrice() {
        int lbin = Main.api.getLbin(this.id);
        int estimate = lbin;

        HashMap<String, Integer> singlePrices = new HashMap<String, Integer>();
        for(Map.Entry attribute : this.attributes.entrySet()) {
            double singlePrice = Math.pow(2, Integer.parseInt(attribute.getValue().toString()) - 1)
                    * Main.api.getAttributePrice(this.id, attribute.getKey().toString());
            singlePrices.put(attribute.getKey().toString(), (int) singlePrice);
            estimate += singlePrice - lbin;
        }

        int combinationPrice = 0;
        if(this.attributes.size() == 2) {
            List<String> targetList = new ArrayList<String>(this.attributes.keySet());
            combinationPrice = Main.api.getCombinationPrice(this.id, targetList.get(0), targetList.get(1));
            estimate += combinationPrice - lbin;
        }

        if(estimate < lbin) estimate = lbin;
        return new Evaluation(singlePrices, combinationPrice, estimate);
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