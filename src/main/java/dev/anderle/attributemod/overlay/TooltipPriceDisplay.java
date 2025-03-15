package dev.anderle.attributemod.overlay;

import dev.anderle.attributemod.utils.Attribute;
import dev.anderle.attributemod.utils.Helper;
import dev.anderle.attributemod.utils.ItemWithAttributes;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

import java.util.ArrayList;
import java.util.List;

public class TooltipPriceDisplay {

    public void onRenderToolTip(ItemTooltipEvent e) {
        ItemWithAttributes item = ItemWithAttributes.fromItemStack(e.itemStack);
        if(item == null) return;

        ItemWithAttributes.Evaluation evaluation = item.getDetailedPrice();
        List<Attribute> attributes = item.getAttributes();
        List<String> toolTip = new ArrayList<>(e.toolTip);

        int lastAttributeIndex = 0;
        for(String line : toolTip) {
            for(int i = 0; i < attributes.size(); i++) {
                Attribute attribute = attributes.get(i);

                // this is to filter lines that contain the word but are not actually a new attribute
                if(!line.contains(attribute.getName() + " ") || line.length() >= 3 * attribute.getName().length()) continue;

                int price = i == 0 ? evaluation.getFirstSingleValue() : evaluation.getSecondSingleValue();
                String newLine = line + " " + EnumChatFormatting.GOLD + Helper.formatNumber(price);
                int index = toolTip.indexOf(line);
                lastAttributeIndex = index;
                e.toolTip.remove(index);
                e.toolTip.add(index, newLine);
            }
        }
        if(item.hasTwoAttributes()) {
            e.toolTip.add(getIndexOfNextFreeLine(lastAttributeIndex, toolTip),
                EnumChatFormatting.AQUA + "Combination: " + EnumChatFormatting.GOLD + Helper.formatNumber(evaluation.getCombinationPrice()) +
                EnumChatFormatting.AQUA + " Estimated Price: " + EnumChatFormatting.GOLD + Helper.formatNumber(evaluation.getEstimate())
            );
        }
    }
    private int getIndexOfNextFreeLine(int currentIndex, List<String> toolTip) {
        int index = currentIndex;
        while(!toolTip.get(index).equals("" + EnumChatFormatting.DARK_PURPLE + EnumChatFormatting.ITALIC)) {
            if(index == toolTip.size() - 1) return index;
            index++;
        }
        return index;
    }
}
