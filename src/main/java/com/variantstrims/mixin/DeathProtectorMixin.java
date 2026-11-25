package com.variantstrims.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.variantstrims.TrimEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LivingEntity.class)
public abstract class DeathProtectorMixin {

    @WrapMethod(method = "tryUseDeathProtector(Lnet/minecraft/entity/damage/DamageSource;)Z")
    private boolean variantstrims$deathProtector(DamageSource source, Operation<Boolean> original) {
        if (!((Object) this instanceof ServerPlayerEntity p)) return original.call(source);
        ItemStack main = p.getMainHandStack().copy();
        ItemStack off = p.getOffHandStack().copy();
        boolean used = original.call(source);
        try {
            if (used && TrimEffects.vexSave(p)) TrimEffects.restoreTotem(p, main, off);
        } catch (Throwable ignored) {
        }
        return used;
    }
}
