package dev.anderle.attributemod.utils;

import dev.anderle.attributemod.AttributeMod;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ItemWithAttributes {
    private Attribute firstAttribute;
    private Attribute secondAttribute;
    private int firstLevel;
    private int secondLevel;
    private final Slot slot;
    private final String id;
    private final String displayName;
    private Evaluation price = null;

    public ItemWithAttributes(String itemId, Slot slot) {
        this.slot = slot;
        this.id = itemId;
        this.displayName = Helper.itemIdToName(itemId, true);
    }

    public ItemWithAttributes(String itemId) {
        this(itemId, null);
    }

    /** Add an attribute to this item. */
    public void addAttribute(Attribute attribute, int level) {
        if(firstAttribute == null) {
            firstAttribute = attribute;
            firstLevel = level;
        } else {
            secondAttribute = attribute;
            secondLevel = level;

            // Make the popular attribute come first.
            if(!firstAttribute.isPopular() && secondAttribute.isPopular()) {
                Attribute temp = firstAttribute;
                firstAttribute = secondAttribute;
                secondAttribute = temp;
            }
        }
    }

    /** Returns a list with one or two Attribute objects. */
    public List<Attribute> getAttributes() {
        if(hasTwoAttributes()) return Arrays.asList(firstAttribute, secondAttribute);
        else return Collections.singletonList(firstAttribute);
    }

    /** Returns a list with one or two attribute levels. */
    public List<Integer> getAttributeLevels() {
        if(hasTwoAttributes()) return Arrays.asList(firstLevel, secondLevel);
        else return Collections.singletonList(firstLevel);
    }

    /** Whether this item has one or two attributes. */
    public boolean hasTwoAttributes() {
        return secondAttribute != null;
    }

    /** Calculates the item price if needed and returns it. */
    public Evaluation getDetailedPrice() {
        if(price != null) return price; // so it doesn't calculate more than 1 time

        int lbin = AttributeMod.backend.getLbin(id);
        int firstSingleValue = (int) Math.pow(2, firstLevel - 1) * AttributeMod.backend.getAttributePrice(id, firstAttribute.getName());

        if(!hasTwoAttributes()) {
            int estimate = firstSingleValue + lbin;
            return new Evaluation(firstSingleValue, 0,0, estimate);
        }

        int secondSingleValue = (int) Math.pow(2, secondLevel - 1) * AttributeMod.backend.getAttributePrice(id, secondAttribute.getName());
        int combinationPrice = AttributeMod.backend.getCombinationPrice(id, firstAttribute.getName(), secondAttribute.getName());

        int singlesValue = firstLevel < 6 && secondLevel < 6
                ? Math.max(firstSingleValue, secondSingleValue) - lbin
                : firstSingleValue + secondSingleValue - 2 * lbin;

        if(id.equals("HELLFIRE_ROD")) singlesValue += 2 * lbin;

        int estimate = singlesValue + combinationPrice;
        if(estimate < lbin) estimate = lbin;
        if(estimate < singlesValue + lbin) estimate = singlesValue + lbin;
        if(estimate < combinationPrice) estimate = combinationPrice;

        price = new Evaluation(firstSingleValue, secondSingleValue, combinationPrice, estimate);
        return price;
    }

    /** Returns the Hypixel display name of this item. */
    public String getDisplayName() {
        return displayName;
    }

    /** Returns the container slot this item is in. */
    public @Nullable Slot getSlot() {
        return slot;
    }

    /** Returns all valid ItemWithAttribute objects for a list of container slots. */
    public static List<ItemWithAttributes> fromContainer(List<Slot> slots) {
        List<ItemWithAttributes> items = new ArrayList<>();

        for(Slot slot : slots) {
            if(!slot.getHasStack()) continue;

            NBTTagCompound extra = slot.getStack().serializeNBT().getCompoundTag("tag").getCompoundTag("ExtraAttributes");
            NBTTagCompound attributeCompound = extra.getCompoundTag("attributes");
            Set<String> attributeCompoundKeySet = attributeCompound.getKeySet();
            String itemId = extra.getString("id");

            if(itemId.isEmpty() || attributeCompoundKeySet.isEmpty()) continue;

            ItemWithAttributes item = new ItemWithAttributes(Helper.removeExtraItemIdParts(itemId), slot);

            Arrays.stream(Attribute.values())
                    .filter(a -> attributeCompoundKeySet.contains(a.getHypixelName()))
                    .forEach(a -> item.addAttribute(a, attributeCompound.getInteger(a.getHypixelName())));

            items.add(item);
        }

        items.sort(Comparator.comparingInt(i -> - i.getDetailedPrice().getEstimate()));
        return items;
    }

    /** Get a valid ItemWithAttributes from an ItemStack. Returns null if the item doesn't have attributes. */
    public static ItemWithAttributes fromItemStack(ItemStack itemStack) {
        NBTTagCompound extra = itemStack.serializeNBT().getCompoundTag("tag").getCompoundTag("ExtraAttributes");
        NBTTagCompound attributeCompound = extra.getCompoundTag("attributes");
        Set<String> attributeCompoundKeySet = attributeCompound.getKeySet();
        String itemId = extra.getString("id");

        if(itemId.isEmpty() || attributeCompoundKeySet.isEmpty()) return null;

        ItemWithAttributes item = new ItemWithAttributes(Helper.removeExtraItemIdParts(itemId));

        Arrays.stream(Attribute.values())
                .filter(a -> attributeCompoundKeySet.contains(a.getHypixelName()))
                .forEach(a -> item.addAttribute(a, attributeCompound.getInteger(a.getHypixelName())));

        return item;
    }

    /** Data structure for item price. */
    public static class Evaluation {
        private final int firstSingleValue;
        private final int secondSingleValue;
        private final int combinationPrice;
        private final int estimate;

        public Evaluation(int firstSingleValue, int secondSingleValue, int combinationPrice, int estimate) {
            this.combinationPrice = combinationPrice;
            this.firstSingleValue = firstSingleValue;
            this.secondSingleValue = secondSingleValue;
            this.estimate = estimate;
        }

        public int getFirstSingleValue() {
            return firstSingleValue;
        }

        public int getSecondSingleValue() {
            return secondSingleValue;
        }

        public int getCombinationPrice() {
            return combinationPrice;
        }

        public int getEstimate() {
            return estimate;
        }
    }
}