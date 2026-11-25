package com.variantsmp.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.variantsmp.Stages;
import net.minecraft.block.vault.VaultConfig;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(targets = "net.minecraft.block.entity.VaultBlockEntity$Server")
public abstract class VaultServerMixin {

    @ModifyReturnValue(method = "generateLoot", at = @At("RETURN"))
    private static List<ItemStack> variantsmp$generateLoot(List<ItemStack> original, ServerWorld world, VaultConfig config,
                                                           BlockPos pos, PlayerEntity player, ItemStack key) {
        return Stages.vaultLoot(original, player, key);
    }
}
