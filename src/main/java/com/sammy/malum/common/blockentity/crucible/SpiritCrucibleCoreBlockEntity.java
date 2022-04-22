package com.sammy.malum.common.blockentity.crucible;

import com.sammy.malum.client.screen.codex.ProgressionBookScreen;
import com.sammy.malum.common.blockentity.tablet.ITabletTracker;
import com.sammy.malum.common.blockentity.tablet.TwistedTabletBlockEntity;
import com.sammy.malum.common.item.impetus.ImpetusItem;
import com.sammy.malum.common.item.spirit.MalumSpiritItem;
import com.sammy.malum.common.packets.particle.altar.AltarConsumeParticlePacket;
import com.sammy.malum.common.packets.particle.altar.AltarCraftParticlePacket;
import com.sammy.malum.common.recipe.SpiritFocusingRecipe;
import com.sammy.malum.common.recipe.SpiritRepairRecipe;
import com.sammy.malum.core.helper.BlockHelper;
import com.sammy.malum.core.helper.DataHelper;
import com.sammy.malum.core.helper.SpiritHelper;
import com.sammy.malum.core.setup.client.ParticleRegistry;
import com.sammy.malum.core.setup.content.SoundRegistry;
import com.sammy.malum.core.setup.content.block.BlockEntityRegistry;
import com.sammy.malum.core.setup.content.block.BlockRegistry;
import com.sammy.malum.core.systems.blockentity.SimpleBlockEntityInventory;
import com.sammy.malum.core.systems.multiblock.MultiBlockCoreEntity;
import com.sammy.malum.core.systems.multiblock.MultiBlockStructure;
import com.sammy.malum.core.systems.recipe.SpiritWithCount;
import com.sammy.malum.core.systems.rendering.particle.ParticleBuilders;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.network.PacketDistributor;
import org.checkerframework.checker.units.qual.A;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.sammy.malum.core.setup.server.PacketRegistry.INSTANCE;

public class SpiritCrucibleCoreBlockEntity extends MultiBlockCoreEntity implements IAccelerationTarget, ITabletTracker {
    public static final Supplier<MultiBlockStructure> STRUCTURE = () -> (MultiBlockStructure.of(new MultiBlockStructure.StructurePiece(0, 1, 0, BlockRegistry.SPIRIT_CRUCIBLE_COMPONENT.get().defaultBlockState())));

    public SimpleBlockEntityInventory inventory;
    public SimpleBlockEntityInventory spiritInventory;
    public SpiritFocusingRecipe focusingRecipe;
    public SpiritRepairRecipe repairRecipe;
    public boolean updateRecipe;
    public float spiritAmount;
    public float spiritSpin;
    public float speed;
    public float damageChance;
    public int maxDamage;
    public float progress;

    public int queuedCracks;
    public int crackTimer;

    public ArrayList<BlockPos> tabletPositions = new ArrayList<>();
    public ArrayList<TwistedTabletBlockEntity> twistedTablets = new ArrayList<>();
    public TwistedTabletBlockEntity validTablet;
    public int tabletFetchCooldown;

    public ArrayList<BlockPos> acceleratorPositions = new ArrayList<>();
    public ArrayList<ICrucibleAccelerator> accelerators = new ArrayList<>();

    public SpiritCrucibleCoreBlockEntity(BlockEntityType<? extends SpiritCrucibleCoreBlockEntity> type, MultiBlockStructure structure, BlockPos pos, BlockState state) {
        super(type, structure, pos, state);
        inventory = new SimpleBlockEntityInventory(1, 1, t -> !(t.getItem() instanceof MalumSpiritItem)) {
            @Override
            public void onContentsChanged(int slot) {
                super.onContentsChanged(slot);
                updateRecipe = true;
                BlockHelper.updateAndNotifyState(level, worldPosition);
            }
        };
        spiritInventory = new SimpleBlockEntityInventory(4, 64, t -> t.getItem() instanceof MalumSpiritItem) {
            @Override
            public void onContentsChanged(int slot) {
                super.onContentsChanged(slot);
                updateRecipe = true;
                spiritAmount = Math.max(1, Mth.lerp(0.15f, spiritAmount, nonEmptyItemAmount + 1));
                BlockHelper.updateAndNotifyState(level, worldPosition);
            }
        };
    }

