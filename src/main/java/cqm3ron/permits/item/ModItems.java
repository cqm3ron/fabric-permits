package cqm3ron.permits.item;

import cqm3ron.permits.Permits;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import java.util.function.Function;

import static cqm3ron.permits.component.ModDataComponentTypes.PERMIT_RARITY;

public class ModItems {
    public static Item register(String name, Function<Item.Settings, Item> itemFactory, Item.Settings settings) {
        // Create the item key.
        RegistryKey<Item> itemKey = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Permits.MOD_ID, name));

        // Create the item instance.
        Item item = itemFactory.apply(settings.registryKey(itemKey));

        // Register the item.
        Registry.register(Registries.ITEM, itemKey, item);

        return item;
    }

public static final Item PERMIT= register("permit", PermitItem::new, new Item.Settings().maxCount(1));




    private static Item registerItem(String name, Item item){
        return Registry.register(Registries.ITEM, Identifier.of(Permits.MOD_ID, name), item);
    }

    public static void registerModItems() {
        Permits.LOGGER.info("Registering Mod Items for " + Permits.MOD_ID);

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(entries -> {entries.add(PERMIT);});
    }

}

