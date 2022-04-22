package com.sammy.malum.common.spiritaffinity;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector4f;
import com.sammy.malum.common.capability.PlayerDataCapability;
import com.sammy.malum.config.CommonConfig;
import com.sammy.malum.core.handlers.ScreenParticleHandler;
import com.sammy.malum.core.helper.DataHelper;
import com.sammy.malum.core.helper.ItemHelper;
import com.sammy.malum.core.setup.client.ScreenParticleRegistry;
import com.sammy.malum.core.setup.content.AttributeRegistry;
import com.sammy.malum.core.setup.content.damage.DamageSourceRegistry;
import com.sammy.malum.core.setup.content.SoundRegistry;
import com.sammy.malum.core.setup.content.SpiritTypeRegistry;
import com.sammy.malum.core.setup.content.item.ItemRegistry;
import com.sammy.malum.core.helper.RenderHelper;
import com.sammy.malum.core.setup.client.ShaderRegistry;
import com.sammy.malum.core.systems.rendering.particle.ParticleBuilders;
import com.sammy.malum.core.systems.rendering.particle.screen.base.ScreenParticle;
import com.sammy.malum.core.systems.spirit.MalumSpiritAffinity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.EntityDamageSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.gui.ForgeIngameGui;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

public class ArcaneAffinity extends MalumSpiritAffinity {
    public ArcaneAffinity() {
        super(SpiritTypeRegistry.ARCANE_SPIRIT);
    }

    public static void recoverSoulWard(TickEvent.PlayerTickEvent event) {
        Player player = event.player;
        PlayerDataCapability.getCapability(player).ifPresent(c -> {
            AttributeInstance cap = player.getAttribute(AttributeRegistry.SOUL_WARD_CAP.get());
            if (cap != null) {
                if (c.soulWard < cap.getValue() && c.soulWardProgress <= 0) {
                    c.soulWard++;
                    if (player.level.isClientSide && !player.isCreative()) {
                        player.playSound(c.soulWard >= cap.getValue() ? SoundRegistry.SOUL_WARD_CHARGE.get() : SoundRegistry.SOUL_WARD_GROW.get(), 1, Mth.nextFloat(player.getRandom(), 0.6f, 1.4f));
                    }
                    c.soulWardProgress = getSoulWardCooldown(player);
                } else {
                    c.soulWardProgress--;
                }
                if (c.soulWard > cap.getValue()) {
                    c.soulWard = (float) cap.getValue();
                }
            }
        });
    }

    public static void consumeSoulWard(LivingHurtEvent event) {
        if (event.isCanceled() || event.getAmount() <= 0) {
            return;
        }
        if (event.getEntityLiving() instanceof Player player) {
            if (!player.level.isClientSide) {
                PlayerDataCapability.getCapability(player).ifPresent(c -> {
                    AttributeInstance instance = player.getAttribute(AttributeRegistry.SOUL_WARD_SHATTER_COOLDOWN.get());
                    if (instance != null) {
                        c.soulWardProgress = (float) (CommonConfig.SOUL_WARD_RATE.get() * 6 * Math.exp(-0.15 * instance.getValue()));
                        if (c.soulWard > 0) {
                            DamageSource source = event.getSource();

                            float amount = event.getAmount();
                            float multiplier = source.isMagic() ? CommonConfig.SOUL_WARD_MAGIC.get().floatValue() : CommonConfig.SOUL_WARD_PHYSICAL.get().floatValue();
                            float result = amount * multiplier;
                            float absorbed = amount - result;
                            double strength = player.getAttributeValue(AttributeRegistry.SOUL_WARD_STRENGTH.get());
                            if (strength != 0) {
                                c.soulWard = (float) Math.max(0, c.soulWard - (absorbed / strength));
                            } else {
                                c.soulWard = 0;
                            }

                            player.level.playSound(null, player.blockPosition(), SoundRegistry.SOUL_WARD_HIT.get(), SoundSource.PLAYERS, 1, Mth.nextFloat(player.getRandom(), 1.5f, 2f));
                            event.setAmount(result);
                            if (source.getEntity() != null) {
                                if (ItemHelper.hasCurioEquipped(player, ItemRegistry.MAGEBANE_BELT)) {
                                    if (source instanceof EntityDamageSource entityDamageSource) {
                                        if (entityDamageSource.isThorns()) {
                                            return;
                                        }
                                    }
                                    source.getEntity().hurt(DamageSourceRegistry.causeMagebaneDamage(player), absorbed + 2);
                                }
                            }
                            PlayerDataCapability.syncTrackingAndSelf(player);
                        }
                    }
                });
            }
        }
    }

