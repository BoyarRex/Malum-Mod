package com.sammy.malum.common.item.ether;

import com.sammy.malum.core.helper.ColorHelper;
import com.sammy.malum.core.setup.client.ScreenParticleRegistry;
import com.sammy.malum.core.systems.easing.Easing;
import com.sammy.malum.core.systems.rendering.particle.ParticleBuilders;
import com.sammy.malum.core.systems.rendering.particle.screen.base.ScreenParticle;
import com.sammy.malum.core.systems.rendering.particle.screen.emitter.ItemParticleEmitter;
import com.sammy.malum.core.systems.rendering.particle.screen.emitter.ParticleEmitter;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.awt.*;
import java.util.Random;

import static net.minecraft.util.Mth.nextFloat;

public abstract class AbstractEtherItem extends BlockItem implements DyeableLeatherItem, ItemParticleEmitter {
    public static final String firstColor = "firstColor";
    public static final String secondColor = "secondColor";
    public static final int defaultFirstColor = 15712278;
    public static final int defaultSecondColor = 4607909;

    public final boolean iridescent;

    public AbstractEtherItem(Block blockIn, Properties builder, boolean iridescent) {
        super(blockIn, builder);
        this.iridescent = iridescent;
    }

    public String colorLookup() {
        return iridescent ? secondColor : firstColor;
    }

    public int getSecondColor(ItemStack stack) {
        if (!iridescent) {
            return getFirstColor(stack);
        }
        CompoundTag tag = stack.getTagElement("display");

        return tag != null && tag.contains(secondColor, 99) ? tag.getInt(secondColor) : defaultSecondColor;
    }

    public void setSecondColor(ItemStack stack, int color) {
        stack.getOrCreateTagElement("display").putInt(secondColor, color);
    }

    public int getFirstColor(ItemStack stack) {
        CompoundTag tag = stack.getTagElement("display");
        return tag != null && tag.contains(firstColor, 99) ? tag.getInt(firstColor) : defaultFirstColor;
    }

    public void setFirstColor(ItemStack stack, int color) {
        stack.getOrCreateTagElement("display").putInt(firstColor, color);
    }

    @Override
    public int getColor(ItemStack stack) {
        CompoundTag tag = stack.getTagElement("display");
        return tag != null && tag.contains(colorLookup(), 99) ? tag.getInt(colorLookup()) : defaultFirstColor;
    }

    @Override
    public boolean hasCustomColor(ItemStack stack) {
        CompoundTag tag = stack.getTagElement("display");
        return tag != null && tag.contains(colorLookup(), 99);
    }

    @Override
    public void clearColor(ItemStack stack) {
        CompoundTag tag = stack.getTagElement("display");
        if (tag != null && tag.contains(colorLookup())) {
            tag.remove(colorLookup());
        }
    }

    @Override
    public void setColor(ItemStack stack, int color) {
        stack.getOrCreateTagElement("display").putInt(colorLookup(), color);
    }
}