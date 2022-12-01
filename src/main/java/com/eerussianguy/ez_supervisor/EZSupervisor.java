package com.eerussianguy.ez_supervisor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import com.eerussianguy.ez_supervisor.client.ClientForgeEvents;
import com.eerussianguy.ez_supervisor.client.ClientModEvents;
import com.eerussianguy.ez_supervisor.common.ForgeEvents;
import com.eerussianguy.ez_supervisor.common.data.BiomeSpawn;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

@Mod(EZSupervisor.MOD_ID)
public class EZSupervisor
{
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final String MOD_ID = "ez_supervisor";

    @Nullable public static File configDir = null;
    @Nullable public static List<BiomeSpawn> spawns = null;

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
        createConfigFiles();
    }

    public static void createConfigFiles()
    {
        configDir = getConfigDir();
        spawns = BiomeSpawn.readAll(readJsonArray(configDir, "spawn"));
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
                        LOGGER.error("Failed reading config file named: " + name + ".json");
                        throw new RuntimeException("Failed to read config called: " + name + ".json", e);
                    }
                }
                catch (IOException e)
                {
                    LOGGER.error("Failed reading config file named: " + name + ".json");
                    throw new RuntimeException("Failed to read config called: " + name + ".json", e);
                }
            }
        }
        catch (IOException e)
        {
            LOGGER.error("Failed reading config file named: " + name + ".json");
            throw new RuntimeException("Failed to read config called: " + name + ".json", e);
        }
    }

    public static <T> List<T> mapArray(JsonArray array, Function<JsonElement, T> mapper)
    {
        final List<T> data = new ArrayList<>(array.size());
        array.forEach(e -> data.add(mapper.apply(e)));
        return data;
    }

    public static ResourceLocation identifier(String path)
    {
        return new ResourceLocation(EZSupervisor.MOD_ID, path);
    }

}
