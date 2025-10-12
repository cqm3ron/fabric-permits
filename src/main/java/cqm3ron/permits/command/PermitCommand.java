package cqm3ron.permits.command;

import com.mojang.brigadier.CommandDispatcher;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.CommandSource;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import cqm3ron.permits.item.ModItems;
import net.minecraft.text.Text;

import java.util.Objects;

import static cqm3ron.permits.component.ModDataComponentTypes.PERMIT_OWNER;
import static cqm3ron.permits.component.ModDataComponentTypes.PERMIT_RARITY;

public class PermitCommand{

private static final SuggestionProvider<ServerCommandSource> RARITY_SUGGESTIONS = (context, builder) -> CommandSource.suggestMatching(new String[]{"iron", "gold", "diamond"}, builder);


    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
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
                            .then(CommandManager.argument("player", StringArgumentType.word()))

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
                                                    "§cInvalid rarity. Allowed: iron, gold, diamond, blank."
                                            ));
                                            return 0;
                                        }

                                        heldStack.set(PERMIT_RARITY, rarity);
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
