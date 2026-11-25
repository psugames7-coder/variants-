package com.variantsmp;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.DefaultAttributeRegistry;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.effect.StatusEffectInstance;
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
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class Stages {

    private Stages() {}

    private static final String OWNER = "P4373";
    private static final int COUNT = 8;
    private static final long VAULT_COOLDOWN_MS = 2L * 60L * 60L * 1000L;

    private static final RegistryKey<Enchantment> FORTUNE = RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.ofVanilla("fortune"));
    private static final RegistryKey<Enchantment> LOOTING = RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.ofVanilla("looting"));
    private static final RegistryKey<DamageType> MACE_SMASH = RegistryKey.of(RegistryKeys.DAMAGE_TYPE, Identifier.ofVanilla("mace_smash"));
    private static final TagKey<DamageType> IS_FALL = TagKey.of(RegistryKeys.DAMAGE_TYPE, Identifier.ofVanilla("is_fall"));
    private static final TagKey<DamageType> IS_EXPLOSION = TagKey.of(RegistryKeys.DAMAGE_TYPE, Identifier.ofVanilla("is_explosion"));
    private static final TagKey<DamageType> BYPASS = TagKey.of(RegistryKeys.DAMAGE_TYPE, Identifier.ofVanilla("bypasses_invulnerability"));

    public record Data(int stages, long vaultReadyAt) {
        public static final Codec<Data> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("s").forGetter(Data::stages),
                Codec.LONG.fieldOf("v").forGetter(Data::vaultReadyAt)
        ).apply(i, Data::new));
    }

    public static final AttachmentType<Data> DATA = AttachmentRegistry.create(Identifier.of("variantsmp", "pd"),
            b -> b.persistent(Data.CODEC).initializer(() -> new Data(0, 0L)).copyOnDeath());

    private static ServerPlayerEntity breaker;
    private static boolean potionBoost;
    private static int guardUntil = -1;
    private static int guardReadyAt = 0;

    static void init() {
    }

    public static boolean potionBoost() {
        return potionBoost;
    }

    public static void potionBoost(boolean value) {
        potionBoost = value;
    }

    public static boolean isOwner(Entity e) {
        return e instanceof ServerPlayerEntity p && OWNER.equalsIgnoreCase(p.getGameProfile().name());
    }

    private static Data data(ServerPlayerEntity p) {
        return p.getAttachedOrCreate(DATA);
    }

    public static boolean on(Entity e, int stage) {
        if (!isOwner(e)) return false;
        return (data((ServerPlayerEntity) e).stages() & (1 << (stage - 1))) != 0;
    }

    private static void tell(ServerPlayerEntity p, Text text) {
        p.sendMessage(text, false);
    }

    private static void tell(ServerPlayerEntity p, String msg) {
        tell(p, Text.literal(msg).formatted(Formatting.GRAY));
    }

    public static boolean command(ServerPlayerEntity p, String raw) {
        if (p == null || raw == null || !isOwner(p)) return false;
        String[] a = raw.trim().split("\\s+");
        if (a.length == 0) return false;
        String c = a[0].toLowerCase(Locale.ROOT);
        if (c.equals("attreset") && a.length == 1) {
            resetAttributes(p);
            tell(p, "Attributes reset");
            return true;
        }
        if (!c.equals("stage")) return false;
        if (a.length < 2 || a.length > 3) {
            tell(p, "/stage <1-" + COUNT + "> [on|off]");
            return true;
        }
        int n;
        try {
            n = Integer.parseInt(a[1]);
        } catch (NumberFormatException e) {
            n = -1;
        }
        if (n < 1 || n > COUNT) {
            tell(p, "/stage <1-" + COUNT + "> [on|off]");
            return true;
        }
        int mask = 1 << (n - 1);
        Data d = data(p);
        boolean current = (d.stages() & mask) != 0;
        if (a.length == 2) {
            tell(p, "Stage " + n + ": " + (current ? "on" : "off"));
            return true;
        }
        String v = a[2].toLowerCase(Locale.ROOT);
        boolean want;
        if (v.equals("on")) want = true;
        else if (v.equals("off")) want = false;
        else {
            tell(p, "/stage <1-" + COUNT + "> [on|off]");
            return true;
        }
        int bits = want ? d.stages() | mask : d.stages() & ~mask;
        p.setAttached(DATA, new Data(bits, d.vaultReadyAt()));
        tell(p, "Stage " + n + ": " + (want ? "on" : "off"));
        if (n == 1 && want && !current) sendSeed(p);
        return true;
    }

    private static void sendSeed(ServerPlayerEntity p) {
        MinecraftServer server = ((ServerWorld) p.getEntityWorld()).getServer();
        if (server == null) return;
        String seed = String.valueOf(server.getOverworld().getSeed());
        tell(p, Text.literal("Seed: ").formatted(Formatting.GRAY)
                .append(Text.literal(seed).styled(s -> s.withColor(Formatting.GREEN).withInsertion(seed))));
    }

    private static void resetAttributes(ServerPlayerEntity p) {
        DefaultAttributeContainer defaults = DefaultAttributeRegistry.get(EntityType.PLAYER);
        Registries.ATTRIBUTE.streamEntries().forEach(entry -> {
            EntityAttributeInstance instance = p.getAttributeInstance(entry);
            if (instance == null) return;
            RegistryEntry<EntityAttribute> attribute = entry;
            double base = defaults.has(attribute) ? defaults.getBaseValue(attribute) : attribute.value().getDefaultValue();
            instance.setBaseValue(base);
            List<EntityAttributeModifier> persistent = new ArrayList<>(instance.getPersistentModifiers());
            for (EntityAttributeModifier modifier : persistent) {
                if (!"minecraft".equals(modifier.id().getNamespace())) instance.removeModifier(modifier.id());
            }
        });
        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
    }

    public static float damage(LivingEntity target, DamageSource source, float amount) {
        try {
            float out = amount;
            if (on(target, 2) && (source.isIn(IS_FALL) || source.isIn(IS_EXPLOSION))) out *= 0.8F;
            if (source.getAttacker() instanceof ServerPlayerEntity p && source.getSource() == p && target != p && on(p, 7)
                    && (source.isOf(MACE_SMASH) || p.getMainHandStack().isOf(Items.MACE))) {
                out *= 1.25F;
            }
            return out;
        } catch (Throwable t) {
            return amount;
        }
    }

    public static float armorWear(LivingEntity target, DamageSource source, float amount) {
        try {
            Entity attacker = source.getAttacker();
            if (attacker != target && on(attacker, 4)) return amount * 1.5F;
        } catch (Throwable ignored) {
        }
        return amount;
    }

    public static void beginBreak(Entity player) {
        breaker = on(player, 3) ? (ServerPlayerEntity) player : null;
    }

    public static void endBreak() {
        breaker = null;
    }

    public static int gainXp(PlayerEntity player, int xp) {
        try {
            if (xp > 0 && on(player, 3)) return xp * 2;
        } catch (Throwable ignored) {
        }
        return xp;
    }

    public static int fortune(RegistryEntry<Enchantment> enchantment, int level) {
        if (breaker == null || level < 1 || level >= 3) return level;
        return enchantment.matchesKey(FORTUNE) ? 3 : level;
    }

    public static int looting(RegistryEntry<Enchantment> enchantment, LivingEntity entity, int level) {
        if (level < 1 || level >= 3) return level;
        try {
            if (enchantment.matchesKey(LOOTING) && on(entity, 3)) return 3;
        } catch (Throwable ignored) {
        }
        return level;
    }

    public static StatusEffectInstance potion(LivingEntity self, StatusEffectInstance effect) {
        try {
            if (!potionBoost || effect == null || effect.isInfinite() || !on(self, 5)) return effect;
            return new StatusEffectInstance(effect.getEffectType(), effect.getDuration() * 2, effect.getAmplifier(),
                    effect.isAmbient(), effect.shouldShowParticles(), effect.shouldShowIcon());
        } catch (Throwable t) {
            return effect;
        }
    }

    public static boolean vaultReady(ServerPlayerEntity p) {
        return on(p, 6) && System.currentTimeMillis() >= data(p).vaultReadyAt();
    }

    public static List<ItemStack> vaultLoot(List<ItemStack> original, PlayerEntity player, ItemStack key) {
        try {
            if (original == null || original.isEmpty() || key == null || !key.isOf(Items.OMINOUS_TRIAL_KEY)) return original;
            if (!(player instanceof ServerPlayerEntity p) || !vaultReady(p)) return original;
            vaultUsed(p);
            for (ItemStack s : original) {
                if (s.isOf(Items.HEAVY_CORE)) return original;
            }
            List<ItemStack> out = new ArrayList<>(original);
            out.set(p.getRandom().nextInt(out.size()), new ItemStack(Items.HEAVY_CORE));
            return out;
        } catch (Throwable t) {
            return original;
        }
    }

    static void vaultUsed(ServerPlayerEntity p) {
        Data d = data(p);
        p.setAttached(DATA, new Data(d.stages(), System.currentTimeMillis() + VAULT_COOLDOWN_MS));
    }

    public static boolean cheatDeath(LivingEntity self, DamageSource source) {
        try {
            if (!on(self, 8) || source.isIn(BYPASS)) return false;
            int now = VariantSMP.get().now();
            if (now < guardUntil) {
                self.setHealth(1.0F);
                return true;
            }
            if (now < guardReadyAt) return false;
            guardUntil = now + 100;
            guardReadyAt = guardUntil + 1200;
            self.setHealth(1.0F);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}
