package com.eerussianguy.ez_supervisor;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.eerussianguy.ez_supervisor.client.ClientForgeEvents;
import com.eerussianguy.ez_supervisor.client.ClientModEvents;
import com.eerussianguy.ez_supervisor.common.ForgeEvents;
import com.eerussianguy.ez_supervisor.common.ParsingUtils;
import com.eerussianguy.ez_supervisor.common.SpawnModifierProvider;
import com.eerussianguy.ez_supervisor.common.data.BiomeSpawn;
import com.eerussianguy.ez_supervisor.common.data.LootFilter;
import com.eerussianguy.ez_supervisor.common.data.SpawnRestriction;
import com.eerussianguy.ez_supervisor.common.data.SpawnRestrictionTypes;
import com.eerussianguy.ez_supervisor.compat.TFCIntegration;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.NoneStructureModifier;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

@Mod(EZSupervisor.MOD_ID)
public class EZSupervisor
{
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final String MOD_ID = "ez_supervisor";

    @Nullable public static File configDir = null;
    @Nullable public static List<BiomeSpawn> spawns = null;
    @Nullable public static Map<EntityType<?>, SpawnRestriction> restrictions = null;
    @Nullable public static Map<String, SpawnRestriction> ruleSets = null;
    @Nullable public static List<LootFilter> entityLootFilters = null;
    public static boolean tfc = false;

    private static final DeferredRegister<Codec<? extends BiomeModifier>> BIOME_MODIFIER_SERIALIZERS = DeferredRegister.create(ForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, MOD_ID);

    public static final RegistryObject<Codec<SpawnModifierProvider>> ADD_EZ_SPAWNS_BIOME_MODIFIER_TYPE = BIOME_MODIFIER_SERIALIZERS.register("add_ez_spawns", () -> Codec.unit(SpawnModifierProvider.INSTANCE));

    public EZSupervisor()
    {
        if (FMLEnvironment.dist == Dist.CLIENT)
        {
            ClientForgeEvents.init();
            ClientModEvents.init();
        }
        ForgeEvents.init();
        BIOME_MODIFIER_SERIALIZERS.register(FMLJavaModLoadingContext.get().getModEventBus());

        FMLJavaModLoadingContext.get().getModEventBus().addListener(EZSupervisor::setup);
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
        return new ResourceLocation(EZSupervisor.MOD_ID, path);
    }

}
