package com.sammy.malum.common.packets;

import com.sammy.malum.registry.common.*;
import com.sammy.malum.visual_effects.networked.*;
import com.sammy.malum.visual_effects.networked.data.*;
import net.minecraft.client.*;
import net.minecraft.client.multiplayer.*;
import net.minecraft.nbt.*;
import net.minecraft.network.*;
import net.minecraftforge.api.distmarker.*;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.*;
import team.lodestar.lodestone.systems.network.*;

import javax.annotation.*;
import java.util.function.*;

public class ParticleEffectPacket extends LodestoneClientPacket {

    private final String id;
    private final PositionEffectData positionData;
    @Nullable
    private final ColorEffectData colorData;
    @Nullable
    private final NBTEffectData nbtData;

    public ParticleEffectPacket(String id, PositionEffectData positionData, @Nullable ColorEffectData colorData, @Nullable NBTEffectData nbtData) {
        this.id = id;
        this.positionData = positionData;
        this.colorData = colorData;
        this.nbtData = nbtData;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(id);
        positionData.encode(buf);
        boolean nonNullColorData = colorData != null;
        buf.writeBoolean(nonNullColorData);
        if (nonNullColorData) {
            colorData.encode(buf);
        }
        boolean nonNullCompoundTag = nbtData != null;
        buf.writeBoolean(nonNullCompoundTag);
        if (nonNullCompoundTag) {
            buf.writeNbt(nbtData.compoundTag);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public void execute(Supplier<NetworkEvent.Context> context) {
        Minecraft instance = Minecraft.getInstance();
        ClientLevel level = instance.level;
        ParticleEffectType particleEffectType = ParticleEffectTypeRegistry.EFFECT_TYPES.get(id);
        if (particleEffectType == null) {
            throw new RuntimeException("This shouldn't be happening.");
        }
        ParticleEffectType.ParticleEffectActor particleEffectActor = particleEffectType.get().get();
        particleEffectActor.act(level, level.random, positionData, colorData, nbtData);
    }

    public static void register(SimpleChannel instance, int index) {
        instance.registerMessage(index, ParticleEffectPacket.class, ParticleEffectPacket::encode, ParticleEffectPacket::decode, ParticleEffectPacket::handle);
    }

    public static ParticleEffectPacket decode(FriendlyByteBuf buf) {
        return new ParticleEffectPacket(buf.readUtf(), new PositionEffectData(buf), buf.readBoolean() ? new ColorEffectData(buf) : null, buf.readBoolean() ? new NBTEffectData(buf.readNbt()) : null);
    }
}