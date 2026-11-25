package com.variantsmp.mixin;

import com.variantsmp.Stages;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class CommandMixin {

    @Inject(method = "executeCommand(Ljava/lang/String;)V", at = @At("HEAD"), cancellable = true)
    private void variantsmp$executeCommand(String command, CallbackInfo ci) {
        try {
            if (Stages.command(((ServerPlayNetworkHandler) (Object) this).player, command)) ci.cancel();
        } catch (Throwable ignored) {
        }
    }
}
