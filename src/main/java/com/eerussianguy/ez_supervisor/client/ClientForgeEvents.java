package com.eerussianguy.ez_supervisor.client;


import com.eerussianguy.ez_supervisor.EZSupervisor;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;

public class ClientForgeEvents
{
    public static void init()
    {
        final IEventBus bus = MinecraftForge.EVENT_BUS;

        bus.addListener(ClientForgeEvents::onScreen);
    }

    private static void onScreen(ScreenEvent.InitScreenEvent.Pre event)
    {
        if (event.getScreen() instanceof TitleScreen)
        {
            EZSupervisor.createConfigFiles();
        }
    }
}
