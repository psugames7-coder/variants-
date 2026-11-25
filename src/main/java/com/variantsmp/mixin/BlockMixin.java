package com.variantsmp.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.variantsmp.Stages;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Block.class)
public abstract class BlockMixin {

    @WrapMethod(method = "afterBreak(Lnet/minecraft/world/World;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/block/entity/BlockEntity;Lnet/minecraft/item/ItemStack;)V")
    private void variantsmp$afterBreak(World world, PlayerEntity player, BlockPos pos, BlockState state,
                                       BlockEntity blockEntity, ItemStack tool, Operation<Void> original) {
        try {
            Stages.beginBreak(player);
        } catch (Throwable ignored) {
        }
        try {
            original.call(world, player, pos, state, blockEntity, tool);
        } finally {
            Stages.endBreak();
        }
    }
}