    public SpiritCrucibleCoreBlockEntity(BlockPos pos, BlockState state) {
        this(BlockEntityRegistry.SPIRIT_CRUCIBLE.get(), STRUCTURE.get(), pos, state);
    }

    @Override
    protected void saveAdditional(CompoundTag compound) {
        if (spiritAmount != 0) {
            compound.putFloat("spiritAmount", spiritAmount);
        }
        if (progress != 0) {
            compound.putFloat("progress", progress);
        }
        if (speed != 0) {
            compound.putFloat("speed", speed);
        }
        if (damageChance != 0) {
            compound.putFloat("damageChance", damageChance);
        }
        if (maxDamage != 0) {
            compound.putInt("maxDamage", maxDamage);
        }
        if (queuedCracks != 0) {
            compound.putInt("queuedCracks", queuedCracks);
        }

        inventory.save(compound);
        spiritInventory.save(compound, "spiritInventory");

        saveTwistedTabletData(compound);
        saveAcceleratorData(compound);
    }

    @Override
    public void load(CompoundTag compound) {
        spiritAmount = compound.getFloat("spiritAmount");
        progress = compound.getFloat("progress");
        speed = compound.getFloat("speed");
        damageChance = compound.getFloat("damageChance");
        maxDamage = compound.getInt("maxDamage");
        queuedCracks = compound.getInt("queuedCracks");

        updateRecipe = true;
        inventory.load(compound);
        spiritInventory.load(compound, "spiritInventory");

        loadTwistedTabletData(level, compound);
        loadAcceleratorData(level, compound);
        super.load(compound);
    }

    @Override
    public InteractionResult onUse(Player player, InteractionHand hand) {
        if (level.isClientSide) {
            return InteractionResult.FAIL;
        }
        if (hand.equals(InteractionHand.MAIN_HAND)) {
            fetchTablets(level, worldPosition.above());
            ItemStack heldStack = player.getMainHandItem();
            recalibrateAccelerators(level, worldPosition);
            if (!(heldStack.getItem() instanceof MalumSpiritItem)) {
                ItemStack stack = inventory.interact(level, player, hand);
                if (!stack.isEmpty()) {
                    return InteractionResult.SUCCESS;
                }
            }
            spiritInventory.interact(level, player, hand);
            if (heldStack.isEmpty()) {
                return InteractionResult.SUCCESS;
            } else {
                return InteractionResult.FAIL;
            }
        }
        return super.onUse(player, hand);
    }

    @Override
    public void onBreak() {
        inventory.dumpItems(level, worldPosition);
        spiritInventory.dumpItems(level, worldPosition);
        super.onBreak();
    }

    @Override
    public void fetchTablets(Level level, BlockPos pos) {
        ITabletTracker.super.fetchTablets(level, pos);
        if (focusingRecipe == null && !getTablets().isEmpty()) {
            for (TwistedTabletBlockEntity tablet : getTablets()) {
                repairRecipe = SpiritRepairRecipe.getRecipe(level, inventory.getStackInSlot(0), tablet.inventory.getStackInSlot(0), spiritInventory.nonEmptyStacks);
                if (repairRecipe != null) {
                    validTablet = tablet;
                    break;
                }
            }
        } else {
            repairRecipe = null;
            validTablet = null;
        }
    }

    @Override
    public ArrayList<TwistedTabletBlockEntity> getTablets() {
        return twistedTablets;
    }

    @Override
    public ArrayList<BlockPos> getTabletPositions() {
        return tabletPositions;
    }

    @Override
    public boolean canBeAccelerated() {
        return focusingRecipe != null;
    }

    @Override
    public ArrayList<ICrucibleAccelerator> getAccelerators() {
        return accelerators;
    }

    @Override
    public ArrayList<BlockPos> getAcceleratorPositions() {
        return acceleratorPositions;
    }

    @Override
    public int getLookupRange() {
        return 4;
    }

