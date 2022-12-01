package com.eerussianguy.ez_supervisor.common;

import com.eerussianguy.ez_supervisor.EZSupervisor;
import com.mojang.logging.LogUtils;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.BiomeLoadingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import org.slf4j.Logger;

public class ForgeEvents
{
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void init()
    {
        final IEventBus bus = MinecraftForge.EVENT_BUS;

        bus.addListener(ForgeEvents::onBiomeLoad);
    }

    public static void onBiomeLoad(BiomeLoadingEvent event)
    {
        assert EZSupervisor.spawns != null;
        EZSupervisor.spawns.forEach(spawn -> {
            if (spawn.biomes().isEmpty() || spawn.biomes().contains(event.getName()))
            {
                LOGGER.debug("Adding spawn {} to biome {}", spawn.type(), event.getName());
                event.getSpawns().addSpawn(spawn.type().getCategory(), new MobSpawnSettings.SpawnerData(spawn.type(), spawn.weight(), spawn.minCount(), spawn.maxCount()));
            }
        });
    }
}
