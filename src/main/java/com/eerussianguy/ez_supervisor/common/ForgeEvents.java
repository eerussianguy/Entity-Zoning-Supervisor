package com.eerussianguy.ez_supervisor.common;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import com.eerussianguy.ez_supervisor.EZSupervisor;
import com.eerussianguy.ez_supervisor.common.data.LootFilter;
import com.eerussianguy.ez_supervisor.common.data.SpawnPredicate;
import com.eerussianguy.ez_supervisor.common.data.SpawnRestriction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;

public class ForgeEvents
{

    public static void init()
    {
        final IEventBus bus = MinecraftForge.EVENT_BUS;

        bus.addListener(EventPriority.LOW, ForgeEvents::onCheckSpawn);
        bus.addListener(EventPriority.LOW, ForgeEvents::onEntityLoot);
    }

    public static void onCheckSpawn(MobSpawnEvent.PositionCheck event)
    {
        var server = event.getLevel();
        {
            final LivingEntity entity = event.getEntity();
            final SpawnRestriction restriction = Objects.requireNonNull(EZSupervisor.restrictions).get(entity.getType());
            if (restriction != null)
            {
                final List<SpawnPredicate> predicates = restriction.predicates();
                for (SpawnPredicate predicate : predicates)
                {
                    if (!predicate.test(entity, server, event.getSpawnType(), entity.blockPosition(), entity.getRandom()))
                    {
                        event.setResult(Event.Result.DENY);
                        break;
                    }
                }
            }

        }
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
                else if ((filter.entities().isEmpty() || filter.entities().contains(entity.getType())) && filter.ingredient().test(stack))
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
}
