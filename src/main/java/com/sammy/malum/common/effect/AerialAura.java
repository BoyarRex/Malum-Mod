package com.sammy.malum.common.effect;

import com.sammy.malum.core.helper.ColorHelper;
import com.sammy.malum.core.setup.content.SpiritTypeRegistry;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class AerialAura extends MobEffect
{
    public AerialAura()
    {
        super(MobEffectCategory.BENEFICIAL, ColorHelper.getDecimal(SpiritTypeRegistry.AERIAL_SPIRIT_COLOR));
        addAttributeModifier(Attributes.MOVEMENT_SPEED, "e3f9c028-d6cc-4cf2-86a6-d5b5efd86be6", 0.2f, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }
    
    @Override
    public void applyEffectTick(LivingEntity entityLivingBaseIn, int amplifier)
    {
    }
}