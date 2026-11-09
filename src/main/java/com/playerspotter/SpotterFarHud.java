package com.playerspotter;

import com.mojang.datafixers.util.Either;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.world.waypoint.TrackedWaypoint;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Meter list for players who are too far away to be loaded in your game.
 *
 * The server sends the locator bar data (the player's waypoint) even when the player
 * is far outside your render distance and their chunk is not loaded. This list reads
 * that data and shows [meter] name distance, top left of the screen.
 *
 * Limits: the server must have the locator bar on, and players who sneak, wear a
 * pumpkin or mob head, or are invisible are hidden by the server. Far players may only
 * give a direction, with no distance ("direction only").
 */
public final class SpotterFarHud {
	/** The far bar is full next to you and empty at this distance. */
	private static final double FAR_EMPTY_BLOCKS = 2000.0;
	private static final int MAX_ROWS = 8;
	private static boolean reported = false;

	private SpotterFarHud() {
	}

	private record Row(String name, double dist) {
	}

	public static void render(DrawContext ctx, MinecraftClient mc, SpotterConfig cfg) {
		if (!cfg.showMeter || mc.player == null || mc.world == null) {
			return;
		}
		ClientPlayNetworkHandler handler = mc.getNetworkHandler();
		if (handler == null) {
			return;
		}
		try {
			List<Row> rows = collectRows(mc, cfg, handler);
			if (rows.isEmpty()) {
				return;
			}
			TextRenderer tr = mc.textRenderer;
			int x0 = 8;
			int y = 30;
			ctx.drawText(tr, "Far players", x0, y, 0xFFAAAAAA, true);
			y += 12;
			for (Row row : rows) {
				double d = row.dist();
				boolean known = !Double.isNaN(d);
				double fill = known ? Math.max(0.0, Math.min(1.0, 1.0 - d / FAR_EMPTY_BLOCKS)) : 0.0;
				if (!cfg.autoFill) {
					fill = 1.0;
				}
				SpotterHud.drawMeter(ctx, x0 + 28, y + 2, fill, cfg.argb());
				String text = row.name() + "  " + (known ? Math.round(d) + "m" : "direction only");
				ctx.drawText(tr, text, x0 + 64, y, 0xFFFFFFFF, true);
				y += 12;
			}
		} catch (Throwable t) {
			if (!reported) {
				reported = true;
				System.err.println("[PlayerSpotter] Far player list failed: " + t);
			}
		}
	}

	private static List<Row> collectRows(MinecraftClient mc, SpotterConfig cfg, ClientPlayNetworkHandler handler) {
		Set<UUID> loaded = new HashSet<>();
		for (AbstractClientPlayerEntity p : mc.world.getPlayers()) {
			loaded.add(p.getUuid());
		}
		Set<UUID> seen = new HashSet<>();
		List<Row> rows = new ArrayList<>();

		for (TrackedWaypoint waypoint : findWaypoints(handler.getWaypointHandler())) {
			Either<UUID, String> source = waypoint.getSource();
			Optional<UUID> id = source.left();
			if (id.isEmpty()) {
				continue;
			}
			UUID uuid = id.get();
			// Loaded players already get the tag above their head.
			if (uuid.equals(mc.player.getUuid()) || loaded.contains(uuid) || !seen.add(uuid)) {
				continue;
			}
			PlayerListEntry entry = handler.getPlayerListEntry(uuid);
			String name = entry != null ? SpotterPlayersScreen.profileName(entry.getProfile()) : null;
			if (name == null || !cfg.allows(name)) {
				continue;
			}
			double d2 = waypoint.squaredDistanceTo(mc.player);
			double dist = (Double.isNaN(d2) || Double.isInfinite(d2)) ? Double.NaN : Math.sqrt(d2);
			rows.add(new Row(name, dist));
		}

		rows.sort(Comparator.comparingDouble(r -> Double.isNaN(r.dist()) ? Double.MAX_VALUE : r.dist()));
		return rows.size() > MAX_ROWS ? new ArrayList<>(rows.subList(0, MAX_ROWS)) : rows;
	}

	/**
	 * Finds the waypoints stored inside the game's waypoint handler by looking at its
	 * fields by type, so it does not depend on the field's name in this version.
	 */
	private static List<TrackedWaypoint> findWaypoints(Object handler) {
		List<TrackedWaypoint> out = new ArrayList<>();
		if (handler == null) {
			return out;
		}
		Class<?> type = handler.getClass();
		while (type != null && type != Object.class) {
			for (Field field : type.getDeclaredFields()) {
				if (Modifier.isStatic(field.getModifiers())) {
					continue;
				}
				try {
					field.setAccessible(true);
					collect(field.get(handler), out, 0);
				} catch (Exception ignored) {
					// skip fields we cannot read
				}
			}
			type = type.getSuperclass();
		}
		return out;
	}

	private static void collect(Object value, List<TrackedWaypoint> out, int depth) {
		if (value == null || depth > 2) {
			return;
		}
		if (value instanceof TrackedWaypoint w) {
			out.add(w);
		} else if (value instanceof Map<?, ?> map) {
			for (Object v : map.values()) {
				collect(v, out, depth + 1);
			}
		} else if (value instanceof Iterable<?> it) {
			for (Object v : it) {
				collect(v, out, depth + 1);
			}
		}
	}
}
