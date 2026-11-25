package com.macelimit.mixin;

import com.macelimit.MaceRegistry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {

    @Inject(method = "remove(Lnet/minecraft/entity/Entity$RemovalReason;)V", at = @At("HEAD"))
    private void macelimit$remove(Entity.RemovalReason reason, CallbackInfo ci) {
        try {
            if (reason.shouldDestroy()) MaceRegistry.destroyed(((ItemEntity) (Object) this).getStack());
        } catch (Throwable ignored) {
        }
    }
}
