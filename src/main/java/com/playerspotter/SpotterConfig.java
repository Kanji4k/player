package com.playerspotter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Saved to .minecraft/config/playerspotter.json */
public class SpotterConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("playerspotter.json");

	public static SpotterConfig INSTANCE = new SpotterConfig();

	public boolean enabled = true;
	public boolean glow = true;
	public boolean showNames = true;
	public boolean showMeter = true;
	public boolean showArrow = true;
	/** Meter bar fills up automatically: closer player = fuller bar. */
	public boolean autoFill = true;
	/** Use the name as the server shows it (team prefix, nickname) instead of the raw account name. */
	public boolean useServerName = true;
	/** 1 to 16 chunks. Ignored while unlimitedRange is on. */
	public int rangeChunks = 16;
	/** No distance limit: glow, name, meter and arrow show for every loaded listed player. */
	public boolean unlimitedRange = true;
	/** When true, only the players in the list below are shown. */
	public boolean onlyListed = true;
	public java.util.List<String> players = new java.util.ArrayList<>();
	public int red = 255;
	public int green = 64;
	public int blue = 64;

	public int rgb() {
		return (clamp(red) << 16) | (clamp(green) << 8) | clamp(blue);
	}

	public int argb() {
		return 0xFF000000 | rgb();
	}

	/** True if this player should get glow / name / meter / arrow. */
	public boolean allows(String name) {
		if (!onlyListed) {
			return true;
		}
		if (players == null) {
			return false;
		}
		for (String p : players) {
			if (p != null && p.equalsIgnoreCase(name)) {
				return true;
			}
		}
		return false;
	}

	public double rangeBlocks() {
		if (unlimitedRange) {
			return Double.MAX_VALUE;
		}
		return Math.max(1, Math.min(16, rangeChunks)) * 16.0;
	}

	private static int clamp(int v) {
		return Math.max(0, Math.min(255, v));
	}

	public static void load() {
		try {
			if (Files.exists(PATH)) {
				SpotterConfig loaded = GSON.fromJson(Files.readString(PATH), SpotterConfig.class);
				if (loaded != null) {
					loaded.rangeChunks = Math.max(1, Math.min(16, loaded.rangeChunks));
					INSTANCE = loaded;
				}
			} else {
				save();
			}
		} catch (Exception e) {
			System.err.println("[PlayerSpotter] Could not read config, using defaults: " + e);
		}
	}

	public static void save() {
		try {
			Files.writeString(PATH, GSON.toJson(INSTANCE));
		} catch (IOException e) {
			System.err.println("[PlayerSpotter] Could not save config: " + e);
		}
	}
}
