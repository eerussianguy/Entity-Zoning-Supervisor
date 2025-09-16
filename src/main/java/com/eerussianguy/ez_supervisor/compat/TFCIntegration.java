package com.eerussianguy.ez_supervisor.compat;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.eerussianguy.ez_supervisor.common.ParsingUtils;
import com.eerussianguy.ez_supervisor.common.data.SpawnPredicate;
import com.eerussianguy.ez_supervisor.common.data.SpawnRestrictionType;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

import net.dries007.tfc.client.overworld.SolarCalculator;
import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.util.calendar.Calendars;
import net.dries007.tfc.util.calendar.Season;
import net.dries007.tfc.world.TFCChunkGenerator;
import net.dries007.tfc.world.chunkdata.ChunkData;
import net.dries007.tfc.world.placement.ClimatePlacement;

@SuppressWarnings("unused")
public class TFCIntegration
{
    public static void init() {}

    public static final SpawnRestrictionType CLIMATE = register("climate", TFCIntegration::getClimate);
    public static final SpawnRestrictionType SEASON = register("season", TFCIntegration::getSeason);

    public static SpawnPredicate getClimate(JsonObject json)
    {
        final ClimatePlacement placement = ParsingUtils.decodeCodec(ClimatePlacement.CODEC.codec(), json.get("climate"));
        return ((entity, level, type, pos, random) -> {
            if (!(level.getLevel().getChunkSource().getGenerator() instanceof TFCChunkGenerator))
            {
                return true;
            }
            return placement.isValid(ChunkData.get(level, pos), pos, random, SolarCalculator.getInNorthernHemisphere(pos, level.getLevel()));
        });
    }

    public static final Codec<Season> SEASON_CODEC = StringRepresentable.fromEnum(Season::values);

    public static SpawnPredicate getSeason(JsonObject json)
    {
        final JsonElement el = json.get("season");
        final List<Season> list = el.isJsonPrimitive() ? List.of(ParsingUtils.decodeCodec(SEASON_CODEC, el)) : ParsingUtils.mapArray(el.getAsJsonArray(), e -> ParsingUtils.decodeCodec(SEASON_CODEC, e));
        final Set<Season> set = list.stream().collect(Collectors.toUnmodifiableSet());
        return ((entity, level, type, pos, random) -> set.contains(Calendars.get(level).getHemispheralCalendarMonthOfYear(SolarCalculator.getInNorthernHemisphere(pos, level.getLevel())).getSeason()));
    }

    private static SpawnRestrictionType register(String id, Function<JsonObject, SpawnPredicate> deserializer)
    {
        return SpawnRestrictionType.register(Helpers.identifier(id), new SpawnRestrictionType(deserializer));
    }

}
