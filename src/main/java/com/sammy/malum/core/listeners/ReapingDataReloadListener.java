package com.sammy.malum.core.listeners;

import com.google.gson.*;
import com.sammy.malum.MalumMod;
import com.sammy.malum.common.blockentity.storage.ItemStandBlockEntity;
import com.sammy.malum.core.helper.SpiritHelper;
import com.sammy.malum.core.systems.reaping.MalumReapingDropsData;
import com.sammy.malum.core.systems.recipe.SpiritWithCount;
import com.sammy.malum.core.systems.spirit.MalumEntitySpiritData;
import team.lodestar.lodestone.systems.recipe.IngredientWithCount;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.event.AddReloadListenerEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ReapingDataReloadListener extends SimpleJsonResourceReloadListener {
    public static Map<ResourceLocation, ArrayList<MalumReapingDropsData>> REAPING_DATA = new HashMap<>();
    private static final Gson GSON = (new GsonBuilder()).create();

    public ReapingDataReloadListener() {
        super(GSON, "reaping_data");
    }

    public static void register(AddReloadListenerEvent event) {
        event.addListener(new ReapingDataReloadListener());
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objectIn, ResourceManager resourceManagerIn, ProfilerFiller profilerIn) {
        REAPING_DATA.clear();
        for (int i = 0; i < objectIn.size(); i++) {
            ResourceLocation location = (ResourceLocation) objectIn.keySet().toArray()[i];
            JsonObject object = objectIn.get(location).getAsJsonObject();
            String name = object.getAsJsonPrimitive("registry_name").getAsString();
            ResourceLocation resourceLocation = new ResourceLocation(name);
            if (!Registry.ENTITY_TYPE.containsKey(resourceLocation)) {
                continue;
            }
            if (REAPING_DATA.containsKey(resourceLocation)) {
                MalumMod.LOGGER.info("entity with registry name: " + name + " already has reaping data associated with it. Overwriting.");
            }
            JsonArray drops = object.getAsJsonArray("drops");
            ArrayList<MalumReapingDropsData> dropsList = new ArrayList<>();
            for (JsonElement drop : drops) {
                JsonObject dropObject = drop.getAsJsonObject();
                if (!dropObject.has("ingredient")) {
                    MalumMod.LOGGER.info("entity with registry name: " + name + " lacks a reaping ingredient. Skipping drops entry.");
                    continue;
                }
                Ingredient dropIngredient = Ingredient.fromJson(dropObject.getAsJsonObject("ingredient"));
                float chance = dropObject.getAsJsonPrimitive("chance").getAsFloat();
                int min = dropObject.getAsJsonPrimitive("min").getAsInt();
                int max = dropObject.getAsJsonPrimitive("max").getAsInt();
                dropsList.add(new MalumReapingDropsData(dropIngredient, chance, min, max));
            }
            REAPING_DATA.put(resourceLocation, dropsList);
        }
    }
}