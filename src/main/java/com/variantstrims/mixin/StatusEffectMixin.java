package com.variantstrims.mixin;

import com.variantstrims.TrimEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class StatusEffectMixin {

    @Inject(method = "canHaveStatusEffect(Lnet/minecraft/entity/effect/StatusEffectInstance;)Z", at = @At("HEAD"), cancellable = true)
    private void variantstrims$canHave(StatusEffectInstance effect, CallbackInfoReturnable<Boolean> cir) {
        try {
            if (TrimEffects.blocks((LivingEntity) (Object) this, effect)) cir.setReturnValue(false);
        } catch (Throwable ignored) {
        }
    }

    @ModifyVariable(method = "addStatusEffect(Lnet/minecraft/entity/effect/StatusEffectInstance;Lnet/minecraft/entity/Entity;)Z",
            at = @At("HEAD"), argsOnly = true)
    private StatusEffectInstance variantstrims$addStatusEffect(StatusEffectInstance effect) {
        return TrimEffects.potion((LivingEntity) (Object) this, effect);
    }
}
