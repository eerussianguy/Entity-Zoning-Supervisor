package com.eerussianguy.ez_supervisor.common;

import com.eerussianguy.ez_supervisor.EZSupervisor;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.MobSpawnSettingsBuilder;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;
import org.slf4j.Logger;

public record SpawnModifierProvider() implements BiomeModifier
{
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final SpawnModifierProvider INSTANCE = new SpawnModifierProvider();

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder)
    {
        MobSpawnSettingsBuilder spawns = builder.getMobSpawnSettings();

        if (phase == Phase.ADD)
        {
            assert EZSupervisor.spawns != null;
            EZSupervisor.spawns.forEach(spawn -> {
                final ResourceLocation biomeKey = biome.unwrapKey().get().location();
                if (spawn.biomes().isEmpty() || spawn.biomes().contains(biomeKey))
                {
                    LOGGER.debug("Adding spawn {} to biome {}", spawn.types(), biomeKey);
                    spawn.types().forEach(entity -> {
                        spawns.addSpawn(entity.value().getCategory(), new MobSpawnSettings.SpawnerData(entity.value(), spawn.weight(), spawn.minCount(), spawn.maxCount()));
                    });
                }
            });
        }
    }


    @Override
    public MapCodec<? extends BiomeModifier> codec()
    {
        return EZSupervisor.ADD_EZ_SPAWNS_BIOME_MODIFIER_TYPE.get();
    }
}
