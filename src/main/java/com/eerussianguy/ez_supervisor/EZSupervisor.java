package com.eerussianguy.ez_supervisor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import com.eerussianguy.ez_supervisor.client.ClientForgeEvents;
import com.eerussianguy.ez_supervisor.client.ClientModEvents;
import com.eerussianguy.ez_supervisor.common.ForgeEvents;
import com.eerussianguy.ez_supervisor.common.data.BiomeSpawn;
import com.eerussianguy.ez_supervisor.common.data.SpawnRestriction;
import com.eerussianguy.ez_supervisor.common.data.SpawnRestrictionTypes;
import com.eerussianguy.ez_supervisor.compat.TFCIntegration;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import net.dries007.tfc.util.JsonHelpers;

@Mod(EZSupervisor.MOD_ID)
public class EZSupervisor
{
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final String MOD_ID = "ez_supervisor";

    @Nullable public static File configDir = null;
    @Nullable public static List<BiomeSpawn> spawns = null;
    @Nullable public static Map<EntityType<?>, SpawnRestriction> restrictions = null;
    @Nullable public static Map<String, SpawnRestriction> ruleSets = null;
    public static boolean tfc = false;

    public EZSupervisor()
    {
        if (FMLEnvironment.dist == Dist.CLIENT)
        {
            ClientForgeEvents.init();
            ClientModEvents.init();
        }
        ForgeEvents.init();

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
        spawns = BiomeSpawn.readAll(readJsonArray(configDir, "spawn"));
        ruleSets = SpawnRestriction.readRuleSets(readJsonObject(configDir, "rule_set"));
        restrictions = SpawnRestriction.readAll(readJsonArray(configDir, "spawn_restriction"));

        if (spawns == null) throw new RuntimeException("JSON Parsing error loading spawn.json");
        // noinspection ConstantConditions
        if (restrictions == null) throw new RuntimeException("JSON Parsing error loading spawn_restriction.json");
        if (ruleSets == null) throw new RuntimeException("JSON Parsing error loading rule_set.json");

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

    public static JsonObject readJsonObject(File parent, String name)
    {
        return readJson(parent, name, GsonHelper::parse, new JsonObject());
    }

    public static JsonArray readJsonArray(File parent, String name)
    {
        return readJson(parent, name, GsonHelper::parseArray, new JsonArray());
    }

    public static <T> T readJson(File parent, String name, Function<Reader, T> mapper, T defaultValue)
    {
        final File file = new File(parent, name + ".json");
        try
        {
            if (file.createNewFile())
            {
                LOGGER.debug("Created config file named: " + name + ".json");
                return defaultValue;
            }
            else
            {
                try (InputStream stream = new FileInputStream(file))
                {
                    try (Reader reader = new InputStreamReader(stream))
                    {
                        return mapper.apply(reader);
                    }
                    catch (IOException e)
                    {
                        throw new RuntimeException("Failed to read config called: " + name + ".json", e);
                    }
                }
                catch (IOException e)
                {
                    throw new RuntimeException("Failed to read config called: " + name + ".json", e);
                }
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to read config called: " + name + ".json", e);
        }
    }

    public static <T> List<T> mapArray(JsonArray array, Function<JsonElement, T> mapper)
    {
        final List<T> data = new ArrayList<>(array.size());
        array.forEach(e -> data.add(mapper.apply(e)));
        return data;
    }

    public static <K, V> Map<K, V> mapArrayMap(JsonArray array, Function<JsonElement, V> valueMapper, Function<JsonElement, K> keyMapper)
    {
        final Map<K, V> map = new HashMap<>();
        array.forEach(e -> map.put(keyMapper.apply(e), valueMapper.apply(e)));
        return map;
    }

    public static List<EntityType<?>> getAsEntityList(JsonObject json)
    {
        return json.get("entity").isJsonPrimitive() ? List.of(EZSupervisor.getAsEntity(json)) : EZSupervisor.mapArray(json.getAsJsonArray("entity"), e -> EZSupervisor.getAsEntity(e.getAsString()));
    }

    public static EntityType<?> getAsEntity(JsonObject json)
    {
        return Objects.requireNonNull(ForgeRegistries.ENTITIES.getValue(new ResourceLocation(GsonHelper.getAsString(json, "entity"))));
    }

    public static EntityType<?> getAsEntity(String name)
    {
        return Objects.requireNonNull(ForgeRegistries.ENTITIES.getValue(new ResourceLocation(name)));
    }

    public static <E extends Enum<E>> E getEnum(JsonObject obj, String key, Class<E> enumClass, E defaultValue)
    {
        if (obj.has(key))
        {
            return getEnum(obj.get(key), enumClass);
        }
        return defaultValue;
    }

    public static <E extends Enum<E>> E getEnum(JsonElement json, Class<E> enumClass)
    {
        final String enumName = JsonHelpers.convertToString(json, enumClass.getSimpleName());
        try
        {
            return Enum.valueOf(enumClass, enumName.toUpperCase(Locale.ROOT));
        }
        catch (IllegalArgumentException e)
        {
            throw new JsonParseException("No " + enumClass.getSimpleName() + " named: " + enumName);
        }
    }

    public static ResourceLocation identifier(String path)
    {
        return new ResourceLocation(EZSupervisor.MOD_ID, path);
    }

}
