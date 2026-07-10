# AutoPayDouble (Fabric client mod)

Makes a "so-and-so paid you $X" chat message clickable. Clicking it either:
- **types** `/pay PlayerName <double the amount>` into your chat box for you to send yourself (default), or
- **sends it instantly**, if you flip auto-send on.

## Before you do anything else: find the real message format

The regex in `AutoPayDoubleClient.java` is a **guess**. Every economy plugin
formats its payment messages differently (wording, currency symbol, color
codes, whether it's "X paid you Y" vs "You received Y from X", etc).

1. Join Donut SMP.
2. Have a friend pay you a small test amount (or pay yourself if allowed).
3. Look at the exact chat line. If you want the raw string including color
   codes, you can temporarily log `message.getString()` in the mod, or use
   a chat-logging mod, or check the game's log file.
4. Update the `PAYMENT_PATTERNS` array in `AutoPayDoubleClient.java` to match
   exactly. The named groups `player` and `amount` are required.

If the message format uses a chat prefix, extra brackets, or a different
currency symbol than `$`, adjust the regex accordingly.

## Build

Requires a JDK (21 recommended for modern Minecraft/Fabric) and internet
access to Fabric/Mojang/Maven repositories (this sandbox doesn't have that
access, so the project can't be compiled here — build it on your own
machine).

```bash
cd autopaydouble
./gradlew build
```

The compiled mod will show up in `build/libs/autopaydouble-1.0.0.jar`.
Drop that into your `mods` folder alongside Fabric Loader and Fabric API
for the matching Minecraft version.

Double check `gradle.properties` matches:
- The Minecraft version Donut SMP is on
- A compatible Fabric Loader / Fabric API / Yarn mappings version

(see https://fabricmc.net/develop/ for current version numbers)

## In-game commands

- `/autopaydouble toggle` — turn the whole feature on/off
- `/autopaydouble autosend true` — clicking the message sends `/pay` instantly
- `/autopaydouble autosend false` — (default) clicking just fills the chat box, you press Enter

## Things worth knowing

- **This is still a form of chat automation/macro.** Even with auto-send off,
  it's pre-filling a command based on parsed chat content. Check Donut SMP's
  rules on macros/auto-clickers before using this — a lot of SMPs disallow
  this kind of thing even when it's just a convenience feature, and bans for
  macro use are common.
- **Financial risk**: if you ever do turn auto-send on, anyone can drain your
  balance fast by sending you a stream of tiny payments, since each one gets
  doubled back automatically with no human review.
- **Fragile by nature**: if the server updates its economy plugin and the
  message wording changes even slightly, the regex will silently stop
  matching. Worth testing after any server-side update.
