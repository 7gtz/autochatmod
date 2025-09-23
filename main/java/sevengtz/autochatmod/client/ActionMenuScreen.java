package sevengtz.autochatmod.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.math.ColorHelper;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sevengtz.autochatmod.AutoChatMod;

public class ActionMenuScreen {
    private static final Logger LOGGER = LoggerFactory.getLogger(ActionMenuScreen.class);
    private static boolean isOverlayVisible = false;
    private static String username = null;
    private static boolean escKeyWasPressed = false;

    public static void showOverlay(String username) {
        if (isVisible()) {
            hideOverlay();
        }
        LOGGER.info("[ActionMenuScreen]: Showing actions overlay for: {}", username);
        ActionMenuScreen.username = username;
        isOverlayVisible = true;
        escKeyWasPressed = false;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.getSoundManager().play(
                    new net.minecraft.client.sound.PositionedSoundInstance(
                            SoundEvents.UI_BUTTON_CLICK.value(),
                            net.minecraft.sound.SoundCategory.MASTER,
                            0.5F, 1.0F,
                            net.minecraft.util.math.random.Random.create(),
                            client.player.getBlockPos()
                    )
            );
        }
    }

    public static void hideOverlay() {
        LOGGER.debug("[ActionMenuScreen]: Hiding overlay, username was: {}", username);
        isOverlayVisible = false;
        username = null;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.getSoundManager().play(
                    new net.minecraft.client.sound.PositionedSoundInstance(
                            SoundEvents.UI_BUTTON_CLICK.value(),
                            net.minecraft.sound.SoundCategory.MASTER,
                            0.5F, 1.0F,
                            net.minecraft.util.math.random.Random.create(),
                            client.player.getBlockPos()
                    )
            );
        }
    }

    public static void registerOverlay() {
        LOGGER.info("[ActionMenuScreen]: Registering HUD render callback");
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            if (!isOverlayVisible || username == null || MinecraftClient.getInstance().player == null) {
                LOGGER.debug("[ActionMenuScreen]: Skipping render - isOverlayVisible: {}, username: {}, player: {}",
                        isOverlayVisible, username, MinecraftClient.getInstance().player);
                return;
            }

            TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
            int screenWidth = MinecraftClient.getInstance().getWindow().getScaledWidth();
            int screenHeight = MinecraftClient.getInstance().getWindow().getScaledHeight();

            // Background rectangle
            int padding = 5;
            int boxWidth = 150;
            int boxHeight = 75;
            int x = screenWidth - boxWidth - padding;
            int y = padding;
            drawContext.fill(x, y, x + boxWidth, y + boxHeight, ColorHelper.getArgb(180, 0, 0, 0));
            LOGGER.debug("[ActionMenuScreen]: Rendering HUD at x: {}, y: {}, width: {}, height: {}", x, y, boxWidth, boxHeight);

            // Title
            drawContext.drawText(textRenderer, "Actions for: " + username, x + padding, y + padding, 0xFFFFFF, false);

            // Action 1: Teleport
            String teleportKey = AutoChatMod.getKeyBindTeleport().getBoundKeyLocalizedText().getString();
            Text teleportText = Text.literal("Teleport to User (")
                    .append(Text.literal(teleportKey).setStyle(Style.EMPTY.withColor(0xFFFF00)))
                    .append(")");
            drawContext.drawText(textRenderer, teleportText, x + padding, y + padding + 20, 0x00FF00, false);

            // Action 2: Punish
            String punishKey = AutoChatMod.getKeyBindPunish().getBoundKeyLocalizedText().getString();
            Text punishText = Text.literal("Punish User (")
                    .append(Text.literal(punishKey).setStyle(Style.EMPTY.withColor(0xFFFF00)))
                    .append(")");
            drawContext.drawText(textRenderer, punishText, x + padding, y + padding + 40, 0xFF0000, false);
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!isOverlayVisible) {
                LOGGER.debug("[ActionMenuScreen]: Skipping tick - overlay not visible");
                return;
            }
        });
    }

    public static boolean isVisible() {
        return isOverlayVisible;
    }

    public static String getUsername() {
        return username;
    }
}