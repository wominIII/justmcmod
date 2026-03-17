package com.zmer.testmod.energy;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.zmer.testmod.ExampleMod;
import com.zmer.testmod.item.AiBeltItem;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Debug/test command: /exoenergy set <0-100>
 * Sets the exoskeleton energy level of the executing player.
 */
@Mod.EventBusSubscriber(modid = ExampleMod.MODID)
public class ExoEnergyCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
            Commands.literal("exoenergy")
                .requires(src -> src.hasPermission(2)) // OP level 2+
                .then(Commands.literal("set")
                    .then(Commands.argument("amount", IntegerArgumentType.integer(0, 100))
                        .executes(ctx -> {
                            CommandSourceStack source = ctx.getSource();
                            ServerPlayer player = source.getPlayerOrException();
                            int amount = IntegerArgumentType.getInteger(ctx, "amount");

                            ExoskeletonEnergyManager.setEnergy(player, amount);

                            source.sendSuccess(
                                () -> Component.literal("Exoskeleton energy set to " + amount + "%"),
                                true
                            );
                            return 1;
                        })
                    )
                )
                .then(Commands.literal("get")
                    .executes(ctx -> {
                        CommandSourceStack source = ctx.getSource();
                        ServerPlayer player = source.getPlayerOrException();
                        int energy = ExoskeletonEnergyManager.getEnergy(player);

                        source.sendSuccess(
                            () -> Component.literal("Exoskeleton energy: " + energy + "%"),
                            false
                        );
                        return energy;
                    })
                )
        );

        // SYNC command for debugging Sync levels
        dispatcher.register(
            Commands.literal("exosync")
                .requires(src -> src.hasPermission(2)) // OP level 2+
                .then(Commands.literal("set")
                    .then(Commands.argument("amount", IntegerArgumentType.integer(0, 100))
                        .executes(ctx -> {
                            int amount = IntegerArgumentType.getInteger(ctx, "amount");
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            SyncManager.setSync(player, amount);
                            ctx.getSource().sendSuccess(() -> Component.literal("Sync set to " + amount + "%"), true);
                            return 1;
                        })
                    )
                )
                .then(Commands.literal("add")
                    .then(Commands.argument("amount", IntegerArgumentType.integer(-100, 100))
                        .executes(ctx -> {
                            int amount = IntegerArgumentType.getInteger(ctx, "amount");
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            int current = SyncManager.getSync(player);
                            int next = Math.max(0, Math.min(100, current + amount));
                            SyncManager.setSync(player, next);
                            ctx.getSource().sendSuccess(() -> Component.literal("Sync added. Now: " + next + "%"), true);
                            return 1;
                        })
                    )
                )
                .then(Commands.literal("get")
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        int current = SyncManager.getSync(player);
                        ctx.getSource().sendSuccess(() -> Component.literal("Current Sync: " + current + "%"), false);
                        return current;
                    })
                )
        );

        // AI Belt test command
        String[] directiveNames = java.util.Arrays.stream(AiBeltItem.Directive.values())
                .map(Enum::name).map(String::toLowerCase).toArray(String[]::new);

        dispatcher.register(
            Commands.literal("aibelt")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("trigger")
                    // /aibelt trigger — random directive
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        AiBeltItem.forceDirective(player, null);
                        ctx.getSource().sendSuccess(() -> Component.literal("AI Belt: triggered random directive"), true);
                        return 1;
                    })
                    // /aibelt trigger <type>
                    .then(Commands.argument("type", StringArgumentType.word())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(directiveNames, builder))
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            String typeName = StringArgumentType.getString(ctx, "type").toUpperCase();
                            try {
                                AiBeltItem.Directive d = AiBeltItem.Directive.valueOf(typeName);
                                AiBeltItem.forceDirective(player, d);
                                ctx.getSource().sendSuccess(() -> Component.literal("AI Belt: triggered " + typeName), true);
                            } catch (IllegalArgumentException e) {
                                ctx.getSource().sendFailure(Component.literal("Unknown directive: " + typeName));
                            }
                            return 1;
                        })
                    )
                )
                .then(Commands.literal("list")
                    .executes(ctx -> {
                        StringBuilder sb = new StringBuilder("§dAI Belt Directives:\n");
                        for (AiBeltItem.Directive d : AiBeltItem.Directive.values()) {
                            sb.append("§e- ").append(d.name().toLowerCase()).append("\n");
                        }
                        ctx.getSource().sendSuccess(() -> Component.literal(sb.toString()), false);
                        return 1;
                    })
                )
        );
    }
}
