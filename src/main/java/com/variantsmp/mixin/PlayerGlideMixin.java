package com.variantsmp.mixin;

import com.variantsmp.Flight;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerGlideMixin {

    @Inject(method = "checkGliding()Z", at = @At("HEAD"), cancellable = true)
    private void variantsmp$checkGliding(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof ServerPlayerEntity player && !Flight.canStartGliding(player)) {
            cir.setReturnValue(false);
        }
    }
}
