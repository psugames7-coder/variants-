package com.variantstrims.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.variantstrims.TrimEffects;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LivingEntity.class)
public abstract class EquipmentMixin {

    @WrapMethod(method = "damageEquipment(Lnet/minecraft/entity/damage/DamageSource;F[Lnet/minecraft/entity/EquipmentSlot;)V")
    private void variantstrims$damageEquipment(DamageSource source, float amount, EquipmentSlot[] slots, Operation<Void> original) {
        float scaled = amount;
        try {
            scaled = TrimEffects.armorWear((LivingEntity) (Object) this, source, amount);
        } catch (Throwable ignored) {
        }
        original.call(source, scaled, slots);
    }
}
