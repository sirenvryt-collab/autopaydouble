package com.example.autopaydouble;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutoPayDoubleClient implements ClientModInitializer {

	// Amount now also captures an optional k/m/b/t shorthand suffix, e.g. "40m".
	private static final String AMOUNT_REGEX = "\\$?\\s*(?<amount>[0-9]+(?:\\.[0-9]+)?)(?<suffix>[kKmMbBtT])?";

private static final Pattern[] PAYMENT_PATTERNS = new Pattern[] {
		Pattern.compile("^(?<player>\\.?[A-Za-z0-9_]{1,16}) (?:paid|has paid) you " + AMOUNT_REGEX),
		Pattern.compile("^You received " + AMOUNT_REGEX + " from (?<player>\\.?[A-Za-z0-9_]{1,16})")
};

	private static boolean autoSend = false;
	private static boolean enabled = true;

	@Override
	public void onInitializeClient() {

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			dispatcher.register(ClientCommandManager.literal("autopaydouble")
					.then(ClientCommandManager.literal("toggle")
							.executes(ctx -> {
								enabled = !enabled;
								feedback("Auto-pay-double is now " + (enabled ? "ENABLED" : "DISABLED"));
								return 1;
							}))
					.then(ClientCommandManager.literal("autosend")
							.then(ClientCommandManager.argument("value", com.mojang.brigadier.arguments.BoolArgumentType.bool())
									.executes(ctx -> {
										autoSend = com.mojang.brigadier.arguments.BoolArgumentType.getBool(ctx, "value");
										feedback("Auto-send is now " + (autoSend ? "ON (click pays instantly)" : "OFF (click fills chat box)"));
										return 1;
									}))));
		});

		ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
			if (!enabled || overlay) {
				return true;
			}

			String plain = message.getString().replaceAll("§.", "").trim();

			for (Pattern pattern : PAYMENT_PATTERNS) {
				Matcher matcher = pattern.matcher(plain);
				if (matcher.find()) {
					String player = matcher.group("player");
					double rawAmount = Double.parseDouble(matcher.group("amount"));
					String suffix = matcher.group("suffix");

					double realValue = rawAmount * multiplierFor(suffix);
					double doubled = realValue * 2;

					String amountStr = formatCompact(doubled);
					String command = "/pay " + player + " " + amountStr;

					ClickEvent clickEvent = autoSend
							? new ClickEvent.RunCommand(command)
							: new ClickEvent.SuggestCommand(command);

					HoverEvent hoverEvent = new HoverEvent.ShowText(
							Text.literal("Click to " + (autoSend ? "pay" : "fill in") + " double back: " + command)
					);

					Style newStyle = message.getStyle().withClickEvent(clickEvent).withHoverEvent(hoverEvent);
					MutableText newMessage = message.copy().setStyle(newStyle);

					MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(newMessage);
					return false;
				}
			}

			return true;
		});
	}

	private static double multiplierFor(String suffix) {
		if (suffix == null) {
			return 1;
		}
		return switch (Character.toLowerCase(suffix.charAt(0))) {
			case 'k' -> 1_000d;
			case 'm' -> 1_000_000d;
			case 'b' -> 1_000_000_000d;
			case 't' -> 1_000_000_000_000d;
			default -> 1;
		};
	}

	private static String formatCompact(double value) {
		String[] suffixes = { "", "k", "m", "b", "t" };
		int tier = 0;
		double reduced = value;

		while (Math.abs(reduced) >= 1000 && tier < suffixes.length - 1) {
			reduced /= 1000;
			tier++;
		}

		String numberPart;
		if (reduced == Math.floor(reduced)) {
			numberPart = String.valueOf((long) reduced);
		} else {
			numberPart = String.format("%.1f", reduced);
			if (numberPart.endsWith(".0")) {
				numberPart = numberPart.substring(0, numberPart.length() - 2);
			}
		}

		return numberPart + suffixes[tier];
	}

	private static void feedback(String msg) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player != null) {
			client.player.sendMessage(Text.literal("[AutoPayDouble] " + msg), false);
		}
	}
}