    public static int getSoulWardCooldown(Player player) {
        return (int) (CommonConfig.SOUL_WARD_RATE.get() * Math.exp(-0.15 * player.getAttributeValue(AttributeRegistry.SOUL_WARD_RECOVERY_SPEED.get())));
    }

    public static class ClientOnly {
        private static final ResourceLocation ICONS_TEXTURE = DataHelper.prefix("textures/gui/icons.png");

        public static void renderSoulWard(RenderGameOverlayEvent.Post event) {
            Minecraft minecraft = Minecraft.getInstance();
            LocalPlayer player = minecraft.player;
            if (event.getType() == RenderGameOverlayEvent.ElementType.ALL && !player.isCreative() && !player.isSpectator()) {
                PlayerDataCapability.getCapability(player).ifPresent(c -> {
                    PoseStack poseStack = event.getMatrixStack();
                    if (c.soulWard > 0) {
                        float absorb = Mth.ceil(player.getAbsorptionAmount());
                        float maxHealth = (float) player.getAttribute(Attributes.MAX_HEALTH).getValue();
                        float armor = (float) player.getAttribute(Attributes.ARMOR).getValue();

                        int left = event.getWindow().getGuiScaledWidth() / 2 - 91;
                        int top = event.getWindow().getGuiScaledHeight() - ((ForgeIngameGui) Minecraft.getInstance().gui).left_height;

                        if (armor == 0) {
                            top += 4;
                        }
                        int healthRows = Mth.ceil((maxHealth + absorb) / 2.0F / 10.0F);
                        int rowHeight = Math.max(10 - (healthRows - 2), 3);

                        poseStack.pushPose();
                        RenderSystem.depthMask(false);
                        RenderSystem.enableBlend();
                        RenderSystem.defaultBlendFunc();
                        RenderSystem.setShaderTexture(0, ICONS_TEXTURE);
                        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
                        ShaderInstance shaderInstance = ShaderRegistry.distortedTexture.getInstance().get();
                        shaderInstance.safeGetUniform("YFrequency").set(15f);
                        shaderInstance.safeGetUniform("XFrequency").set(15f);
                        shaderInstance.safeGetUniform("Speed").set(550f);
                        shaderInstance.safeGetUniform("Intensity").set(600f);
                        for (int i = 0; i < Math.ceil(c.soulWard / 3f); i++) {
                            int row = (int) (Math.ceil(i) / 10f);
                            int x = left + i % 10 * 8;
                            int y = top - row * 4 + rowHeight * 2 - 15;
                            int progress = Math.min(3, (int) c.soulWard - i * 3);
                            int xTextureOffset = 1 + (3 - progress) * 15;

                            shaderInstance.safeGetUniform("UVCoordinates").set(new Vector4f(xTextureOffset / 256f, (xTextureOffset + 12) / 256f, 16 / 256f, 28 / 256f));
                            shaderInstance.safeGetUniform("TimeOffset").set(i * 150f);

                            RenderHelper.blit(poseStack, ShaderRegistry.distortedTexture, x - 2, y - 2, 13, 13, 1, 1, 1, 1, xTextureOffset, 16, 256f);

                            if (ScreenParticleHandler.canSpawnParticles) {
                                ParticleBuilders.create(ScreenParticleRegistry.WISP)
                                        .setLifetime(20)
                                        .setColor(SpiritTypeRegistry.ARCANE_SPIRIT_COLOR, SpiritTypeRegistry.ARCANE_SPIRIT.endColor)
                                        .setAlphaCurveMultiplier(0.75f)
                                        .setScale(0.2f*progress, 0f)
                                        .setAlpha(0.05f, 0)
                                        .setSpin(Minecraft.getInstance().level.random.nextFloat() * 6.28f)
                                        .setSpinOffset(Minecraft.getInstance().level.random.nextFloat() * 6.28f)
                                        .randomOffset(2)
                                        .randomMotion(0.5f, 0.5f)
                                        .overwriteRenderOrder(ScreenParticle.RenderOrder.BEFORE_UI)
                                        .repeat(x + 5, y + 5, 1);
                            }
                        }
                        RenderSystem.depthMask(true);
                        RenderSystem.disableBlend();
                        poseStack.popPose();
                    }
                });
            }
        }
    }
}