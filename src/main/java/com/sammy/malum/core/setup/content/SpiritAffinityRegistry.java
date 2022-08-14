package com.sammy.malum.core.setup.content;

import com.sammy.malum.common.spiritaffinity.ArcaneAffinity;
import com.sammy.malum.common.spiritaffinity.EarthenAffinity;
import com.sammy.malum.core.systems.spirit.MalumSpiritAffinity;

import java.util.HashMap;
import java.util.Map;

public class SpiritAffinityRegistry
{
    public static Map<String, MalumSpiritAffinity> AFFINITIES = new HashMap<>();

    public static final MalumSpiritAffinity ARCANE_AFFINITY = create(new ArcaneAffinity());
    public static final MalumSpiritAffinity EARTHEN_AFFINITY = create(new EarthenAffinity());

    public static MalumSpiritAffinity create(MalumSpiritAffinity affinity)
    {
        AFFINITIES.put(affinity.identifier, affinity);
        return affinity;
    }
}
