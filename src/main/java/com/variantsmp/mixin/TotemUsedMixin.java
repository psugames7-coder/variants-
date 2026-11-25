package com.variantsmp.mixin;

import com.variantsmp.CombatLimits;
import net.minecraft.advancement.criterion.UsedTotemCriterion;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(UsedTotemCriterion.class)
public abstract class TotemUsedMixin {

    @Inject(method = "trigger(Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/item/ItemStack;)V", at = @At("HEAD"))
    private void variantsmp$trigger(ServerPlayerEntity player, ItemStack stack, CallbackInfo ci) {
        CombatLimits.onUse(player, CombatLimits.Kind.TOTEM);
    }
}
