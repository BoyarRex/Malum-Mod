package com.sammy.malum.common.item.equipment.curios;

import com.sammy.malum.core.helper.ItemHelper;
import com.sammy.malum.core.setup.content.item.ItemRegistry;
import net.minecraft.client.gui.screens.social.PlayerEntry;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.items.ItemHandlerHelper;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.UUID;

import static top.theillusivec4.curios.api.type.capability.ICurio.DropRule.ALWAYS_KEEP;

public class CurioTokenOfGratitude extends MalumCurioItem {
    public static final ArrayList<UUID> GRADITUDE_CERTIFIED = new ArrayList<>();
    public static final UUID SAMMY = getUuid("0ca54301-6170-4c44-b3e0-b8afa6b81ed2");
    public static final UUID OWL_PERSON = getUuid("309b2cf6-caa1-4c9a-912d-7b4be827dc8c");

    public CurioTokenOfGratitude(Properties builder) {
        super(builder);
    }

    public static UUID getUuid(String uuid) {
        UUID id = UUID.fromString(uuid);
        GRADITUDE_CERTIFIED.add(id);
        return id;
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (slotContext.entity() instanceof Player player) {
            if (player.getUUID().equals(SAMMY)) {
                int interval = player.isCrouching() ? 10 : 4000;
                if (player.getLevel().getGameTime() % interval == 0) {
                    SoundEvent soundEvent = player.getRandom().nextInt(8) == 0 ? SoundEvents.CAT_PURREOW : SoundEvents.CAT_PURR;
                    player.level.playSound(player, player.blockPosition(), soundEvent, SoundSource.PLAYERS, 1, 1);
                }
            }
        }
    }

    public static void giveItem(EntityJoinWorldEvent event) {
        if (event.getEntity() instanceof Player playerEntity) {
            if (!playerEntity.level.isClientSide) {
                if (GRADITUDE_CERTIFIED.stream().anyMatch(u -> u.equals(playerEntity.getUUID()))) {
                    if (ItemHelper.findCosmeticCurio(s -> s.getItem().equals(ItemRegistry.TOKEN_OF_GRATITUDE.get()), playerEntity).isEmpty()) {
                        ItemHandlerHelper.giveItemToPlayer(playerEntity, ItemRegistry.TOKEN_OF_GRATITUDE.get().getDefaultInstance());
                    }
                }
            }
        }
    }

    @Override
    public boolean isOrnate() {
        return true;
    }

    @Override
    public boolean isGilded() {
        return true;
    }

    @Nonnull
    @Override
    public ICurio.DropRule getDropRule(LivingEntity livingEntity, ItemStack stack) {
        return ALWAYS_KEEP;
    }
}