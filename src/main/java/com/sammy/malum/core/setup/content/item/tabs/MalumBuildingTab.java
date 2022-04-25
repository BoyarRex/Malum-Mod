package com.sammy.malum.core.setup.content.item.tabs;

import com.sammy.malum.MalumMod;
import com.sammy.malum.core.setup.content.item.ItemRegistry;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;

public class MalumBuildingTab extends CreativeModeTab
{
    public static final MalumBuildingTab INSTANCE = new MalumBuildingTab();
    
    public MalumBuildingTab() {
        super(MalumMod.MALUM + "_shaped_stones");
    }
    
    @Nonnull
    @Override
    public ItemStack makeIcon() {
        return new ItemStack(ItemRegistry.TAINTED_ROCK.get());
    }
}
