package com.eerussianguy.ez_supervisor.common.data;

import java.util.List;
import com.eerussianguy.ez_supervisor.common.ParsingUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;

public record LootFilter(Ingredient ingredient, Item output, float outputMultiplier)
{
    public static List<LootFilter> readAll(JsonArray array)
    {
        return ParsingUtils.mapArray(array, e -> create(e.getAsJsonObject()));
    }

    public static LootFilter create(JsonObject json)
    {
        final JsonElement ingredientJson = json.get("ingredient");
        final Ingredient ingredient = ingredientJson.isJsonPrimitive() ? Ingredient.of(ParsingUtils.getAsItem(json, "ingredient")) : Ingredient.fromJson(ingredientJson);
        final Item outputItem = ParsingUtils.getAsItem(json, "output");
        final float outputMultiplier = GsonHelper.getAsFloat(json, "output_count", 1f);
        return new LootFilter(ingredient, outputItem, outputMultiplier);
    }

}
