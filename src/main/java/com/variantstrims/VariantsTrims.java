package com.variantstrims;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public final class VariantsTrims implements ModInitializer {

    public static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("variantstrims");

    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(this::tick);
        ServerLivingEntityEvents.AFTER_DEATH.register(TrimEffects::onDeath);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            TrimState.remove(handler.player.getUuid());
            TrimEffects.forget(handler.player.getUuid());
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            TrimState.clear();
            TrimEffects.clear();
        });
        LOGGER.info("variantstrims loaded");
    }

    private void tick(MinecraftServer server) {
        boolean slow = server.getTicks() % 20 == 0;
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            try {
                TrimState.update(player);
                if (slow) TrimEffects.passives(player);
            } catch (Throwable t) {
                LOGGER.warn("variantstrims: tick failed for {}", player.getGameProfile().name(), t);
            }
        }
        if (slow) {
            try {
                TrimEffects.sentry(server);
            } catch (Throwable t) {
                LOGGER.warn("variantstrims: sentry failed", t);
            }
        }
    }
}
