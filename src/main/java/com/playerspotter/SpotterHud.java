package com.playerspotter;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

/**
 * Draws name, distance meter and arrow for every player in range.
 * It projects the player's head position onto the screen itself, so it is drawn
 * over walls and works the same on every server.
 */
public final class SpotterHud {
	private static final int METER_SEGMENTS = 10;
	private static final int SEGMENT_W = 4;
	private static final int SEGMENT_GAP = 1;

	private SpotterHud() {
	}

	/** Used by the glow mixin. Only other client-side players, only inside the chosen range. */
	public static boolean shouldGlow(Entity entity) {
		if (!(entity instanceof AbstractClientPlayerEntity other)) {
			return false;
		}
		SpotterConfig cfg = SpotterConfig.INSTANCE;
		if (!cfg.enabled || !cfg.glow) {
			return false;
		}
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc.player == null || other == mc.player) {
			return false;
		}
		if (!cfg.allows(other.getName().getString())) {
			return false;
		}
		return mc.player.distanceTo(other) <= cfg.rangeBlocks();
	}

	public static void render(DrawContext ctx, RenderTickCounter tickCounter) {
		SpotterConfig cfg = SpotterConfig.INSTANCE;
		MinecraftClient mc = MinecraftClient.getInstance();
		if (!cfg.enabled || mc.player == null || mc.world == null || mc.options.hudHidden) {
			return;
		}
		if (!cfg.showNames && !cfg.showMeter && !cfg.showArrow) {
			return;
		}

		float delta = tickCounter.getTickProgress(false);
		Entity camEntity = mc.getCameraEntity() != null ? mc.getCameraEntity() : mc.player;
		Vec3d eye = camEntity.getLerpedPos(delta).add(0, camEntity.getEyeHeight(camEntity.getPose()), 0);
		double yaw = Math.toRadians(camEntity.getYaw(delta));
		double pitch = Math.toRadians(camEntity.getPitch(delta));

		// Camera basis vectors (Minecraft: yaw 0 looks toward +Z).
		double fx = -Math.sin(yaw) * Math.cos(pitch);
		double fy = -Math.sin(pitch);
		double fz = Math.cos(yaw) * Math.cos(pitch);
		double rx = -Math.cos(yaw);
		double rz = -Math.sin(yaw);
		// up = right x forward
		double ux = -rz * fy;
		double uy = rz * fx - rx * fz;
		double uz = rx * fy;

		int scaledW = mc.getWindow().getScaledWidth();
		int scaledH = mc.getWindow().getScaledHeight();
		double aspect = (double) mc.getWindow().getFramebufferWidth() / (double) mc.getWindow().getFramebufferHeight();
		double tanHalfFov = Math.tan(Math.toRadians(mc.options.getFov().getValue()) / 2.0);

		TextRenderer tr = mc.textRenderer;
		double maxBlocks = cfg.rangeBlocks();
		// What a "full" meter means. With unlimited range, use the game's render distance.
		double fillRef = cfg.unlimitedRange
			? Math.max(2, mc.options.getClampedViewDistance()) * 16.0
			: maxBlocks;

		for (AbstractClientPlayerEntity player : mc.world.getPlayers()) {
			if (player == mc.player || player == camEntity || player.isRemoved()) {
				continue;
			}
			if (!cfg.allows(player.getName().getString())) {
				continue;
			}
			double dist = mc.player.distanceTo(player);
			if (dist > maxBlocks) {
				continue;
			}

			Vec3d head = player.getLerpedPos(delta).add(0, player.getHeight() + 0.15, 0);
			double dx = head.x - eye.x;
			double dy = head.y - eye.y;
			double dz = head.z - eye.z;

			double cx = dx * rx + dz * rz;
			double cy = dx * ux + dy * uy + dz * uz;
			double cz = dx * fx + dy * fy + dz * fz;
			if (cz <= 0.1) {
				continue; // behind the camera
			}

			double ndcX = cx / (cz * tanHalfFov * aspect);
			double ndcY = cy / (cz * tanHalfFov);
			int sx = (int) Math.round((ndcX * 0.5 + 0.5) * scaledW);
			int sy = (int) Math.round((0.5 - ndcY * 0.5) * scaledH);
			if (sx < -60 || sx > scaledW + 60 || sy < -60 || sy > scaledH + 60) {
				continue;
			}

			drawTag(ctx, tr, cfg, player, dist, fillRef, sx, sy);
		}

		// Players too far to be loaded: meter list from the locator bar data.
		SpotterFarHud.render(ctx, mc, cfg);
	}

	/** (sx, sy) is the point just above the player's head. Everything is stacked upward from there. */
	private static void drawTag(DrawContext ctx, TextRenderer tr, SpotterConfig cfg,
								AbstractClientPlayerEntity player, double dist, double maxBlocks, int sx, int sy) {
		int color = cfg.argb();
		int cur = sy;

		if (cfg.showArrow) {
			drawArrow(ctx, sx, cur - 6, color);
			cur -= 8;
		}

		if (cfg.showMeter) {
			double fill = cfg.autoFill ? Math.max(0.0, Math.min(1.0, 1.0 - dist / maxBlocks)) : 1.0;
			drawMeter(ctx, sx, cur - 5, fill, color);
			cur -= 8;

			String label = (int) Math.round(dist) + "m";
			int w = tr.getWidth(label);
			ctx.fill(sx - w / 2 - 2, cur - 10, sx + (w + 1) / 2 + 2, cur - 1, 0x80000000);
			ctx.drawText(tr, label, sx - w / 2, cur - 9, 0xFFFFFFFF, true);
			cur -= 11;
		}

		if (cfg.showNames) {
			Text name = cfg.useServerName ? player.getDisplayName() : player.getName();
			if (name == null) {
				name = player.getName();
			}
			int w = tr.getWidth(name);
			ctx.fill(sx - w / 2 - 2, cur - 10, sx + (w + 1) / 2 + 2, cur - 1, 0x80000000);
			ctx.drawText(tr, name, sx - w / 2, cur - 9, color, true);
		}
	}

	/** Downward triangle, 11 px wide and 6 px tall. The tip is at (cx, topY + 5). */
	private static void drawArrow(DrawContext ctx, int cx, int topY, int color) {
		for (int i = 0; i < 6; i++) {
			ctx.fill(cx - 5 + i, topY + 1 + i, cx + 6 - i, topY + 2 + i, 0xFF000000);
		}
		for (int i = 0; i < 6; i++) {
			ctx.fill(cx - 5 + i, topY + i, cx + 6 - i, topY + 1 + i, color);
		}
	}

	/** [||||||    ] style bar, 10 segments. */
	static void drawMeter(DrawContext ctx, int cx, int top, double fill, int color) {
		int total = METER_SEGMENTS * SEGMENT_W + (METER_SEGMENTS - 1) * SEGMENT_GAP;
		int left = cx - total / 2;
		int right = left + total;
		int filled = (int) Math.round(fill * METER_SEGMENTS);

		// Dark backing so it stays readable on bright sky or snow.
		ctx.fill(left - 4, top - 2, right + 4, top + 7, 0x80000000);

		for (int i = 0; i < METER_SEGMENTS; i++) {
			int x = left + i * (SEGMENT_W + SEGMENT_GAP);
			ctx.fill(x, top, x + SEGMENT_W, top + 5, i < filled ? color : 0xFF505050);
		}

		int white = 0xFFFFFFFF;
		// left bracket [
		ctx.fill(left - 3, top - 1, left - 2, top + 6, white);
		ctx.fill(left - 3, top - 1, left, top, white);
		ctx.fill(left - 3, top + 5, left, top + 6, white);
		// right bracket ]
		ctx.fill(right + 2, top - 1, right + 3, top + 6, white);
		ctx.fill(right, top - 1, right + 3, top, white);
		ctx.fill(right, top + 5, right + 3, top + 6, white);
	}
}
