package cqm3ron.permits.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import cqm3ron.permits.item.ModItems;

import java.util.*;
import java.util.concurrent.*;

import static cqm3ron.permits.component.ModDataComponentTypes.*;

public class PermitCommand{

private static final SuggestionProvider<ServerCommandSource> RARITY_SUGGESTIONS = (context, builder) -> CommandSource.suggestMatching(new String[]{"iron", "gold", "diamond"}, builder);


    // ----- TRADE SYSTEM -----
    private static final Map<UUID, TradeRequest> TRADE_REQUESTS = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService TRADE_TIMEOUT_EXECUTOR = Executors.newSingleThreadScheduledExecutor();
    private static final int TRADE_TIMEOUT_SECONDS = 30;

    private record TradeRequest(UUID sender, long expiryTime) {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess) {

        dispatcher.register(
            CommandManager.literal("permit")
                    .then(CommandManager.literal("give")
                            .requires(cs -> cs.hasPermissionLevel(2))
                            // No arguments: just give a blank permit
                            .executes(context -> {
                                var player = context.getSource().getPlayer();
                                if (player == null) {
                                    context.getSource().sendError(Text.literal("§cOnly players can receive permits."));
                                    return 0;
                                }

                                ItemStack permit = new ItemStack(ModItems.PERMIT, 1);
                                player.giveOrDropStack(permit);
                                context.getSource().sendFeedback(() -> Text.literal("§aGave a blank permit."), false);
                                return 1;
                            })
                            // Optional rarity argument
                            .then(CommandManager.argument("rarity", StringArgumentType.word())
                                    .suggests(RARITY_SUGGESTIONS)
                                    .executes(context -> {
                                        var player = context.getSource().getPlayer();
                                        if (player == null) return 0;

                                        String rarity = StringArgumentType.getString(context, "rarity").toLowerCase();

                                        if (!rarity.equals("iron") && !rarity.equals("gold") && !rarity.equals("diamond")
                                                && !rarity.equals("kermit") && !rarity.equals("blank")) {
                                            context.getSource().sendError(Text.literal("§cInvalid rarity. Allowed: iron, gold, diamond, kermit, blank."));
                                            return 0;
                                        }

                                        ItemStack permit = new ItemStack(ModItems.PERMIT, 1);
                                        permit.set(PERMIT_RARITY, rarity);

                                        player.giveOrDropStack(permit);
                                        context.getSource().sendFeedback(() -> Text.literal("§aGave a " + rarity + " permit."), false);
                                        return 1;
                                    })
                                    // Optional name argument
                                    .then(CommandManager.argument("name", StringArgumentType.greedyString())
                                            .executes(context -> {
                                                var player = context.getSource().getPlayer();
                                                if (player == null) return 0;

                                                String rarity = StringArgumentType.getString(context, "rarity").toLowerCase();
                                                String inputName = StringArgumentType.getString(context, "name").trim().toUpperCase();

                                                if (!rarity.equals("iron") && !rarity.equals("gold") && !rarity.equals("diamond")
                                                        && !rarity.equals("kermit") && !rarity.equals("blank")) {
                                                    context.getSource().sendError(Text.literal("§cInvalid rarity. Allowed: iron, gold, diamond, kermit, blank."));
                                                    return 0;
                                                }

                                                ItemStack permit = new ItemStack(ModItems.PERMIT, 1);
                                                permit.set(PERMIT_RARITY, rarity);

                                                if (!inputName.isEmpty()) {
                                                    Formatting color = switch (rarity) {
                                                        case "iron" -> Formatting.GRAY;
                                                        case "gold" -> Formatting.GOLD;
                                                        case "diamond" -> Formatting.AQUA;
                                                        case "kermit" -> Formatting.GREEN;
                                                        default -> Formatting.WHITE;
                                                    };

                                                    Text customName = Text.literal("✦ " + inputName + " ✦")
                                                            .styled(s -> s.withColor(color).withBold(true).withItalic(false));
                                                    permit.set(DataComponentTypes.CUSTOM_NAME, customName);
                                                }

                                                player.giveOrDropStack(permit);
                                                context.getSource().sendFeedback(() -> Text.literal("§aGave permit " + (inputName.isEmpty() ? "" : "'" + inputName + "'")), false);
                                                return 1;
                                            })
                                    )
                            )
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
                                        if (!Objects.requireNonNull(heldStack.getComponents().get(PERMIT_OWNER)).isEmpty()){
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
                                        ServerPlayerEntity sender = context.getSource().getPlayer();
                                        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "target");
                                        assert sender != null;

                                        ItemStack heldStack = sender.getMainHandStack();
                                        if (heldStack.isEmpty() || !heldStack.isOf(ModItems.PERMIT)) {
                                            sender.sendMessage(Text.literal("§cYou must be holding a permit to trade it."));
                                            return 0;
                                        }

                                        String owner = heldStack.get(PERMIT_OWNER);
                                        if (owner == null || !owner.equalsIgnoreCase(sender.getStringifiedName())) {
                                            sender.sendMessage(Text.literal("§cYou must own the permit to trade it."));
                                            return 0;
                                        }

                                        if (sender.getUuid().equals(target.getUuid())) {
                                            sender.sendMessage(Text.literal("§cYou cannot trade with yourself."));
                                            return 0;
                                        }

                                        if (TRADE_REQUESTS.containsKey(target.getUuid())) {
                                            sender.sendMessage(Text.literal("§cThis player already has a pending trade request."));
                                            return 0;
                                        }

                                        long expiryTime = System.currentTimeMillis() + (TRADE_TIMEOUT_SECONDS * 1000L);
                                        TRADE_REQUESTS.put(target.getUuid(), new TradeRequest(sender.getUuid(), expiryTime));

                                        sender.sendMessage(Text.literal("§aTrade request sent to " + target.getName().getString()));

                                        Text acceptMsg = Text.literal(sender.getName().getString() + " wants to trade a permit with you. ")
                                                .append(
                                                        Text.literal("[ACCEPT]")
                                                                .styled(style -> style
                                                                        .withColor(Formatting.GREEN)
                                                                        .withBold(true)
                                                                        .withClickEvent(new ClickEvent.RunCommand("/permit accept")))
                                                )
                                                .append(" ")
                                                .append(
                                                        Text.literal("[DENY]")
                                                                .styled(style -> style
                                                                        .withColor(Formatting.RED)
                                                                        .withBold(true)
                                                                        .withClickEvent(new ClickEvent.RunCommand("/permit deny")))
                                                );

                                        target.sendMessage(acceptMsg);

                                        // schedule timeout
                                        TRADE_TIMEOUT_EXECUTOR.schedule(() -> {
                                            TradeRequest req = TRADE_REQUESTS.get(target.getUuid());
                                            if (req != null && req.expiryTime == expiryTime) {
                                                TRADE_REQUESTS.remove(target.getUuid());
                                                sender.sendMessage(Text.literal("§cYour trade request to " + target.getName().getString() + " expired."));
                                                target.sendMessage(Text.literal("§cTrade request from " + sender.getName().getString() + " expired."));
                                            }
                                        }, TRADE_TIMEOUT_SECONDS, TimeUnit.SECONDS);

                                        return 1;
                                    })
                            )
                    )



                    .then(CommandManager.literal("name")
                            .requires(cs -> cs.hasPermissionLevel(2))
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

                    .then(CommandManager.literal("accept")
                            .executes(context -> {
                                ServerPlayerEntity target = context.getSource().getPlayer();
                                TradeRequest request = TRADE_REQUESTS.remove(target.getUuid());

                                if (request == null) {
                                    target.sendMessage(Text.literal("§cYou have no pending trade requests."));
                                    return 0;
                                }

                                ServerPlayerEntity sender = target.getEntityWorld().getServer().getPlayerManager().getPlayer(request.sender());
                                if (sender == null) {
                                    target.sendMessage(Text.literal("§cThe sender is no longer online."));
                                    return 0;
                                }

                                ItemStack heldStack = sender.getMainHandStack();
                                if (heldStack.isEmpty() || !heldStack.isOf(ModItems.PERMIT)) {
                                    target.sendMessage(Text.literal("§cThe sender no longer holds a permit."));
                                    return 0;
                                }

                                heldStack.set(PERMIT_OWNER, target.getStringifiedName());
                                sender.setStackInHand(sender.getActiveHand(), ItemStack.EMPTY);

                                boolean added = target.getInventory().insertStack(heldStack);
                                if (!added) target.dropItem(heldStack, false);

                                sender.sendMessage(Text.literal("§aTrade successful!"));
                                target.sendMessage(Text.literal("§aYou accepted the trade and received the permit."));

                                return 1;
                            })
                    )

                    .then(CommandManager.literal("deny")
                            .executes(context -> {
                                ServerPlayerEntity target = context.getSource().getPlayer();
                                TradeRequest request = TRADE_REQUESTS.remove(target.getUuid());

                                if (request == null) {
                                    target.sendMessage(Text.literal("§cYou have no pending trade requests."));
                                    return 0;
                                }

                                ServerPlayerEntity sender = target.getEntityWorld().getServer().getPlayerManager().getPlayer(request.sender());
                                if (sender != null) {
                                    sender.sendMessage(Text.literal("§cYour trade request was denied by " + target.getName().getString() + "."));
                                }

                                target.sendMessage(Text.literal("§cYou denied the trade request."));
                                return 1;
                            })
                    )



                    .then(CommandManager.literal("add")
                            .requires(cs -> cs.hasPermissionLevel(2))
                            .then(CommandManager.argument("item", ItemStackArgumentType.itemStack(registryAccess))
                                    .suggests((context, builder) -> {
                                        var player = context.getSource().getPlayer();
                                        assert player != null;
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

                                            assert player != null;
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
                                            context.getSource().sendError(Text.literal("§cError: " + e));
                                        }
                                        return 1;
                                    })
                            )
                    )


                    .then(CommandManager.literal("rarity")
                            .requires(cs -> cs.hasPermissionLevel(2))
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
