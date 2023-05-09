package com.eerussianguy.ez_supervisor.common.data;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import com.eerussianguy.ez_supervisor.EZSupervisor;
import com.eerussianguy.ez_supervisor.common.ParsingUtils;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import static com.eerussianguy.ez_supervisor.common.ParsingUtils.*;

@SuppressWarnings("unused")
public class SpawnRestrictionTypes
{
    // EZS
    public static final SpawnRestrictionType DISTANCE_BELOW_SEA_LEVEL = register("distance_below_sea_level", SpawnRestrictionTypes::getDistanceBelowSeaLevel);
    public static final SpawnRestrictionType SOLID_GROUND = register("solid_ground", SpawnRestrictionTypes::getSolidGround);
    public static final SpawnRestrictionType BRIGHTNESS = register("brightness", SpawnRestrictionTypes::getBrightness);
    public static final SpawnRestrictionType NEVER = register("never", SpawnRestrictionTypes::getNope);
    public static final SpawnRestrictionType DAY_COUNT = register("day_count", SpawnRestrictionTypes::getDayCount);
    public static final SpawnRestrictionType DIFFICULTY = register("difficulty", SpawnRestrictionTypes::getDifficulty);
    public static final SpawnRestrictionType DAY_TIME = register("day_time", SpawnRestrictionTypes::getDayTime);
    public static final SpawnRestrictionType PLAYER_DISTANCE = register("player_distance", SpawnRestrictionTypes::getPlayerDistance);
    public static final SpawnRestrictionType SEE_SKY = register("see_sky", SpawnRestrictionTypes::getSeeSky);
    public static final SpawnRestrictionType TAG = register("tag", SpawnRestrictionTypes::getTag);
    public static final SpawnRestrictionType BLOCK = register("block", SpawnRestrictionTypes::getBlock);
    public static final SpawnRestrictionType FLUID = register("fluid", SpawnRestrictionTypes::getFluid);
    public static final SpawnRestrictionType MOD_LOADED = register("mod_loaded", SpawnRestrictionTypes::getModLoaded);
    public static final SpawnRestrictionType SLIME_CHUNK = register("slime_chunk", SpawnRestrictionTypes::getSlimeChunk);
    public static final SpawnRestrictionType HEIGHT_FILTER = register("height_filter", SpawnRestrictionTypes::getHeightFilter);
    public static final SpawnRestrictionType COUNT_NEARBY = register("count_nearby", SpawnRestrictionTypes::getCountNearby);
    public static final SpawnRestrictionType RANDOM = register("random", SpawnRestrictionTypes::getRandom);
    public static final SpawnRestrictionType SPAWN_TYPE = register("spawn_type", SpawnRestrictionTypes::getSpawnType);
    public static final SpawnRestrictionType RULE_SET = register("rule_set", SpawnRestrictionTypes::getRuleSet);
    public static final SpawnRestrictionType BIOME_TAG = register("biome_tag", SpawnRestrictionTypes::getBiomeTag);
    public static final SpawnRestrictionType OR = register("or", SpawnRestrictionTypes::getOr);
    public static final SpawnRestrictionType AND = register("and", SpawnRestrictionTypes::getOr);
    public static final SpawnRestrictionType COPY = register("copy", SpawnRestrictionTypes::getCopy);

    // Vanilla
    public static final SpawnRestrictionType MOB = register("mob", SpawnRestrictionTypes::getMob, true);
    public static final SpawnRestrictionType MONSTER = register("monster", SpawnRestrictionTypes::getMonster, true);
    public static final SpawnRestrictionType ANIMAL = register("animal", SpawnRestrictionTypes::getAnimal, true);

    public static void init() {}

    public static SpawnPredicate getOr(JsonObject json)
    {
        final List<SpawnPredicate> predicates = SpawnRestriction.readPredicatesList(json);
        return (entity, level, type, pos, random) -> {
            for (SpawnPredicate predicate : predicates)
            {
                if (predicate.test(entity, level, type, pos, random))
                {
                    return true;
                }
            }
            return false;
        };
    }

    public static SpawnPredicate getAnd(JsonObject json)
    {
        final List<SpawnPredicate> predicates = SpawnRestriction.readPredicatesList(json);
        return (entity, level, type, pos, random) -> {
            for (SpawnPredicate predicate : predicates)
            {
                if (!predicate.test(entity, level, type, pos, random))
                {
                    return false;
                }
            }
            return true;
        };
    }

