package cqm3ron.permits.util;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public class ItemHelper {

    public static Item getItemFromString(String itemId) {
        Identifier id = Identifier.of(itemId); // Use Identifier.of() instead of new
        Item item = Registries.ITEM.get(id);   // returns null if not found
        if (item == null) {
            System.out.println("Item not found: " + itemId);
        }
        return item;
    }
}
