package com.variantsmp.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.variantsmp.CombatLimits;
import com.variantsmp.Stages;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityStageMixin {

    @WrapMethod(method = "damageEquipment(Lnet/minecraft/entity/damage/DamageSource;F[Lnet/minecraft/entity/EquipmentSlot;)V")
    private void variantsmp$damageEquipment(DamageSource source, float amount, EquipmentSlot[] slots, Operation<Void> original) {
        original.call(source, Stages.armorWear((LivingEntity) (Object) this, source, amount), slots);
    }

    @ModifyVariable(method = "addStatusEffect(Lnet/minecraft/entity/effect/StatusEffectInstance;Lnet/minecraft/entity/Entity;)Z",
            at = @At("HEAD"), argsOnly = true)
    private StatusEffectInstance variantsmp$addStatusEffect(StatusEffectInstance effect) {
        return Stages.potion((LivingEntity) (Object) this, effect);
    }

    @Inject(method = "tryUseDeathProtector(Lnet/minecraft/entity/damage/DamageSource;)Z", at = @At("HEAD"), cancellable = true)
    private void variantsmp$deathProtector(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (Stages.cheatDeath(self, source)) {
            cir.setReturnValue(true);
            return;
        }
        if (CombatLimits.totemBlocked(self)) cir.setReturnValue(false);
    }
}
