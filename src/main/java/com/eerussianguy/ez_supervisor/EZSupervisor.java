package com.eerussianguy.ez_supervisor;

import java.io.File;
import java.util.List;
import java.util.Map;
import com.eerussianguy.ez_supervisor.client.ClientForgeEvents;
import com.eerussianguy.ez_supervisor.common.ForgeEvents;
import com.eerussianguy.ez_supervisor.common.ParsingUtils;
import com.eerussianguy.ez_supervisor.common.SpawnModifierProvider;
import com.eerussianguy.ez_supervisor.common.data.BiomeSpawn;
import com.eerussianguy.ez_supervisor.common.data.LootFilter;
import com.eerussianguy.ez_supervisor.common.data.SpawnRestriction;
import com.eerussianguy.ez_supervisor.common.data.SpawnRestrictionTypes;
import com.eerussianguy.ez_supervisor.compat.TFCIntegration;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

@Mod(EZSupervisor.MOD_ID)
public class EZSupervisor
{
    public static final String MOD_ID = "ez_supervisor";

    @Nullable public static File configDir = null;
    @Nullable public static List<BiomeSpawn> spawns = null;
    @Nullable public static Map<EntityType<?>, SpawnRestriction> restrictions = null;
    @Nullable public static Map<String, SpawnRestriction> ruleSets = null;
    @Nullable public static List<LootFilter> entityLootFilters = null;
    public static boolean tfc = false;

    private static final DeferredRegister<MapCodec<? extends BiomeModifier>> BIOME_MODIFIER_SERIALIZERS = DeferredRegister.create(NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, MOD_ID);

    public static final DeferredHolder<MapCodec<? extends BiomeModifier>, MapCodec<SpawnModifierProvider>> ADD_EZ_SPAWNS_BIOME_MODIFIER_TYPE = BIOME_MODIFIER_SERIALIZERS.register("add_ez_spawns", () -> MapCodec.unit(SpawnModifierProvider.INSTANCE));

    public EZSupervisor(ModContainer mod, IEventBus bus)
    {
        if (FMLEnvironment.dist == Dist.CLIENT)
        {
            ClientForgeEvents.init();
        }
        ForgeEvents.init();
        BIOME_MODIFIER_SERIALIZERS.register(bus);

        bus.addListener(EZSupervisor::setup);
    }

    public static void setup(FMLCommonSetupEvent event)
    {
        SpawnRestrictionTypes.init();
        if (ModList.get().isLoaded("tfc"))
        {
            tfc = true;
            TFCIntegration.init();
        }
        event.enqueueWork(EZSupervisor::createConfigFiles);
    }

    public static void createConfigFiles()
    {
        configDir = getConfigDir();
        spawns = BiomeSpawn.readAll(ParsingUtils.readJsonArray(configDir, "spawn"));
        ruleSets = SpawnRestriction.readRuleSets(ParsingUtils.readJsonObject(configDir, "rule_set"));
        restrictions = SpawnRestriction.readAll(ParsingUtils.readJsonArray(configDir, "spawn_restriction"));
        entityLootFilters = LootFilter.readAll(ParsingUtils.readJsonArray(configDir, "entity_loot_filter"));

        if (spawns == null) throw new RuntimeException("JSON Parsing error loading spawn.json");
        if (restrictions == null) throw new RuntimeException("JSON Parsing error loading spawn_restriction.json");
        if (ruleSets == null) throw new RuntimeException("JSON Parsing error loading rule_set.json");
        // noinspection ConstantConditions
        if (entityLootFilters == null) throw new RuntimeException("JSON Parsing error loading entity_loot_filter.json");

    }

    public static File getConfigDir()
    {
        final File dir = new File(FMLPaths.CONFIGDIR.get().toFile(), "entity_zoning_supervisor");
        if (!dir.exists())
        {
            try
            {
                if (!dir.mkdir())
                {
                    throw new RuntimeException("Failed to make config directory " + dir);
                }
            }
            catch (SecurityException e)
            {
                throw new RuntimeException("Failed to make config directory " + dir, e);
            }
        }
        return dir;
    }

    public static ResourceLocation identifier(String path)
    {
        return ResourceLocation.fromNamespaceAndPath(EZSupervisor.MOD_ID, path);
    }

}
