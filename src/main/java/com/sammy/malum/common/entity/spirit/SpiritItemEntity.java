package com.sammy.malum.common.entity.spirit;

import com.sammy.malum.common.entity.FloatingItemEntity;
import com.sammy.malum.core.handlers.SpiritHarvestHandler;
import com.sammy.malum.core.setup.content.AttributeRegistry;
import com.sammy.malum.core.setup.content.entity.EntityRegistry;
import com.sammy.malum.core.helper.SpiritHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class SpiritItemEntity extends FloatingItemEntity {
    public UUID ownerUUID;
    public LivingEntity owner;

    public SpiritItemEntity(Level level) {
        super(EntityRegistry.NATURAL_SPIRIT.get(), level);
        maxAge = 4000;
    }

    public SpiritItemEntity(Level level, UUID ownerUUID, ItemStack stack, double posX, double posY, double posZ, double velX, double velY, double velZ) {
        super(EntityRegistry.NATURAL_SPIRIT.get(), level);
        setOwner(ownerUUID);
        setItem(stack);
        setPos(posX, posY, posZ);
        setDeltaMovement(velX, velY, velZ);
        maxAge = 800;
    }

    public float getRange() {
        return level.noCollision(this) ? range : range * 5f;
    }

    public void setOwner(UUID ownerUUID) {
        this.ownerUUID = ownerUUID;
        updateOwner();
    }

    public void updateOwner() {
        if (!level.isClientSide) {
            owner = (LivingEntity) ((ServerLevel) level).getEntity(ownerUUID);
            if (owner != null) {
                range = (int) owner.getAttributeValue(AttributeRegistry.SPIRIT_REACH.get());
            }
        }
    }

    @Override
    public void spawnParticles(double x, double y, double z) {
        SpiritHelper.spawnSpiritParticles(level, x, y, z, color, endColor);
    }

    @Override
    public void move() {
        float friction = 0.94f;
        setDeltaMovement(getDeltaMovement().multiply(friction, friction, friction));
        float range = getRange();
        if (owner == null || !owner.isAlive()) {
            if (level.getGameTime() % 40L == 0) {
                Player playerEntity = level.getNearestPlayer(this, range * 5f);
                if (playerEntity != null) {
                    setOwner(playerEntity.getUUID());
                }
            }
            return;
        }
        Vec3 desiredLocation = owner.position().add(0, owner.getBbHeight() / 3, 0);
        float distance = (float) distanceToSqr(desiredLocation);
        float velocity = Mth.lerp(Math.min(moveTime, 10) / 10f, 0.05f, 0.4f + (range * 0.075f));
        if (moveTime != 0 || distance < range) {
            moveTime++;
            Vec3 desiredMotion = desiredLocation.subtract(position()).normalize().multiply(velocity, velocity, velocity);
            float easing = 0.01f;
            float xMotion = (float) Mth.lerp(easing, getDeltaMovement().x, desiredMotion.x);
            float yMotion = (float) Mth.lerp(easing, getDeltaMovement().y, desiredMotion.y);
            float zMotion = (float) Mth.lerp(easing, getDeltaMovement().z, desiredMotion.z);
            Vec3 resultingMotion = new Vec3(xMotion, yMotion, zMotion);
            setDeltaMovement(resultingMotion);
        }

        if (distance < 0.4f) {
            if (isAlive()) {
                ItemStack stack = getItem();
                SpiritHarvestHandler.pickupSpirit(stack, owner);
                remove(RemovalReason.DISCARDED);
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        if (ownerUUID != null) {
            compound.putUUID("ownerUUID", ownerUUID);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("ownerUUID")) {
            setOwner(compound.getUUID("ownerUUID"));
        }
    }
}