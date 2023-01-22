package com.sammy.malum.common.spiritrite.greater;

import com.sammy.malum.common.blockentity.totem.TotemBaseBlockEntity;
import com.sammy.malum.common.packets.particle.entity.MajorEntityEffectParticlePacket;
import com.sammy.malum.registry.common.DamageSourceRegistry;
import com.sammy.malum.core.systems.rites.EntityAffectingRiteEffect;
import com.sammy.malum.core.systems.rites.MalumRiteEffect;
import com.sammy.malum.core.systems.rites.MalumRiteType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.sammy.malum.registry.common.SpiritTypeRegistry.*;
import static com.sammy.malum.registry.common.PacketRegistry.MALUM_CHANNEL;

public class EldritchWickedRiteType extends MalumRiteType {
    public EldritchWickedRiteType() {
        super("greater_wicked_rite", ELDRITCH_SPIRIT, ARCANE_SPIRIT, WICKED_SPIRIT, WICKED_SPIRIT);
    }

    @Override
    public MalumRiteEffect getNaturalRiteEffect() {
        return new EntityAffectingRiteEffect() {
            @Override
            public void riteEffect(TotemBaseBlockEntity totemBase) {
                getNearbyEntities(totemBase, LivingEntity.class).forEach(e -> {
                    if (e.getHealth() <= 2.5f && !e.isInvulnerableTo(DamageSourceRegistry.VOODOO)) {
                        MALUM_CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> e), new MajorEntityEffectParticlePacket(getEffectSpirit().getColor(), e.getX(), e.getY()+ e.getBbHeight() / 2f, e.getZ()));
                        e.hurt(DamageSourceRegistry.SOUL_STRIKE, 10f);
                    }
                });
            }
        };
    }

    @Override
    public MalumRiteEffect getCorruptedEffect() {
        return new EntityAffectingRiteEffect() {
            @Override
            public void riteEffect(TotemBaseBlockEntity totemBase) {
                final List<Animal> nearbyEntities = getNearbyEntities(totemBase, Animal.class, e -> e.getAge() > 0 && !e.isInvulnerableTo(DamageSourceRegistry.VOODOO)).collect(Collectors.toList());
                if (nearbyEntities.size() < 30) {
                    return;
                }
                int maxKills = nearbyEntities.size() - 30;
                nearbyEntities.removeIf(Animal::isInLove);
                for (Animal entity : nearbyEntities) {
                    entity.hurt(DamageSourceRegistry.VOODOO, entity.getMaxHealth());
                    MALUM_CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), new MajorEntityEffectParticlePacket(WICKED_SPIRIT.getColor(), entity.getX(), entity.getY() + entity.getBbHeight() / 2f, entity.getZ()));
                    if (maxKills-- <= 0) {
                        return;
                    }
                }
            }
        };
    }
}