package com.variantsmp.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerEntity.class)
public interface PlayerEntityAccessor {

    @Accessor("currentExplosionImpactPos")
    void variantsmp$setCurrentExplosionImpactPos(Vec3d pos);

    @Accessor("ignoreFallDamageFromCurrentExplosion")
    void variantsmp$setIgnoreFallDamageFromCurrentExplosion(boolean value);
}
