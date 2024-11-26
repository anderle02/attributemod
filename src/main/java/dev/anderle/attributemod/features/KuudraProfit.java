package dev.anderle.attributemod.features;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.anderle.attributemod.Main;
import dev.anderle.attributemod.api.PriceApi;
import dev.anderle.attributemod.utils.ItemWithAttributes;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.util.JsonUtils;

import java.util.ArrayList;

public class KuudraProfit {
    private ItemWithAttributes[] chestItems;

    public KuudraProfit() {

    }

    // I would get all items instantly, but I think Hypixel sends those later.
    public void onGuiOpen(ContainerChest chest) {
        new Thread(() -> {
            try {
                Thread.sleep(100);
                getProfitData(chest);
            } catch (InterruptedException ignored) {}
        }).start();
    }

    public void getProfitData(ContainerChest chest) {
        chestItems = ItemWithAttributes.getValidItems(chest.inventorySlots.subList(0, chest.inventorySlots.size() - 36));
        JsonArray array = new JsonArray();
        for(ItemWithAttributes item : chestItems) {
            array.add(item.getNBTAsJson());
        }
        Main.api.sendPostRequest("/kuudrachest", "", array.toString(), new PriceApi.ResponseCallback() {
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
