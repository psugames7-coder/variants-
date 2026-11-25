package com.variantsmp.mixin;

import com.variantsmp.SpearAbility;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerExhaustionMixin {

    @Inject(method = "addExhaustion(F)V", at = @At("HEAD"), cancellable = true)
    private void variantsmp$addExhaustion(float exhaustion, CallbackInfo ci) {
        try {
            if ((Object) this instanceof ServerPlayerEntity player && SpearAbility.freeExhaustion(player)) {
                ci.cancel();
            }
        } catch (Throwable ignored) {
        }
    }
}
