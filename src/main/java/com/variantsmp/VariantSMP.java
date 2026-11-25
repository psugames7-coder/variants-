package com.variantsmp;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class VariantSMP implements ModInitializer {

    public static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("variantSMP");

    private static VariantSMP instance;

    public ModConfig config;
    public PlayerDataStore data;
    public MinecraftServer server;
    private PearlCatch pearlCatch;
    private final Map<UUID, PlayerState> states = new HashMap<>();

    public static VariantSMP get() {
        return instance;
    }

    public PearlCatch pearlCatch() {
        return pearlCatch;
    }

    @Override
    public void onInitialize() {
        instance = this;
        Stages.init();
        Path dir = FabricLoader.getInstance().getConfigDir();
        config = ModConfig.load(dir.resolve("variantsmp.json"));
        data = new PlayerDataStore(dir.resolve("variantsmp_players.json"));

        ServerLifecycleEvents.SERVER_STARTED.register(s -> {
            server = s;
            CombatLog.stopping = false;
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(s -> {
            CombatLog.stopping = true;
            data.save();
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(s -> {
            server = null;
            states.clear();
            CombatLog.clearAll();
            Hud.clearAll();
            Flight.clearCache();
            data.save();
        });
        ServerTickEvents.END_SERVER_TICK.register(this::tick);
        ServerLivingEntityEvents.AFTER_DEATH.register(this::onDeath);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, s) -> data.get(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, s) -> {
            try {
                CombatLog.onDisconnect(handler.player, now());
            } catch (Throwable t) {
                LOGGER.warn("variantSMP: combat log failed", t);
            }
            Hud.remove(handler.player.getUuid());
            data.saveIfDirty();
        });

        pearlCatch = new PearlCatch(this);
        pearlCatch.register();

        Commands.register(this);
        LOGGER.info("variantSMP loaded");
    }

    public int now() {
        return server == null ? 0 : server.getTicks();
    }

    public void creditKill(ServerPlayerEntity killer) {
        Flight.onKill(killer);
        MaceAbility.onKill(killer, state(killer));
    }

    public PlayerState state(ServerPlayerEntity player) {
        return states.computeIfAbsent(player.getUuid(), k -> new PlayerState());
    }

    public PlayerState peek(ServerPlayerEntity player) {
        return states.get(player.getUuid());
    }

    public void resetState(UUID id) {
        PlayerState s = states.get(id);
        if (s == null) return;
        s.resetMace();
        ServerPlayerEntity online = server == null ? null : server.getPlayerManager().getPlayer(id);
        if (online != null) MaceAbility.clearItemCooldown(online);
        s.resetSpear();
        CombatLimits.reset(online, s);
    }

    private void tick(MinecraftServer s) {
        int now = s.getTicks();
        for (ServerPlayerEntity player : s.getPlayerManager().getPlayerList()) {
            try {
                Flight.tick(player, now);
                PlayerState st = state(player);
                boolean combat = CombatLog.inCombat(player, now);
                if (!combat) MaceAbility.outOfCombat(player, st, st.wasInCombat);
                if (st.wasInCombat && !combat) st.resetProgress();
                st.wasInCombat = combat;
                CombatLimits.tick(player, st, now, combat);
                MaceAbility.tick(player, st, now);
                SpearAbility.tick(player, st, now);
                st.macePendingLevel = -1;
                st.maceHit = false;
                st.maceSmash = false;
                st.maceLoadTarget = false;
            } catch (Throwable t) {
                LOGGER.warn("variantSMP: tick failed for {}", player.getGameProfile().name(), t);
            }
            try {
                Hud.tick(player, now);
            } catch (Throwable t) {
                LOGGER.warn("variantSMP: hud failed for {}", player.getGameProfile().name(), t);
            }
        }
        if (now % 1200 == 0) data.saveIfDirty();
    }

    private void onDeath(LivingEntity entity, DamageSource source) {
        if (!(entity instanceof ServerPlayerEntity player)) return;
        try {
            ServerPlayerEntity killer = null;
            if (source.getAttacker() instanceof ServerPlayerEntity p) killer = p;
            else if (player.getAttackingPlayer() instanceof ServerPlayerEntity p) killer = p;
            if (killer != null && killer != player) creditKill(killer);

            CombatLog.clear(player.getUuid());
            resetState(player.getUuid());
            PlayerState st = state(player);
            st.resetProgress();
            st.wasInCombat = false;
            Flight.resetOnDeath(player);
        } catch (Throwable t) {
            LOGGER.warn("variantSMP: death handling failed", t);
        }
    }
}
