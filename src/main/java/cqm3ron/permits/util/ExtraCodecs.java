package cqm3ron.permits.util;

import com.mojang.serialization.Codec;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public class ExtraCodecs {
    public static final Codec<Item> ITEM_CODEC = Identifier.CODEC.xmap(
            Registries.ITEM::get,
            Registries.ITEM::getId
    );

    public static final Codec<java.util.List<Item>> ITEM_LIST_CODEC = ITEM_CODEC.listOf();
}
