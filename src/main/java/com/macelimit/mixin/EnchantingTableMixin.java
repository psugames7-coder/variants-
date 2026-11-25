package com.macelimit.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.screen.EnchantmentScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(EnchantmentScreenHandler.class)
public abstract class EnchantingTableMixin {

    @ModifyReturnValue(method = "generateEnchantments", at = @At("RETURN"))
    private List<EnchantmentLevelEntry> macelimit$generateEnchantments(List<EnchantmentLevelEntry> original,
                                                                     DynamicRegistryManager registries, ItemStack stack,
                                                                     int slot, int level) {
        return stack.isOf(Items.MACE) ? List.of() : original;
    }
}