    @Override
    public void tick() {
        spiritAmount = Math.max(1, Mth.lerp(0.1f, spiritAmount, spiritInventory.nonEmptyItemAmount));
        if (queuedCracks > 0) {
            crackTimer++;
            if (crackTimer % 7 == 0) {
                float pitch = 0.95f + (crackTimer - 8) * 0.015f + level.random.nextFloat() * 0.05f;
                level.playSound(null, worldPosition, SoundRegistry.IMPETUS_CRACK.get(), SoundSource.BLOCKS, 0.7f, pitch);
                queuedCracks--;
                if (queuedCracks == 0) {
                    crackTimer = 0;
                }
            }
        }
        if (updateRecipe) {
            if (level.isClientSide && focusingRecipe == null && repairRecipe == null) {
                CrucibleSoundInstance.playSound(this);
            }
            fetchTablets(level, worldPosition.above());
            focusingRecipe = SpiritFocusingRecipe.getRecipe(level, inventory.getStackInSlot(0), spiritInventory.nonEmptyStacks);
            updateRecipe = false;
            BlockHelper.updateAndNotifyState(level, worldPosition);
        }

        if (level.isClientSide) {
            spiritSpin += (1 + Math.cos(Math.sin(level.getGameTime() * 0.1f))) * (1 + speed * 0.1f);
            passiveParticles();
        } else {
            if (focusingRecipe != null) {
                if (!accelerators.isEmpty()) {
                    boolean canAccelerate = true;
                    for (ICrucibleAccelerator accelerator : accelerators) {
                        boolean canAcceleratorAccelerate = accelerator.canAccelerate();
                        if (!canAcceleratorAccelerate) {
                            canAccelerate = false;
                        }
                    }
                    if (!canAccelerate) {
                        recalibrateAccelerators(level, worldPosition);
                        BlockHelper.updateAndNotifyState(level, worldPosition);
                    }
                }
            } else if (speed > 0) {
                speed = 0f;
                damageChance = 0f;
                maxDamage = 0;
                BlockHelper.updateAndNotifyState(level, worldPosition);
            }

            if (focusingRecipe != null) {
                progress += 1 + speed;
                if (progress >= focusingRecipe.time) {
                    craft();
                }
                return;
            } else if (repairRecipe != null && !getTablets().isEmpty()) {
                ItemStack damagedItem = inventory.getStackInSlot(0);

                int time = 400 + damagedItem.getDamageValue() * 5;
                progress++;
                if (!repairRecipe.repairMaterial.matches(validTablet.inventory.getStackInSlot(0))) {
                    fetchTablets(level, worldPosition.above());
                }
                if (progress >= time) {
                    repair();
                }
            }
            if (focusingRecipe == null && repairRecipe == null) {
                progress = 0;
            }

            if (focusingRecipe == null) {
                tabletFetchCooldown--;
                if (tabletFetchCooldown <= 0) {
                    tabletFetchCooldown = 5;
                    fetchTablets(level, worldPosition.above());
                    BlockHelper.updateAndNotifyState(level, getBlockPos());
                }
            }
        }
    }

