package com.variantsmp;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class Commands {

    private Commands() {}

    public static void register(VariantSMP mod) {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(literal("variants")
                        .requires(Commands::isAdmin)
                        .then(literal("pvp")
                                .then(literal("settings").executes(Commands::menu))
                                .then(literal("default")
                                        .executes(ctx -> {
                                            mod.config.resetSettings();
                                            msg(ctx, "§aAll settings are back to default.");
                                            return 1;
                                        })
                                        .then(literal("set").executes(ctx -> {
                                            mod.config.saveAsDefaults();
                                            msg(ctx, "§aCurrent settings saved as the new defaults.");
                                            return 1;
                                        }))
                                        .then(literal("get").executes(ctx -> {
                                            msg(ctx, "§6Defaults");
                                            for (Settings.Def d : Settings.defs()) {
                                                msg(ctx, "§e" + d.label() + ": " + d.display(mod.config.defaults));
                                            }
                                            return 1;
                                        })))
                                .then(literal("elytra")
                                        .executes(ctx -> {
                                            msg(ctx, "§eElytra flight limit: " + onOff(mod.config.flightEnabled));
                                            return 1;
                                        })
                                        .then(literal("on").executes(ctx -> {
                                            mod.config.flightEnabled = true;
                                            mod.config.save();
                                            msg(ctx, "§eElytra flight limit: " + onOff(true));
                                            return 1;
                                        }))
                                        .then(literal("off").executes(ctx -> {
                                            mod.config.flightEnabled = false;
                                            mod.config.save();
                                            msg(ctx, "§eElytra flight limit: " + onOff(false));
                                            return 1;
                                        })))
                                .then(setNode(mod))
                                .then(pearlNode(mod)))));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> setNode(VariantSMP mod) {
        LiteralArgumentBuilder<ServerCommandSource> set = literal("set");
        for (Settings.Def d : Settings.defs()) {
            LiteralArgumentBuilder<ServerCommandSource> node = literal(d.id()).executes(ctx -> {
                msg(ctx, "§e" + d.id() + ": §f" + d.plain(mod.config));
                return 1;
            });
            switch (d.kind()) {
                case BOOL -> node.then(argument("value", BoolArgumentType.bool()).executes(ctx -> {
                    d.set().accept(mod.config, BoolArgumentType.getBool(ctx, "value") ? 1 : 0);
                    return saved(ctx, mod, d);
                }));
                case INT -> node.then(argument("value", IntegerArgumentType.integer((int) d.min(), (int) d.max())).executes(ctx -> {
                    d.set().accept(mod.config, IntegerArgumentType.getInteger(ctx, "value"));
                    return saved(ctx, mod, d);
                }));
                case DOUBLE -> node.then(argument("value", DoubleArgumentType.doubleArg(d.min(), d.max())).executes(ctx -> {
                    d.set().accept(mod.config, DoubleArgumentType.getDouble(ctx, "value"));
                    return saved(ctx, mod, d);
                }));
                case MODE -> {
                    for (int i = 0; i < Settings.MACE_MODES.length; i++) {
                        final int index = i;
                        node.then(literal(Settings.MACE_MODES[i]).executes(ctx -> {
                            d.set().accept(mod.config, index);
                            return saved(ctx, mod, d);
                        }));
                    }
                }
            }
            set.then(node);
        }
        return set;
    }

    private static int saved(CommandContext<ServerCommandSource> ctx, VariantSMP mod, Settings.Def d) {
        mod.config.save();
        msg(ctx, "§a" + d.id() + " set to §f" + d.plain(mod.config));
        return 1;
    }

    private static LiteralArgumentBuilder<ServerCommandSource> pearlNode(VariantSMP mod) {
        return literal("pearlcatch")
                .executes(ctx -> {
                    msg(ctx, "PearlCatch " + onOff(mod.config.pearlCatchEnabled)
                            + " | radius " + mod.config.pearlCollisionRadius
                            + " | minflight " + mod.config.pearlMinFlightDistance
                            + " | blockzone " + mod.config.pearlBlockZone
                            + " | blendzone " + mod.config.pearlBlendZone
                            + " | ipcwindow " + mod.config.pearlIpcWindowMs + "ms");
                    return 1;
                })
                .then(literal("on").executes(ctx -> {
                    mod.config.pearlCatchEnabled = true;
                    mod.config.save();
                    msg(ctx, "PearlCatch ON");
                    return 1;
                }))
                .then(literal("off").executes(ctx -> {
                    mod.config.pearlCatchEnabled = false;
                    mod.config.save();
                    msg(ctx, "PearlCatch OFF");
                    return 1;
                }))
                .then(literal("radius").then(argument("v", DoubleArgumentType.doubleArg(0.1, 8.0)).executes(ctx -> {
                    mod.config.pearlCollisionRadius = DoubleArgumentType.getDouble(ctx, "v");
                    mod.config.save();
                    msg(ctx, "Collision radius " + mod.config.pearlCollisionRadius);
                    return 1;
                })))
                .then(literal("minflight").then(argument("v", DoubleArgumentType.doubleArg(0.0, 20.0)).executes(ctx -> {
                    mod.config.pearlMinFlightDistance = DoubleArgumentType.getDouble(ctx, "v");
                    mod.config.save();
                    msg(ctx, "Min pearl flight " + mod.config.pearlMinFlightDistance);
                    return 1;
                })))
                .then(literal("blockzone").then(argument("v", DoubleArgumentType.doubleArg(0.0, 20.0)).executes(ctx -> {
                    mod.config.pearlBlockZone = DoubleArgumentType.getDouble(ctx, "v");
                    mod.config.save();
                    msg(ctx, "Same-block zone " + mod.config.pearlBlockZone + " blocks");
                    return 1;
                })))
                .then(literal("blendzone").then(argument("v", DoubleArgumentType.doubleArg(0.0, 10.0)).executes(ctx -> {
                    mod.config.pearlBlendZone = DoubleArgumentType.getDouble(ctx, "v");
                    mod.config.save();
                    msg(ctx, "Blend zone " + mod.config.pearlBlendZone + " blocks");
                    return 1;
                })))
                .then(literal("ipcwindow").then(argument("ms", DoubleArgumentType.doubleArg(0.0, 100.0)).executes(ctx -> {
                    mod.config.pearlIpcWindowMs = DoubleArgumentType.getDouble(ctx, "ms");
                    mod.config.save();
                    msg(ctx, "Insta catch window " + mod.config.pearlIpcWindowMs + "ms");
                    return 1;
                })))
                .then(literal("debug").executes(ctx -> {
                    PearlCatch pc = mod.pearlCatch();
                    if (pc == null) return 0;
                    pc.debug = !pc.debug;
                    msg(ctx, "PearlCatch debug " + onOff(pc.debug));
                    return 1;
                }));
    }

    private static int menu(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendError(Text.literal("Run this in game, or use /variants pvp set <setting> <value>"));
            return 0;
        }
        Menu.open(player);
        return 1;
    }

    private static String onOff(boolean b) {
        return b ? "§aON" : "§cOFF";
    }

    private static void msg(CommandContext<ServerCommandSource> ctx, String text) {
        ctx.getSource().sendFeedback(() -> Text.literal(text), false);
    }

    private static boolean isAdmin(ServerCommandSource src) {
        try {
            ServerPlayerEntity p = src.getPlayer();
            if (p == null) return true;
            MinecraftServer s = p.getEntityWorld().getServer();
            if (s == null || s.isSingleplayer()) return true;
            net.minecraft.server.PlayerConfigEntry entry = new net.minecraft.server.PlayerConfigEntry(p.getGameProfile());
            if (s.isHost(entry)) return true;
            return s.getPlayerManager().isOperator(entry);
        } catch (Throwable t) {
            VariantSMP.LOGGER.warn("variantSMP: isAdmin check failed, allowing", t);
            return true;
        }
    }
}
