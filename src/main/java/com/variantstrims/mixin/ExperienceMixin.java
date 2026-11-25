package com.variantstrims.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.variantstrims.TrimEffects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public abstract class ExperienceMixin {

    @ModifyReturnValue(method = "getExperienceToDrop(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/Entity;)I",
            at = @At("RETURN"))
    private int variantstrims$experience(int original, ServerWorld world, Entity attacker) {
        try {
            return TrimEffects.mobXp((LivingEntity) (Object) this, attacker, original);
        } catch (Throwable t) {
            return original;
        }
    }
}
