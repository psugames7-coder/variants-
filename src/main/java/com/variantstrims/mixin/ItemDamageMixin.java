package com.variantstrims.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.variantstrims.TrimEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;

import java.util.function.Consumer;

@Mixin(ItemStack.class)
public abstract class ItemDamageMixin {

    @WrapMethod(method = "damage(ILnet/minecraft/server/world/ServerWorld;Lnet/minecraft/server/network/ServerPlayerEntity;Ljava/util/function/Consumer;)V")
    private void variantstrims$damage(int amount, ServerWorld world, ServerPlayerEntity player, Consumer<Item> breakCallback,
                                      Operation<Void> original) {
        int scaled = amount;
        try {
            scaled = TrimEffects.armorDurability((ItemStack) (Object) this, player, amount);
        } catch (Throwable ignored) {
        }
        original.call(scaled, world, player, breakCallback);
    }
}
