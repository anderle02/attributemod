package dev.anderle.attributemod.features;

import dev.anderle.attributemod.utils.Helper;
import dev.anderle.attributemod.utils.ItemWithAttributes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TooltipPriceDisplay {

    @SubscribeEvent
    public void onRenderToolTip(ItemTooltipEvent e) {
        List<String> toolTip = new ArrayList<String>(e.toolTip);

        NBTTagCompound extra = e.itemStack.serializeNBT()
                .getCompoundTag("tag").getCompoundTag("ExtraAttributes");
        NBTTagCompound attributeCompound = extra.getCompoundTag("attributes");
        String itemId = extra.getString("id");

        if(itemId == null || attributeCompound.getKeySet().size() == 0) return;
        else itemId = Helper.removeExtraItemIdParts(itemId);

        ItemWithAttributes item = new ItemWithAttributes(itemId);
        for(String attribute : attributeCompound.getKeySet()) {
            item.addAttribute(Helper.formatAttribute(attribute), attributeCompound.getInteger(attribute));
        }
        ItemWithAttributes.Evaluation price = item.getDetailedPrice();

        int lastAttributeIndex = 0;
        for(String line : toolTip) {
            for(String attribute : price.getSinglePrices().keySet()) {
                boolean containsAttribute = line.contains(attribute + " ")
                    && line.length() < 3 * attribute.length(); // this is to filter lines that contain the word but are not actually a new attribute

                if(containsAttribute) {
                    String newLine = line + " \u00A76" + Helper.format(price.getSinglePrices().get(attribute));
                    int index = toolTip.indexOf(line);
                    lastAttributeIndex = index;
                    e.toolTip.remove(index);
                    e.toolTip.add(index, newLine);
                }
            }
        }
        if(attributeCompound.getKeySet().size() == 2) {
            e.toolTip.add(getIndexOfNextFreeLine(lastAttributeIndex, toolTip),
                "\u00A7bCombination: \u00A76" +
                Helper.format(price.getCombinationPrice()) +
                "\u00A7b Estimated Price: \u00A76" + Helper.format(price.getEstimate())
            );
        }
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
