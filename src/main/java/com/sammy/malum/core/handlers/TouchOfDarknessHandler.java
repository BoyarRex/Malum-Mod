package com.sammy.malum.core.handlers;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.sammy.malum.common.block.curiosities.weeping_well.PrimordialSoupBlock;
import com.sammy.malum.common.block.curiosities.weeping_well.VoidConduitBlockEntity;
import com.sammy.malum.common.capability.MalumLivingEntityDataCapability;
import com.sammy.malum.common.packets.VoidRejectionPacket;
import com.sammy.malum.registry.client.ShaderRegistry;
import com.sammy.malum.registry.common.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.network.PacketDistributor;
import team.lodestar.lodestone.helpers.BlockHelper;
import team.lodestar.lodestone.systems.easing.Easing;
import team.lodestar.lodestone.systems.rendering.VFXBuilders;
import team.lodestar.lodestone.systems.rendering.shader.ExtendedShaderInstance;

import java.util.UUID;
import java.util.function.Consumer;

public class TouchOfDarknessHandler {

    public static final UUID GRAVITY_MODIFIER_UUID = UUID.fromString("d0aea6b5-f6c5-479d-b70c-455e46a62184");
    public static final float MAX_AFFLICTION = 100f;

    public int weepingWellInfluence;
    public int expectedAffliction;
    public int afflictionDuration;
    public float currentAffliction;
    public int rejection;
    public int timeSpentInGoop;

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("weepingWellInfluence", weepingWellInfluence);
        tag.putInt("expectedAffliction", expectedAffliction);
        tag.putInt("afflictionDuration", afflictionDuration);
        tag.putFloat("currentAffliction", currentAffliction);
        tag.putInt("rejection", rejection);
        tag.putInt("timeSpentInGoop", timeSpentInGoop);
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        weepingWellInfluence = tag.getInt("weepingWellInfluence");
        expectedAffliction = tag.getInt("expectedAffliction");
        afflictionDuration = tag.getInt("afflictionDuration");
        currentAffliction = tag.getFloat("currentAffliction");
        rejection = tag.getInt("rejection");
        timeSpentInGoop = tag.getInt("timeSpentInGoop");
    }

    public static void handlePrimordialSoupContact(BlockState pState, LivingEntity livingEntity) {
        //while a living entity is in the primordial soup, we will bring their expected affliction to 100, slow their movement down.
        float intensity = 0.4f;
        TouchOfDarknessHandler touchOfDarknessHandler = MalumLivingEntityDataCapability.getCapability(livingEntity).touchOfDarknessHandler;
        if (touchOfDarknessHandler.isEntityRejected()) {
            return;
        }
        livingEntity.setDeltaMovement(livingEntity.getDeltaMovement().multiply(intensity, intensity, intensity));
        touchOfDarknessHandler.afflict(100);
    }

    public static boolean checkForWeepingWellInfluence(LivingEntity livingEntity) {
        TouchOfDarknessHandler handler = MalumLivingEntityDataCapability.getCapability(livingEntity).touchOfDarknessHandler;
        boolean foundWeepingWell = !BlockHelper.getBlockEntities(VoidConduitBlockEntity.class, livingEntity.level(), livingEntity.blockPosition(), 8).isEmpty();
        if (foundWeepingWell) {
            handler.weepingWellInfluence = 200;
        }
        return handler.weepingWellInfluence > 0;
    }

    public static AttributeModifier getEntityGravityAttributeModifier(LivingEntity livingEntity) {
        return new AttributeModifier(GRAVITY_MODIFIER_UUID, "Weeping Well Gravity Modifier", 0, AttributeModifier.Operation.MULTIPLY_TOTAL) {
            @Override
            public double getAmount() {
                return updateEntityGravity(livingEntity);
            }
        };
    }

    public static double updateEntityGravity(LivingEntity livingEntity) {
        TouchOfDarknessHandler handler = MalumLivingEntityDataCapability.getCapability(livingEntity).touchOfDarknessHandler;
        if (handler.timeSpentInGoop > 0) {
            return -Math.min(60, handler.timeSpentInGoop) / 60f;
        }
        return 0;
    }

    public static void entityTick(LivingEvent.LivingTickEvent event) {
        LivingEntity livingEntity = event.getEntity();
        TouchOfDarknessHandler handler = MalumLivingEntityDataCapability.getCapability(livingEntity).touchOfDarknessHandler;
        boolean isInTheGoop = livingEntity.level().getBlockState(livingEntity.blockPosition()).getBlock() instanceof PrimordialSoupBlock;
        if (handler.weepingWellInfluence > 0) { //tick down weeping well influence, if it reaches 0, the player isn't around the weeping well
            handler.weepingWellInfluence--;
        }
        if (handler.afflictionDuration > 0) { //tick down the duration of touch of darkness.
            handler.afflictionDuration--;
            if (handler.afflictionDuration == 0) { //if it reaches zero, set expectedAffliction to 0, eventually ending the effect
                handler.expectedAffliction = 0;
            }
        }
        if (handler.currentAffliction <= handler.expectedAffliction) { //increment the affliction until it reaches the limit value
            if (handler.currentAffliction < handler.expectedAffliction) {
                float increase = 1.55f;
                if (handler.afflictionDuration < 20) { //increase in affliction strength decreases if effect is about to run out
                    increase *= handler.afflictionDuration / 20f;
                }
                handler.currentAffliction = Math.min(MAX_AFFLICTION, handler.currentAffliction + increase);
            }
            //if the entity's affliction reached the max, and the entity isn't close to actively being rejected, and is in the goop, they are rejected
            //rejection is set to 15, and the entity starts rapidly ascending as a result
            //we do this only on the server, and then communicate the rejection to the client using a packet
            if (!livingEntity.level().isClientSide) {
                if (handler.currentAffliction >= MAX_AFFLICTION && (handler.rejection < 5 || handler.rejection > 25) && handler.timeSpentInGoop > 60) {
                    handler.reject(livingEntity);
                }
            }
        }
        //if the expected affliction is lower than the current affliction, it diminishes.
        //current affliction diminishes faster if the expected value is 0
        if (handler.currentAffliction > handler.expectedAffliction) {
            handler.currentAffliction = Math.max(handler.currentAffliction - (handler.expectedAffliction == 0 ? 1.5f : 0.75f), handler.expectedAffliction);
        }
        //update entity gravity
        AttributeInstance gravity = livingEntity.getAttribute(ForgeMod.ENTITY_GRAVITY.get());
        if (gravity != null) {
            boolean hasModifier = gravity.getModifier(GRAVITY_MODIFIER_UUID) != null;
            if (handler.timeSpentInGoop > 0) {
                if (!hasModifier) {
                    gravity.addTransientModifier(getEntityGravityAttributeModifier(livingEntity));
                }
                gravity.setDirty();
            } else if (hasModifier) {
                gravity.removeModifier(GRAVITY_MODIFIER_UUID);
            }
        }
        //if we in the goop, we in the goop
        if (isInTheGoop) {
            handler.timeSpentInGoop++;
            boolean isPlayer = livingEntity instanceof Player;
            if (isPlayer && livingEntity.level().getGameTime() % 6L == 0) {
                livingEntity.level().playSound(null, livingEntity.blockPosition(), SoundRegistry.SONG_OF_THE_VOID.get(), SoundSource.HOSTILE, 0.5f + handler.timeSpentInGoop * 0.02f, 0.5f + handler.timeSpentInGoop * 0.03f);
            }
            if (!isPlayer) {
                if (livingEntity.getDeltaMovement().y > 0) {
                    livingEntity.setDeltaMovement(livingEntity.getDeltaMovement().multiply(1, 0.5f, 1));
                }
            }
        }
        //if we ain't in the goop, we ain't in the goop.
        if (!isInTheGoop || handler.isEntityRejected()) {
            handler.timeSpentInGoop = 0;
        }
        //rejection grows while at max affliction, and already rejected.
        if (handler.currentAffliction >= TouchOfDarknessHandler.MAX_AFFLICTION && handler.isEntityRejected()) {
            handler.rejection += 2;
        }
        //rejection diminishes naturally
        handler.rejection = Math.max(handler.rejection - 1, 0);
        //handles throwing the entity out, if the entity is being rejected it's Y velocity is forced into a diminishing value
        if (handler.isEntityRejected()) {
            float intensity = (40 - handler.rejection) / 30f;
            livingEntity.setDeltaMovement(livingEntity.getDeltaMovement().x, Math.pow(1.1f * (intensity), 2), livingEntity.getDeltaMovement().z);
            if (handler.rejection >= 40) {
                handler.rejection = 0;
            }
        }
    }

    public void afflict(int expectedAffliction) {
        if (this.expectedAffliction > expectedAffliction) {
            return;
        }
        this.expectedAffliction = expectedAffliction;
        this.afflictionDuration = 60;
    }

    public void reject(LivingEntity livingEntity) {
        if (!(livingEntity instanceof Player)) {
            livingEntity.remove(Entity.RemovalReason.DISCARDED);
            return;
        }
        expectedAffliction = 0;
        currentAffliction += 40f;
        afflictionDuration = 0;
        rejection = 10;
        if (!livingEntity.level().isClientSide) {
            PacketRegistry.MALUM_CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> livingEntity), new VoidRejectionPacket(livingEntity.getId()));
            ParticleEffectTypeRegistry.WEEPING_WELL_REACTS.createEntityEffect(livingEntity);
            livingEntity.hurt(DamageTypeRegistry.create(livingEntity.level(), DamageTypeRegistry.VOODOO), 4);
            livingEntity.level().playSound(null, livingEntity.blockPosition(), SoundRegistry.VOID_REJECTION.get(), SoundSource.HOSTILE, 2f, Mth.nextFloat(livingEntity.getRandom(), 0.5f, 0.8f));
        }
        livingEntity.addEffect(new MobEffectInstance(MobEffectRegistry.REJECTED.get(), 400, 0));
    }

    public boolean isEntityRejected() {
        return rejection >= 10;
    }

    public static class ClientOnly {
        private static final Tesselator INSTANCE = new Tesselator();

        public static void renderDarknessVignette(GuiGraphics guiGraphics) {
            Minecraft minecraft = Minecraft.getInstance();
            PoseStack poseStack = guiGraphics.pose();
            Player player = minecraft.player;
            TouchOfDarknessHandler darknessHandler = MalumLivingEntityDataCapability.getCapability(player).touchOfDarknessHandler;
            if (darknessHandler.currentAffliction == 0f) {
                return;
            }
            int screenWidth = minecraft.getWindow().getGuiScaledWidth();
            int screenHeight = minecraft.getWindow().getGuiScaledHeight();

            float effectStrength = Easing.SINE_IN_OUT.ease(darknessHandler.currentAffliction / MAX_AFFLICTION, 0, 1, 1);
            float alpha = Math.min(1, effectStrength * 5);
            float zoom = 0.5f + Math.min(0.35f, effectStrength);
            float intensity = 1f + (effectStrength > 0.5f ? (effectStrength - 0.5f) * 2.5f : 0);

            ExtendedShaderInstance shaderInstance = (ExtendedShaderInstance) ShaderRegistry.TOUCH_OF_DARKNESS.getInstance().get();
            shaderInstance.safeGetUniform("Speed").set(1000f);
            Consumer<Float> setZoom = f -> shaderInstance.safeGetUniform("Zoom").set(f);
            Consumer<Float> setIntensity = f -> shaderInstance.safeGetUniform("Intensity").set(f);
            VFXBuilders.ScreenVFXBuilder builder = VFXBuilders.createScreen()
                    .setPosColorTexDefaultFormat()
                    .setPositionWithWidth(0, 0, screenWidth, screenHeight)
                    .overrideBufferBuilder(INSTANCE.getBuilder())
                    .setColor(0, 0, 0)
                    .setAlpha(alpha)
                    .setShader(ShaderRegistry.TOUCH_OF_DARKNESS.getInstance());

            poseStack.pushPose();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();

            setZoom.accept(zoom);
            setIntensity.accept(intensity);
            builder.draw(poseStack);

            setZoom.accept(zoom * 1.25f + 0.15f);
            setIntensity.accept(intensity * 0.8f + 0.5f);
            builder.setAlpha(0.5f * alpha).draw(poseStack);

            RenderSystem.disableBlend();
            poseStack.popPose();

            shaderInstance.setUniformDefaults();
        }
    }
}