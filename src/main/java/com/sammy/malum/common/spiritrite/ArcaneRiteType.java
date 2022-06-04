package com.sammy.malum.common.spiritrite;

import com.sammy.malum.common.blockentity.totem.TotemBaseBlockEntity;
import com.sammy.malum.common.recipe.BlockTransmutationRecipe;
import com.sammy.malum.common.worldevent.TotemCreatedBlightEvent;
import com.sammy.malum.core.systems.rites.BlockAffectingRiteEffect;
import com.sammy.malum.core.systems.rites.MalumRiteEffect;
import com.sammy.malum.core.systems.rites.MalumRiteType;
import com.sammy.ortus.handlers.WorldEventHandler;
import com.sammy.ortus.helpers.BlockHelper;
import com.sammy.ortus.setup.OrtusParticleRegistry;
import com.sammy.ortus.systems.rendering.particle.ParticleBuilders;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.awt.*;
import java.util.ArrayList;

import static com.sammy.malum.core.setup.content.SpiritTypeRegistry.ARCANE_SPIRIT;

public class ArcaneRiteType extends MalumRiteType {
    public ArcaneRiteType() {
        super("arcane_rite", ARCANE_SPIRIT, ARCANE_SPIRIT, ARCANE_SPIRIT, ARCANE_SPIRIT, ARCANE_SPIRIT);
    }

    @Override
    public MalumRiteEffect getNaturalRiteEffect() {
        return new MalumRiteEffect() {
            @Override
            public boolean isOneAndDone() {
                return true;
            }

            @SuppressWarnings("ConstantConditions")
            @Override
            public void riteEffect(TotemBaseBlockEntity totemBase) {
                if (totemBase.getLevel().isClientSide) {
                    return;
                }
                WorldEventHandler.addWorldEvent(totemBase.getLevel(), new TotemCreatedBlightEvent().setPosition(totemBase.getBlockPos()).setBlightData(1, 4, 4));
            }
        };
    }

    @Override
    public MalumRiteEffect getCorruptedEffect() {
        return new BlockAffectingRiteEffect() {
            @SuppressWarnings("ConstantConditions")
            @Override
            public void riteEffect(TotemBaseBlockEntity totemBase) {
                Level level = totemBase.getLevel();
                BlockPos pos = totemBase.getBlockPos();

                ArrayList<BlockPos> positions = getNearbyBlocks(totemBase, Block.class);
                positions.forEach(p -> {
                    BlockState state = level.getBlockState(p);
                    BlockTransmutationRecipe recipe = BlockTransmutationRecipe.getRecipe(level, state.getBlock());
                    if (recipe != null) {
                        if (!level.isClientSide) {
                            Block block = recipe.output;
                            BlockEntity entity = level.getBlockEntity(p);
                            BlockState newState = BlockHelper.setBlockStateWithExistingProperties(level, p, block.defaultBlockState(), 3);
                            level.levelEvent(2001, p, Block.getId(newState));
                            if (block instanceof EntityBlock entityBlock) {
                                if (entity != null) {
                                    BlockEntity newEntity = entityBlock.newBlockEntity(pos, newState);
                                    if (newEntity != null) {
                                        if (newEntity.getClass().equals(entity.getClass())) {
                                            level.setBlockEntity(entity);
                                        }
                                    }
                                }
                            }
                        } else {
                            particles(level, p);
                        }
                    }
                });
            }
        };
    }

    public void particles(Level level, BlockPos pos) {
        Color color = ARCANE_SPIRIT.getColor();
        ParticleBuilders.create(OrtusParticleRegistry.TWINKLE_PARTICLE)
                .setAlpha(0.4f, 0f)
                .setLifetime(20)
                .setSpin(0.3f)
                .setScale(0.2f, 0)
                .setColor(color, color)
                .enableNoClip()
                .randomOffset(0.1f, 0.1f)
                .randomMotion(0.001f, 0.001f)
                .evenlyRepeatEdges(level, pos, 4, Direction.UP, Direction.DOWN);
        ParticleBuilders.create(OrtusParticleRegistry.WISP_PARTICLE)
                .setAlpha(0.1f, 0f)
                .setLifetime(40)
                .setSpin(0.1f)
                .setScale(0.6f, 0)
                .setColor(color, color)
                .randomOffset(0.2f)
                .enableNoClip()
                .randomMotion(0.001f, 0.001f)
                .evenlyRepeatEdges(level, pos, 6, Direction.UP, Direction.DOWN);
    }
}