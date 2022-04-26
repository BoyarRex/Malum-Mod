package com.sammy.malum.common.worldgen;

import com.sammy.malum.common.block.MalumLeavesBlock;
import com.sammy.malum.common.block.MalumSaplingBlock;
import com.sammy.malum.core.setup.content.block.BlockRegistry;
import com.sammy.ortus.helpers.BlockHelper;
import com.sammy.ortus.helpers.DataHelper;
import com.sammy.ortus.systems.worldgen.OrtusBlockFiller;
import com.sammy.ortus.systems.worldgen.OrtusBlockFiller.BlockStateEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.Random;

public class RunewoodTreeFeature extends Feature<NoneFeatureConfiguration> {
    public RunewoodTreeFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    private static final int minimumSapBlockCount = 2;
    private static final int extraSapBlockCount = 1;

    private static final int minimumTrunkHeight = 7;
    private static final int extraTrunkHeight = 3;
    private static final int minimumSideTrunkHeight = 0;
    private static final int extraSideTrunkHeight = 2;

    private static final int minimumDownwardsBranchOffset = 2;
    private static final int extraDownwardsBranchOffset = 2;
    private static final int minimumBranchCoreOffset = 2;
    private static final int branchCoreOffsetExtra = 1;
    private static final int minimumBranchHeight = 3;
    private static final int branchHeightExtra = 2;

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos pos = context.origin();
        Random rand = context.random();
        if (level.isEmptyBlock(pos.below()) || !BlockRegistry.RUNEWOOD_SAPLING.get().defaultBlockState().canSurvive(level, pos)) {
            return false;
        }
        BlockState defaultLog = BlockRegistry.RUNEWOOD_LOG.get().defaultBlockState();

        OrtusBlockFiller treeFiller = new OrtusBlockFiller(false);
        OrtusBlockFiller leavesFiller = new OrtusBlockFiller(true);

        int trunkHeight = minimumTrunkHeight + rand.nextInt(extraTrunkHeight + 1);
        BlockPos trunkTop = pos.above(trunkHeight);
        Direction[] directions = new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

        for (int i = 0; i <= trunkHeight; i++) //trunk placement
        {
            BlockPos trunkPos = pos.above(i);
            if (canPlace(level, trunkPos)) {
                treeFiller.entries.add(new BlockStateEntry(defaultLog, trunkPos));
            } else {
                return false;
            }
        }

        makeLeafBlob(leavesFiller, rand, trunkTop);
        for (Direction direction : directions) //side trunk placement
        {
            int sideTrunkHeight = minimumSideTrunkHeight + rand.nextInt(extraSideTrunkHeight + 1);
            for (int i = 0; i < sideTrunkHeight; i++) {
                BlockPos sideTrunkPos = pos.relative(direction).above(i);
                if (canPlace(level, sideTrunkPos)) {
                    treeFiller.entries.add(new BlockStateEntry(defaultLog, sideTrunkPos));
                } else {
                    return false;
                }
            }
            downwardsTrunk(level, treeFiller, pos.relative(direction));
        }
        for (Direction direction : directions) //tree top placement
        {
            int branchCoreOffset = minimumDownwardsBranchOffset + rand.nextInt(extraDownwardsBranchOffset + 1);
            int branchOffset = minimumBranchCoreOffset + rand.nextInt(branchCoreOffsetExtra + 1);
            BlockPos branchStartPos = trunkTop.below(branchCoreOffset).relative(direction, branchOffset);
            for (int i = 0; i < branchOffset; i++) //branch connection placement
            {
                BlockPos branchConnectionPos = branchStartPos.relative(direction.getOpposite(), i);
                if (canPlace(level, branchConnectionPos)) {
                    treeFiller.entries.add(new BlockStateEntry(defaultLog.setValue(RotatedPillarBlock.AXIS, direction.getAxis()), branchConnectionPos));
                } else {
                    return false;
                }
            }
            int branchHeight = minimumBranchHeight + rand.nextInt(branchHeightExtra + 1);
            for (int i = 0; i < branchHeight; i++) //branch placement
            {
                BlockPos branchPos = branchStartPos.above(i);
                if (canPlace(level, branchPos)) {
                    treeFiller.entries.add(new BlockStateEntry(defaultLog, branchPos));
                } else {
                    return false;
                }
            }
            makeLeafBlob(leavesFiller, rand, branchStartPos.above(1));
        }
        int sapBlockCount = minimumSapBlockCount + rand.nextInt(extraSapBlockCount + 1);
        int[] sapBlockIndexes = DataHelper.nextInts(rand, sapBlockCount, treeFiller.entries.size());
        for (Integer index : sapBlockIndexes) {
            treeFiller.replace(index, e -> e.replaceState(BlockHelper.getBlockStateWithExistingProperties(e.state, BlockRegistry.EXPOSED_RUNEWOOD_LOG.get().defaultBlockState())));
        }
        treeFiller.fill(level);
        leavesFiller.fill(level);
        return true;
    }

    public static void downwardsTrunk(WorldGenLevel level, OrtusBlockFiller filler, BlockPos pos) {
        int i = 0;
        do {
            i++;
            BlockPos trunkPos = pos.below(i);
            if (canPlace(level, trunkPos)) {
                filler.entries.add(new BlockStateEntry(BlockRegistry.RUNEWOOD_LOG.get().defaultBlockState(), trunkPos));
            } else {
                break;
            }
            if (i > level.getMaxBuildHeight()) {
                break;
            }
        }
        while (true);
    }

    public static void makeLeafBlob(OrtusBlockFiller filler, Random rand, BlockPos pos) {
        makeLeafSlice(filler, pos, 1, 0);
        makeLeafSlice(filler, pos.above(1), 2, 1);
        makeLeafSlice(filler, pos.above(2), 2, 2);
        makeLeafSlice(filler, pos.above(3), 2, 3);
        makeLeafSlice(filler, pos.above(4), 1, 4);
    }

    public static void makeLeafSlice(OrtusBlockFiller filler, BlockPos pos, int leavesSize, int leavesColor) {
        for (int x = -leavesSize; x <= leavesSize; x++) {
            for (int z = -leavesSize; z <= leavesSize; z++) {
                if (Math.abs(x) == leavesSize && Math.abs(z) == leavesSize) {
                    continue;
                }
                BlockPos leavesPos = new BlockPos(pos).offset(x, 0, z);
                filler.entries.add(new BlockStateEntry(BlockRegistry.RUNEWOOD_LEAVES.get().defaultBlockState().setValue(LeavesBlock.DISTANCE, 1).setValue(MalumLeavesBlock.COLOR, leavesColor), leavesPos));
            }
        }
    }

    public static boolean canPlace(WorldGenLevel level, BlockPos pos) {
        if (level.isOutsideBuildHeight(pos)) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof MalumSaplingBlock || level.isEmptyBlock(pos) || state.getMaterial().isReplaceable();
    }
}