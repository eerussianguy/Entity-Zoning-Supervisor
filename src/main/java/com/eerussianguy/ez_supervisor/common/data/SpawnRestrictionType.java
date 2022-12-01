package com.eerussianguy.ez_supervisor.common.data;

import java.util.Objects;
import java.util.function.Function;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

public record SpawnRestrictionType(Function<JsonObject, SpawnPredicate> deserializer)
{
    private static final BiMap<ResourceLocation, SpawnRestrictionType> REGISTRY = HashBiMap.create();

    public static synchronized SpawnRestrictionType register(ResourceLocation id, SpawnRestrictionType type)
    {
        if (REGISTRY.containsKey(id))
        {
            throw new IllegalArgumentException("Duplicate key: " + id);
        }
        REGISTRY.put(id, type);
        return type;
    }

    public static SpawnRestrictionType getValueOrThrow(ResourceLocation id)
    {
        return Objects.requireNonNull(REGISTRY.get(id));
    }

    public static ResourceLocation getId(SpawnRestrictionType type)
    {
        return REGISTRY.inverse().get(type);
    }

}
