package com.sammy.malum.common.item.augment;

import com.sammy.malum.registry.common.*;

public class BlazingDiodeItem extends AbstractAugmentItem{
    public BlazingDiodeItem(Properties pProperties) {
        super(pProperties, SpiritTypeRegistry.INFERNAL_SPIRIT);
    }

    @Override
    public float getFuelUsageRateIncrease() {
        return -0.25f;
    }

    @Override
    public float getSpeedIncrease() {
        return 0.25f;
    }
}
