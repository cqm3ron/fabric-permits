package cqm3ron.permits.datagen;

import cqm3ron.permits.item.ModItems;
import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.client.data.*;
import net.minecraft.client.render.item.model.ItemModel;
import net.minecraft.client.render.item.property.bool.ComponentBooleanProperty;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.predicate.NbtPredicate;
import net.minecraft.predicate.component.ComponentPredicate;
import net.minecraft.predicate.component.ComponentPredicateTypes;
import net.minecraft.predicate.component.CustomDataPredicate;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

public class ModModelProvider extends FabricModelProvider {
    public ModModelProvider(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void generateBlockStateModels(BlockStateModelGenerator blockStateModelGenerator) {
    }

    @Override
    public void generateItemModels(ItemModelGenerator itemModelGenerator) {
        registerComponentDependantModel(itemModelGenerator);
    }

    private static void registerComponentDependantModel(ItemModelGenerator itemModelGenerator) {
        Item item = ModItems.PERMIT;

        Identifier idFalse = itemModelGenerator.upload(item, Models.GENERATED);
        Identifier idTrue = itemModelGenerator.registerSubModel(item, "_false", Models.GENERATED);
        ItemModel.Unbaked unbakedFalse = ItemModels.basic(idFalse);
        ItemModel.Unbaked unbakedTrue = ItemModels.basic(idTrue);

        NbtCompound nbtCompound = Util.make(new NbtCompound(), nbt -> nbt.putString("permit_rarity", "iron"));

        CustomDataPredicate customDataPredicate = new CustomDataPredicate(new NbtPredicate(nbtCompound));
        ComponentPredicate.Typed<CustomDataPredicate> customDataPredicateTyped = new ComponentPredicate.Typed<>(ComponentPredicateTypes.CUSTOM_DATA, customDataPredicate);
        ComponentBooleanProperty componentBooleanProperty = new ComponentBooleanProperty(customDataPredicateTyped);
        ItemModel.Unbaked unbakedCondition = ItemModels.condition(componentBooleanProperty, unbakedTrue, unbakedFalse);
        itemModelGenerator.output.accept(item, unbakedCondition);
    }
}