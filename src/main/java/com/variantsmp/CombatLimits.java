package com.variantsmp;

import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;

public final class CombatLimits {

    private CombatLimits() {}

    public enum Kind {
        GOD_APPLE("God apples"),
        TOTEM("Totems"),
        PEARL("Pearls");

        final String label;

        Kind(String label) {
            this.label = label;
        }

        Item item() {
            return switch (this) {
                case GOD_APPLE -> Items.ENCHANTED_GOLDEN_APPLE;
                case TOTEM -> Items.TOTEM_OF_UNDYING;
                case PEARL -> Items.ENDER_PEARL;
            };
        }

        int limit(ModConfig c) {
            return switch (this) {
                case GOD_APPLE -> c.godAppleCount;
                case TOTEM -> c.totemCount;
                case PEARL -> c.pearlCount;
            };
        }
    }

    private static final Kind[] KINDS = Kind.values();

    private static boolean exempt(ServerPlayerEntity player) {
        return player.isCreative() || player.isSpectator();
    }

    public static void onUse(ServerPlayerEntity player, Kind kind) {
        try {
            VariantSMP mod = VariantSMP.get();
            if (mod == null || !mod.config.combatItemLimits || exempt(player)) return;
            int now = mod.now();
            PlayerState s = mod.state(player);
            int i = kind.ordinal();
            s.limitUsed[i]++;
            if (!CombatLog.inCombat(player, now)) return;
            if (!s.limitLocked[i] && s.limitUsed[i] >= kind.limit(mod.config)) lock(player, s, kind, now);
        } catch (Throwable t) {
            VariantSMP.LOGGER.warn("variantSMP: combat limit count failed", t);
        }
    }

    public static boolean totemBlocked(Entity entity) {
        try {
            if (!(entity instanceof ServerPlayerEntity player)) return false;
            VariantSMP mod = VariantSMP.get();
            if (mod == null || !mod.config.combatItemLimits || exempt(player)) return false;
            int now = mod.now();
            if (!CombatLog.inCombat(player, now)) return false;
            PlayerState s = mod.state(player);
            int i = Kind.TOTEM.ordinal();
            return s.limitLocked[i] || s.limitUsed[i] >= Kind.TOTEM.limit(mod.config);
        } catch (Throwable t) {
            return false;
        }
    }

    static void tick(ServerPlayerEntity player, PlayerState s, int now, boolean combat) {
        ModConfig c = VariantSMP.get().config;
        if (!combat || !c.combatItemLimits) {
            for (Kind kind : KINDS) {
                int i = kind.ordinal();
                if (s.limitLocked[i]) unlock(player, s, kind);
                s.limitUsed[i] = 0;
            }
            return;
        }
        if (exempt(player)) return;
        for (Kind kind : KINDS) {
            int i = kind.ordinal();
            if (!s.limitLocked[i]) {
                if (s.limitUsed[i] >= kind.limit(c)) lock(player, s, kind, now);
                continue;
            }
            int end = now + CombatLog.ticksLeft(player, now);
            if (end > s.limitLockEnd[i]) {
                s.limitLockEnd[i] = end;
                player.getItemCooldownManager().set(new ItemStack(kind.item()), Math.max(1, end - now));
            }
        }
    }

    private static void lock(ServerPlayerEntity player, PlayerState s, Kind kind, int now) {
        int i = kind.ordinal();
        int left = Math.max(1, CombatLog.ticksLeft(player, now));
        s.limitLocked[i] = true;
        s.limitLockEnd[i] = now + left;
        player.getItemCooldownManager().set(new ItemStack(kind.item()), left);
        Abilities.bar(player, kind.label + " disabled until combat ends", Formatting.RED);
    }

    private static void unlock(ServerPlayerEntity player, PlayerState s, Kind kind) {
        int i = kind.ordinal();
        s.limitLocked[i] = false;
        s.limitLockEnd[i] = 0;
        clearCooldown(player, kind);
        Abilities.ready(player, kind.label + " available");
    }

    static void reset(ServerPlayerEntity player, PlayerState s) {
        for (Kind kind : KINDS) {
            int i = kind.ordinal();
            boolean locked = s.limitLocked[i];
            s.limitUsed[i] = 0;
            s.limitLocked[i] = false;
            s.limitLockEnd[i] = 0;
            if (locked && player != null) clearCooldown(player, kind);
        }
    }

    private static void clearCooldown(ServerPlayerEntity player, Kind kind) {
        try {
            ItemStack stack = new ItemStack(kind.item());
            player.getItemCooldownManager().remove(player.getItemCooldownManager().getGroup(stack));
        } catch (Throwable ignored) {
        }
    }
}
