package com.macelimit;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.inventory.Inventory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Map;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class MaceLimit implements ModInitializer {

    public static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("macelimit");

    @Override
    public void onInitialize() {
        MaceRegistry.load(FabricLoader.getInstance().getConfigDir().resolve("macelimit.json"));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % 20 != 0) return;
            try {
                MaceRegistry.scan(server);
            } catch (Throwable t) {
                LOGGER.warn("macelimit: scan failed", t);
            }
        });
        ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.register((blockEntity, world) -> {
            try {
                if (blockEntity instanceof Inventory inv) MaceRegistry.seeContainer(inv, world, blockEntity.getPos());
            } catch (Throwable t) {
                LOGGER.warn("macelimit: container check failed", t);
            }
        });
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            try {
                MaceRegistry.seeEntity(entity);
            } catch (Throwable t) {
                LOGGER.warn("macelimit: entity check failed", t);
            }
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(literal("macelimit")
                        .requires(MaceLimit::isAdmin)
                        .executes(MaceLimit::status)
                        .then(literal("list").executes(MaceLimit::list))
                        .then(literal("scan").executes(ctx -> {
                            if (WorldScan.running()) {
                                msg(ctx, "§cA scan is already running");
                                return 0;
                            }
                            msg(ctx, "§eSaving the world and scanning every chunk and player file for maces...");
                            WorldScan.start(ctx.getSource().getServer(), ctx.getSource());
                            return 1;
                        }))
                        .then(literal("limit").then(argument("amount", IntegerArgumentType.integer(0, 100)).executes(ctx -> {
                            MaceRegistry.data.limit = IntegerArgumentType.getInteger(ctx, "amount");
                            MaceRegistry.save();
                            msg(ctx, "§aMace limit set to §f" + MaceRegistry.data.limit);
                            return 1;
                        })))
                        .then(literal("delete").then(argument("id", StringArgumentType.word()).executes(ctx -> {
                            String id = StringArgumentType.getString(ctx, "id");
                            String result = MaceRegistry.delete(ctx.getSource().getServer(), id);
                            if (result == null) {
                                msg(ctx, "§cNo tracked mace with id " + id);
                                return 0;
                            }
                            msg(ctx, "§aMace §f" + id + "§a: " + result);
                            return 1;
                        })))
                        .then(literal("remove").then(argument("id", StringArgumentType.word()).executes(ctx -> {
                            String id = StringArgumentType.getString(ctx, "id");
                            if (!MaceRegistry.data.maces.containsKey(id)) {
                                msg(ctx, "§cNo tracked mace with id " + id);
                                return 0;
                            }
                            MaceRegistry.destroyed(id);
                            msg(ctx, "§aStopped tracking mace §f" + id);
                            return 1;
                        })))
                        .then(literal("reset").executes(ctx -> {
                            MaceRegistry.data.maces.clear();
                            MaceRegistry.save();
                            msg(ctx, "§aAll tracked maces cleared");
                            return 1;
                        }))));
        LOGGER.info("macelimit loaded");
    }

    private static int status(CommandContext<ServerCommandSource> ctx) {
        int total = MaceRegistry.data.maces.size();
        int counted = MaceRegistry.counted();
        msg(ctx, "§eCrafted maces counting: §f" + counted + "/" + MaceRegistry.data.limit + " §7(" + total + " maces tracked in total)"
                + (MaceRegistry.full() ? " §cCrafting blocked" : " §aCrafting open"));
        return 1;
    }

    private static int list(CommandContext<ServerCommandSource> ctx) {
        if (MaceRegistry.data.maces.isEmpty()) {
            msg(ctx, "§7No tracked maces");
            return 1;
        }
        long now = System.currentTimeMillis();
        for (Map.Entry<String, MaceRegistry.Entry> e : MaceRegistry.data.maces.entrySet()) {
            MaceRegistry.Entry m = e.getValue();
            String tag = m.pendingDelete ? "§c[deleting]" : m.found ? "§8[not crafted]" : m.op ? "§8[op]" : "§a[counts]";
            String place = m.dimension == null || m.dimension.isEmpty() ? "" : " §7at §f" + m.x + " " + m.y + " " + m.z + " §7" + m.dimension;
            long mins = m.seen <= 0 ? -1 : (now - m.seen) / 60000;
            msg(ctx, "§f" + e.getKey() + " " + tag + " §7" + m.where + place + (mins >= 0 ? " §8(" + mins + "m ago)" : ""));
        }
        return 1;
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
            PlayerConfigEntry entry = new PlayerConfigEntry(p.getGameProfile());
            if (s.isHost(entry)) return true;
            return s.getPlayerManager().isOperator(entry);
        } catch (Throwable t) {
            return true;
        }
    }
}
