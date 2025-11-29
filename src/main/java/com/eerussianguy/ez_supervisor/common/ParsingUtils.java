package com.eerussianguy.ez_supervisor.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;

public class ParsingUtils
{
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().create();

    public static JsonObject readJsonObject(File parent, String name)
    {
        return readJson(parent, name, reader -> GsonHelper.fromJson(GSON, reader, JsonObject.class, true), new JsonObject());
    }

    public static JsonArray readJsonArray(File parent, String name)
    {
        return readJson(parent, name, reader -> GsonHelper.fromJson(GSON, reader, JsonArray.class, true), new JsonArray());
    }

    public static <T> T readJson(File parent, String name, Function<Reader, T> mapper, T defaultValue)
    {
        final File file = new File(parent, name + ".json");
        try
        {
            if (file.createNewFile())
            {
                try (PrintWriter stream = new PrintWriter(new FileOutputStream(file)))
                {
                    stream.print(defaultValue instanceof JsonObject ? "{}" : "[]");
                    stream.flush();
                }
                catch (IOException e)
                {
                    LOGGER.error("Error creating default file at + {}", name);
                }
                LOGGER.debug("Created config file named: {}.json", name);
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

    public static List<Holder<EntityType<?>>> getAsEntityList(JsonObject json)
    {
        return json.get("entity").isJsonPrimitive() ? List.of(getAsEntity(json)) : mapArray(json.getAsJsonArray("entity"), e -> getAsEntity(e.getAsString()));
    }

    public static Holder<EntityType<?>> getAsEntity(JsonObject json)
    {
        return BuiltInRegistries.ENTITY_TYPE
            .getHolder(ResourceLocation.parse(GsonHelper.getAsString(json, "entity")))
            .orElseThrow(() -> new JsonParseException("Entity type not found: " + GsonHelper.getAsString(json, "entity") + " in json " + json));
    }

    public static Holder<EntityType<?>> getAsEntity(String name)
    {
        return BuiltInRegistries.ENTITY_TYPE.getHolder(ResourceLocation.parse(name))
            .orElseThrow(() -> new JsonParseException("Entity type not found: " + name));
    }

    public static Item getAsItem(JsonObject json, String key, Item fallback)
    {
        return json.has(key) ? getAsItem(json, key) : fallback;
    }

    public static Item getAsItem(JsonObject json, String key)
    {
        final String str = GsonHelper.getAsString(json, key);
        final Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(str));
        if (item == Items.AIR)
        {
            throw new JsonParseException("Invalid item string passed to loot filter: " + str);
        }
        return item;
    }

    public static <T> T decodeCodec(Codec<T> codec, JsonElement json)
    {
        return codec.decode(JsonOps.INSTANCE, json).getOrThrow().getFirst();
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
        final String enumName = GsonHelper.convertToString(json, enumClass.getSimpleName());
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
