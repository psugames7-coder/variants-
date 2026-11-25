package com.variantstrims;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class TrimEffects {

    private TrimEffects() {}

    private static final RegistryKey<DamageType> MACE_SMASH = RegistryKey.of(RegistryKeys.DAMAGE_TYPE, Identifier.ofVanilla("mace_smash"));
    private static final RegistryKey<DamageType> TRIDENT = RegistryKey.of(RegistryKeys.DAMAGE_TYPE, Identifier.ofVanilla("trident"));
    private static final RegistryKey<DamageType> ENDER_PEARL = RegistryKey.of(RegistryKeys.DAMAGE_TYPE, Identifier.ofVanilla("ender_pearl"));
    private static final RegistryKey<Enchantment> CHANNELING = RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.ofVanilla("channeling"));
    private static final TagKey<DamageType> IS_FALL = TagKey.of(RegistryKeys.DAMAGE_TYPE, Identifier.ofVanilla("is_fall"));

    private static final int PASSIVE_DURATION = 340;
    private static final int PASSIVE_REFRESH = 240;
    private static final int RAISER_DURATION = 72000;
    private static final int RAISER_MAX = 3;
    private static final int SHAPER_DURATION = 1800;

    private static final List<RegistryEntry<StatusEffect>> GOOD = List.of(
            StatusEffects.SPEED, StatusEffects.HASTE, StatusEffects.STRENGTH, StatusEffects.JUMP_BOOST,
            StatusEffects.REGENERATION, StatusEffects.RESISTANCE, StatusEffects.FIRE_RESISTANCE,
            StatusEffects.WATER_BREATHING, StatusEffects.NIGHT_VISION, StatusEffects.HEALTH_BOOST,
            StatusEffects.ABSORPTION, StatusEffects.LUCK, StatusEffects.SLOW_FALLING,
            StatusEffects.DOLPHINS_GRACE, StatusEffects.CONDUIT_POWER, StatusEffects.HERO_OF_THE_VILLAGE);

    private static final Map<UUID, Map<RegistryEntry<StatusEffect>, Integer>> granted = new HashMap<>();
    private static final Map<UUID, Integer> raiser = new HashMap<>();
    private static boolean potionBoost;

    public static boolean potionBoost() {
        return potionBoost;
    }

    public static void potionBoost(boolean value) {
        potionBoost = value;
    }

    private static int tier(Entity e, Trim t) {
        return TrimState.tier(e, t);
    }

    static void forget(UUID id) {
        granted.remove(id);
        raiser.remove(id);
    }

    static void clear() {
        granted.clear();
        raiser.clear();
    }

    private static void want(Map<RegistryEntry<StatusEffect>, Integer> map, RegistryEntry<StatusEffect> effect, int amplifier) {
        map.merge(effect, amplifier, Math::max);
    }

    static void passives(ServerPlayerEntity p) {
        EnumMap<Trim, Integer> t = TrimState.get(p);
        Map<RegistryEntry<StatusEffect>, Integer> desired = new LinkedHashMap<>();
        int dune = t.getOrDefault(Trim.DUNE, 0);
        if (dune > 0) want(desired, StatusEffects.SPEED, dune - 1);
        int coast = t.getOrDefault(Trim.COAST, 0);
        if (coast >= 1) want(desired, StatusEffects.DOLPHINS_GRACE, 0);
        if (coast >= 2) want(desired, StatusEffects.CONDUIT_POWER, 0);
        int ward = t.getOrDefault(Trim.WARD, 0);
        if (ward > 0) want(desired, StatusEffects.REGENERATION, ward - 1);
        if (t.getOrDefault(Trim.SILENCE, 0) >= 2) want(desired, StatusEffects.RESISTANCE, 0);
        if (t.getOrDefault(Trim.EYE, 0) >= 1) want(desired, StatusEffects.NIGHT_VISION, 0);
        if (t.getOrDefault(Trim.VEX, 0) >= 1) want(desired, StatusEffects.HERO_OF_THE_VILLAGE, 0);
        int tide = t.getOrDefault(Trim.TIDE, 0);
        if (tide >= 1) want(desired, StatusEffects.WATER_BREATHING, 0);
        if (tide >= 2) want(desired, StatusEffects.HASTE, 1);
        if (t.getOrDefault(Trim.RIB, 0) >= 1) want(desired, StatusEffects.FIRE_RESISTANCE, 0);
        if (t.getOrDefault(Trim.BOLT, 0) >= 2) want(desired, StatusEffects.JUMP_BOOST, 0);
        if (t.getOrDefault(Trim.WAYFINDER, 0) >= 1) want(desired, StatusEffects.LUCK, 0);
        if (t.getOrDefault(Trim.SHAPER, 0) >= 1) want(desired, StatusEffects.STRENGTH, 0);

        Map<RegistryEntry<StatusEffect>, Integer> before = granted.getOrDefault(p.getUuid(), Map.of());
        for (Map.Entry<RegistryEntry<StatusEffect>, Integer> e : before.entrySet()) {
            if (desired.containsKey(e.getKey())) continue;
            StatusEffectInstance cur = p.getStatusEffect(e.getKey());
            if (cur != null && cur.isAmbient() && !cur.isInfinite() && cur.getAmplifier() == e.getValue()
                    && cur.getDuration() <= PASSIVE_DURATION) {
                p.removeStatusEffect(e.getKey());
            }
        }
        for (Map.Entry<RegistryEntry<StatusEffect>, Integer> e : desired.entrySet()) {
            StatusEffectInstance cur = p.getStatusEffect(e.getKey());
            int amp = e.getValue();
            boolean apply = cur == null || cur.getAmplifier() < amp
                    || (cur.getAmplifier() == amp && !cur.isInfinite() && cur.getDuration() < PASSIVE_REFRESH);
            if (apply) p.addStatusEffect(new StatusEffectInstance(e.getKey(), PASSIVE_DURATION, amp, true, false, true));
        }
        if (desired.isEmpty()) granted.remove(p.getUuid());
        else granted.put(p.getUuid(), desired);

        if (t.getOrDefault(Trim.SILENCE, 0) >= 1 && p.hasStatusEffect(StatusEffects.DARKNESS)) p.removeStatusEffect(StatusEffects.DARKNESS);
        if (t.getOrDefault(Trim.SNOUT, 0) >= 1 && p.hasStatusEffect(StatusEffects.WITHER)) p.removeStatusEffect(StatusEffects.WITHER);
    }

    static void sentry(MinecraftServer server) {
        List<ServerPlayerEntity> players = server.getPlayerManager().getPlayerList();
        for (ServerPlayerEntity watcher : players) {
            if (tier(watcher, Trim.SENTRY) < 2) continue;
            for (ServerPlayerEntity other : players) {
                if (other == watcher || other.getEntityWorld() != watcher.getEntityWorld()) continue;
                if (!other.hasStatusEffect(StatusEffects.INVISIBILITY)) continue;
                StatusEffectInstance cur = other.getStatusEffect(StatusEffects.GLOWING);
                if (cur == null || (!cur.isInfinite() && cur.getDuration() < 25)) {
                    other.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 30, 0, false, false, true));
                }
            }
        }
    }

    public static boolean blocks(LivingEntity self, StatusEffectInstance effect) {
        if (effect == null) return false;
        if (effect.getEffectType().equals(StatusEffects.DARKNESS) && tier(self, Trim.SILENCE) >= 1) return true;
        return effect.getEffectType().equals(StatusEffects.WITHER) && tier(self, Trim.SNOUT) >= 1;
    }

    private static boolean isMace(DamageSource s) {
        if (s.isOf(MACE_SMASH)) return true;
        return s.getAttacker() instanceof LivingEntity a && s.getSource() == a && a.getMainHandStack().isOf(Items.MACE);
    }

    private static boolean isSpear(DamageSource s) {
        ItemStack w = s.getWeaponStack();
        if (w == null || w.isEmpty()) {
            if (s.getAttacker() instanceof LivingEntity a && s.getSource() == a) w = a.getMainHandStack();
            else return false;
        }
        return !w.isEmpty() && Registries.ITEM.getId(w.getItem()).getPath().endsWith("_spear");
    }

    private static boolean isTrident(DamageSource s) {
        if (s.isOf(TRIDENT)) return true;
        return s.getAttacker() instanceof LivingEntity a && s.getSource() == a && a.getMainHandStack().isOf(Items.TRIDENT);
    }

    private static boolean channeled(LivingEntity target, DamageSource source) {
        if (!source.isOf(TRIDENT)) return false;
        ItemStack trident = source.getWeaponStack();
        if (trident == null || trident.isEmpty()) return false;
        RegistryEntry<Enchantment> channeling = target.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT)
                .getOptional(CHANNELING).orElse(null);
        if (channeling == null || EnchantmentHelper.getLevel(channeling, trident) <= 0) return false;
        World world = target.getEntityWorld();
        return world.isThundering() && world.isSkyVisible(target.getBlockPos());
    }

    public static float damage(LivingEntity target, DamageSource source, float amount) {
        try {
            float out = amount;
            if (target instanceof ServerPlayerEntity) {
                if (tier(target, Trim.EYE) >= 1 && source.isOf(ENDER_PEARL)) return Float.NaN;
                int wild = tier(target, Trim.WILD);
                if (wild > 0 && source.isIn(IS_FALL)) out *= wild >= 2 ? 0.7F : 0.9F;
                if (tier(target, Trim.SILENCE) >= 1 && source.getAttacker() instanceof WardenEntity) out *= 0.6F;
                int spire = tier(target, Trim.SPIRE);
                if (spire > 0) {
                    if (isMace(source)) out *= spire >= 2 ? 0.6F : 0.8F;
                    else if (spire >= 2 && isSpear(source)) out *= 0.6F;
                }
            }
            if (source.getAttacker() instanceof ServerPlayerEntity p && p != target) {
                if (tier(p, Trim.BOLT) >= 1 && isTrident(source)) out *= channeled(target, source) ? 2.5F : 1.5F;
                if (tier(p, Trim.FLOW) >= 1 && isMace(source)) out *= 1.1F;
                if (tier(p, Trim.TIDE) >= 2 && p.isTouchingWater()) out *= 1.1F;
            }
            return out;
        } catch (Throwable t) {
            return amount;
        }
    }

    public static void afterHit(LivingEntity target, DamageSource source) {
        try {
            if (!(source.getAttacker() instanceof ServerPlayerEntity p) || p == target) return;
            if (target instanceof ServerPlayerEntity victim && tier(p, Trim.SENTRY) >= 1
                    && victim.hasStatusEffect(StatusEffects.INVISIBILITY)) {
                victim.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 200, 0, false, false, true));
            }
            if (source.getSource() != p) return;
            if (tier(p, Trim.SNOUT) >= 2 && p.getRandom().nextFloat() < 0.05F) {
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 40, 0), p);
            }
            if (tier(p, Trim.WAYFINDER) >= 2 && p.getRandom().nextFloat() < 0.05F) {
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 60, 0), p);
            }
        } catch (Throwable ignored) {
        }
    }

    static void onDeath(LivingEntity victim, DamageSource source) {
        try {
            if (victim instanceof ServerPlayerEntity dead) raiser.remove(dead.getUuid());
            if (!(victim instanceof ServerPlayerEntity)) return;
            ServerPlayerEntity killer = null;
            if (source.getAttacker() instanceof ServerPlayerEntity p) killer = p;
            else if (victim.getAttackingPlayer() instanceof ServerPlayerEntity p) killer = p;
            if (killer == null || killer == victim) return;

            if (tier(killer, Trim.FLOW) >= 2) {
                killer.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 400, 1));
                killer.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 2400, 3));
                killer.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 6000, 0));
                killer.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 6000, 0));
            }
            if (tier(killer, Trim.RAISER) >= 2) {
                int level = killer.hasStatusEffect(StatusEffects.STRENGTH) ? raiser.getOrDefault(killer.getUuid(), 0) : 0;
                level = Math.min(RAISER_MAX, level + 1);
                raiser.put(killer.getUuid(), level);
                killer.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, RAISER_DURATION, level - 1));
            }
            if (tier(killer, Trim.SHAPER) >= 2) {
                RegistryEntry<StatusEffect> effect = GOOD.get(killer.getRandom().nextInt(GOOD.size()));
                killer.addStatusEffect(new StatusEffectInstance(effect, SHAPER_DURATION, 0));
            }
        } catch (Throwable ignored) {
        }
    }

    public static int mobXp(LivingEntity self, Entity attacker, int xp) {
        if (xp <= 0 || self instanceof PlayerEntity) return xp;
        return tier(attacker, Trim.RAISER) >= 1 ? (int) Math.round(xp * 1.5) : xp;
    }

    public static float armorWear(LivingEntity target, DamageSource source, float amount) {
        Entity attacker = source.getAttacker();
        if (attacker != target && tier(attacker, Trim.RIB) >= 2) return amount * 1.15F;
        return amount;
    }

    public static int armorDurability(ItemStack stack, ServerPlayerEntity player, int amount) {
        if (player == null || amount <= 0 || tier(player, Trim.EYE) < 2) return amount;
        if (!TrimState.isArmorPiece(player, stack)) return amount;
        int kept = 0;
        for (int i = 0; i < amount; i++) {
            if (player.getRandom().nextFloat() >= 0.2F) kept++;
        }
        return kept;
    }

    public static StatusEffectInstance potion(LivingEntity self, StatusEffectInstance effect) {
        try {
            if (!potionBoost || effect == null || effect.isInfinite()) return effect;
            int host = tier(self, Trim.HOST);
            if (host <= 0) return effect;
            int duration = (int) Math.round(effect.getDuration() * (host >= 2 ? 2.0 : 1.5));
            return new StatusEffectInstance(effect.getEffectType(), duration, effect.getAmplifier(),
                    effect.isAmbient(), effect.shouldShowParticles(), effect.shouldShowIcon());
        } catch (Throwable t) {
            return effect;
        }
    }

    public static boolean vexSave(ServerPlayerEntity p) {
        return tier(p, Trim.VEX) >= 2 && p.getRandom().nextFloat() < 0.5F;
    }

    public static boolean isTotem(ItemStack stack) {
        return !stack.isEmpty() && stack.contains(DataComponentTypes.DEATH_PROTECTION);
    }

    public static void restoreTotem(ServerPlayerEntity p, ItemStack main, ItemStack off) {
        if (isTotem(main) && !ItemStack.areEqual(p.getMainHandStack(), main)) p.setStackInHand(Hand.MAIN_HAND, main);
        else if (isTotem(off) && !ItemStack.areEqual(p.getOffHandStack(), off)) p.setStackInHand(Hand.OFF_HAND, off);
    }
}
