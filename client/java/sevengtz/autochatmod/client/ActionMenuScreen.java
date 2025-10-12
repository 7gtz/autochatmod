package sevengtz.autochatmod.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
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

    // Dragging functionality
    private static boolean isDragging = false;
    private static double dragStartX = 0;
    private static double dragStartY = 0;
    private static int hudX = -1; // -1 means not initialized (will use default position)
    private static int hudY = -1;
    private static boolean wasMousePressed = false;

    // HUD dimensions
    private static final int BOX_WIDTH = 250;
    private static final int BOX_HEIGHT = 100;
    private static final int PADDING = 5;

    public static void showOverlay(String username) {
        if (isVisible()) {
            hideOverlay();
        }
        LOGGER.info("[ActionMenuScreen]: Showing actions overlay for: {}", username);
        ActionMenuScreen.username = username;
        isOverlayVisible = true;
        escKeyWasPressed = false;

        // Initialize HUD position if not set
        if (hudX == -1 || hudY == -1) {
            MinecraftClient client = MinecraftClient.getInstance();
            int screenWidth = client.getWindow().getScaledWidth();
            hudX = screenWidth - BOX_WIDTH - PADDING;
            hudY = PADDING;
        }

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
        isDragging = false; // Reset dragging state

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
                return;
            }

            TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
            int screenWidth = MinecraftClient.getInstance().getWindow().getScaledWidth();
            int screenHeight = MinecraftClient.getInstance().getWindow().getScaledHeight();

            // Ensure HUD stays within screen bounds
            hudX = Math.max(0, Math.min(hudX, screenWidth - BOX_WIDTH));
            hudY = Math.max(0, Math.min(hudY, screenHeight - BOX_HEIGHT));

            // Background rectangle (with different color if dragging)
            int backgroundColor = isDragging ?
                    ColorHelper.getArgb(200, 50, 50, 100) : // Slightly blue tint when dragging
                    ColorHelper.getArgb(180, 0, 0, 0);

            drawContext.fill(hudX, hudY, hudX + BOX_WIDTH, hudY + BOX_HEIGHT, backgroundColor);

            // Title bar (draggable area indicator)
            int titleBarHeight = 15;
            drawContext.fill(hudX, hudY, hudX + BOX_WIDTH, hudY + titleBarHeight,
                    ColorHelper.getArgb(220, 100, 100, 100));

            // Title
            drawContext.drawText(textRenderer, "Actions for: " + username, hudX + PADDING, hudY + PADDING, 0xFFFFFF, false);

            // Action 1: Teleport
            String teleportKey = AutoChatMod.getKeyBindTeleport().getBoundKeyLocalizedText().getString();
            Text teleportText = Text.literal("Teleport to User (")
                    .append(Text.literal(teleportKey).setStyle(Style.EMPTY.withColor(0xFFFF00)))
                    .append(")");
            drawContext.drawText(textRenderer, teleportText, hudX + PADDING, hudY + PADDING + 20, 0x00FF00, false);

            // Action 2: Punish
            String punishKey = AutoChatMod.getKeyBindPunish().getBoundKeyLocalizedText().getString();
            Text punishText = Text.literal("Punish User (")
                    .append(Text.literal(punishKey).setStyle(Style.EMPTY.withColor(0xFFFF00)))
                    .append(")");
            drawContext.drawText(textRenderer, punishText, hudX + PADDING, hudY + PADDING + 40, 0xFF0000, false);

            // Visual indicator when dragging
            if (isDragging) {
                drawContext.drawText(textRenderer, "Dragging...", hudX + PADDING, hudY + BOX_HEIGHT - 15, 0xFFFF00, false);
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!isOverlayVisible) {
                return;
            }

            handleMouseInput(client);
        });
    }

    private static void handleMouseInput(MinecraftClient client) {
        if (client.mouse == null) return;

        double mouseX = client.mouse.getX() * client.getWindow().getScaledWidth() / client.getWindow().getWidth();
        double mouseY = client.mouse.getY() * client.getWindow().getScaledHeight() / client.getWindow().getHeight();

        boolean mousePressed = GLFW.glfwGetMouseButton(client.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;

        // Check if mouse is on the overlay
        boolean mouseOverHUD = mouseX >= hudX && mouseX <= hudX + BOX_WIDTH &&
                mouseY >= hudY && mouseY <= hudY + BOX_HEIGHT;

        if (mousePressed && !wasMousePressed && mouseOverHUD) {
            isDragging = true;
            dragStartX = mouseX - hudX;
            dragStartY = mouseY - hudY;
            LOGGER.debug("[ActionMenuScreen]: Started dragging HUD");
        }

        if (isDragging && mousePressed) {
            // Update position
            hudX = (int)(mouseX - dragStartX);
            hudY = (int)(mouseY - dragStartY);
        }

        if (isDragging && !mousePressed) {
            // Stop dragging
            isDragging = false;
            LOGGER.debug("[ActionMenuScreen]: Stopped dragging HUD at position ({}, {})", hudX, hudY);
        }

        wasMousePressed = mousePressed;
    }

    public static boolean isVisible() {
        return isOverlayVisible;
    }

    public static String getUsername() {
        return username;
    }

    // Methods to save/restore HUD position
    public static void setHudPosition(int x, int y) {
        hudX = x;
        hudY = y;
    }

    public static int getHudX() {
        return hudX;
    }

    public static int getHudY() {
        return hudY;
    }
}