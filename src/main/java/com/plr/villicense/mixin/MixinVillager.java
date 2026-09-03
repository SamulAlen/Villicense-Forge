package com.plr.villicense.mixin;

import com.plr.villicense.Villicense;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * When a player right-clicks a villager while holding the license, {@link Villager#mobInteract}
 * would normally open the trading screen and consume the interaction before the item's
 * {@code interactLivingEntity} runs. This mixin short-circuits that so the license item can handle
 * the interaction itself (same as the 1.20.1 original).
 */
@Mixin(Villager.class)
public abstract class MixinVillager extends AbstractVillager {
    public MixinVillager(EntityType<? extends AbstractVillager> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
    private void villicense$mobInteract(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (player.getMainHandItem().is(Villicense.VILLAGER_LICENSE.get())) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }
}
