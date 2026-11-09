package com.playerspotter;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/**
 * Type a player name and press Add. Only the players in this list get glow,
 * name, meter and arrow.
 *
 * Auto fill: while you type, players who are online on the server (the tab list)
 * show up as buttons. Click one to fill the name, then press Add.
 */
public class SpotterPlayersScreen extends Screen {
	private static final int MAX_NAMES = 12;
	private static final int SUGGESTIONS = 3;

	private final Screen parent;
	private String typed = "";
	private String status = "";
	private int statusColor = 0xFF55FF55;
	private TextFieldWidget input;
	private final ButtonWidget[] suggestButtons = new ButtonWidget[SUGGESTIONS];
	private final String[] suggestNames = new String[SUGGESTIONS];
	private int listEnd;

	public SpotterPlayersScreen(Screen parent) {
		super(Text.literal("Choose players"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		SpotterConfig c = SpotterConfig.INSTANCE;
		int left = this.width / 2 - 155;
		int right = this.width / 2 + 5;

		input = new TextFieldWidget(this.textRenderer, left, 34, 200, 20, Text.literal("Player name"));
		input.setMaxLength(20);
		input.setPlaceholder(Text.literal("Type player name..."));
		input.setText(typed);
		input.setChangedListener(s -> {
			typed = s;
			status = "";
			refreshSuggestions();
		});
		addDrawableChild(input);
		setInitialFocus(input);

		addDrawableChild(ButtonWidget.builder(Text.literal("Add"), b -> addTyped())
			.dimensions(left + 205, 34, 105, 20).build());

		for (int i = 0; i < SUGGESTIONS; i++) {
			final int idx = i;
			suggestButtons[i] = ButtonWidget.builder(Text.empty(), b -> {
				if (suggestNames[idx] != null) {
					input.setText(suggestNames[idx]);
				}
			}).dimensions(left + i * 105, 58, 100, 20).build();
			suggestButtons[i].visible = false;
			addDrawableChild(suggestButtons[i]);
		}

		addDrawableChild(ButtonWidget.builder(onlyLabel(c.onlyListed), b -> {
			c.onlyListed = !c.onlyListed;
			b.setMessage(onlyLabel(c.onlyListed));
		}).dimensions(left, 84, 310, 20).build());

		// The saved list: click a name to remove it. Green = loaded near you right now.
		int rows = 0;
		for (int i = 0; i < c.players.size() && i < MAX_NAMES; i++) {
			final String name = c.players.get(i);
			int x = (i % 2 == 0) ? left : right;
			int y = 112 + (i / 2) * 22;
			Text label = Text.literal("[x] ")
				.append(Text.literal(name).formatted(isLoaded(name) ? Formatting.GREEN : Formatting.GRAY));
			addDrawableChild(ButtonWidget.builder(label, b -> {
				c.players.removeIf(p -> p.equalsIgnoreCase(name));
				SpotterConfig.save();
				status = "Removed " + name;
				statusColor = 0xFFFFAA00;
				clearAndInit();
			}).dimensions(x, y, 150, 20).build());
			rows = i / 2 + 1;
		}

		listEnd = 112 + Math.max(rows, 1) * 22;
		addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> close())
			.dimensions(this.width / 2 - 100, listEnd + 16, 200, 20).build());

		refreshSuggestions();
	}

	private static Text onlyLabel(boolean on) {
		return Text.literal("Only listed players: ")
			.append(Text.literal(on ? "ON" : "OFF (show everyone)").formatted(on ? Formatting.GREEN : Formatting.RED));
	}

	private void addTyped() {
		String name = typed.trim();
		SpotterConfig c = SpotterConfig.INSTANCE;
		if (name.isEmpty()) {
			status = "Type a name first";
			statusColor = 0xFFFF5555;
			return;
		}
		if (c.players.size() >= MAX_NAMES) {
			status = "List is full (" + MAX_NAMES + " names). Remove one first.";
			statusColor = 0xFFFF5555;
			return;
		}
		for (String p : c.players) {
			if (p.equalsIgnoreCase(name)) {
				status = name + " is already in the list";
				statusColor = 0xFFFFAA00;
				typed = "";
				clearAndInit();
				return;
			}
		}
		c.players.add(name);
		SpotterConfig.save();
		status = "Added " + name + (isLoaded(name) ? " (near you now)" : " (not loaded near you yet)");
		statusColor = 0xFF55FF55;
		typed = "";
		clearAndInit();
	}

