package com.variantsmp.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.variantsmp.Abilities;
import com.variantsmp.Stages;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LivingEntity.class)
public abstract class LivingEntityDamageMixin {

    @WrapMethod(method = "damage(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/damage/DamageSource;F)Z")
    private boolean variantsmp$damage(ServerWorld world, DamageSource source, float amount, Operation<Boolean> original) {
        LivingEntity self = (LivingEntity) (Object) this;
        float scaled = Abilities.beforeDamage(self, source, amount);
        if (Float.isNaN(scaled)) return false;
        scaled = Stages.damage(self, source, scaled);
        float before = self.getHealth() + self.getAbsorptionAmount();
        boolean hit = original.call(world, source, scaled);
        float after = self.isDead() ? 0.0F : self.getHealth() + self.getAbsorptionAmount();
        Abilities.afterDamage(self, source, hit, Math.max(0.0F, before - after));
        return hit;
    }
}
