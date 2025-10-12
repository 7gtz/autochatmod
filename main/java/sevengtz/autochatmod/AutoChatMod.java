package sevengtz.autochatmod;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry; // New Import
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier; // New Import
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sevengtz.autochatmod.client.ActionMenuScreen;

public class AutoChatMod implements ClientModInitializer {
    public static final String MOD_ID = "autochatmod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static ChatMonitor chatMonitor;

    // Create a single instance of the ActionMenuScreen
    public static final ActionMenuScreen ACTION_MENU = new ActionMenuScreen();
    private static final Identifier ACTION_MENU_ID = Identifier.of(MOD_ID, "action_menu");

    // Keybinds
    private static KeyBinding keyBindTeleport;
    private static KeyBinding keyBindPunish;
    private static KeyBinding keyBindClose;

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

        registerCommands();
        registerKeybinds();

        LOGGER.info("[AutoChatMod]: Initialized successfully!");
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
                                            ACTION_MENU.show(username);
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

    public static ChatMonitor getChatMonitor() {
        return chatMonitor;
    }
}