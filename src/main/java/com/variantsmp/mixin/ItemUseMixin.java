package com.variantsmp.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.variantsmp.CombatLimits;
import com.variantsmp.Stages;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Item.class)
public abstract class ItemUseMixin {

    @WrapMethod(method = "finishUsing(Lnet/minecraft/item/ItemStack;Lnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;)Lnet/minecraft/item/ItemStack;")
    private ItemStack variantsmp$finishUsing(ItemStack stack, World world, LivingEntity user, Operation<ItemStack> original) {
        boolean previous = Stages.potionBoost();
        boolean boost = false;
        ServerPlayerEntity godApple = null;
        try {
            boost = stack.isOf(Items.POTION) && Stages.isOwner(user);
            if (stack.isOf(Items.ENCHANTED_GOLDEN_APPLE) && user instanceof ServerPlayerEntity p) godApple = p;
        } catch (Throwable ignored) {
        }
        if (boost) Stages.potionBoost(true);
        ItemStack result;
        try {
            result = original.call(stack, world, user);
        } finally {
            Stages.potionBoost(previous);
        }
        if (godApple != null) CombatLimits.onUse(godApple, CombatLimits.Kind.GOD_APPLE);
        return result;
    }
}
