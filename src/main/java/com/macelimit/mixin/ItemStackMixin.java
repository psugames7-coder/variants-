package com.macelimit.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.macelimit.MaceRegistry;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    @Inject(method = "onCraftByCrafter(Lnet/minecraft/world/World;)V", at = @At("HEAD"))
    private void macelimit$onCraftByCrafter(World world, CallbackInfo ci) {
        try {
            if (world instanceof ServerWorld) MaceRegistry.register((ItemStack) (Object) this, null);
        } catch (Throwable ignored) {
        }
    }

    @WrapMethod(method = "damage(ILnet/minecraft/server/world/ServerWorld;Lnet/minecraft/server/network/ServerPlayerEntity;Ljava/util/function/Consumer;)V")
    private void macelimit$damage(int amount, ServerWorld world, ServerPlayerEntity player, Consumer<Item> breakCallback,
                                  Operation<Void> original) {
        ItemStack self = (ItemStack) (Object) this;
        String id = MaceRegistry.id(self);
        original.call(amount, world, player, breakCallback);
        try {
            if (id != null && self.isEmpty()) MaceRegistry.destroyed(id);
        } catch (Throwable ignored) {
        }
    }
}
