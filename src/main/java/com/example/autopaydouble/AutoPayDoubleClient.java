package com.example.autopaydouble;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutoPayDoubleClient implements ClientModInitializer {

	/**
	 * TODO: Replace this with the ACTUAL format Donut SMP uses for payment
	 * messages. Join the server, get paid a small amount, and check the raw
	 * chat message (F3 + copy, or a log viewer) to see the exact wording and
	 * currency symbol. The pattern below is a reasonable guess and will need
	 * tweaking.
	 *
	 * Example guessed formats it currently tries to match:
	 *   "PlayerName paid you $100.00"
	 *   "PlayerName has paid you 100 coins"
	 *   "You received $100.00 from PlayerName"
	 */
	private static final Pattern[] PAYMENT_PATTERNS = new Pattern[] {
			Pattern.compile("^(?<player>[A-Za-z0-9_]{1,16}) (?:paid|has paid) you \\$?(?<amount>[0-9]+(?:\\.[0-9]+)?)"),
			Pattern.compile("^You received \\$?(?<amount>[0-9]+(?:\\.[0-9]+)?) from (?<player>[A-Za-z0-9_]{1,16})")
	};

	/**
	 * If true, clicking the message immediately SENDS the /pay command.
	 * If false (default), clicking only TYPES it into the chat box so you
	 * still have to press Enter yourself, which keeps a human in the loop.
	 */
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
					double amount = Double.parseDouble(matcher.group("amount"));
					double doubled = amount * 2;

					String amountStr = (doubled == Math.floor(doubled))
							? String.valueOf((long) doubled)
							: String.valueOf(doubled);

					String command = "/pay " + player + " " + amountStr;

					ClickEvent clickEvent = new ClickEvent(
							autoSend ? ClickEvent.Action.RUN_COMMAND : ClickEvent.Action.SUGGEST_COMMAND,
							command
					);
					HoverEvent hoverEvent = new HoverEvent(
							HoverEvent.Action.SHOW_TEXT,
							Text.literal("Click to " + (autoSend ? "pay" : "fill in") + " double back: " + command)
					);

					Style newStyle = message.getStyle().withClickEvent(clickEvent).withHoverEvent(hoverEvent);
					MutableText newMessage = message.copy().setStyle(newStyle);

					// Re-fire the (now clickable) message instead of the original.
					MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(newMessage);
					return false; // suppress the original, unmodified message
				}
			}

			return true;
		});
	}

	private static void feedback(String msg) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player != null) {
			client.player.sendMessage(Text.literal("[AutoPayDouble] " + msg), false);
		}
	}
}