    public static SpawnPredicate getCopy(JsonObject json)
    {
        final EntityType<?> toCopy = ParsingUtils.getAsEntity(json);
        return (entity, level, type, pos, random) -> {
            for (SpawnPredicate predicate : Objects.requireNonNull(EZSupervisor.restrictions).get(toCopy).predicates())
            {
                if (!predicate.test(entity, level, type, pos, random))
                {
                    return false;
                }
            }
            return true;
        };
    }

    public static SpawnPredicate getBiomeTag(JsonObject json)
    {
        final TagKey<Biome> tag = TagKey.create(ForgeRegistries.Keys.BIOMES, new ResourceLocation(GsonHelper.getAsString(json, "tag")));
        return (entity, level, type, pos, random) -> level.getBiome(pos).is(tag);
    }

    public static SpawnPredicate getRuleSet(JsonObject json)
    {
        final SpawnRestriction restriction = Objects.requireNonNull(EZSupervisor.ruleSets).get(GsonHelper.getAsString(json, "rule"));
        return (entity, level, type, pos, random) -> restriction.predicates().stream().allMatch(p -> p.test(entity, level, type, pos, random));
    }

    public static SpawnPredicate getSpawnType(JsonObject json)
    {
        final MobSpawnType reason = ParsingUtils.getEnum(json.get("reason"), MobSpawnType.class);
        return (entity, level, type, pos, random) -> type == reason;
    }

    public static SpawnPredicate getFluid(JsonObject json)
    {
        final Fluid fluid = ForgeRegistries.FLUIDS.getValue(new ResourceLocation(GsonHelper.getAsString(json, "fluid")));
        if (fluid == null) throw new JsonParseException("Could not find fluid: " + GsonHelper.getAsJsonArray(json, "fluid"));
        return (entity, level, type, pos, random) -> level.getBlockState(pos).getFluidState().is(fluid);
    }

    public static SpawnPredicate getRandom(JsonObject json)
    {
        final float chance = GsonHelper.getAsFloat(json, "chance");
        return (entity, level, type, pos, random) -> random.nextFloat() < chance;
    }

    public static SpawnPredicate getCountNearby(JsonObject json)
    {
        final int distance = GsonHelper.getAsInt(json, "distance");
        final int count = GsonHelper.getAsInt(json, "max_count");
        return (entity, level, type, pos, random) -> level.getEntities(entity, entity.getBoundingBox().inflate(distance), e -> e.getType().equals(entity.getType())).size() < count;
    }

    public static SpawnPredicate getHeightFilter(JsonObject json)
    {
        final int min = GsonHelper.getAsInt(json, "min", -1);
        final int max = GsonHelper.getAsInt(json, "max", -1);
        return (entity, level, type, pos, random) -> (min == -1 || pos.getY() > min) && (max == -1 || pos.getY() < max);
    }

    public static SpawnPredicate getSlimeChunk(JsonObject json)
    {
        final int rarity = GsonHelper.getAsInt(json, "rarity", 10);
        return (entity, level, type, pos, random) -> {
            final ChunkPos chunkpos = new ChunkPos(pos);
            return WorldgenRandom.seedSlimeChunk(chunkpos.x, chunkpos.z, level.getLevel().getSeed(), 987234911L).nextInt(rarity) == 0;
        };
    }

    public static SpawnPredicate getModLoaded(JsonObject json)
    {
        final String modid = GsonHelper.getAsString(json, "mod_id");
        return (entity, level, type, pos, random) -> ModList.get().isLoaded(modid);
    }

