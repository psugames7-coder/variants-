package com.variantsmp.mixin;

import com.variantsmp.Stages;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(PlayerEntity.class)
public abstract class PlayerXpMixin {

    @ModifyVariable(method = "addExperience(I)V", at = @At("HEAD"), argsOnly = true)
    private int variantsmp$addExperience(int experience) {
        return Stages.gainXp((PlayerEntity) (Object) this, experience);
    }
}
