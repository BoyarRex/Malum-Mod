package com.sammy.malum.common.block.curiosities.mana_mote;

import com.sammy.malum.core.handlers.*;
import com.sammy.malum.visual_effects.*;
import net.minecraft.client.particle.*;
import net.minecraft.core.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.client.extensions.common.*;

public class SpiritMoteBlockClientExtension implements IClientBlockExtensions {

    @Override
    public boolean addHitEffects(BlockState state, Level level, HitResult target, ParticleEngine manager) {
        return true;
    }

    @Override
    public boolean addDestroyEffects(BlockState state, Level level, BlockPos pos, ParticleEngine manager) {
        if (state.getBlock() instanceof SpiritMoteBlock) {
            SpiritMoteParticleEffects.destroy(level, pos, state, SpiritHarvestHandler.getSpiritType(state.getValue(SpiritMoteBlock.SPIRIT_TYPE)));
        }
        return true;
    }
}
