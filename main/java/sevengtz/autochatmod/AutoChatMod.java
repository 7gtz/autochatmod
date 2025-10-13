package sevengtz.autochatmod;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry; 
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier; 
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sevengtz.autochatmod.client.ActionMenuScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.lwjgl.glfw.GLFW;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutoChatMod implements ClientModInitializer {
    public static final String MOD_ID = "autochatmod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final Pattern MUTE_EVIDENCE_PATTERN = Pattern.compile("^\\[S] \\[([^\\]]+)\\] (\\w{2,}) was muted by (\\w{2,}) .* for \"(.*)\"\\.?$");


    private static ChatMonitor chatMonitor;

    // Create a single instance of the ActionMenuScreen
    public static final ActionMenuScreen ACTION_MENU = new ActionMenuScreen();
    private static final Identifier ACTION_MENU_ID = Identifier.of(MOD_ID, "action_menu");

    // Keybinds
    private static KeyBinding keyBindTeleport;
    private static KeyBinding keyBindPunish;
    private static KeyBinding keyBindClose;
    private static KeyBinding keyBindSelectPlayer;
    private static KeyBinding keyBindCheckFly;
    private static KeyBinding keyBindAlts;

    @Override
    public void onInitializeClient() {
        LOGGER.info("[AutoChatMod]: Initializing...");

        ConfigManager.init();
        chatMonitor = new ChatMonitor();

        // Register the HUD element with the registry
        HudElementRegistry.attachElementAfter(VanillaHudElements.CHAT, ACTION_MENU_ID, ACTION_MENU);

        ClientReceiveMessageEvents.MODIFY_GAME.register((message, overlay) -> {
            Text modifiedMessage = chatMonitor.makeMessageClickable(message);
            chatMonitor.processMessage(message.getString());
            return modifiedMessage;
        });

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            String text = message.getString();
            ConfigManager.Config config = ConfigManager.getConfig();
    
            LOGGER.info("[AutoChatMod]: Processing message: {}", text);
            LOGGER.info("[AutoChatMod]: Evidence enabled: {}, Moderator name: '{}'", 
                        config.evidenceScreenshotEnabled, config.evidenceModeratorName);
    
            if (!config.evidenceScreenshotEnabled || config.evidenceModeratorName == null || config.evidenceModeratorName.isEmpty())  {
                LOGGER.debug("[AutoChatMod]: Evidence screenshot disabled or no moderator name set");
                return;
            }
            
            Matcher matcher = MUTE_EVIDENCE_PATTERN.matcher(text);
            if (matcher.find()) {
                // --- ADD THIS LINE TO DECLARE serverName ---
                String serverName = matcher.group(1);
                
                String mutedPlayer = matcher.group(2);
                String moderator = matcher.group(3);
                String reason = matcher.group(4);
                
                LOGGER.info("[AutoChatMod]: Found mute message - Server: {}, Player: {}, Moderator: {}, Reason: {}", 
                        serverName, mutedPlayer, moderator, reason);
            
                if (moderator.equalsIgnoreCase(config.evidenceModeratorName)) {
                    LOGGER.info("[AutoChatMod]: Taking screenshot for mute by configured moderator: {}", config.evidenceModeratorName);
                    ScreenshotEvidence.takeScreenshot(mutedPlayer, reason, MinecraftClient.getInstance());
                } else {
                    LOGGER.debug("[AutoChatMod]: Moderator '{}' does not match configured moderator '{}'", 
                            moderator, config.evidenceModeratorName);
                }
            } else {
                // This part is for debugging and can be removed if you want
                LOGGER.info("[AutoChatMod]: Message does not match mute pattern");
                LOGGER.info("[AutoChatMod]: Pattern is: {}", MUTE_EVIDENCE_PATTERN.pattern());
            }
        });
        LOGGER.info("[AutoChatMod]: Initialized successfully!");
        registerCommands();
        registerKeybinds();
    }

    private void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
                    dispatcher.register(ClientCommandManager.literal("autochatmod")
                    .then(ClientCommandManager.literal("action")
                            .then(ClientCommandManager.argument("username", StringArgumentType.word())
                                    .executes(context -> {
                                        String username = StringArgumentType.getString(context, "username");
                                        LOGGER.info("[AutoChatMod]: Executing /autochatmod action for username: {}", username);
                                        MinecraftClient client = MinecraftClient.getInstance();
                                        if (client.player == null) {
                                            LOGGER.warn("[AutoChatMod]: Cannot open HUD - player is null");
                                            return 0;
                                        }
                                        client.execute(() -> {
                                            if (client.currentScreen != null) {
                                                client.setScreen(null);
                                            }
                                            LOGGER.debug("[AutoChatMod]: Calling show for {}", username);
                                            // Updated to call the instance method
                                            ACTION_MENU.show(username, FlagType.MANUAL_CLICK);
                                        });
                                        return 1;
                                    })
                            )
                    )
                    .then(ClientCommandManager.literal("testscreenshot")
                            .executes(context -> {
                                LOGGER.info("[AutoChatMod]: Testing screenshot functionality");
                                MinecraftClient client = MinecraftClient.getInstance();
                                ScreenshotEvidence.takeScreenshot("TestPlayer", "Test Reason", client);
                                return 1;
                            })
                    )
            );
        });
    }

    private void registerKeybinds() {
        // Assign default keys
        keyBindTeleport = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.autochatmod.teleport",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_X,
                "category.autochatmod.main"
        ));

        keyBindPunish = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.autochatmod.punish",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_P,
                "category.autochatmod.main"
        ));

        keyBindClose = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.autochatmod.close",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_C,
                "category.autochatmod.main"
        ));

        keyBindAlts = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.autochatmod.alts",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_L,
                "category.autochatmod.main"
        ));

        keyBindCheckFly = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.autochatmod.checkfly",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                "category.autochatmod.main"
        ));

        keyBindSelectPlayer = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.autochatmod.select_player", 
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G, 
                "category.autochatmod.main"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {

            if (keyBindSelectPlayer.wasPressed()) {
                // Check if the client and crosshair target are valid
                if (client != null && client.crosshairTarget != null && client.crosshairTarget.getType() == HitResult.Type.ENTITY) {
                    EntityHitResult entityHit = (EntityHitResult) client.crosshairTarget;
                    Entity targetEntity = entityHit.getEntity();
        
                    // Check if that entity is a player
                    if (targetEntity instanceof PlayerEntity) {
                        String targetUsername = ((PlayerEntity) targetEntity).getGameProfile().getName();
                        LOGGER.info("[AutoChatMod]: Selected player {} via keybind.", targetUsername);
        
                        // Open the overlay with the new FlagType
                        ACTION_MENU.show(targetUsername, FlagType.MANUAL_SELECT);
                        return; // Stop here to prevent other keybinds from firing on the same tick
                    }
                }
            }

            // Updated to call the instance method
            if (!ACTION_MENU.isVisible() || client.player == null) {
                return; // Simplified the debug log for clarity
            }

            // Updated to call the instance method
            String currentUsername = ACTION_MENU.getUsername();
            if (currentUsername == null) {
                return;
            }

            if (keyBindTeleport.wasPressed()) {
                LOGGER.info("[AutoChatMod]: Teleport keybind (X) pressed for {}", currentUsername);
                client.player.networkHandler.sendChatCommand("tp " + currentUsername);
                MutableText prefix = Text.literal("[").formatted(Formatting.DARK_GRAY)
                        .append(Text.literal("AutoChatMod").formatted(Formatting.YELLOW))
                        .append(Text.literal("] ").formatted(Formatting.DARK_GRAY));
                MutableText message = Text.literal("Teleporting to " + currentUsername).formatted(Formatting.GRAY);
                client.player.sendMessage(prefix.append(message), false);
            }

            if (keyBindCheckFly.wasPressed()) {
                LOGGER.info("[AutoChatMod]: Checking if user {} has /fly on", currentUsername);
                client.player.networkHandler.sendChatCommand("checkfly " + currentUsername);
                MutableText prefix = Text.literal("[").formatted(Formatting.DARK_GRAY)
                        .append(Text.literal("AutoChatMod").formatted(Formatting.YELLOW))
                        .append(Text.literal("] ").formatted(Formatting.DARK_GRAY));
                MutableText message = Text.literal("Checking " + currentUsername + " for /fly!").formatted(Formatting.GRAY);
                client.player.sendMessage(prefix.append(message), false);
            }

            if (keyBindAlts.wasPressed()) {
                LOGGER.info("[AutoChatMod]: Checking if user {} has /fly on", currentUsername);
                client.player.networkHandler.sendChatCommand("alts " + currentUsername + " true");
                MutableText prefix = Text.literal("[").formatted(Formatting.DARK_GRAY)
                        .append(Text.literal("AutoChatMod").formatted(Formatting.YELLOW))
                        .append(Text.literal("] ").formatted(Formatting.DARK_GRAY));
                MutableText message = Text.literal("Checking " + currentUsername + "'s alts").formatted(Formatting.GRAY);
                client.player.sendMessage(prefix.append(message), false);
            }

            if (keyBindPunish.wasPressed()) {
                LOGGER.info("[AutoChatMod]: Punish keybind (P) pressed for {}", currentUsername);
                
                MutableText prefix = Text.literal("[").formatted(Formatting.DARK_GRAY)
                        .append(Text.literal("AutoChatMod").formatted(Formatting.YELLOW))
                        .append(Text.literal("] ").formatted(Formatting.DARK_GRAY));
            
                // Check if it's a spam case with the instant punish option enabled
                boolean isInstantPunishCase = ACTION_MENU.getFlagType() == FlagType.SPAM && ConfigManager.getConfig().instantPunishForSpam;
            
                if (isInstantPunishCase) {
                    client.player.networkHandler.sendChatCommand("punish " + currentUsername + " i:1");
                    MutableText message = Text.literal("Instantly punishing " + currentUsername + " for spam.").formatted(Formatting.GRAY);
                    client.player.sendMessage(prefix.append(message), false);
                    ACTION_MENU.hide();
                } else {
                    client.player.networkHandler.sendChatCommand("punish " + currentUsername);
                    MutableText message = Text.literal("Opening punishment GUI for " + currentUsername).formatted(Formatting.GRAY);
                    client.player.sendMessage(prefix.append(message), false);
                }
            }

            if (keyBindClose.wasPressed()) {
                LOGGER.info("[AutoChatMod]: Close keybind (C) pressed");
                MutableText prefix = Text.literal("[").formatted(Formatting.DARK_GRAY)
                        .append(Text.literal("AutoChatMod").formatted(Formatting.YELLOW))
                        .append(Text.literal("] ").formatted(Formatting.DARK_GRAY));
                MutableText message = Text.literal("HUD closed").formatted(Formatting.GRAY);
                client.player.sendMessage(prefix.append(message), false);
                // Updated to call the instance method
                ACTION_MENU.hide();
            }
        });
    }

    public static KeyBinding getKeyBindTeleport() {
        return keyBindTeleport;
    }

    public static KeyBinding getKeyBindPunish() {
        return keyBindPunish;
    }

    public static KeyBinding getKeyBindClose() {
        return keyBindClose;
    }

    public static KeyBinding getKeyBindCheckFly() {
        return keyBindCheckFly;
    }

    public static KeyBinding getKeyBindSelectPlayer() {
        return keyBindSelectPlayer;
    }

    public static KeyBinding getKeyBindAlts() {
        return keyBindAlts;
    }

    public static ChatMonitor getChatMonitor() {
        return chatMonitor;
    }
    
}