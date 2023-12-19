package dev.anderle.attributemod.features;

import dev.anderle.attributemod.utils.Constants;
import dev.anderle.attributemod.Main;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class TooltipPriceDisplay {

    @SubscribeEvent
    public void onRenderToolTip(ItemTooltipEvent e) {
        NBTTagCompound extra = e.itemStack.serializeNBT()
                .getCompoundTag("tag").getCompoundTag("ExtraAttributes");
        NBTTagCompound attributeCompound = extra.getCompoundTag("attributes");
        String itemId = extra.getString("id");

        if(itemId == null || attributeCompound.getKeySet().size() == 0) return;
        else itemId = replaceExtraItemIdParts(itemId);

        List<String> toolTip = new ArrayList<String>(e.toolTip);

        Set<String> attributes = attributeCompound.getKeySet();
        if(attributes.size() == 0) return;
        int lbin = Main.api.getLbin(itemId);

        int lastAttributeIndex = 0;
        int total = lbin;
        for(String line : toolTip) {
            for(String attribute : attributes) {
                String attributeDisplayName = Main.helper.formatAttribute(attribute);
                if(line.contains("\u00A7b" + attributeDisplayName + " ")) {
                    int level = attributeCompound.getInteger(attribute);
                    double price = Math.pow(2, level - 1) * Main.api.getAttributePrice(itemId, attributeDisplayName);
                    total += price - lbin;
                    String newLine = line + " \u00A76" + Main.helper.format((long) price);
                    int index = toolTip.indexOf(line);
                    lastAttributeIndex = index;
                    e.toolTip.remove(index);
                    e.toolTip.add(index, newLine);
                }
            }
        }
        if(attributes.size() == 2) {
            int nextFreeIndex = getIndexOfNextFreeLine(lastAttributeIndex, toolTip);
            String[] attributeArray = {};
            attributeArray = attributes.toArray(attributeArray);
            int price = Main.api.getCombinationPrice(itemId,
                    Main.helper.formatAttribute(attributeArray[0]),
                    Main.helper.formatAttribute(attributeArray[1]));
            total += price - lbin;
            if(total < lbin) total = lbin;
            e.toolTip.add(nextFreeIndex, "\u00A7bCombination: \u00A76"
                    + Main.helper.format(price) + "\u00A7b Estimated Price: \u00A76"
                    + Main.helper.format(total));
        }
    }
    private String replaceExtraItemIdParts(String itemId) {
        for(String part : Constants.itemIdPartsToIgnore) {
            if(itemId.contains(part)) return itemId.replace(part, "");
        }
        return itemId;
    }
    private int getIndexOfNextFreeLine(int currentIndex, List<String> toolTip) {
        int index = currentIndex;
        while(!toolTip.get(index).equals("\u00A75\u00A7o")) {
            if(index == toolTip.size() - 1) return index;
            index++;
        }
        return index;
    }
}
