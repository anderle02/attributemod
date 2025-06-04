package dev.anderle.attributemod.utils;

import dev.anderle.attributemod.AttributeMod;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.slot.Slot;
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

                int tempLevel = firstLevel;
                firstLevel = secondLevel;
                secondLevel = tempLevel;
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
            if(!slot.hasStack()) continue;

            NbtCompound nbt = Objects.requireNonNull(slot.getStack().get(DataComponentTypes.CUSTOM_DATA)).copyNbt();
            NbtCompound extra = nbt.getCompoundOrEmpty("tag").getCompoundOrEmpty("ExtraAttributes");
            NbtCompound attributeCompound = extra.getCompoundOrEmpty("attributes");
            Set<String> attributeCompoundKeySet = attributeCompound.getKeys();
            String itemId = extra.getString("id", "");

            if(itemId.isEmpty() || attributeCompoundKeySet.isEmpty()) continue;

            ItemWithAttributes item = new ItemWithAttributes(Helper.removeExtraItemIdParts(itemId), slot);

            Arrays.stream(Attribute.values())
                    .filter(a -> attributeCompoundKeySet.contains(a.getHypixelName()))
                    .forEach(a -> item.addAttribute(a, attributeCompound.getInt(a.getHypixelName(), 0)));

            items.add(item);
        }

        items.sort(Comparator.comparingInt(i -> - i.getDetailedPrice().estimate()));
        return items;
    }

    /** Get a valid ItemWithAttributes from an ItemStack. Returns null if the item doesn't have attributes. */
    public static ItemWithAttributes fromItemStack(ItemStack itemStack) {
        NbtCompound nbt = Objects.requireNonNull(itemStack.get(DataComponentTypes.CUSTOM_DATA)).copyNbt();
        NbtCompound extra = nbt.getCompoundOrEmpty("tag").getCompoundOrEmpty("ExtraAttributes");
        NbtCompound attributeCompound = extra.getCompoundOrEmpty("attributes");
        Set<String> attributeCompoundKeySet = attributeCompound.getKeys();
        String itemId = extra.getString("id", "");

        if(itemId.isEmpty() || attributeCompoundKeySet.isEmpty()) return null;

        ItemWithAttributes item = new ItemWithAttributes(Helper.removeExtraItemIdParts(itemId));

        Arrays.stream(Attribute.values())
                .filter(a -> attributeCompoundKeySet.contains(a.getHypixelName()))
                .forEach(a -> item.addAttribute(a, attributeCompound.getInt(a.getHypixelName(), 0)));

        return item;
    }

    /** Data structure for item price. */
    public record Evaluation(int firstSingleValue, int secondSingleValue, int combinationPrice, int estimate) {}
}