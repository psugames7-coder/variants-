package com.variantsmp;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class Hud {

    private Hud() {}

    private static final int RESEND = 20;

    private static final class View {
        String actionText;
        int actionSent = -1000;
    }

    private static final Map<UUID, View> views = new HashMap<>();

    private static View view(ServerPlayerEntity player) {
        return views.computeIfAbsent(player.getUuid(), k -> new View());
    }

    public static void ability(ServerPlayerEntity player, String message, Formatting color) {
        player.sendMessage(Text.literal(message).formatted(color), false);
    }

    static void tick(ServerPlayerEntity player, int now) {
        ModConfig c = VariantSMP.get().config;
        View v = view(player);

        boolean combat = CombatLog.inCombat(player, now);
        String combatText = null;
        if (combat) {
            int secs = (CombatLog.ticksLeft(player, now) + 19) / 20;
            combatText = "§c⚔ " + secs / 60 + ":" + String.format("%02d", secs % 60);
        }
        boolean flight = Flight.enabled()
                && (combat || (c.flightDrainOutsideCombat && player.isGliding()))
                && Flight.hasElytraAdvancement(player);
        String flightText = flight ? Flight.hud(player) : null;

        String line = cooldowns(player, now);
        if (line == null) {
            List<String> parts = new ArrayList<>();
            if (combatText != null) parts.add(combatText);
            if (flightText != null) parts.add(flightText);
            line = parts.isEmpty() ? null : String.join("   ", parts);
        }

        if (line != null) {
            if (!line.equals(v.actionText) || now - v.actionSent >= RESEND) {
                player.sendMessage(Text.literal(line), true);
                v.actionText = line;
                v.actionSent = now;
            }
        } else if (v.actionText != null) {
            player.sendMessage(Text.empty(), true);
            v.actionText = null;
        }
    }

    private static String cooldowns(ServerPlayerEntity player, int now) {
        PlayerState s = VariantSMP.get().peek(player);
        if (s == null) return null;
        List<String> parts = new ArrayList<>();
        if (now < s.maceShowUntil) add(parts, MaceAbility.cooldownText(s, now));
        if (s.spearAirChain || now < s.spearShowUntil) add(parts, SpearAbility.cooldownText(s, now));
        return parts.isEmpty() ? null : String.join("   ", parts);
    }

    private static void add(List<String> parts, String text) {
        if (text != null) parts.add(text);
    }

    static void remove(UUID id) {
        views.remove(id);
    }

    static void clearAll() {
        views.clear();
    }
}
