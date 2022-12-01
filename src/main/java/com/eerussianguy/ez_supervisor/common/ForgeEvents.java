package com.eerussianguy.ez_supervisor.common;

import java.util.List;
import java.util.Objects;
import com.eerussianguy.ez_supervisor.EZSupervisor;
import com.eerussianguy.ez_supervisor.common.data.SpawnPredicate;
import com.eerussianguy.ez_supervisor.common.data.SpawnRestriction;
import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.world.BiomeLoadingEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.IEventBus;
import org.slf4j.Logger;

public class ForgeEvents
{
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void init()
    {
        final IEventBus bus = MinecraftForge.EVENT_BUS;

        bus.addListener(ForgeEvents::onBiomeLoad);
        bus.addListener(ForgeEvents::onCheckSpawn);
    }

    public static void onBiomeLoad(BiomeLoadingEvent event)
    {
        assert EZSupervisor.spawns != null;
        EZSupervisor.spawns.forEach(spawn -> {
            if (spawn.biomes().isEmpty() || spawn.biomes().contains(event.getName()))
            {
                LOGGER.debug("Adding spawn {} to biome {}", spawn.types(), event.getName());
                spawn.types().forEach(entity -> {
                    event.getSpawns().addSpawn(entity.getCategory(), new MobSpawnSettings.SpawnerData(entity, spawn.weight(), spawn.minCount(), spawn.maxCount()));
                });
            }
        });
    }

    public static void onCheckSpawn(LivingSpawnEvent.CheckSpawn event)
    {
        if (event.getWorld() instanceof ServerLevelAccessor server)
        {
            final LivingEntity entity = event.getEntityLiving();
            final SpawnRestriction restriction = Objects.requireNonNull(EZSupervisor.restrictions).get(entity.getType());
            if (restriction != null)
            {
                final List<SpawnPredicate> predicates = restriction.predicates();
                for (SpawnPredicate predicate : predicates)
                {
                    if (!predicate.test(entity, server, event.getSpawnReason(), entity.blockPosition(), entity.getLevel().getRandom()))
                    {
                        event.setResult(Event.Result.DENY);
                        break;
                    }
                }
            }

        }
    }
}
