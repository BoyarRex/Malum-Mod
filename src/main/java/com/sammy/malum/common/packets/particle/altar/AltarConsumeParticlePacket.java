package com.sammy.malum.common.packets.particle.altar;

import com.sammy.malum.core.helper.SpiritHelper;
import com.sammy.malum.core.setup.client.ParticleRegistry;
import com.sammy.malum.core.systems.rendering.particle.ParticleBuilders;
import com.sammy.malum.core.systems.spirit.MalumSpiritType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.simple.SimpleChannel;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class AltarConsumeParticlePacket {
    private final ItemStack stack;
    private final List<String> spirits;
    private final double posX;
    private final double posY;
    private final double posZ;
    private final double altarPosX;
    private final double altarPosY;
    private final double altarPosZ;

    public static void register(SimpleChannel instance, int index) {
        instance.registerMessage(index, AltarConsumeParticlePacket.class, AltarConsumeParticlePacket::encode, AltarConsumeParticlePacket::decode, AltarConsumeParticlePacket::execute);
    }

    public AltarConsumeParticlePacket(ItemStack stack, List<String> spirits, double posX, double posY, double posZ, double altarPosX, double altarPosY, double altarPosZ) {
        this.stack = stack;
        this.spirits = spirits;
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;
        this.altarPosX = altarPosX;
        this.altarPosY = altarPosY;
        this.altarPosZ = altarPosZ;
    }

    public static AltarConsumeParticlePacket decode(FriendlyByteBuf buf) {
        ItemStack stack = buf.readItem();
        int strings = buf.readInt();
        ArrayList<String> spirits = new ArrayList<>();
        for (int i = 0; i < strings; i++) {
            spirits.add(buf.readUtf());
        }
        double posX = buf.readDouble();
        double posY = buf.readDouble();
        double posZ = buf.readDouble();
        double altarPosX = buf.readDouble();
        double altarPosY = buf.readDouble();
        double altarPosZ = buf.readDouble();
        return new AltarConsumeParticlePacket(stack, spirits, posX, posY, posZ, altarPosX, altarPosY, altarPosZ);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeItem(stack);
        buf.writeInt(spirits.size());
        for (String string : spirits) {
            buf.writeUtf(string);
        }
        buf.writeDouble(posX);
        buf.writeDouble(posY);
        buf.writeDouble(posZ);
        buf.writeDouble(altarPosX);
        buf.writeDouble(altarPosY);
        buf.writeDouble(altarPosZ);
    }

    public void execute(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> ClientOnly.addParticles(stack, spirits, posX, posY, posZ, altarPosX, altarPosY, altarPosZ));
        context.get().setPacketHandled(true);
    }

    public static class ClientOnly {
        public static void addParticles(ItemStack stack, List<String> spirits, double posX, double posY, double posZ, double altarPosX, double altarPosY, double altarPosZ) {
            Level level = Minecraft.getInstance().level;
            ArrayList<MalumSpiritType> types = new ArrayList<>();
            for (String string : spirits) {
                types.add(SpiritHelper.getSpiritType(string));
            }
            float alpha = 0.1f / types.size();
            for (MalumSpiritType type : types) {
                Color color = type.color;
                Color endColor = type.endColor;
                ParticleBuilders.create(ParticleRegistry.TWINKLE_PARTICLE)
                        .setAlpha(alpha * 2, 0f)
                        .setLifetime(60)
                        .setScale(0.4f, 0)
                        .setColor(color, endColor)
                        .randomOffset(0.25f, 0.1f)
                        .randomMotion(0.004f, 0.004f)
                        .enableNoClip()
                        .repeat(level, posX, posY, posZ, 12);

                ParticleBuilders.create(ParticleRegistry.WISP_PARTICLE)
                        .setAlpha(alpha, 0f)
                        .setLifetime(30)
                        .setScale(0.2f, 0)
                        .setColor(color, endColor)
                        .randomOffset(0.05f, 0.05f)
                        .randomMotion(0.002f, 0.002f)
                        .enableNoClip()
                        .repeat(level, posX, posY, posZ, 8);

                Vec3 velocity = new Vec3(posX, posY, posZ).subtract(altarPosX, altarPosY, altarPosZ).normalize().scale(-0.05f);
                ParticleBuilders.create(ParticleRegistry.WISP_PARTICLE)
                        .setAlpha(alpha, 0f)
                        .setLifetime(40)
                        .setScale(0.3f, 0)
                        .randomOffset(0.15f)
                        .randomMotion(0.005f, 0.005f)
                        .setColor(color, color.darker())
                        .addMotion(velocity.x, velocity.y, velocity.z)
                        .enableNoClip()
                        .repeat(level, posX, posY, posZ, 36);
            }
        }
    }
}