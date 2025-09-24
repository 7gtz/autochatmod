package sevengtz.autochatmod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sevengtz.autochatmod.client.ActionMenuScreen;

public class AutoChatMod implements ClientModInitializer {
    public static final String MOD_ID = "autochatmod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static ChatMonitor chatMonitor;

    // Keybinds
    private static KeyBinding keyBindTeleport;
    private static KeyBinding keyBindPunish;
    private static KeyBinding keyBindClose;

    @Override
    public void onInitializeClient() {
        LOGGER.info("[AutoChatMod]: Initializing...");

        ConfigManager.init();
        chatMonitor = new ChatMonitor();
        ActionMenuScreen.registerOverlay();

        ClientReceiveMessageEvents.MODIFY_GAME.register((message, overlay) -> {
            Text modifiedMessage = chatMonitor.makeMessageClickable(message);
            chatMonitor.processMessage(message.getString());
            return modifiedMessage;
        });

        registerCommands();
        registerKeybinds();

        LOGGER.info("[AutoChatMod]: Initialized successfully!");
    }

    private void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("autochatmod")
                    .then(ClientCommandManager.literal("action")
                            .then(ClientCommandManager.argument("username", com.mojang.brigadier.arguments.StringArgumentType.word())
                                    .executes(context -> {
                                        String username = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "username");
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
                                            LOGGER.debug("[AutoChatMod]: Calling showOverlay for {}", username);
                                            ActionMenuScreen.showOverlay(username);
                                        });
                                        return 1;
                                    })
                            )
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

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!ActionMenuScreen.isVisible() || client.player == null) {
                LOGGER.debug("[AutoChatMod]: Skipping keybind check - isVisible: {}, player: {}",
                        ActionMenuScreen.isVisible(), client.player);
                return;
            }

            String currentUsername = ActionMenuScreen.getUsername();
            if (currentUsername == null) {
                LOGGER.debug("[AutoChatMod]: Skipping keybind check - no username in HUD");
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

            if (keyBindPunish.wasPressed()) {
                LOGGER.info("[AutoChatMod]: Punish keybind (P) pressed for {}", currentUsername);
                client.player.networkHandler.sendChatCommand("punish " + currentUsername);
                MutableText prefix = Text.literal("[").formatted(Formatting.DARK_GRAY)
                        .append(Text.literal("AutoChatMod").formatted(Formatting.YELLOW))
                        .append(Text.literal("] ").formatted(Formatting.DARK_GRAY));
                MutableText message = Text.literal("Opening punishment GUI for " + currentUsername).formatted(Formatting.GRAY);
                client.player.sendMessage(prefix.append(message), false);
            }

            if (keyBindClose.wasPressed()) {
                LOGGER.info("[AutoChatMod]: Close keybind (C) pressed");
                MutableText prefix = Text.literal("[").formatted(Formatting.DARK_GRAY)
                        .append(Text.literal("AutoChatMod").formatted(Formatting.YELLOW))
                        .append(Text.literal("] ").formatted(Formatting.DARK_GRAY));
                MutableText message = Text.literal("HUD closed").formatted(Formatting.GRAY);
                client.player.sendMessage(prefix.append(message), false);
                ActionMenuScreen.hideOverlay();
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

    public static ChatMonitor getChatMonitor() {
        return chatMonitor;
    }
}