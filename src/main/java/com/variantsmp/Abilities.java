package com.variantsmp;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;

public final class Abilities {
    private Abilities() {}

    public static int sweepDepth;

    public static final int SHOW_TICKS = 40;

    private static ItemStack weapon(DamageSource source) {
        ItemStack weapon = source.getWeaponStack();
        return weapon == null ? ItemStack.EMPTY : weapon;
    }

    public static float beforeDamage(LivingEntity target, DamageSource source, float amount) {
        try {
            if (sweepDepth > 0) return amount;
            if (!(source.getAttacker() instanceof ServerPlayerEntity player) || source.getSource() != player) return amount;
            VariantSMP mod = VariantSMP.get();
            return MaceAbility.beforeDamage(player, mod.state(player), target, source, weapon(source), amount, mod.now());
        } catch (Throwable t) {
            VariantSMP.LOGGER.warn("variantSMP: beforeDamage failed", t);
            return amount;
        }
    }

    public static void afterDamage(LivingEntity target, DamageSource source, boolean hit, float dealt) {
        try {
            VariantSMP mod = VariantSMP.get();
            int now = mod.now();
            Entity attackerEntity = source.getAttacker();

            if (target instanceof ServerPlayerEntity victim
                    && attackerEntity instanceof ServerPlayerEntity attacker && attacker != victim) {
                CombatLog.onPvpHit(victim, attacker, now);
                if (hit) Flight.onDamageDealt(attacker, dealt);
            }

            if (sweepDepth > 0) return;
            if (!(attackerEntity instanceof ServerPlayerEntity player) || source.getSource() != player) return;
            MaceAbility.afterDamage(player, mod.state(player), hit, now);
        } catch (Throwable t) {
            VariantSMP.LOGGER.warn("variantSMP: afterDamage failed", t);
        }
    }

    public static RegistryEntry<Enchantment> enchant(Entity entity, RegistryKey<Enchantment> key) {
        return entity.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT).getOptional(key).orElse(null);
    }

    public static void bar(ServerPlayerEntity player, String message, Formatting color) {
        Hud.ability(player, message, color);
    }

    public static void ready(ServerPlayerEntity player, String message) {
        Hud.ability(player, message, Formatting.GREEN);
    }

    public static int secondsLeft(int until, int now) {
        return Math.max(0, (until - now + 19) / 20);
    }

    public static String fmt(double value) {
        if (value == Math.rint(value)) return String.valueOf((long) value);
        return String.valueOf(Math.round(value * 100.0) / 100.0);
    }
}