	/** True if this player is loaded in your game right now, so glow/meter can show. */
	private static boolean isLoaded(String name) {
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc.world == null) {
			return false;
		}
		for (AbstractClientPlayerEntity p : mc.world.getPlayers()) {
			if (p.getName().getString().equalsIgnoreCase(name)) {
				return true;
			}
		}
		return false;
	}

	/** Players near you first, then everyone in the server tab list. Never yourself. */
	private static List<String> onlineNames() {
		MinecraftClient mc = MinecraftClient.getInstance();
		LinkedHashSet<String> out = new LinkedHashSet<>();
		String me = mc.player != null ? mc.player.getName().getString() : "";
		if (mc.world != null) {
			for (AbstractClientPlayerEntity p : mc.world.getPlayers()) {
				out.add(p.getName().getString());
			}
		}
		if (mc.getNetworkHandler() != null) {
			for (PlayerListEntry e : mc.getNetworkHandler().getPlayerList()) {
				String n = profileName(e.getProfile());
				if (n != null && !n.isEmpty()) {
					out.add(n);
				}
			}
		}
		out.removeIf(n -> n.equalsIgnoreCase(me));
		return new ArrayList<>(out);
	}

	/** Reads the account name from a GameProfile without caring how this version names the method. */
	static String profileName(Object profile) {
		if (profile == null) {
			return null;
		}
		for (String method : new String[] {"name", "getName"}) {
			try {
				Object r = profile.getClass().getMethod(method).invoke(profile);
				if (r instanceof String s) {
					return s;
				}
			} catch (Exception ignored) {
				// try the next name
			}
		}
		return null;
	}

	private void refreshSuggestions() {
		for (int i = 0; i < SUGGESTIONS; i++) {
			suggestNames[i] = null;
			if (suggestButtons[i] != null) {
				suggestButtons[i].visible = false;
			}
		}
		String typedLower = typed.trim().toLowerCase(Locale.ROOT);
		SpotterConfig c = SpotterConfig.INSTANCE;
		List<String> starts = new ArrayList<>();
		List<String> contains = new ArrayList<>();
		for (String n : onlineNames()) {
			boolean listed = false;
			for (String p : c.players) {
				if (p.equalsIgnoreCase(n)) {
					listed = true;
					break;
				}
			}
			if (listed) {
				continue;
			}
			String lower = n.toLowerCase(Locale.ROOT);
			if (lower.startsWith(typedLower)) {
				starts.add(n);
			} else if (lower.contains(typedLower)) {
				contains.add(n);
			}
		}
		starts.addAll(contains);
		for (int i = 0; i < SUGGESTIONS && i < starts.size(); i++) {
			suggestNames[i] = starts.get(i);
			if (suggestButtons[i] != null) {
				suggestButtons[i].setMessage(Text.literal(starts.get(i)));
				suggestButtons[i].visible = true;
			}
		}
	}

	@Override
	public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
		super.render(ctx, mouseX, mouseY, delta);
		ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 6, 0xFFFFFFFF);
		if (!status.isEmpty()) {
			ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal(status), this.width / 2, 20, statusColor);
		}
		if (SpotterConfig.INSTANCE.players.isEmpty()) {
			ctx.drawCenteredTextWithShadow(this.textRenderer,
				Text.literal("No players yet. Type a name and press Add."), this.width / 2, 118, 0xFFAAAAAA);
		} else {
			ctx.drawCenteredTextWithShadow(this.textRenderer,
				Text.literal("Green = near you now (can show). Gray = not loaded yet."), this.width / 2, listEnd + 4, 0xFFAAAAAA);
		}
	}

	@Override
	public void close() {
		this.client.setScreen(parent);
	}

	@Override
	public void removed() {
		SpotterConfig.save();
	}
}
