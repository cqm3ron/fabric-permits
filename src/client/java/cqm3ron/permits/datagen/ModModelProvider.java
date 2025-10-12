package cqm3ron.permits.datagen;

import cqm3ron.permits.Permits;
import cqm3ron.permits.component.ModDataComponentTypes;
import cqm3ron.permits.item.ModItems;
import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.client.data.*;
import net.minecraft.client.render.item.model.ItemModel;
import net.minecraft.client.render.item.model.SelectItemModel;
import net.minecraft.client.render.item.property.select.ComponentSelectProperty;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// This whole bit was done by @.7410 on the Fabric Discord. He is awesome!

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
        Identifier blankPermit = registerPermitModel(itemModelGenerator, "blank");

        String[] types = { "iron", "gold", "diamond", "kermit" };
        List<SelectItemModel.SwitchCase<String>> switchCases = new ArrayList<>();
        for (String type : types) {
            Identifier model = registerPermitModel(itemModelGenerator, type);
            switchCases.add(ItemModels.switchCase(type, ItemModels.basic(model)));
        }
        ItemModel.Unbaked selectUnbaked = ItemModels.select(
                new ComponentSelectProperty<>(ModDataComponentTypes.PERMIT_RARITY),
                ItemModels.basic(blankPermit),
                switchCases
        );
        itemModelGenerator.output.accept(ModItems.PERMIT, selectUnbaked);
    }

    private static Identifier registerPermitModel(ItemModelGenerator itemModelGenerator, String type) {
        TextureKey textureKey0 = TextureKey.of("0");
        TextureKey textureKey2 = TextureKey.of("2");
        TextureKey textureKey3 = TextureKey.of("3");
        return new Model(
                Optional.of(Identifier.of(Permits.MOD_ID, "item/template_permit")),
                Optional.empty(),
                textureKey0,
                textureKey2,
                textureKey3
        ).upload(
                Identifier.of(Permits.MOD_ID, type + "_permit"),
                new TextureMap()
                        .put(textureKey0, Identifier.of(Permits.MOD_ID, "item/" + type + "_permit/scroll_" + type))
                        .put(textureKey2, Identifier.of(Permits.MOD_ID, "item/" + type + "_permit/bottom_" + type))
                        .put(textureKey3, Identifier.of(Permits.MOD_ID, "item/" + type + "_permit/badge_" + type)),
                itemModelGenerator.modelCollector
        );
    }
}