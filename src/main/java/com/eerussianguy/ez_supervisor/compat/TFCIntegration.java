package com.eerussianguy.ez_supervisor.compat;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.eerussianguy.ez_supervisor.common.ParsingUtils;
import com.eerussianguy.ez_supervisor.common.data.SpawnPredicate;
import com.eerussianguy.ez_supervisor.common.data.SpawnRestrictionType;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.ChunkPos;

import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.util.JsonHelpers;
import net.dries007.tfc.util.calendar.Calendars;
import net.dries007.tfc.util.calendar.Season;
import net.dries007.tfc.world.TFCChunkGenerator;
import net.dries007.tfc.world.chunkdata.ChunkData;
import net.dries007.tfc.world.chunkdata.ChunkDataProvider;
import net.dries007.tfc.world.chunkdata.ForestType;

@SuppressWarnings("unused")
public class TFCIntegration
{
    public static void init() {}

    public static final SpawnRestrictionType CLIMATE = register("climate", TFCIntegration::getClimate);
    public static final SpawnRestrictionType SEASON = register("season", TFCIntegration::getSeason);

    public static SpawnPredicate getClimate(JsonObject json)
    {
        final ClimatePlacement placement = JsonHelpers.decodeCodec(json, ClimatePlacement.CODEC, "climate");
        final String dim = GsonHelper.getAsString(json, "dimension", "minecraft:overworld");
        return ((entity, level, type, pos, random) -> {
            if (!(level.getLevel().getChunkSource().getGenerator() instanceof TFCChunkGenerator))
            {
                return true;
            }
            final ChunkData data = level instanceof WorldGenRegion region
                ? ChunkDataProvider.get(region).get(new ChunkPos(pos))
                : ChunkData.get(level, pos);
            return placement.isValid(data, pos, random);
        });
    }

    public static SpawnPredicate getSeason(JsonObject json)
    {
        final JsonElement el = json.get("season");
        final List<Season> list = el.isJsonPrimitive() ? List.of(JsonHelpers.getEnum(el, Season.class)) : ParsingUtils.mapArray(el.getAsJsonArray(), e -> JsonHelpers.getEnum(e, Season.class));
        final Set<Season> set = list.stream().collect(Collectors.toUnmodifiableSet());
        return ((entity, level, type, pos, random) -> set.contains(Calendars.get(level).getCalendarMonthOfYear().getSeason()));
    }

    private static SpawnRestrictionType register(String id, Function<JsonObject, SpawnPredicate> deserializer)
    {
        return SpawnRestrictionType.register(Helpers.identifier(id), new SpawnRestrictionType(deserializer));
    }

    /**
     * TFC Class, licensed under EUPL v1.2. Copied for stupid obf reasons.
     */
    public static class ClimatePlacement
    {
        public static final Codec<ClimatePlacement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.optionalFieldOf("min_temperature", -Float.MAX_VALUE).forGetter(c -> c.minTemp),
            Codec.FLOAT.optionalFieldOf("max_temperature", Float.MAX_VALUE).forGetter(c -> c.maxTemp),
            Codec.FLOAT.optionalFieldOf("min_rainfall", -Float.MAX_VALUE).forGetter(c -> c.minRainfall),
            Codec.FLOAT.optionalFieldOf("max_rainfall", Float.MAX_VALUE).forGetter(c -> c.maxRainfall),
            ForestType.CODEC.optionalFieldOf("min_forest", ForestType.NONE).forGetter(c -> c.minForest),
            ForestType.CODEC.optionalFieldOf("max_forest", ForestType.OLD_GROWTH).forGetter(c -> c.maxForest),
            Codec.BOOL.optionalFieldOf("fuzzy", false).forGetter(c -> c.fuzzy)
        ).apply(instance, ClimatePlacement::new));

        private final float minTemp;
        private final float maxTemp;
        private final float targetTemp;
        private final float minRainfall;
        private final float maxRainfall;
        private final float targetRainfall;
        private final ForestType minForest;
        private final ForestType maxForest;
        private final boolean fuzzy;

        public ClimatePlacement(float minTemp, float maxTemp, float minRainfall, float maxRainfall, ForestType minForest, ForestType maxForest, boolean fuzzy)
        {
            this.minTemp = minTemp;
            this.maxTemp = maxTemp;
            this.targetTemp = (minTemp + maxTemp) / 2f;
            this.minRainfall = minRainfall;
            this.maxRainfall = maxRainfall;
            this.targetRainfall = (minRainfall + maxRainfall) / 2f;
            this.minForest = minForest;
            this.maxForest = maxForest;
            this.fuzzy = fuzzy;
        }

        public boolean isValid(ChunkData data, BlockPos pos, Random random)
        {
            final float temperature = data.getAverageTemp(pos);
            final float rainfall = data.getRainfall(pos);
            final ForestType forestType = data.getForestType();

            if (minTemp <= temperature && temperature <= maxTemp && minRainfall <= rainfall && rainfall <= maxRainfall && minForest.ordinal() <= forestType.ordinal() && forestType.ordinal() <= maxForest.ordinal())
            {
                if (fuzzy)
                {
                    float normTempDelta = Math.abs(temperature - targetTemp) / (maxTemp - minTemp);
                    float normRainfallDelta = Math.abs(rainfall - targetRainfall) / (maxRainfall - minRainfall);
                    return random.nextFloat() * random.nextFloat() > Math.max(normTempDelta, normRainfallDelta);
                }
                return true;
            }
            return false;
        }
    }
}
