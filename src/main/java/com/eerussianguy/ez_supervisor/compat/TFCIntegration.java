package com.eerussianguy.ez_supervisor.compat;

import java.util.function.Function;
import com.eerussianguy.ez_supervisor.common.data.SpawnPredicate;
import com.eerussianguy.ez_supervisor.common.data.SpawnRestrictionType;
import com.google.gson.JsonObject;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;

import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.util.JsonHelpers;
import net.dries007.tfc.util.calendar.Calendars;
import net.dries007.tfc.util.calendar.Season;
import net.dries007.tfc.world.chunkdata.ChunkData;
import net.dries007.tfc.world.chunkdata.ChunkDataProvider;
import net.dries007.tfc.world.placement.ClimatePlacement;

@SuppressWarnings("unused")
public class TFCIntegration
{
    public static void init() {}

    public static final SpawnRestrictionType CLIMATE = register("climate", TFCIntegration::getClimate);
    public static final SpawnRestrictionType SEASON = register("season", TFCIntegration::getSeason);

    public static SpawnPredicate getClimate(JsonObject json)
    {
        final ClimatePlacement placement = JsonHelpers.decodeCodec(json, ClimatePlacement.CODEC, "climate");
        return ((entity, level, type, pos, random) -> {
            final ChunkData data = level instanceof WorldGenRegion region
                ? ChunkDataProvider.get(region).get(new ChunkPos(pos))
                : ChunkData.get(level, pos);
            return placement.isValid(data, pos, random);
        });
    }

    public static SpawnPredicate getSeason(JsonObject json)
    {
        final Season season = JsonHelpers.getEnum(json.get("season"), Season.class);
        return ((entity, level, type, pos, random) -> Calendars.get(level).getCalendarMonthOfYear().getSeason() == season);
    }

    private static SpawnRestrictionType register(String id, Function<JsonObject, SpawnPredicate> deserializer)
    {
        return SpawnRestrictionType.register(Helpers.identifier(id), new SpawnRestrictionType(deserializer));
    }
}
