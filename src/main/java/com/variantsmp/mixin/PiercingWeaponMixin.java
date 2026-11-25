package com.variantsmp.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.variantsmp.SpearAbility;
import net.minecraft.component.type.PiercingWeaponComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(PiercingWeaponComponent.class)
public abstract class PiercingWeaponMixin {

    @WrapMethod(method = "stab(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/entity/EquipmentSlot;)V")
    private void variantsmp$stab(LivingEntity attacker, EquipmentSlot slot, Operation<Void> original) {
        HungerManager hunger = null;
        int food = 0;
        float saturation = 0.0F;
        float exhaustion = 0.0F;
        try {
            if (attacker instanceof ServerPlayerEntity player && SpearAbility.beginStab(player)) {
                HungerManager h = player.getHungerManager();
                food = h.getFoodLevel();
                saturation = h.getSaturationLevel();
                exhaustion = ((HungerManagerAccessor) h).variantsmp$getExhaustion();
                hunger = h;
            }
        } catch (Throwable ignored) {
            hunger = null;
        }
        try {
            original.call(attacker, slot);
        } finally {
            boolean lunged = SpearAbility.endStab();
            if (hunger != null && !lunged) {
                try {
                    hunger.setFoodLevel(food);
                    hunger.setSaturationLevel(saturation);
                    ((HungerManagerAccessor) hunger).variantsmp$setExhaustion(exhaustion);
                } catch (Throwable ignored) {
                }
            }
        }
    }
}
