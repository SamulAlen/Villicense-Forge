package com.plr.villicense.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Client-only helpers, e.g. resolving the localized name of the Sneak key for tooltips. */
@OnlyIn(Dist.CLIENT)
public final class VillicenseClient {
    private VillicenseClient() {
    }

    public static Component getName4CrouchingKey() {
        return Minecraft.getInstance().options.keyShift.getTranslatedKeyMessage()
                .copy().withStyle(style -> style.withColor(ChatFormatting.YELLOW));
    }
}
