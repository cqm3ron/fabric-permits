package cqm3ron.permits.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import cqm3ron.permits.Permits;
import net.minecraft.component.ComponentType;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.UUID;
import java.util.function.UnaryOperator;

public class ModDataComponentTypes {

    public static final ComponentType<String> PERMIT_RARITY = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(Permits.MOD_ID, "permit_rarity"),
            ComponentType.<String>builder().codec(Codec.STRING).build()
    );
    public static final ComponentType<String> PERMIT_OWNER = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(Permits.MOD_ID, "permit_owner"),
            ComponentType.<String>builder().codec(Codec.STRING).build()
    );



    private static <T> ComponentType<T> register(String name, UnaryOperator<ComponentType.Builder<T>> builderOperator) {
        return Registry.register(Registries.DATA_COMPONENT_TYPE, Identifier.of(Permits.MOD_ID, name), builderOperator.apply(ComponentType.builder()).build());
    }

    public static void registerDataComponentTypes() {
        Permits.LOGGER.info("Registering Data Component Types for " + Permits.MOD_ID);
    }
}
