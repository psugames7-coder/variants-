package com.macelimit.mixin;

import com.macelimit.MaceRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Item.class)
public abstract class CraftByPlayerMixin {

    @Inject(method = "onCraftByPlayer(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/player/PlayerEntity;)V", at = @At("HEAD"))
    private void macelimit$onCraftByPlayer(ItemStack stack, PlayerEntity player, CallbackInfo ci) {
        try {
            if (player instanceof ServerPlayerEntity sp) MaceRegistry.register(stack, sp);
        } catch (Throwable ignored) {
        }
    }
}
