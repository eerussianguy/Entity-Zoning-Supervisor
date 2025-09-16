package com.eerussianguy.ez_supervisor.common.data;

import java.util.List;
import com.eerussianguy.ez_supervisor.common.ParsingUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.Nullable;

public record LootFilter(List<EntityType<?>> entities, @Nullable Ingredient ingredient, Item output, float outputMultiplier, boolean killedByPlayer)
{
    public static List<LootFilter> readAll(JsonArray array)
    {
        return ParsingUtils.mapArray(array, e -> create(e.getAsJsonObject()));
    }

    public static LootFilter create(JsonObject json)
    {
        final List<EntityType<?>> type = json.has("entity") ? ParsingUtils.getAsEntityList(json) : List.of();
        final Ingredient ingredient;
        if (json.has("ingredient"))
        {
            final JsonElement ingredientJson = json.get("ingredient");
            ingredient = ingredientJson.isJsonPrimitive() ? Ingredient.of(ParsingUtils.getAsItem(json, "ingredient")) : Ingredient.CODEC.decode(JsonOps.INSTANCE, ingredientJson).getOrThrow().getFirst();
        }
        else
        {
            ingredient = null;
        }
        final Item outputItem = ParsingUtils.getAsItem(json, "output", Items.AIR);
        final float outputMultiplier = GsonHelper.getAsFloat(json, "multiplier", 1f);
        final boolean killedByPlayer = GsonHelper.getAsBoolean(json, "killed_by_player", false);
        return new LootFilter(type, ingredient, outputItem, outputMultiplier, killedByPlayer);
    }

}
