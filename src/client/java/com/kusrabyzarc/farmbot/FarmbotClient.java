package com.kusrabyzarc.farmbot;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.consume.UseAction;
import net.minecraft.network.DisconnectionInfo;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.lwjgl.glfw.GLFW;

public class FarmbotClient implements ClientModInitializer {
	private static final KeyBinding.Category KEY_CATEGORY = KeyBinding.Category.create(
		Identifier.of(FarmbotMod.MOD_ID, "controls")
	);
	private static final String KEY_TOGGLE = "key.farmbot.toggle";
	private static final KeyBinding TOGGLE_KEY = KeyBindingHelper.registerKeyBinding(
		new KeyBinding(KEY_TOGGLE, InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F6, KEY_CATEGORY)
	);
	private static boolean suppressVanillaUseRelease;

	private boolean farmingEnabled;
	private int startTotalExperience;
	private int startLevel;

	@Override
	public void onInitializeClient() {
		FarmbotConfig.load();
		ClientTickEvents.END_CLIENT_TICK.register(this::onEndTick);
	}

	private void onEndTick(MinecraftClient client) {
		while (TOGGLE_KEY.wasPressed()) {
			if (farmingEnabled) {
				stopFarming(client, Text.translatable("message.farmbot.disabled"), null);
			} else {
				startFarming(client);
			}
		}

		if (!farmingEnabled) {
			suppressVanillaUseRelease = false;
			return;
		}

		tickFarming(client);
	}

	private void startFarming(MinecraftClient client) {
		if (client.player == null || client.world == null || client.interactionManager == null) {
			return;
		}

		ClientPlayerEntity player = client.player;
		if (!hasSwordInMainHand(player)) {
			sendMessage(client, Text.translatable("message.farmbot.no_sword"));
			return;
		}

		if (!hasFoodInOffHand(player)) {
			sendMessage(client, Text.translatable("message.farmbot.no_food_start"));
			return;
		}

		farmingEnabled = true;
		startTotalExperience = player.totalExperience;
		startLevel = player.experienceLevel;
		sendMessage(client, Text.translatable("message.farmbot.enabled"));
	}

	private void tickFarming(MinecraftClient client) {
		if (client.player == null || client.world == null || client.interactionManager == null) {
			stopFarming(client, null, null);
			return;
		}

		FarmbotConfig config = FarmbotConfig.get();
		ClientPlayerEntity player = client.player;
		if (config.escapeOnLowHealth && player.getHealth() < config.criticalHealthThreshold) {
			disconnectAsKicked(
				client,
				Text.translatable("disconnect.farmbot.low_health"),
				buildSummaryText(player)
			);
			return;
		}

		if (!hasSwordInMainHand(player)) {
			stopFarming(client, Text.translatable("message.farmbot.no_sword_stop"), null);
			return;
		}

		if (!hasFoodInOffHand(player)) {
			if (config.disconnectWhenFoodRunsOut) {
				disconnectFromServer(
					client,
					Text.translatable("message.farmbot.no_food_disconnect"),
					buildSummaryText(player)
				);
			} else {
				stopFarming(client, Text.translatable("message.farmbot.no_food_disconnect"), null);
			}
			return;
		}

		if (client.currentScreen != null || client.isPaused()) {
			stopUsingOffHandItem(client);
			return;
		}

		if (handleAutoEat(client, player)) {
			return;
		}

		if (player.getAttackCooldownProgress(0.0F) < 1.0F) {
			return;
		}

		HitResult hitResult = client.crosshairTarget;
		if (!(hitResult instanceof EntityHitResult entityHitResult)) {
			return;
		}

		Entity target = entityHitResult.getEntity();
		if (!target.isAlive()) {
			return;
		}

		client.interactionManager.attackEntity(player, target);
		player.swingHand(Hand.MAIN_HAND);
	}

	private boolean handleAutoEat(MinecraftClient client, ClientPlayerEntity player) {
		if (isUsingOffHandItem(player)) {
			suppressVanillaUseRelease = true;
			return true;
		}

		if (!hasFoodInOffHand(player) || !player.canConsume(false)) {
			suppressVanillaUseRelease = false;
			return false;
		}

		ActionResult actionResult = client.interactionManager.interactItem(player, Hand.OFF_HAND);
		boolean startedUsing = isUsingOffHandItem(player);
		suppressVanillaUseRelease = startedUsing || actionResult.isAccepted();
		return suppressVanillaUseRelease;
	}

	private void stopFarming(MinecraftClient client, Text stateMessage, Text disconnectReason) {
		if (!farmingEnabled) {
			stopUsingOffHandItem(client);
			if (disconnectReason != null) {
				client.disconnect(disconnectReason);
			}
			return;
		}

		farmingEnabled = false;
		stopUsingOffHandItem(client);

		ClientPlayerEntity player = client.player;
		Text summaryText = player != null ? buildSummaryText(player) : null;
		if (stateMessage != null) {
			sendMessage(client, stateMessage);
		}
		if (summaryText != null) {
			sendMessage(client, summaryText);
		}
		if (disconnectReason != null) {
			client.disconnect(disconnectReason);
		}
	}

	private void disconnectFromServer(MinecraftClient client, Text reason, Text summaryText) {
		Text disconnectMessage = summaryText == null
			? reason
			: reason.copy().append(Text.literal("\n")).append(summaryText);
		stopFarming(client, reason, disconnectMessage);
	}

	private void disconnectAsKicked(MinecraftClient client, Text reason, Text summaryText) {
		farmingEnabled = false;
		stopUsingOffHandItem(client);

		if (summaryText != null) {
			sendMessage(client, summaryText);
		}

		ClientPlayNetworkHandler networkHandler = client.getNetworkHandler();
		if (networkHandler == null) {
			client.disconnect(reason);
			return;
		}

		DisconnectionInfo disconnectionInfo = new DisconnectionInfo(reason);
		networkHandler.getConnection().disconnect(disconnectionInfo);
		networkHandler.getConnection().handleDisconnection();
	}

	private Text buildSummaryText(ClientPlayerEntity player) {
		int gainedExperience = Math.max(0, player.totalExperience - startTotalExperience);
		int gainedLevels = Math.max(0, player.experienceLevel - startLevel);
		return Text.translatable("message.farmbot.summary", gainedExperience, gainedLevels);
	}

	private boolean hasSwordInMainHand(ClientPlayerEntity player) {
		return player.getMainHandStack().isIn(ItemTags.SWORDS);
	}

	private boolean hasFoodInOffHand(ClientPlayerEntity player) {
		ItemStack offHandStack = player.getOffHandStack();
		if (offHandStack.isEmpty()) {
			return false;
		}

		return offHandStack.getUseAction() == UseAction.EAT;
	}

	private boolean isUsingOffHandItem(ClientPlayerEntity player) {
		return player.isUsingItem() && player.getActiveHand() == Hand.OFF_HAND;
	}

	private void stopUsingOffHandItem(MinecraftClient client) {
		suppressVanillaUseRelease = false;

		if (client.player == null || client.interactionManager == null || !isUsingOffHandItem(client.player)) {
			return;
		}

		client.interactionManager.stopUsingItem(client.player);
	}

	public static boolean shouldSuppressVanillaUseRelease(ClientPlayerEntity player) {
		return suppressVanillaUseRelease && player.isUsingItem() && player.getActiveHand() == Hand.OFF_HAND;
	}

	private void sendMessage(MinecraftClient client, Text message) {
		if (client.player != null) {
			client.player.sendMessage(message, false);
		}
	}
}
