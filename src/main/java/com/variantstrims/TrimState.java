package com.variantstrims;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.equipment.trim.ArmorTrim;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class TrimState {

    private TrimState() {}

    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final int MAX_WEAK = 2;
    private static final int WARMUP = 600;

    private static final class Track {
        int effective;
        int pending;
        int since;
    }

    private static final Map<UUID, EnumMap<Trim, Integer>> active = new HashMap<>();
    private static final Map<UUID, EnumMap<Trim, Track>> tracks = new HashMap<>();

    public static int tier(Entity e, Trim t) {
        if (!(e instanceof ServerPlayerEntity p)) return 0;
        EnumMap<Trim, Integer> m = active.get(p.getUuid());
        if (m == null) return 0;
        Integer v = m.get(t);
        return v == null ? 0 : v;
    }

    public static EnumMap<Trim, Integer> get(ServerPlayerEntity p) {
        EnumMap<Trim, Integer> m = active.get(p.getUuid());
        return m == null ? new EnumMap<>(Trim.class) : m;
    }

    static boolean isArmorPiece(ServerPlayerEntity p, ItemStack stack) {
        for (EquipmentSlot slot : ARMOR) {
            if (p.getEquippedStack(slot) == stack) return true;
        }
        return false;
    }

    static void update(ServerPlayerEntity p) {
        int now = ((ServerWorld) p.getEntityWorld()).getServer().getTicks();
        EnumMap<Trim, Integer> computed = compute(p);
        EnumMap<Trim, Track> tr = tracks.computeIfAbsent(p.getUuid(), k -> new EnumMap<>(Trim.class));
        EnumMap<Trim, Integer> effective = new EnumMap<>(Trim.class);
        for (Trim t : Trim.values()) {
            int c = computed.getOrDefault(t, 0);
            Track k = tr.get(t);
            if (k == null) {
                if (c == 0) continue;
                k = new Track();
                tr.put(t, k);
            }
            if (c < k.effective) k.effective = c;
            if (c > k.effective) {
                if (k.pending != c) {
                    k.pending = c;
                    k.since = now;
                }
                if (now - k.since >= WARMUP) k.effective = c;
            } else {
                k.pending = k.effective;
            }
            if (k.effective > 0) effective.put(t, k.effective);
            else if (c == 0) tr.remove(t);
        }
        active.put(p.getUuid(), effective);
    }

    static void remove(UUID id) {
        active.remove(id);
        tracks.remove(id);
    }

    static void clear() {
        active.clear();
        tracks.clear();
    }

    private static EnumMap<Trim, Integer> compute(ServerPlayerEntity p) {
        EnumMap<Trim, Integer> count = new EnumMap<>(Trim.class);
        EnumMap<Trim, Integer> durability = new EnumMap<>(Trim.class);
        for (EquipmentSlot slot : ARMOR) {
            ItemStack stack = p.getEquippedStack(slot);
            if (stack.isEmpty()) continue;
            ArmorTrim trim = stack.get(DataComponentTypes.TRIM);
            if (trim == null) continue;
            Trim t = trim.pattern().getKey().map(k -> Trim.byId(k.getValue().getPath())).orElse(null);
            if (t == null) continue;
            count.merge(t, 1, Integer::sum);
            int left = stack.isDamageable() ? stack.getMaxDamage() - stack.getDamage() : Integer.MAX_VALUE;
            durability.merge(t, left, Math::min);
        }
        EnumMap<Trim, Integer> result = new EnumMap<>(Trim.class);
        List<Trim> weak = new ArrayList<>();
        for (Map.Entry<Trim, Integer> e : count.entrySet()) {
            Trim t = e.getKey();
            int n = e.getValue();
            if (t.strong) {
                if (n >= 4) result.put(t, 2);
                else if (n >= 2) result.put(t, 1);
            } else {
                weak.add(t);
            }
        }
        weak.sort(Comparator.<Trim>comparingInt(t -> -count.get(t)).thenComparingInt(durability::get));
        for (int i = 0; i < weak.size() && i < MAX_WEAK; i++) {
            Trim t = weak.get(i);
            result.put(t, count.get(t) >= 4 ? 2 : 1);
        }
        return result;
    }
}
