package com.eerussianguy.ez_supervisor.common;

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
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import net.dries007.tfc.util.JsonHelpers;

public class ParsingUtils
{
    private static final Logger LOGGER = LogUtils.getLogger();

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
        return json.get("entity").isJsonPrimitive() ? List.of(getAsEntity(json)) : mapArray(json.getAsJsonArray("entity"), e -> getAsEntity(e.getAsString()));
    }

    public static EntityType<?> getAsEntity(JsonObject json)
    {
        return Objects.requireNonNull(ForgeRegistries.ENTITIES.getValue(new ResourceLocation(GsonHelper.getAsString(json, "entity"))));
    }

    public static EntityType<?> getAsEntity(String name)
    {
        return Objects.requireNonNull(ForgeRegistries.ENTITIES.getValue(new ResourceLocation(name)));
    }

    public static Item getAsItem(JsonObject json, String key)
    {
        final String str = GsonHelper.getAsString(json, key);
        final Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(str));
        if (item == null || item == Items.AIR)
        {
            throw new JsonParseException("Invalid item string passed to loot filter: " + str);
        }
        return item;
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
}