    public static SpawnPredicate getBlock(JsonObject json)
    {
        final Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(GsonHelper.getAsString(json, "block")));
        if (block == null || block == Blocks.AIR)
        {
            throw new JsonParseException("Air or unknown block passed to block predicate");
        }
        return (entity, level, type, pos, random) -> level.getBlockState(pos.below()).is(block);
    }

    public static SpawnPredicate getTag(JsonObject json)
    {
        final TagKey<Block> tag = TagKey.create(ForgeRegistries.Keys.BLOCKS, new ResourceLocation(GsonHelper.getAsString(json, "tag")));
        return (entity, level, type, pos, random) -> level.getBlockState(pos.below()).is(tag);
    }

    public static SpawnPredicate getSeeSky(JsonObject json)
    {
        final boolean seeing = GsonHelper.getAsBoolean(json, "can_see");
        return (entity, level, type, pos, random) -> level.canSeeSky(pos) == seeing;
    }

    public static SpawnPredicate getPlayerDistance(JsonObject json)
    {
        final int min = GsonHelper.getAsInt(json, "min", 0);
        final int max = GsonHelper.getAsInt(json, "max", 64);
        return (entity, level, type, pos, random) -> {
            final Player player = level.getNearestPlayer(entity, max);
            if (player != null && !player.isSpectator())
            {
                final double distSqr = player.distanceToSqr(entity);
                return distSqr > min * min && distSqr < max * max;
            }
            return false;
        };
    }

    public static SpawnPredicate getDayTime(JsonObject json)
    {
        final int minTicks = GsonHelper.getAsInt(json, "min", 0);
        final int maxTicks = GsonHelper.getAsInt(json, "max", 24000);
        return ((entity, level, type, pos, random) -> {
            final long current = level.dayTime() % 24000L;
            return current >= minTicks && current <= maxTicks;
        });
    }

    public static SpawnPredicate getDifficulty(JsonObject json)
    {
        final int min = ParsingUtils.getEnum(json, "min", Difficulty.class, Difficulty.PEACEFUL).ordinal();
        final int max = ParsingUtils.getEnum(json, "max", Difficulty.class, Difficulty.HARD).ordinal();
        return ((entity, level, type, pos, random) -> level.getDifficulty().ordinal() >= min && level.getDifficulty().ordinal() <= max);
    }

    public static SpawnPredicate getDayCount(JsonObject json)
    {
        final int minDays = GsonHelper.getAsInt(json, "min", -1);
        final int maxDays = GsonHelper.getAsInt(json, "max", -1);
        final int everyNth = GsonHelper.getAsInt(json, "every_n_days", -1);
        return ((entity, level, type, pos, random) -> {
            final long passed = level.dayTime() / 24000L;
            return ((minDays == -1 || passed > minDays) && (maxDays == -1 || passed < maxDays)) && (everyNth == -1 || passed % everyNth == 0);
        });
    }

    public static SpawnPredicate getNope(JsonObject json)
    {
        return ((entity, level, type, pos, random) -> false);
    }

    @SuppressWarnings("unchecked")
    public static SpawnPredicate getMonster(JsonObject json)
    {
        final boolean anyLight = GsonHelper.getAsBoolean(json, "any_light", false);
        return (entity, level, type, pos, rand) ->{
            return entity instanceof Monster && anyLight
                ? Monster.checkAnyLightMonsterSpawnRules((EntityType<? extends Monster>) entity.getType(), level, type, pos, rand)
                : Monster.checkMonsterSpawnRules((EntityType<? extends Monster>) entity.getType(), level, type, pos, rand);
        };
    }


    @SuppressWarnings("unchecked")
    public static SpawnPredicate getAnimal(JsonObject json)
    {
        return (entity, level, type, pos, rand) -> entity instanceof Animal && Animal.checkAnimalSpawnRules((EntityType<? extends Animal>) entity.getType(), level, type, pos, rand);
    }

    @SuppressWarnings("unchecked")
    public static SpawnPredicate getMob(JsonObject json)
    {
        return (entity, level, type, pos, rand) -> entity instanceof Mob &&  Mob.checkMobSpawnRules((EntityType<? extends Mob>) entity.getType(), level, type, pos, rand);
    }

    public static SpawnPredicate getBrightness(JsonObject json)
    {
        final int max = GsonHelper.getAsInt(json, "max", -1);
        final int min = GsonHelper.getAsInt(json, "min", -1);
        if (max == -1 && min == -1)
        {
            throw new JsonSyntaxException("Failed to parse brightness json, must specify max or min");
        }
        return (entity, level, type, pos, rand) -> {
            final int brightness = level.getRawBrightness(pos, 0);
            return (max == -1 || brightness <= max) && (min == -1 && brightness >= min);
        };
    }

    public static SpawnPredicate getSolidGround(JsonObject json)
    {
        return (entity, level, type, pos, rand) -> level.getBlockState(pos.below()).is(BlockTags.VALID_SPAWN);
    }

    public static SpawnPredicate getDistanceBelowSeaLevel(JsonObject json)
    {
        final int distance = GsonHelper.getAsInt(json, "distance");
        return (entity, level, type, pos, rand) -> pos.getY() < level.getLevel().getChunkSource().getGenerator().getSeaLevel() - distance;
    }

    private static SpawnRestrictionType register(String id, Function<JsonObject, SpawnPredicate> deserializer)
    {
        return register(id, deserializer, false);
    }

    private static SpawnRestrictionType register(String id, Function<JsonObject, SpawnPredicate> deserializer, boolean vanilla)
    {
        return SpawnRestrictionType.register(vanilla ? new ResourceLocation(id) : EZSupervisor.identifier(id), new SpawnRestrictionType(deserializer));
    }

}
