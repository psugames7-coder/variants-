package com.variantstrims.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.variantstrims.TrimEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LivingEntity.class)
public abstract class DamageMixin {

    @WrapMethod(method = "damage(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/damage/DamageSource;F)Z")
    private boolean variantstrims$damage(ServerWorld world, DamageSource source, float amount, Operation<Boolean> original) {
        LivingEntity self = (LivingEntity) (Object) this;
        float scaled = TrimEffects.damage(self, source, amount);
        if (Float.isNaN(scaled)) return false;
        boolean hit = original.call(world, source, scaled);
        if (hit) TrimEffects.afterHit(self, source);
        return hit;
    }
}
