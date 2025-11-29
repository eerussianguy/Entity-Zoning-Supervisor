package com.eerussianguy.ez_supervisor.common;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import com.eerussianguy.ez_supervisor.EZSupervisor;
import com.eerussianguy.ez_supervisor.common.data.LootFilter;
import com.eerussianguy.ez_supervisor.common.data.SpawnPredicate;
import com.eerussianguy.ez_supervisor.common.data.SpawnRestriction;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import org.jetbrains.annotations.Nullable;

public class ForgeEvents
{

    public static void init()
    {
        final IEventBus bus = NeoForge.EVENT_BUS;

        bus.addListener(EventPriority.LOW, ForgeEvents::onCheckSpawn);
        bus.addListener(EventPriority.LOW, ForgeEvents::onCheckSpawnPlacement);
        bus.addListener(EventPriority.LOW, ForgeEvents::onEntityLoot);
        bus.addListener(ForgeEvents::registerCommands);
    }

    public static void onCheckSpawnPlacement(MobSpawnEvent.SpawnPlacementCheck event)
    {
        final SpawnRestriction restriction = getSpawnRestriction(event.getEntityType());
        if (restriction == null)
            return;
        if (restriction.wipeOriginal())
        {
            event.setResult(MobSpawnEvent.SpawnPlacementCheck.Result.SUCCEED);
        }
    }

    public static void onCheckSpawn(MobSpawnEvent.PositionCheck event)
    {
        var server = event.getLevel();
        {
            final LivingEntity entity = event.getEntity();
            final SpawnRestriction restriction = getSpawnRestriction(entity.getType());
            if (restriction == null)
                return;
            final List<SpawnRestriction.Named> predicates = restriction.predicates();
            for (var named : predicates)
            {
                if (!named.predicate().test(entity, server, event.getSpawnType(), entity.blockPosition(), entity.getRandom()))
                {
                    event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
                    break;
                }
            }
        }
    }

    @Nullable
    private static SpawnRestriction getSpawnRestriction(EntityType<?> type)
    {
        assert EZSupervisor.restrictions != null;
        return EZSupervisor.restrictions.keySet().stream().filter(key -> key.value().equals(type)).findFirst().map(key -> {
            final SpawnRestriction restriction = EZSupervisor.restrictions.get(key);
            assert restriction != null;
            return restriction;
        }).orElse(null);
    }

    public static void onEntityLoot(LivingDropsEvent event)
    {
        final Set<LootFilter> usedNoIngredientFilters = new HashSet<>();
        final LivingEntity deadEntity = event.getEntity();
        for (ItemEntity entity : event.getDrops())
        {
            final ItemStack stack = entity.getItem();
            final int count = stack.getCount();

            Objects.requireNonNull(EZSupervisor.entityLootFilters).forEach(filter -> {
                if (filter.ingredient() == null)
                {
                    if (filter.output() != Items.AIR && !usedNoIngredientFilters.contains(filter))
                    {
                        usedNoIngredientFilters.add(filter);
                        deadEntity.spawnAtLocation(new ItemStack(filter.output(), Mth.ceil(filter.outputMultiplier())));
                    }
                }
                else if ((filter.entities().isEmpty() || filter.entities().stream().map(Holder::value).anyMatch(e -> e.equals(entity.getType()))) && filter.ingredient().test(stack))
                {
                    if (!filter.killedByPlayer() || (event.getSource().getEntity() instanceof Player))
                    {
                        entity.setItem(ItemStack.EMPTY);
                        entity.discard();
                        if (filter.output() != Items.AIR)
                        {
                            deadEntity.spawnAtLocation(new ItemStack(filter.output(), Mth.ceil(filter.outputMultiplier() * count)));
                        }
                    }
                }
            });
        }
    }

    public static void registerCommands(RegisterCommandsEvent event)
    {
        EZSCommands.registerCommands(event.getDispatcher(), event.getBuildContext());
    }
}
