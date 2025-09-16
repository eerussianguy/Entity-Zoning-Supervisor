package com.eerussianguy.ez_supervisor.client;


import com.eerussianguy.ez_supervisor.EZSupervisor;
import net.minecraft.client.gui.screens.TitleScreen;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

public class ClientForgeEvents
{
    public static void init()
    {
        final IEventBus bus = NeoForge.EVENT_BUS;

        bus.addListener(ClientForgeEvents::onScreen);
    }

    private static void onScreen(ScreenEvent.Init.Pre event)
    {
        if (event.getScreen() instanceof TitleScreen)
        {
            EZSupervisor.createConfigFiles();
        }
    }
}
