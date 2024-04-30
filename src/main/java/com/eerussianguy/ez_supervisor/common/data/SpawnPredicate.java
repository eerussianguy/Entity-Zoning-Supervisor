package com.eerussianguy.ez_supervisor.common.data;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ServerLevelAccessor;

@FunctionalInterface
public interface SpawnPredicate
{
    boolean test(Entity entity, ServerLevelAccessor level, MobSpawnType type, BlockPos pos, RandomSource random);
}
