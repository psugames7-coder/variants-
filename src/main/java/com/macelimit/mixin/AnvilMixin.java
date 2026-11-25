package com.macelimit.mixin;

import com.macelimit.MaceEnchants;
import net.minecraft.screen.AnvilScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnvilScreenHandler.class)
public abstract class AnvilMixin {

    @Inject(method = "updateResult()V", at = @At("TAIL"))
    private void macelimit$updateResult(CallbackInfo ci) {
        try {
            ForgingAccess access = (ForgingAccess) this;
            MaceEnchants.filterAnvil(access.macelimit$getInput(), access.macelimit$getOutput());
        } catch (Throwable ignored) {
        }
    }
}
