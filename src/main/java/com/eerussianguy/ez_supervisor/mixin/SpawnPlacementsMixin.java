package com.eerussianguy.ez_supervisor.mixin;

import java.util.Map;
import java.util.Objects;
import java.util.Random;
import com.eerussianguy.ez_supervisor.EZSupervisor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SpawnPlacements.class)
public abstract class SpawnPlacementsMixin
{

    @Inject(method = "checkSpawnRules", at = @At("HEAD"), cancellable = true)
    private static void inject$checkSpawnRules(EntityType<Entity> entity, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos, Random rand, CallbackInfoReturnable<Boolean> cir)
    {
        if (Objects.requireNonNull(EZSupervisor.restrictions).containsKey(entity) && EZSupervisor.restrictions.get(entity).wipeOriginal())
        {
            cir.setReturnValue(true);
        }
    }
}
