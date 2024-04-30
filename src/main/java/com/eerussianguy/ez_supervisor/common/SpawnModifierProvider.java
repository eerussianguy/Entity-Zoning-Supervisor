package com.eerussianguy.ez_supervisor.common;

import com.eerussianguy.ez_supervisor.EZSupervisor;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.MobSpawnSettingsBuilder;
import net.minecraftforge.common.world.ModifiableBiomeInfo;
import net.minecraftforge.common.world.NoneStructureModifier;
import org.slf4j.Logger;

public record SpawnModifierProvider() implements BiomeModifier {

    private static final Logger LOGGER = LogUtils.getLogger();
    public static final SpawnModifierProvider INSTANCE = new SpawnModifierProvider();

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        MobSpawnSettingsBuilder spawns = builder.getMobSpawnSettings();

        if (phase == Phase.ADD) {
            assert EZSupervisor.spawns != null;
            EZSupervisor.spawns.forEach(spawn -> {
                if (spawn.biomes().isEmpty() || spawn.biomes().contains(biome.unwrapKey().get().location())) {
                    LOGGER.debug("Adding spawn {} to biome {}", spawn.types(), biome.unwrapKey().get().location());
                    spawn.types().forEach(entity -> {
                        spawns.addSpawn(entity.getCategory(), new MobSpawnSettings.SpawnerData(entity, spawn.weight(), spawn.minCount(), spawn.maxCount()));
                    });
                }
            });
        }
    }


    @Override
    public Codec<? extends BiomeModifier> codec() {
        return EZSupervisor.ADD_EZ_SPAWNS_BIOME_MODIFIER_TYPE.get();
    }
}
