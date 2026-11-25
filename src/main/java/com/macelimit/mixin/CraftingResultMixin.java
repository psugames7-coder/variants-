package com.macelimit.mixin;

import com.macelimit.MaceRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.CraftingResultInventory;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CraftingScreenHandler.class)
public abstract class CraftingResultMixin {

    @Inject(method = "updateResult", at = @At("TAIL"))
    private static void macelimit$updateResult(ScreenHandler handler, ServerWorld world, PlayerEntity player,
                                               RecipeInputInventory craftingInventory, CraftingResultInventory resultInventory,
                                               RecipeEntry<CraftingRecipe> recipe, CallbackInfo ci) {
        try {
            if (resultInventory.getStack(0).isOf(Items.MACE) && MaceRegistry.full()) {
                resultInventory.setStack(0, ItemStack.EMPTY);
                handler.sendContentUpdates();
            }
        } catch (Throwable ignored) {
        }
    }
}
