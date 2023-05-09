package com.eerussianguy.ez_supervisor.common.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.eerussianguy.ez_supervisor.common.ParsingUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.EntityType;

public record SpawnRestriction(List<SpawnPredicate> predicates, boolean wipeOriginal)
{
    public static Map<EntityType<?>, SpawnRestriction> readAll(JsonArray array)
    {
        return ParsingUtils.mapArrayMap(array, e -> create(e.getAsJsonObject()), e -> ParsingUtils.getAsEntity(e.getAsJsonObject()));
    }

    public static Map<String, SpawnRestriction> readRuleSets(JsonObject json)
    {
        final Map<String, SpawnRestriction> map = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : json.entrySet())
        {
            map.put(entry.getKey(), SpawnRestriction.create(entry.getValue().getAsJsonObject()));
        }
        return map;
    }

    public static List<SpawnPredicate> readPredicatesList(JsonObject json)
    {
        final List<SpawnPredicate> list = new ArrayList<>();
        final JsonArray predicates = GsonHelper.getAsJsonArray(json, "predicates");
        for (JsonElement element : predicates)
        {
            final JsonObject predicateJson = element.getAsJsonObject();
            final ResourceLocation id = new ResourceLocation(GsonHelper.getAsString(predicateJson, "type"));
            final SpawnRestrictionType type = SpawnRestrictionType.getValueOrThrow(id);
            list.add(type.deserializer().apply(predicateJson));
        }
        return list;
    }

    public static SpawnRestriction create(JsonObject json)
    {
        final boolean wipe = GsonHelper.getAsBoolean(json, "overwrite_mod_predicate", false);
        return new SpawnRestriction(readPredicatesList(json), wipe);
    }
}
