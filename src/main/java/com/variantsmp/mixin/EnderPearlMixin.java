package com.variantsmp.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.variantsmp.CombatLimits;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.EnderPearlItem;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(EnderPearlItem.class)
public abstract class EnderPearlMixin {

    @WrapMethod(method = "use(Lnet/minecraft/world/World;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Hand;)Lnet/minecraft/util/ActionResult;")
    private ActionResult variantsmp$use(World world, PlayerEntity user, Hand hand, Operation<ActionResult> original) {
        ActionResult result = original.call(world, user, hand);
        try {
            if (user instanceof ServerPlayerEntity player && result.isAccepted()) {
                CombatLimits.onUse(player, CombatLimits.Kind.PEARL);
            }
        } catch (Throwable ignored) {
        }
        return result;
    }
}
