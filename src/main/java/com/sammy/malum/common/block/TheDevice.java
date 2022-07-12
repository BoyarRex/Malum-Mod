package com.sammy.malum.common.block;

import com.sammy.malum.core.setup.content.SoundRegistry;
import com.sammy.ortus.helpers.BlockHelper;
import com.sammy.ortus.network.screenshake.PositionedScreenshakePacket;
import com.sammy.ortus.setup.OrtusPacketRegistry;
import com.sammy.ortus.systems.easing.Easing;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.PacketDistributor;

public class TheDevice extends Block {
    public TheDevice(Properties p_49795_) {
        super(p_49795_);
    }

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        if (pHand.equals(InteractionHand.MAIN_HAND)) {
            pPlayer.swing(pHand, true);
            playSound(pLevel, pPos);
            return InteractionResult.SUCCESS;
        }
        return super.use(pState, pLevel, pPos, pPlayer, pHand, pHit);
    }

    @Override
    public void neighborChanged(BlockState pState, Level pLevel, BlockPos pPos, Block pBlock, BlockPos pFromPos, boolean pIsMoving) {
        boolean flag = pLevel.hasNeighborSignal(pPos);
        if (flag) {
            playSound(pLevel, pPos);
        }
    }

    public void playSound(Level level, BlockPos pos) {
        if (level instanceof ServerLevel serverLevel) {
            OrtusPacketRegistry.ORTUS_CHANNEL.send(PacketDistributor.TRACKING_CHUNK.with(() -> serverLevel.getChunkAt(pos)), new PositionedScreenshakePacket(40, BlockHelper.fromBlockPos(pos),0.4f,1f, Easing.EXPO_OUT).setIntensity(0.25f, 0));
        }
        level.playSound(null, pos, SoundRegistry.SUSPICIOUS_SOUND.get(), SoundSource.BLOCKS, 1, 1);
    }
}