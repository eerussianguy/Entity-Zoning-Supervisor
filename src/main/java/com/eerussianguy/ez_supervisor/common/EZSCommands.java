package com.eerussianguy.ez_supervisor.common;

import com.eerussianguy.ez_supervisor.EZSupervisor;
import com.eerussianguy.ez_supervisor.common.data.SpawnRestriction;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class EZSCommands
{
    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context)
    {
        dispatcher.register(Commands.literal("ezs")
            .then(Commands.literal("list").requires(source -> source.hasPermission(2))
                .then(Commands.literal("spawns").executes(
                        c -> listSpawns(c.getSource())
                    )
                )
                .then(Commands.literal("restrictions").executes(
                        c -> listSpawnRestrictions(c.getSource())
                    )
                )
            )
        );
    }

    public static int listSpawns(CommandSourceStack context)
    {
        assert EZSupervisor.spawns != null;
        context.source.sendSystemMessage(Component.literal("Found " + EZSupervisor.spawns.size() + " spawns: ").withStyle(ChatFormatting.ITALIC));
        EZSupervisor.spawns.forEach(spawn -> {
            context.source.sendSystemMessage(
                Component.literal(spawn.toString()).withStyle(ChatFormatting.GRAY)
            );
        });
        return Command.SINGLE_SUCCESS;
    }

    public static int listSpawnRestrictions(CommandSourceStack context)
    {
        assert EZSupervisor.restrictions != null;
        context.source.sendSystemMessage(Component.literal("Found " + EZSupervisor.restrictions.size() + " spawn restrictions: ").withStyle(ChatFormatting.ITALIC));
        EZSupervisor.restrictions.forEach((key, value) -> {
            context.source.sendSystemMessage(Component.literal("Entity: " + key.getRegisteredName()).withStyle(ChatFormatting.GRAY));
            context.source.sendSystemMessage(Component.literal("Found " + value.predicates().size() + " spawn predicates: ").withStyle(ChatFormatting.GRAY));
            value.predicates().forEach(named -> {
                context.source.sendSystemMessage(Component.literal("  - " + named.id()));
            });
        });
        return Command.SINGLE_SUCCESS;
    }
}
