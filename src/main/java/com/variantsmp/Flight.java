package com.variantsmp;

import com.variantsmp.mixin.LivingEntityInvoker;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public final class Flight {

    private Flight() {}

    private static final Identifier SKYS_THE_LIMIT = Identifier.ofVanilla("end/elytra");
    private static AdvancementEntry skysTheLimit;
    private static boolean advancementWarned;

    static boolean enabled() {
        ModConfig c = VariantSMP.get().config;
        return c.pvpFeatures && c.flightEnabled;
    }

    private static boolean drains(ServerPlayerEntity player, int now) {
        return VariantSMP.get().config.flightDrainOutsideCombat || CombatLog.inCombat(player, now);
    }

    public static boolean canStartGliding(ServerPlayerEntity player) {
        try {
            VariantSMP mod = VariantSMP.get();
            if (mod == null || !enabled()) return true;
            if (player.isCreative() || player.isSpectator()) return true;
            int now = mod.now();
            if (!drains(player, now)) return true;
            PlayerDataStore.Entry d = mod.data.get(player);
            return !d.flightLocked && d.flightTicks > 0;
        } catch (Throwable t) {
            VariantSMP.LOGGER.warn("variantSMP: glide check failed", t);
            return true;
        }
    }

    static void tick(ServerPlayerEntity player, int now) {
        VariantSMP mod = VariantSMP.get();
        if (!enabled() || !player.isGliding() || !drains(player, now)) return;
        if (player.isCreative() || player.isSpectator()) return;
        PlayerDataStore.Entry d = mod.data.get(player);
        if (d.flightLocked || d.flightTicks <= 0) {
            stopGliding(player);
            return;
        }
        d.flightTicks--;
        mod.data.markDirty();
        if (d.flightTicks <= 0) {
            d.flightTicks = 0;
            d.flightLocked = true;
            stopGliding(player);
            Abilities.bar(player, "Out of flight", Formatting.RED);
        }
    }

    private static void stopGliding(ServerPlayerEntity player) {
        try {
            ((LivingEntityInvoker) player).variantsmp$stopGliding();
        } catch (Throwable t) {
            VariantSMP.LOGGER.warn("variantSMP: could not stop gliding", t);
        }
    }

    static void add(ServerPlayerEntity player, int ticks) {
        VariantSMP mod = VariantSMP.get();
        if (!enabled() || ticks <= 0) return;
        PlayerDataStore.Entry d = mod.data.get(player);
        int max = Math.max(0, mod.config.flightMaxSeconds) * 20;
        d.flightTicks = Math.min(max, d.flightTicks + ticks);
        if (d.flightLocked && d.flightTicks >= mod.config.flightUnlockSeconds * 20) {
            d.flightLocked = false;
            Abilities.bar(player, "Flight unlocked", Formatting.GREEN);
        }
        mod.data.markDirty();
    }

    static void onDamageDealt(ServerPlayerEntity attacker, float dealt) {
        VariantSMP mod = VariantSMP.get();
        if (!enabled() || dealt <= 0) return;
        double step = Math.max(0.5, mod.config.flightDamageHearts * 2.0);
        PlayerDataStore.Entry d = mod.data.get(attacker);
        d.damageBank += dealt;
        int rewards = 0;
        while (d.damageBank >= step) {
            d.damageBank -= step;
            rewards++;
        }
        mod.data.markDirty();
        if (rewards > 0) add(attacker, (int) Math.round(rewards * Math.max(0.0, mod.config.flightPerDamageSeconds) * 20.0));
    }

    static void onKill(ServerPlayerEntity killer) {
        add(killer, Math.max(0, VariantSMP.get().config.flightPerKillSeconds) * 20);
    }

    static String secs(int ticks) {
        return Abilities.fmt(Math.round(Math.max(0, ticks) / 2.0) / 10.0);
    }

    static void resetOnDeath(ServerPlayerEntity player) {
        VariantSMP mod = VariantSMP.get();
        PlayerDataStore.Entry d = mod.data.get(player);
        d.flightTicks = Math.max(0, mod.config.flightStartSeconds) * 20;
        d.flightLocked = d.flightTicks <= 0;
        d.damageBank = 0;
        mod.data.markDirty();
    }

    static boolean hasElytraAdvancement(ServerPlayerEntity player) {
        try {
            if (skysTheLimit == null) {
                MinecraftServer server = VariantSMP.get().server;
                if (server == null) return false;
                skysTheLimit = server.getAdvancementLoader().get(SKYS_THE_LIMIT);
                if (skysTheLimit == null) return false;
            }
            return player.getAdvancementTracker().getProgress(skysTheLimit).isDone();
        } catch (Throwable t) {
            if (!advancementWarned) {
                advancementWarned = true;
                VariantSMP.LOGGER.warn("variantSMP: could not check Sky's the Limit, flight HUD shown to everyone", t);
            }
            return true;
        }
    }

    static void clearCache() {
        skysTheLimit = null;
    }

    static String hud(ServerPlayerEntity player) {
        VariantSMP mod = VariantSMP.get();
        PlayerDataStore.Entry d = mod.data.get(player);
        if (d.flightLocked) return "§8✈ " + secs(d.flightTicks) + "/" + mod.config.flightUnlockSeconds + "s";
        return "§b✈ " + secs(d.flightTicks) + "s";
    }
}
