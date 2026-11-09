package com.playerspotter;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/** The settings screen. Opens from Mod Menu or with the K key. */
public class SpotterScreen extends Screen {
	private static final String[] PRESET_NAMES = {"Red", "Orange", "Yellow", "Green", "Cyan", "Blue", "Purple", "Pink", "White"};
	private static final int[][] PRESETS = {
		{255, 64, 64}, {255, 150, 30}, {255, 235, 40}, {70, 230, 70}, {50, 230, 230},
		{70, 110, 255}, {170, 80, 255}, {255, 110, 200}, {255, 255, 255}
	};

	private final Screen parent;
	private int presetIndex = 0;

	public SpotterScreen(Screen parent) {
		super(Text.translatable("screen.playerspotter.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		SpotterConfig c = SpotterConfig.INSTANCE;
		int colW = 150;
		int left = this.width / 2 - 155;
		int right = this.width / 2 + 5;
		int y = Math.max(28, this.height / 2 - 100);
		int step = 22;

		addDrawableChild(toggle(left, y, colW, "option.playerspotter.enabled", () -> c.enabled, v -> c.enabled = v));
		addDrawableChild(toggle(right, y, colW, "option.playerspotter.glow", () -> c.glow, v -> c.glow = v));
		y += step;
		addDrawableChild(toggle(left, y, colW, "option.playerspotter.names", () -> c.showNames, v -> c.showNames = v));
		addDrawableChild(toggle(right, y, colW, "option.playerspotter.meter", () -> c.showMeter, v -> c.showMeter = v));
		y += step;
		addDrawableChild(toggle(left, y, colW, "option.playerspotter.arrow", () -> c.showArrow, v -> c.showArrow = v));
		addDrawableChild(toggle(right, y, colW, "option.playerspotter.autofill", () -> c.autoFill, v -> c.autoFill = v));
		y += step;
		addDrawableChild(toggle(left, y, colW, "option.playerspotter.displayname", () -> c.useServerName, v -> c.useServerName = v));
		addDrawableChild(ButtonWidget.builder(Text.literal("Color preset: " + PRESET_NAMES[presetIndex]), b -> {
			presetIndex = (presetIndex + 1) % PRESETS.length;
			c.red = PRESETS[presetIndex][0];
			c.green = PRESETS[presetIndex][1];
			c.blue = PRESETS[presetIndex][2];
			clearAndInit(); // refresh the RGB sliders
		}).dimensions(right, y, colW, 20).build());
		y += step;

		String listInfo = c.onlyListed ? c.players.size() + " listed" : "all players";
		addDrawableChild(ButtonWidget.builder(Text.literal("Choose players... (" + listInfo + ")"),
			b -> this.client.setScreen(new SpotterPlayersScreen(this))).dimensions(left, y, 310, 20).build());
		y += step;

		IntSlider rangeSlider = new IntSlider(right, y, colW, 20, "option.playerspotter.range", 1, 16, c.rangeChunks, v -> c.rangeChunks = v);
		rangeSlider.active = !c.unlimitedRange;
		addDrawableChild(ButtonWidget.builder(unlimitedLabel(c.unlimitedRange), b -> {
			c.unlimitedRange = !c.unlimitedRange;
			b.setMessage(unlimitedLabel(c.unlimitedRange));
			rangeSlider.active = !c.unlimitedRange;
		}).dimensions(left, y, colW, 20).build());
		addDrawableChild(rangeSlider);
		y += step;
		addDrawableChild(new IntSlider(left, y, colW, 20, "option.playerspotter.red", 0, 255, c.red, v -> c.red = v));
		addDrawableChild(new IntSlider(right, y, colW, 20, "option.playerspotter.green", 0, 255, c.green, v -> c.green = v));
		y += step;
		addDrawableChild(new IntSlider(left, y, colW, 20, "option.playerspotter.blue", 0, 255, c.blue, v -> c.blue = v));
		addDrawableChild(ButtonWidget.builder(Text.literal("Reset to defaults"), b -> {
			SpotterConfig.INSTANCE = new SpotterConfig();
			clearAndInit();
		}).dimensions(right, y, colW, 20).build());
		y += step + 4;

		addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> close())
			.dimensions(this.width / 2 - 100, y, 200, 20).build());
	}

	private ButtonWidget toggle(int x, int y, int w, String key, BooleanSupplier get, Consumer<Boolean> set) {
		return ButtonWidget.builder(label(key, get.getAsBoolean()), b -> {
			set.accept(!get.getAsBoolean());
			b.setMessage(label(key, get.getAsBoolean()));
		}).dimensions(x, y, w, 20).build();
	}

	private static Text unlimitedLabel(boolean on) {
		return Text.literal("Unlimited range: ")
			.append(Text.literal(on ? "ON" : "OFF").formatted(on ? Formatting.GREEN : Formatting.RED));
	}

	private static Text label(String key, boolean on) {
		return Text.translatable(key)
			.append(Text.literal(": "))
			.append(Text.literal(on ? "ON" : "OFF").formatted(on ? Formatting.GREEN : Formatting.RED));
	}

	@Override
	public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
		super.render(ctx, mouseX, mouseY, delta);
		ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 12, 0xFFFFFFFF);
		// color swatch next to the title
		int sx = this.width / 2 + 60;
		ctx.fill(sx - 1, 9, sx + 25, 23, 0xFFFFFFFF);
		ctx.fill(sx, 10, sx + 24, 22, SpotterConfig.INSTANCE.argb());
	}

	@Override
	public void close() {
		this.client.setScreen(this.parent);
	}

	@Override
	public void removed() {
		SpotterConfig.save();
	}

	private static class IntSlider extends SliderWidget {
		private final String key;
		private final int min;
		private final int max;
		private final IntConsumer onChange;

		IntSlider(int x, int y, int w, int h, String key, int min, int max, int value, IntConsumer onChange) {
			super(x, y, w, h, Text.empty(), (double) (value - min) / (max - min));
			this.key = key;
			this.min = min;
			this.max = max;
			this.onChange = onChange;
			updateMessage();
		}

		private int current() {
			return min + (int) Math.round(this.value * (max - min));
		}

		@Override
		protected void updateMessage() {
			if (key == null) {
				return;
			}
			setMessage(Text.translatable(key, current()));
		}

		@Override
		protected void applyValue() {
			onChange.accept(current());
		}
	}
}
