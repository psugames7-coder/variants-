package com.variantstrims.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.variantstrims.TrimEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Item.class)
public abstract class PotionDrinkMixin {

    @WrapMethod(method = "finishUsing(Lnet/minecraft/item/ItemStack;Lnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;)Lnet/minecraft/item/ItemStack;")
    private ItemStack variantstrims$finishUsing(ItemStack stack, World world, LivingEntity user, Operation<ItemStack> original) {
        boolean previous = TrimEffects.potionBoost();
        if (stack.isOf(Items.POTION)) TrimEffects.potionBoost(true);
        try {
            return original.call(stack, world, user);
        } finally {
            TrimEffects.potionBoost(previous);
        }
    }
}
