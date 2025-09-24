Of course. Based on the Java files you've provided for your Minecraft moderation mod, here is a comprehensive `README.md` file tailored for other moderators.

---

# AutoChatMod - A Moderator's Assistant

AutoChatMod is a client-side Minecraft mod designed to assist moderators by automatically flagging suspicious chat messages, detecting spam, and streamlining the moderation process. It listens to chat in real-time and alerts you to potential rule-breaking behaviour, allowing you to focus on the game while it keeps an eye on the chat.

## Key Features

* **Chat Monitoring**: Automatically detects spam, flagged terms (with typo/similarity detection), and flagged phrases.
* **Discord Webhook Integration**: Get instant notifications in a designated Discord channel when a message is flagged, complete with the message content and sender. You can configure it to ping a specific user.
* **In-Game Action Overlay**: A clean, draggable overlay that appears when you click on a player's name in a flagged message. It provides quick access to common moderation commands.
* **Smart Nickname Resolution**: If a flagged message is from a player using a nickname, the mod will automatically attempt to resolve their real username before you take action.
* **Click-to-Act Functionality**: All incoming chat messages are subtly modified to be clickable. Clicking a message from a user will resolve their real name (if they're using a nick) and open the Action HUD.
* **Fully Configurable**: Every feature can be toggled and tweaked in-game through the ModMenu configuration screen. Customize flagged words, whitelists, spam sensitivity, and more.
* **Audible Alerts**: A distinct sound plays whenever a message is flagged, ensuring you don't miss an alert.

## Installation

This is a client-side mod. It only needs to be installed on the moderator's computer, not on the server.

**Prerequisites:**
1.  [Fabric Loader](https://fabricmc.net/use/installer/)
2.  [Fabric API](https://www.curseforge.com/minecraft/mc-mods/fabric-api)
3.  [ModMenu](https://www.curseforge.com/minecraft/mc-mods/modmenu) (To access the in-game configuration)
4.  [Cloth Config API](https://www.curseforge.com/minecraft/mc-mods/cloth-config) (A dependency for the config screen)
5.  Minecraft 1.21.4 only.

**Steps:**
1.  Download the latest `autochatmod-x.x.x.jar` file.
2.  Place the JAR file, along with the prerequisites, into your `/.minecraft/mods/` folder.
3.  Launch Minecraft using the Fabric profile.

## Configuration

The power of this mod comes from its customizability. To configure it:
1.  From the main menu, click **Mods**.
2.  Find **AutoChatMod** in the list and click the gear icon in the top right.

Here is a breakdown of the configuration options:

### General Settings
| Option | Description |
| :--- | :--- |
| **Enabled** | The master switch. Toggles the entire mod on or off. |
| **Discord Webhook URL** | Your Discord webhook URL. Alerts will be sent here. See below for setup instructions. |
| **User Mention ID** | The Discord User or Role ID to ping in alerts. |
| **Ping on Discord Alert** | If enabled, the `User Mention ID` will be pinged in every alert message. |

### Detection Settings
| Option | Description |
| :--- | :--- |
| **Spam Detection** | Enable or disable the spam detection module. |
| **Term Detection** | Enable or disable flagging messages based on individual words (e.g., slurs). |
| **Phrase Detection** | Enable or disable flagging messages based on exact phrases (e.g., "kill yourself"). |
| **Similarity Threshold** | A value from 0.0 to 1.0. This controls how similar a word needs to be to a flagged term to trigger an alert. Recommended: `0.8`. This helps catch common typos or attempts to bypass filters (e.g., "n1gger"). |

### Spam Detection
| Option | Description |
| :--- | :--- |
| **Spam Similarity Threshold** | How similar two messages must be to be considered part of a spam sequence. Recommended: `0.9`. |
| **Spam Message Count** | The number of similar messages required to trigger a spam alert. |
| **Spam Time Window (seconds)** | The time frame in which the `Spam Message Count` must be reached. |
| **Spam Whitelist Prefixes** | A list of prefixes to ignore for spam detection (e.g., server broadcasts like `[Broadcast]`). |

### Flagged Terms & Phrases
* **Flagged Terms**: A list of individual words to watch for.
* **Whitelisted Terms**: Words that should never trigger an alert, even if they are similar to a flagged term (e.g., "bigger" being similar to a slur).
* **Flagged Phrases**: A list of exact phrases to watch for.
* **Whitelisted Phrases**: Phrases that should be ignored.

### How to Get a Discord Webhook URL
1.  In your Discord server, go to `Server Settings` -> `Integrations`.
2.  Click `Webhooks` -> `New Webhook`.
3.  Give it a name (e.g., "MC Alerts") and choose a channel for the notifications.
4.  Click `Copy Webhook URL` and paste it into the mod's configuration.

## Usage Workflow

Here's a typical moderation scenario using the mod:

1.  **Passive Monitoring**: Play the game as usual. The mod is analysing chat in the background.
2.  **Alert!**: A player types a message that triggers one of the detection modules.
    * **In-Game**: You will hear an alert sound, and the message will appear in your chat prefixed with `[FLAGGED]` or `[SPAM]` in red.
    * **On Discord**: A notification is instantly sent to your configured webhook, containing the message and sender.
3.  **Take Action**:
    * In your Minecraft chat, the flagged message is now interactive. **Click the player's username**.
    * The **Action HUD** will appear in the top-right corner of your screen.
    * *(If the user had a nickname, the mod automatically runs `/realname` and opens the HUD for the resolved username.)*
4.  **Use the HUD**: With the HUD open, you can now use the keybinds to act on the user shown in the HUD.
    * Press **`X`** to teleport to the user (`/tp <username>`).
    * Press **`P`** to open the punishment GUI for the user (`/punish <username>`).
    * Press **`C`** to close the HUD.
5.  **Overlay Management**: The Action HUD is draggable. Simply click and drag the top bar of the HUD to move it to a more convenient location on your screen.

## Commands

The mod has one primary command for manual use:

* `/autochatmod action <username>`
    * Manually opens the Action HUD for the specified username. 

## Default Keybinds

| Key | Action | Description |
| :-- | :--- | :--- |
| **`X`** | Teleport | When the Action HUD is open, teleports you to the target player. |
| **`P`** | Punish | When the Action HUD is open, opens the punish GUI for the target player. |
| **`C`** | Close | Closes the Action HUD. |

These keybinds can be changed in Minecraft's standard `Options -> Controls -> Key Binds` menu under the "AutoChatMod" category.

---

Good luck, and happy moderating!
