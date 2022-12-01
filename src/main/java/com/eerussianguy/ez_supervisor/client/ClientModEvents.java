package com.eerussianguy.ez_supervisor.client;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import net.dries007.tfc.config.FoodExpiryTooltipStyle;
import net.dries007.tfc.config.TFCConfig;

public class ClientModEvents
{
    public static void init()
    {
        final IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();

        bus.addListener(ClientModEvents::setup);
    }

    private static void setup(FMLClientSetupEvent event)
    {
    }
}
