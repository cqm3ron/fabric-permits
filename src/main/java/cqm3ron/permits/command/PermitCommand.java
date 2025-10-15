package cqm3ron.permits.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import cqm3ron.permits.util.Animations;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.component.Component;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import cqm3ron.permits.item.ModItems;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.text.Normalizer;
import java.util.*;

import static cqm3ron.permits.component.ModDataComponentTypes.PERMIT_OWNER;
import static cqm3ron.permits.component.ModDataComponentTypes.PERMIT_RARITY;
import static cqm3ron.permits.component.ModDataComponentTypes.PERMIT_ITEMS;

public class PermitCommand{

private static final SuggestionProvider<ServerCommandSource> RARITY_SUGGESTIONS = (context, builder) -> CommandSource.suggestMatching(new String[]{"iron", "gold", "diamond"}, builder);


    public static <list> void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(
            CommandManager.literal("permit")
            .requires(cs -> cs.hasPermissionLevel(2))

            .then(CommandManager.literal("give")
                .executes(context -> {
                    var player = context.getSource().getPlayer();
                    assert player != null;
                    player.giveOrDropStack(new ItemStack(ModItems.PERMIT, 1));
                    return 1;
                })
            )
                    .then(CommandManager.literal("claim")
                            .executes(context -> {
                                var player = context.getSource().getPlayer();
                                assert player != null;
                                var heldStack = player.getMainHandStack();
                                try {
                                    if (heldStack.isEmpty() ||
                                            !(heldStack.isOf(ModItems.PERMIT))) {
                                        context.getSource().sendError(Text.literal(
                                                "§cYou must be holding a permit item to claim it."
                                        ));
                                        return 0;
                                    }
                                    else if (heldStack.getComponents().get(PERMIT_OWNER) != null){
                                        if (!heldStack.getComponents().get(PERMIT_OWNER).isEmpty()){
                                            context.getSource().sendError(Text.literal(
                                                    "§cYou may not claim an owned permit."
                                            ));
                                            return 0;
                                        }
                                    }
                                } catch (Exception exception){
                                    context.getSource().sendError(Text.literal(
                                            "Error: " + exception
                                    ));
                                }

                                heldStack.set(PERMIT_OWNER, player.getStringifiedName());
                                player.setStackInHand(player.getActiveHand(), heldStack);
                                context.getSource().sendFeedback(
                                        () -> Text.literal("§aPermit claimed!"),
                                        false
                                );
                                return 1;
                            })
                    )

                    .then(CommandManager.literal("trade")
                            .then(CommandManager.argument("target", EntityArgumentType.player())
                                    .executes(context -> {
                                        var sender = context.getSource().getPlayer();
                                        assert sender != null;

                                        ItemStack heldStack = sender.getMainHandStack();
                                        if (heldStack.isEmpty() || !heldStack.isOf(ModItems.PERMIT)) {
                                            context.getSource().sendError(Text.literal("§cYou must be holding a permit to trade it."));
                                            return 0;
                                        }

                                        // Get the target player
                                        var target = EntityArgumentType.getPlayer(context, "target");

                                        if (target == sender) {
                                            context.getSource().sendError(Text.literal("§cYou cannot trade with yourself."));
                                            return 0;
                                        }

                                        if (heldStack.get(PERMIT_OWNER) != target.getStringifiedName()){
                                            context.getSource().sendError(Text.literal("§cYou must own the permit to trade it."));
                                            return 0;
                                        }

                                        // Remove the permit from sender's hand
                                        sender.setStackInHand(sender.getActiveHand(), ItemStack.EMPTY);

                                        // Update PERMIT_OWNER to target
                                        heldStack.set(PERMIT_OWNER, target.getStringifiedName());

                                        // Try to give it to target's inventory
                                        boolean added = target.getInventory().insertStack(heldStack);
                                        if (!added) {
                                            // Inventory full → drop on ground in front of target
                                            target.dropItem(heldStack, false);
                                        }

                                        // Feedback messages
                                        context.getSource().sendFeedback(
                                                () -> Text.literal("§aTraded permit to " + target.getStringifiedName()), false);
                                        target.sendMessage(Text.literal("§aYou received a permit from " + sender.getStringifiedName()), false);

                                        return 1;
                                    })
                            )
                    )


                    .then(CommandManager.literal("name")
                            // handle calling /permit name with no name provided
                            .executes(context -> {
                                context.getSource().sendError(Text.literal("§cUsage: /permit name <name>"));
                                return 0;
                            })
                            // the greedyString argument must be last so it can capture spaces
                            .then(CommandManager.argument("name", StringArgumentType.greedyString())
                                    .executes(context -> {
                                        var player = context.getSource().getPlayer();
                                        if (player == null) {
                                            context.getSource().sendError(Text.literal("§cOnly players can use this."));
                                            return 0;
                                        }

                                        var heldStack = player.getMainHandStack();
                                        if (heldStack.isEmpty() || !heldStack.isOf(ModItems.PERMIT)) {
                                            context.getSource().sendError(Text.literal("§cYou must be holding a permit item to rename it."));
                                            return 0;
                                        }

                                        // read the whole remaining text (multi-word names)
                                        String inputName = StringArgumentType.getString(context, "name").trim().toUpperCase();
                                        if (inputName.isEmpty()) {
                                            context.getSource().sendError(Text.literal("§cName cannot be empty."));
                                            return 0;
                                        }

                                        // Get rarity from your component (assumes it's stored as a string)
                                        String rarity = heldStack.get(PERMIT_RARITY); // adjust if your getter differs

                                        Formatting color;
                                        if (rarity == null) color = Formatting.WHITE;
                                        else {
                                            switch (rarity) {
                                                case "iron" -> color = Formatting.GRAY;
                                                case "gold" -> color = Formatting.GOLD;
                                                case "diamond" -> color = Formatting.AQUA;
                                                case "kermit" -> color = Formatting.GREEN;
                                                default -> color = Formatting.WHITE;
                                            }
                                        }

                                        // Build the Text name with ✦︎ on both sides and bold + chosen color
                                        Text customName = Text.literal("✦ " + inputName + " ✦")
                                                .styled(style -> style.withColor(color)
                                                        .withBold(true)
                                                        .withItalic(false));

                                        // Set the custom name on the itemstack.
                                        // Use whichever method you used previously — example with DataComponentTypes.CUSTOM_NAME:
                                        heldStack.set(DataComponentTypes.CUSTOM_NAME, customName);
                                        // If you use setCustomName or another method, replace the line above accordingly.

                                        // Update the player's held item so client updates immediately
                                        player.setStackInHand(player.getActiveHand(), heldStack);

                                        context.getSource().sendFeedback(() ->
                                                Text.literal("§aRenamed permit to ").append(customName), false);

                                        return 1;
                                    })
                            )
                    )


                    .then(CommandManager.literal("add")
                            .then(CommandManager.argument("item", ItemStackArgumentType.itemStack(registryAccess))
                                    .suggests((context, builder) -> {
                                        var player = context.getSource().getPlayer();
                                        ItemStack heldStack = player.getMainHandStack();
                                        List<Item> alreadyAdded = heldStack.get(PERMIT_ITEMS);
                                        if (alreadyAdded == null) alreadyAdded = List.of();

                                        final String remaining = builder.getRemaining().toLowerCase();

                                        List<Item> finalAlreadyAdded = alreadyAdded;
                                        Registries.ITEM.forEach(item -> {
                                            String id = Registries.ITEM.getId(item).toString();
                                            if (id.toLowerCase().contains(remaining) && !finalAlreadyAdded.contains(item)) {
                                                builder.suggest(id);
                                            }
                                        });

                                        return builder.buildFuture();
                                    })

                                    .executes(context -> {
                                        try {
                                            var player = context.getSource().getPlayer();
                                            var itemInput = ItemStackArgumentType.getItemStackArgument(context, "item");
                                            var itemStack = itemInput.createStack(1, false);
                                            var itemToAdd = itemStack.getItem();

                                            if (itemToAdd == null) {
                                                context.getSource().sendError(Text.literal("§cItem not found!"));
                                                return 0;
                                            }

                                            ItemStack heldStack = player.getMainHandStack();
                                            if (heldStack.isEmpty() || !heldStack.isOf(ModItems.PERMIT)) {
                                                context.getSource().sendError(Text.literal("§cYou must be holding a permit item."));
                                                return 0;
                                            }

                                            List<Item> permitItems = heldStack.get(PERMIT_ITEMS);
                                            if (permitItems == null) {
                                                permitItems = new ArrayList<>();
                                            } else {
                                                permitItems = new ArrayList<>(permitItems);
                                            }

                                            if (!permitItems.contains(itemToAdd)) {
                                                permitItems.add(itemToAdd);
                                                heldStack.set(PERMIT_ITEMS, permitItems);
                                                player.setStackInHand(player.getActiveHand(), heldStack);
                                            }


                                            context.getSource().sendFeedback(() ->
                                                    Text.literal("§aAdded " + Registries.ITEM.getId(itemToAdd) + " to permit."), false);
                                            return 1;
                                        } catch (Exception e) {
                                            context.getSource().sendError(Text.literal("§cError: " + e.toString()));
                                            ;
                                        }
                                        return 1;
                                    })
                            )
                    )


                    .then(CommandManager.literal("rarity")
                            .then(CommandManager.argument("permit_rarity", StringArgumentType.word())
                                    .suggests(RARITY_SUGGESTIONS)
                                    .executes(context -> {
                                        var player = context.getSource().getPlayer();
                                        assert player != null;

                                        String rarity = StringArgumentType.getString(context, "permit_rarity").toLowerCase();
                                        ItemStack heldStack = player.getMainHandStack();

                                        if (heldStack.isEmpty() || !(heldStack.isOf(ModItems.PERMIT))){
                                            context.getSource().sendError(Text.literal(
                                                    "§cYou must be holding a permit item to set its rarity."
                                            ));
                                            return 0;
                                        }

                                        if (!rarity.equals("iron") && !rarity.equals("gold") && !rarity.equals("diamond") &&
                                                !rarity.equals("kermit") && !rarity.equals("blank")) {
                                            context.getSource().sendError(Text.literal(
                                                    "§cInvalid rarity. Allowed: iron, gold, diamond, kermit, blank."
                                            ));
                                            return 0;
                                        }

                                        // Update the PERMIT_RARITY component
                                        heldStack.set(PERMIT_RARITY, rarity);

                                        // Update the item name colour if it has a custom name
                                        Text currentName = heldStack.get(DataComponentTypes.CUSTOM_NAME);
                                        if (currentName != null) {
                                            // Determine new colour based on rarity
                                            Formatting color;
                                            switch (rarity) {
                                                case "iron" -> color = Formatting.GRAY;
                                                case "gold" -> color = Formatting.GOLD;
                                                case "diamond" -> color = Formatting.AQUA;
                                                case "kermit" -> color = Formatting.GREEN;
                                                default -> color = Formatting.WHITE;
                                            }

                                            // Rebuild the display name with same text, new colour, and bold
                                            String nameText = currentName.getString().replaceAll("[✦︎]", "").trim(); // remove previous stars
                                            Text newName = Text.literal("✦ " + nameText + " ✦")
                                                    .styled(style -> style.withColor(color)
                                                            .withBold(true)
                                                            .withItalic(false));                                            heldStack.set(DataComponentTypes.CUSTOM_NAME, newName);
                                        }

                                        // Update the player’s hand so client sees changes immediately
                                        player.setStackInHand(player.getActiveHand(), heldStack);

                                        context.getSource().sendFeedback(
                                                () -> Text.literal("§aPermit updated to " + rarity + " rarity."),
                                                false
                                        );
                                        return 1;
                                    })

                            )
                    )

        );

    }
}
