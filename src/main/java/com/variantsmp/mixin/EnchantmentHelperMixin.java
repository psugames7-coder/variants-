package com.variantsmp.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.variantsmp.MaceAbility;
import com.variantsmp.SpearAbility;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;

import java.util.function.Consumer;

@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentHelperMixin {

    @WrapMethod(method = "onTargetDamaged(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/Entity;Lnet/minecraft/entity/damage/DamageSource;Lnet/minecraft/item/ItemStack;)V")
    private static void variantsmp$targetDamaged(ServerWorld world, Entity target, DamageSource source,
                                                 ItemStack weapon, Operation<Void> original) {
        MaceAbility.Prepared prep = MaceAbility.prepare(source, weapon);
        try {
            original.call(world, target, source, prep.weapon());
        } finally {
            prep.restore();
        }
    }

    @WrapMethod(method = "onTargetDamaged(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/Entity;Lnet/minecraft/entity/damage/DamageSource;Lnet/minecraft/item/ItemStack;Ljava/util/function/Consumer;)V")
    private static void variantsmp$targetDamagedCallback(ServerWorld world, Entity target, DamageSource source,
                                                         ItemStack weapon, Consumer<Item> breakCallback,
                                                         Operation<Void> original) {
        MaceAbility.Prepared prep = MaceAbility.prepare(source, weapon);
        try {
            original.call(world, target, source, prep.weapon(), breakCallback);
        } finally {
            prep.restore();
        }
    }

    @WrapMethod(method = "onAttack(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/Entity;)V")
    private static void variantsmp$onAttack(ServerWorld world, Entity attacker, Operation<Void> original) {
        if (!SpearAbility.allowLunge(attacker)) return;
        SpearAbility.beginLunge(attacker);
        try {
            original.call(world, attacker);
        } finally {
            SpearAbility.endLunge();
        }
    }
}
