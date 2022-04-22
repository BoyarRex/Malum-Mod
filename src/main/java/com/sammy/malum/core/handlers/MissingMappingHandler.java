package com.sammy.malum.core.handlers;

import com.sammy.malum.core.helper.DataHelper;
import com.sammy.malum.core.setup.content.block.BlockRegistry;
import com.sammy.malum.core.setup.content.item.ItemRegistry;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.RegistryEvent;

public class MissingMappingHandler {

    public static void correctMissingItemMappings(RegistryEvent.MissingMappings<Item> event) {
        for (RegistryEvent.MissingMappings.Mapping<Item> mapping : event.getAllMappings()) {
            if (mapping.key.equals(DataHelper.prefix("brilliance_cluster"))) {
                mapping.remap(ItemRegistry.CLUSTER_OF_BRILLIANCE.get());
            }
            if (mapping.key.equals(DataHelper.prefix("brilliance_chunk"))) {
                mapping.remap(ItemRegistry.CHUNK_OF_BRILLIANCE.get());
            }
            if (mapping.key.equals(DataHelper.prefix("soulstone_cluster"))) {
                mapping.remap(ItemRegistry.RAW_SOULSTONE.get());
            }
            if (mapping.key.equals(DataHelper.prefix("tainted_rock_pillar_cap"))) {
                mapping.remap(ItemRegistry.TAINTED_ROCK_COLUMN_CAP.get());
            }
            if (mapping.key.equals(DataHelper.prefix("tainted_rock_pillar"))) {
                mapping.remap(ItemRegistry.TAINTED_ROCK_COLUMN.get());
            }
            if (mapping.key.equals(DataHelper.prefix("twisted_rock_pillar_cap"))) {
                mapping.remap(ItemRegistry.TWISTED_ROCK_COLUMN_CAP.get());
            }
            if (mapping.key.equals(DataHelper.prefix("twisted_rock_pillar"))) {
                mapping.remap(ItemRegistry.TWISTED_ROCK_COLUMN.get());
            }
        }
    }

    public static void correctMissingBlockMappings(RegistryEvent.MissingMappings<Block> event) {
        for (RegistryEvent.MissingMappings.Mapping<Block> mapping : event.getAllMappings()) {
            if (mapping.key.equals(DataHelper.prefix("tainted_rock_pillar_cap"))) {
                mapping.remap(BlockRegistry.TAINTED_ROCK_COLUMN_CAP.get());
            }
            if (mapping.key.equals(DataHelper.prefix("tainted_rock_pillar"))) {
                mapping.remap(BlockRegistry.TAINTED_ROCK_COLUMN.get());
            }
            if (mapping.key.equals(DataHelper.prefix("twisted_rock_pillar_cap"))) {
                mapping.remap(BlockRegistry.TWISTED_ROCK_COLUMN_CAP.get());
            }
            if (mapping.key.equals(DataHelper.prefix("twisted_rock_pillar"))) {
                mapping.remap(BlockRegistry.TWISTED_ROCK_COLUMN.get());
            }
        }
    }
}