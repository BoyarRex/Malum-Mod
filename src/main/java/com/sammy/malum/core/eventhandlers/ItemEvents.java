package com.sammy.malum.core.eventhandlers;

import com.sammy.malum.MalumMod;
import com.sammy.malum.common.entity.boomerang.ScytheBoomerangEntity;
import com.sammy.malum.common.item.equipment.CeaselessImpetusItem;
import com.sammy.malum.core.registry.AttributeRegistry;
import com.sammy.malum.core.registry.enchantment.MalumEnchantments;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid= MalumMod.MODID, bus= Mod.EventBusSubscriber.Bus.FORGE)
public class ItemEvents {

    @SubscribeEvent
    public static void triggerCeaselessImpetus(LivingDeathEvent event) {
        if (event.getEntityLiving() instanceof Player player) {
            if (CeaselessImpetusItem.checkTotemDeathProtection(player, event.getSource())) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void throwScythe(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntityLiving() instanceof Player player) {
            ItemStack stack = event.getItemStack();
            int enchantmentLevel = EnchantmentHelper.getItemEnchantmentLevel(MalumEnchantments.REBOUND.get(), stack);
            if (enchantmentLevel > 0) {
                Level level = player.level;
                if (!level.isClientSide) {
                    player.setItemInHand(event.getHand(), ItemStack.EMPTY);
                    double baseDamage = player.getAttributes().getValue(Attributes.ATTACK_DAMAGE);
                    float multiplier = 1.2f;
                    double damage = 1.0F + baseDamage * multiplier;

                    int slot = event.getHand() == InteractionHand.OFF_HAND ? player.getInventory().getContainerSize() - 1 : player.getInventory().selected;
                    ScytheBoomerangEntity entity = new ScytheBoomerangEntity(level);
                    entity.setPos(player.position().x, player.position().y + player.getBbHeight() / 2f, player.position().z);

                    entity.setData((float) damage, player.getUUID(), slot, stack);
                    entity.getEntityData().set(ScytheBoomerangEntity.SCYTHE, stack);

                    entity.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, (float) (1.5F + player.getAttributeValue(AttributeRegistry.SCYTHE_PROFICIENCY) * 0.125f), 0F);
                    level.addFreshEntity(entity);
                }
                player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
            }
        }
    }
}