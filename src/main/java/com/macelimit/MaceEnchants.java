package com.macelimit;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.inventory.CraftingResultInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import java.util.Objects;
import java.util.Set;

public final class MaceEnchants {

    private MaceEnchants() {}

    private static final Set<RegistryKey<Enchantment>> ALLOWED = Set.of(
            RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.ofVanilla("unbreaking")),
            RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.ofVanilla("mending")),
            RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.ofVanilla("wind_burst")));
    private static final RegistryKey<Enchantment> UNBREAKING = RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.ofVanilla("unbreaking"));

    private static boolean allowed(RegistryEntry<Enchantment> entry) {
        for (RegistryKey<Enchantment> key : ALLOWED) {
            if (entry.matchesKey(key)) return true;
        }
        return false;
    }

    public static void filterAnvil(Inventory input, CraftingResultInventory output) {
        ItemStack base = input.getStack(0);
        ItemStack result = output.getStack(0);
        if (result.isEmpty() || !result.isOf(Items.MACE)) return;
        ItemEnchantmentsComponent before = base.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);
        ItemEnchantmentsComponent after = result.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);
        ItemEnchantmentsComponent.Builder builder = new ItemEnchantmentsComponent.Builder(ItemEnchantmentsComponent.DEFAULT);
        for (RegistryEntry<Enchantment> entry : after.getEnchantments()) {
            int level = after.getLevel(entry);
            int had = before.getLevel(entry);
            if (allowed(entry)) {
                if (entry.matchesKey(UNBREAKING)) level = Math.min(level, 3);
                builder.set(entry, Math.max(level, had));
            } else if (had > 0) {
                builder.set(entry, had);
            }
        }
        ItemEnchantmentsComponent filtered = builder.build();
        boolean renamed = !Objects.equals(base.get(DataComponentTypes.CUSTOM_NAME), result.get(DataComponentTypes.CUSTOM_NAME));
        boolean repaired = result.getDamage() < base.getDamage();
        if (filtered.equals(before) && !renamed && !repaired) {
            output.setStack(0, ItemStack.EMPTY);
            return;
        }
        if (!filtered.equals(after)) {
            ItemStack copy = result.copy();
            copy.set(DataComponentTypes.ENCHANTMENTS, filtered);
            output.setStack(0, copy);
        }
    }
}
