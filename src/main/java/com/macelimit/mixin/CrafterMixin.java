package com.macelimit.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.macelimit.MaceRegistry;
import net.minecraft.block.CrafterBlock;
import net.minecraft.item.Items;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;

@Mixin(CrafterBlock.class)
public abstract class CrafterMixin {

    @ModifyReturnValue(method = "getCraftingRecipe", at = @At("RETURN"))
    private static Optional<RecipeEntry<CraftingRecipe>> macelimit$getCraftingRecipe(Optional<RecipeEntry<CraftingRecipe>> original,
                                                                                    ServerWorld world, CraftingRecipeInput input) {
        try {
            if (original.isPresent() && MaceRegistry.full()
                    && original.get().value().craft(input, world.getRegistryManager()).isOf(Items.MACE)) {
                return Optional.empty();
            }
        } catch (Throwable ignored) {
        }
        return original;
    }
}
