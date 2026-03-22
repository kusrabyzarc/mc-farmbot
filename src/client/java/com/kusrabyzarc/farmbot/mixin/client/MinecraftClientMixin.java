package com.kusrabyzarc.farmbot.mixin.client;

import com.kusrabyzarc.farmbot.FarmbotClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(net.minecraft.client.MinecraftClient.class)
public abstract class MinecraftClientMixin {
	@Redirect(
		method = "handleInputEvents",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;stopUsingItem(Lnet/minecraft/entity/player/PlayerEntity;)V"
		)
	)
	private void farmbot$suppressVanillaReleaseDuringAutoEat(
		ClientPlayerInteractionManager interactionManager,
		PlayerEntity player
	) {
		if (player instanceof ClientPlayerEntity clientPlayer
			&& FarmbotClient.shouldSuppressVanillaUseRelease(clientPlayer)) {
			return;
		}

		interactionManager.stopUsingItem(player);
	}
}
