package com.kusrabyzarc.farmbot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

public final class FarmbotConfig {
	public static final float MIN_CRITICAL_HEALTH_THRESHOLD = 2.0F;
	public static final float MAX_CRITICAL_HEALTH_THRESHOLD = 20.0F;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG_PATH = FabricLoader.getInstance()
		.getConfigDir()
		.resolve(FarmbotMod.MOD_ID + ".json");

	private static FarmbotConfig instance = defaults();

	public boolean escapeOnLowHealth = true;
	public float criticalHealthThreshold = 10.0F;
	public boolean disconnectWhenFoodRunsOut = true;

	public static FarmbotConfig get() {
		return instance;
	}

	public static FarmbotConfig defaults() {
		return new FarmbotConfig();
	}

	public static void load() {
		if (!Files.exists(CONFIG_PATH)) {
			instance = defaults();
			save();
			return;
		}

		try {
			String json = Files.readString(CONFIG_PATH, StandardCharsets.UTF_8);
			FarmbotConfig loaded = GSON.fromJson(json, FarmbotConfig.class);
			instance = loaded != null ? loaded : defaults();
			instance.sanitize();
		} catch (Exception exception) {
			FarmbotMod.LOGGER.warn("Failed to load config from {}", CONFIG_PATH, exception);
			instance = defaults();
		}
	}

	public static void save() {
		instance.sanitize();

		try {
			Files.createDirectories(CONFIG_PATH.getParent());
			Files.writeString(CONFIG_PATH, GSON.toJson(instance), StandardCharsets.UTF_8);
		} catch (IOException exception) {
			FarmbotMod.LOGGER.warn("Failed to save config to {}", CONFIG_PATH, exception);
		}
	}

	public FarmbotConfig copy() {
		FarmbotConfig copy = new FarmbotConfig();
		copy.escapeOnLowHealth = this.escapeOnLowHealth;
		copy.criticalHealthThreshold = this.criticalHealthThreshold;
		copy.disconnectWhenFoodRunsOut = this.disconnectWhenFoodRunsOut;
		return copy;
	}

	public void copyFrom(FarmbotConfig other) {
		this.escapeOnLowHealth = other.escapeOnLowHealth;
		this.criticalHealthThreshold = other.criticalHealthThreshold;
		this.disconnectWhenFoodRunsOut = other.disconnectWhenFoodRunsOut;
		this.sanitize();
	}

	public void sanitize() {
		this.criticalHealthThreshold = Math.max(
			MIN_CRITICAL_HEALTH_THRESHOLD,
			Math.min(MAX_CRITICAL_HEALTH_THRESHOLD, this.criticalHealthThreshold)
		);
	}
}
