package com.variantstrims.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.variantstrims.TrimEffects;
import net.minecraft.entity.projectile.thrown.SplashPotionEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.HitResult;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(SplashPotionEntity.class)
public abstract class SplashPotionMixin {

    @WrapMethod(method = "spawnAreaEffectCloud(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/item/ItemStack;Lnet/minecraft/util/hit/HitResult;)V")
    private void variantstrims$splash(ServerWorld world, ItemStack stack, HitResult hit, Operation<Void> original) {
        boolean previous = TrimEffects.potionBoost();
        TrimEffects.potionBoost(true);
        try {
            original.call(world, stack, hit);
        } finally {
            TrimEffects.potionBoost(previous);
        }
    }
}
