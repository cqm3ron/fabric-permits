package cqm3ron.permits.item;

import cqm3ron.permits.component.ModDataComponentTypes;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.UUID;
import java.util.function.Consumer;

public class PermitItem extends Item {

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> tooltip, TooltipType type) {
        String rarity = stack.get(ModDataComponentTypes.PERMIT_RARITY);

        String capitalised = rarity == null || rarity.isEmpty()
                ? "No"
                : rarity.substring(0, 1).toUpperCase() + rarity.substring(1);


        boolean showOwner = false;
        String ownerName = stack.get(ModDataComponentTypes.PERMIT_OWNER);
        if (ownerName != null){
            showOwner = true;
    }


        Formatting colour = Formatting.WHITE;
        if ("iron".equals(rarity)) colour = Formatting.GRAY;
        else if ("gold".equals(rarity)) colour = Formatting.GOLD;
        else if ("diamond".equals(rarity)) colour = Formatting.AQUA;
        else if ("kermit".equals(rarity)) colour = Formatting.GREEN;

        tooltip.accept(Text.translatable("item.permits.permit.rarity", capitalised).formatted(colour));
        tooltip.accept(Text.empty());
        tooltip.accept(Text.translatable("item.permits.permit.items").append(":").formatted(Formatting.GREEN));
        tooltip.accept(Text.empty());
        tooltip.accept(Text.empty());
        if (showOwner){
            tooltip.accept(Text.translatable("item.permits.permit.owner").append(Text.literal(": ")).formatted(Formatting.YELLOW).append(Text.literal(ownerName)));
        }
        else {
            tooltip.accept(Text.translatable("item.permits.permit.unowned"));
        }
    }

    public PermitItem(Settings settings) {
        super(settings);

    }


}
