package com.variantsmp.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.variantsmp.Stages;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentLevelMixin {

    @ModifyReturnValue(method = "getLevel(Lnet/minecraft/registry/entry/RegistryEntry;Lnet/minecraft/item/ItemStack;)I", at = @At("RETURN"))
    private static int variantsmp$getLevel(int original, RegistryEntry<Enchantment> enchantment, ItemStack stack) {
        return Stages.fortune(enchantment, original);
    }

    @ModifyReturnValue(method = "getEquipmentLevel(Lnet/minecraft/registry/entry/RegistryEntry;Lnet/minecraft/entity/LivingEntity;)I", at = @At("RETURN"))
    private static int variantsmp$getEquipmentLevel(int original, RegistryEntry<Enchantment> enchantment, LivingEntity entity) {
        return Stages.looting(enchantment, entity, original);
    }
}
