package dev.anderle.attributemod.features;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.anderle.attributemod.Main;
import dev.anderle.attributemod.api.PriceApi;
import dev.anderle.attributemod.utils.Helper;
import dev.anderle.attributemod.utils.ItemWithAttributes;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.Slot;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.JsonUtils;

import java.util.ArrayList;
import java.util.List;

public class KuudraProfit {
    private JsonArray chestItems;

    public KuudraProfit() {

    }

    // I would get all items instantly, but I think Hypixel sends those later.
    public void onGuiOpen(ContainerChest chest) {
        chestItems = null;
        new Thread(() -> {
            try {
                Thread.sleep(100);
                getProfitData(chest);
            } catch (InterruptedException ignored) {}
        }).start();
    }

    public void getProfitData(ContainerChest chest) {
        List<Slot> slots = chest.inventorySlots.subList(0, chest.inventorySlots.size() - 36);
        chestItems = new JsonArray();

        for(Slot slot : slots) {
            if(!slot.getHasStack()) continue;

            JsonObject nbt = Helper.convertNBTToJson(slot.getStack().writeToNBT(new NBTTagCompound()));
            String id = nbt.get("id").getAsString();

            if(id.equals("minecraft:stained_glass_pane") || id.equals("minecraft:chest") || id.equals("minecraft:barrier")) continue;
            if(Helper.getHypixelId(nbt).equals("KUUDRA_TEETH")) nbt.add("amount", new JsonPrimitive(slot.getStack().stackSize));

            chestItems.add(nbt);
            System.out.println("Slot " + slot.getSlotIndex() + ": " + nbt);
        }

        Main.api.sendPostRequest("/kuudrachest", "", chestItems.toString(), new PriceApi.ResponseCallback() {
            @Override
            public void onResponse(String a) {
                System.out.println(a);
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void onDrawGuiBackground(ContainerChest chest) {

    }
}