    public void repair() {
        Vec3 itemPos = getItemPos(this);
        Vec3 providedItemPos = validTablet.getItemPos();
        ItemStack damagedItem = inventory.getStackInSlot(0);
        ItemStack repairMaterial = validTablet.inventory.getStackInSlot(0);
        ItemStack result = SpiritRepairRecipe.getRepairRecipeOutput(damagedItem);
        result.setDamageValue(Math.max(0, result.getDamageValue() - (int) (result.getMaxDamage() * repairRecipe.durabilityPercentage)));
        inventory.setStackInSlot(0, result);
        if (!repairRecipe.spirits.isEmpty()) {
            INSTANCE.send(PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(worldPosition)), new AltarConsumeParticlePacket(repairMaterial, repairRecipe.spirits.stream().map(s -> s.type.identifier).collect(Collectors.toList()), providedItemPos.x, providedItemPos.y, providedItemPos.z, itemPos.x, itemPos.y, itemPos.z));
        } else if (repairRecipe.repairMaterial.getItem() instanceof MalumSpiritItem malumSpiritItem) {
            INSTANCE.send(PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(worldPosition)), new AltarConsumeParticlePacket(repairMaterial, List.of(malumSpiritItem.type.identifier), providedItemPos.x, providedItemPos.y, providedItemPos.z, itemPos.x, itemPos.y, itemPos.z));
        }
        INSTANCE.send(PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(worldPosition)), new AltarConsumeParticlePacket(repairMaterial, repairRecipe.spirits.stream().map(s -> s.type.identifier).collect(Collectors.toList()), providedItemPos.x, providedItemPos.y, providedItemPos.z, itemPos.x, itemPos.y, itemPos.z));
        repairMaterial.shrink(repairRecipe.repairMaterial.getCount());
        for (SpiritWithCount spirit : repairRecipe.spirits) {
            for (int i = 0; i < spiritInventory.slotCount; i++) {
                ItemStack spiritStack = spiritInventory.getStackInSlot(i);
                if (spirit.matches(spiritStack)) {
                    spiritStack.shrink(spirit.count);
                    break;
                }
            }
        }
        if (!repairRecipe.spirits.isEmpty()) {
            INSTANCE.send(PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(worldPosition)), new AltarCraftParticlePacket(repairRecipe.spirits.stream().map(s -> s.type.identifier).collect(Collectors.toList()), itemPos.x, itemPos.y, itemPos.z));
        } else if (repairRecipe.repairMaterial.getItem() instanceof MalumSpiritItem malumSpiritItem) {
            INSTANCE.send(PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(worldPosition)), new AltarCraftParticlePacket(List.of(malumSpiritItem.type.identifier), itemPos.x, itemPos.y, itemPos.z));
        }
        repairRecipe = SpiritRepairRecipe.getRecipe(level, damagedItem, repairMaterial, spiritInventory.nonEmptyStacks);
        fetchTablets(level, worldPosition.above());
        BlockHelper.updateAndNotifyState(level, validTablet.getBlockPos());
        finishRecipe();
    }

    public void craft() {
        Vec3 itemPos = getItemPos(this);
        ItemStack stack = inventory.getStackInSlot(0);
        ItemStack outputStack = focusingRecipe.output.getStack();
        if (focusingRecipe.durabilityCost != 0 && stack.isDamageableItem()) {
            int durabilityCost = focusingRecipe.durabilityCost;
            float chance = damageChance;
            while (durabilityCost < durabilityCost + maxDamage) {
                if (level.random.nextFloat() < chance) {
                    durabilityCost++;
                    chance *= chance;
                } else {
                    break;
                }
            }
            queuedCracks = durabilityCost;
            boolean success = stack.hurt(durabilityCost, level.random, null);
            if (success && stack.getItem() instanceof ImpetusItem impetusItem) {
                inventory.setStackInSlot(0, impetusItem.getCrackedVariant().getDefaultInstance());
                updateRecipe = true;
            }
        }
        for (SpiritWithCount spirit : focusingRecipe.spirits) {
            for (int i = 0; i < spiritInventory.slotCount; i++) {
                ItemStack spiritStack = spiritInventory.getStackInSlot(i);
                if (spirit.matches(spiritStack)) {
                    spiritStack.shrink(spirit.count);
                    break;
                }
            }
        }
        level.addFreshEntity(new ItemEntity(level, itemPos.x, itemPos.y, itemPos.z, outputStack));
        INSTANCE.send(PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(worldPosition)), new AltarCraftParticlePacket(focusingRecipe.spirits.stream().map(s -> s.type.identifier).collect(Collectors.toList()), itemPos.x, itemPos.y, itemPos.z));
        focusingRecipe = SpiritFocusingRecipe.getRecipe(level, stack, spiritInventory.nonEmptyStacks);
        finishRecipe();
    }

    public void finishRecipe() {
        level.playSound(null, worldPosition, SoundRegistry.CRUCIBLE_CRAFT.get(), SoundSource.BLOCKS, 1, 0.75f + level.random.nextFloat() * 0.5f);
        progress = 0;
        inventory.updateData();
        spiritInventory.updateData();
        recalibrateAccelerators(level, worldPosition);
        BlockHelper.updateAndNotifyState(level, worldPosition);
    }

    @Override
    public HashMap<ICrucibleAccelerator.CrucibleAcceleratorType, Integer> recalibrateAccelerators(Level level, BlockPos pos) {
        HashMap<ICrucibleAccelerator.CrucibleAcceleratorType, Integer> accelerators = IAccelerationTarget.super.recalibrateAccelerators(level, pos);
        speed = 0f;
        damageChance = 0f;
        maxDamage = 0;
        accelerators.forEach((e, c) -> {
            speed += e.getAcceleration(c);
            damageChance = Math.min(damageChance + e.getDamageChance(c), 1);
            maxDamage += e.getMaximumDamage(c);
        });
        return accelerators;
    }

    public static Vec3 getItemPos(SpiritCrucibleCoreBlockEntity blockEntity) {
        return DataHelper.fromBlockPos(blockEntity.getBlockPos()).add(blockEntity.itemOffset());
    }

    public Vec3 itemOffset() {
        return new Vec3(0.5f, 1.6f, 0.5f);
    }

    public static Vec3 spiritOffset(SpiritCrucibleCoreBlockEntity blockEntity, int slot) {
        float distance = 0.75f + (float) Math.sin(blockEntity.spiritSpin / 20f) * 0.025f;
        float height = 1.75f;
        return DataHelper.rotatingCircleOffset(new Vec3(0.5f, height, 0.5f), distance, slot, blockEntity.spiritAmount, (long) blockEntity.spiritSpin, 360);
    }

    public void passiveParticles() {
        Vec3 itemPos = getItemPos(this);
        //passive spirit particles
        if (!spiritInventory.isEmpty()) {
            for (int i = 0; i < spiritInventory.slotCount; i++) {
                ItemStack item = spiritInventory.getStackInSlot(i);
                if (item.getItem() instanceof MalumSpiritItem spiritSplinterItem) {
                    Vec3 offset = spiritOffset(this, i);
                    Color color = spiritSplinterItem.type.color;
                    Color endColor = spiritSplinterItem.type.endColor;
                    double x = getBlockPos().getX() + offset.x();
                    double y = getBlockPos().getY() + offset.y();
                    double z = getBlockPos().getZ() + offset.z();
                    SpiritHelper.spawnSpiritParticles(level, x, y, z, color, endColor);
                }
            }
        }
        //spirit particles shot out from the twisted tablet
        if (repairRecipe != null) {
            TwistedTabletBlockEntity tabletBlockEntity = validTablet;

            ArrayList<Color> colors = new ArrayList<>();
            ArrayList<Color> endColors = new ArrayList<>();
            if (tabletBlockEntity.inventory.getStackInSlot(0).getItem() instanceof MalumSpiritItem spiritItem && repairRecipe.repairMaterial.getItem() instanceof MalumSpiritItem) {
                colors.add(spiritItem.type.color);
                endColors.add(spiritItem.type.endColor);
            } else if (!spiritInventory.isEmpty()) {
                for (int i = 0; i < spiritInventory.slotCount; i++) {
                    ItemStack item = spiritInventory.getStackInSlot(i);
                    if (item.getItem() instanceof MalumSpiritItem spiritItem) {
                        colors.add(spiritItem.type.color);
                        endColors.add(spiritItem.type.endColor);
                    }
                }
            }
            for (int i = 0; i < colors.size(); i++) {
                Color color = colors.get(i);
                Color endColor = endColors.get(i);
                Vec3 tabletItemPos = tabletBlockEntity.getItemPos();
                Vec3 velocity = tabletItemPos.subtract(itemPos).normalize().scale(-0.1f);

                ParticleBuilders.create(ParticleRegistry.STAR_PARTICLE)
                        .setAlpha(0.24f / colors.size(), 0f)
                        .setLifetime(15)
                        .setScale(0.45f + level.random.nextFloat() * 0.15f, 0)
                        .randomOffset(0.05)
                        .setSpinOffset((0.075f * level.getGameTime()) % 6.28f)
                        .setColor(color, endColor)
                        .enableNoClip()
                        .repeat(level, itemPos.x, itemPos.y, itemPos.z, 1);

                ParticleBuilders.create(ParticleRegistry.STAR_PARTICLE)
                        .setAlpha(0.24f / colors.size(), 0f)
                        .setLifetime(15)
                        .setScale(0.45f + level.random.nextFloat() * 0.15f, 0)
                        .randomOffset(0.05)
                        .setSpinOffset((-0.075f * level.getGameTime()) % 6.28f)
                        .setColor(color, endColor)
                        .enableNoClip()
                        .repeat(level, tabletItemPos.x, tabletItemPos.y, tabletItemPos.z, 1);

                ParticleBuilders.create(ParticleRegistry.WISP_PARTICLE)
                        .setAlpha(0.4f / colors.size(), 0f)
                        .setLifetime((int) (10 + level.random.nextInt(8) + Math.sin((0.5 * level.getGameTime()) % 6.28f)))
                        .setScale(0.2f + level.random.nextFloat() * 0.15f, 0)
                        .randomOffset(0.05)
                        .setSpinOffset((0.075f * level.getGameTime() % 6.28f))
                        .setSpin(0.1f + level.random.nextFloat() * 0.05f)
                        .setColor(color.brighter(), endColor)
                        .setAlphaCurveMultiplier(0.5f)
                        .setColorCurveMultiplier(0.75f)
                        .setMotion(velocity.x, velocity.y, velocity.z)
                        .enableNoClip()
                        .repeat(level, tabletItemPos.x, tabletItemPos.y, tabletItemPos.z, 1);
            }
            return;
        }
        if (focusingRecipe != null) {
            for (int i = 0; i < spiritInventory.slotCount; i++) {
                ItemStack item = spiritInventory.getStackInSlot(i);
                for (ICrucibleAccelerator accelerator : accelerators) {
                    if (accelerator != null) {
                        accelerator.addParticles(worldPosition, itemPos);
                    }
                }
                if (item.getItem() instanceof MalumSpiritItem spiritSplinterItem) {
                    Vec3 offset = spiritOffset(this, i);
                    Color color = spiritSplinterItem.type.color;
                    Color endColor = spiritSplinterItem.type.endColor;
                    double x = getBlockPos().getX() + offset.x();
                    double y = getBlockPos().getY() + offset.y();
                    double z = getBlockPos().getZ() + offset.z();
                    Vec3 velocity = new Vec3(x, y, z).subtract(itemPos).normalize().scale(-0.03f);
                    for (ICrucibleAccelerator accelerator : accelerators) {
                        if (accelerator != null) {
                            accelerator.addParticles(color, endColor, 0.08f / spiritInventory.nonEmptyItemAmount, worldPosition, itemPos);
                        }
                    }
                    ParticleBuilders.create(ParticleRegistry.WISP_PARTICLE)
                            .setAlpha(0.30f, 0f)
                            .setLifetime(40)
                            .setScale(0.2f, 0)
                            .randomOffset(0.02f)
                            .randomMotion(0.01f, 0.01f)
                            .setColor(color, endColor)
                            .setColorCurveMultiplier(0.75f)
                            .randomMotion(0.0025f, 0.0025f)
                            .addMotion(velocity.x, velocity.y, velocity.z)
                            .enableNoClip()
                            .repeat(level, x, y, z, 1);

                    ParticleBuilders.create(ParticleRegistry.WISP_PARTICLE)
                            .setAlpha(0.12f / spiritInventory.nonEmptyItemAmount, 0f)
                            .setLifetime(25)
                            .setScale(0.2f + level.random.nextFloat() * 0.1f, 0)
                            .randomOffset(0.05)
                            .setSpinOffset((0.075f * level.getGameTime() % 6.28f))
                            .setColor(color, endColor)
                            .enableNoClip()
                            .repeat(level, itemPos.x, itemPos.y, itemPos.z, 1);

                    ParticleBuilders.create(ParticleRegistry.STAR_PARTICLE)
                            .setAlpha(0.16f / spiritInventory.nonEmptyItemAmount, 0f)
                            .setLifetime(25)
                            .setScale(0.45f + level.random.nextFloat() * 0.1f, 0)
                            .randomOffset(0.05)
                            .setSpinOffset((0.075f * level.getGameTime() % 6.28f))
                            .setColor(color, endColor)
                            .enableNoClip()
                            .repeat(level, itemPos.x, itemPos.y, itemPos.z, 1);
                }
            }
        }
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap) {
        if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return inventory.inventoryOptional.cast();
        }
        return super.getCapability(cap);
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, Direction side) {
        if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return inventory.inventoryOptional.cast();
        }
        return super.getCapability(cap, side);
    }
}