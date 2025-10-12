package sevengtz.autochatmod.client;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext; // Correct import for modern rendering
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import org.lwjgl.glfw.GLFW;
import sevengtz.autochatmod.AutoChatMod;

public class ActionMenuScreen implements HudElement {

    private static boolean isOverlayVisible = false;
    private static String username = null;

    // Dragging functionality
    private boolean isDragging = false;
    private double dragStartX = 0;
    private double dragStartY = 0;
    private int hudX = -1;
    private int hudY = -1;
    private boolean wasMousePressed = false;

    // HUD dimensions
    private static final int BOX_WIDTH = 250;
    private static final int BOX_HEIGHT = 100;
    private static final int PADDING = 5;

    // Method to show the overlay
    public void show(String user) {
        username = user;
        isOverlayVisible = true;
        // Initialization of position can be done here or in the constructor
        if (hudX == -1) {
            MinecraftClient client = MinecraftClient.getInstance();
            int screenWidth = client.getWindow().getScaledWidth();
            hudX = screenWidth - BOX_WIDTH - PADDING;
            hudY = PADDING;
        }
    }

    // Method to hide the overlay
    public void hide() {
        isOverlayVisible = false;
        username = null;
        isDragging = false;
    }

    public boolean isVisible() {
        return isOverlayVisible;
    }

    public String getUsername() {
        return username;
    }

    @Override
    public void render(DrawContext drawContext, RenderTickCounter tickCounter) {
        if (!isOverlayVisible || username == null || MinecraftClient.getInstance().player == null) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer textRenderer = client.textRenderer;
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        // Handle mouse input for dragging
        handleMouseInput(client);

        // Ensure HUD stays within screen bounds
        hudX = Math.max(0, Math.min(hudX, screenWidth - BOX_WIDTH));
        hudY = Math.max(0, Math.min(hudY, screenHeight - BOX_HEIGHT));

        // Background rectangle
        int backgroundColor = isDragging ? ColorHelper.getArgb(200, 50, 50, 100) : ColorHelper.getArgb(180, 0, 0, 0);
        drawContext.fill(hudX, hudY, hudX + BOX_WIDTH, hudY + BOX_HEIGHT, backgroundColor);

        // Title bar
        int titleBarHeight = 15;
        drawContext.fill(hudX, hudY, hudX + BOX_WIDTH, hudY + titleBarHeight, ColorHelper.getArgb(220, 100, 100, 100));

        // Title - Using a safer opaque white and enabling shadow
        drawContext.drawText(textRenderer, "Actions for: " + username, hudX + PADDING, hudY + PADDING, 0xFFFFFFFF, true);

        // Action 1: Teleport
        String teleportKey = AutoChatMod.getKeyBindTeleport().getBoundKeyLocalizedText().getString();
        Text teleportText = Text.literal("Teleport to User (").append(Text.literal(teleportKey).setStyle(Style.EMPTY.withColor(0xFFFF00))).append(")");
        // Corrected color to be opaque green (0xFF00FF00) and enabled shadow
        drawContext.drawText(textRenderer, teleportText, hudX + PADDING, hudY + PADDING + 20, 0xFF00FF00, true);

        // Action 2: Punish
        String punishKey = AutoChatMod.getKeyBindPunish().getBoundKeyLocalizedText().getString();
        Text punishText = Text.literal("Punish User (").append(Text.literal(punishKey).setStyle(Style.EMPTY.withColor(0xFFFF00))).append(")");
        // Corrected color to be opaque red (0xFFFF0000) and enabled shadow
        drawContext.drawText(textRenderer, punishText, hudX + PADDING, hudY + PADDING + 40, 0xFFFF0000, true);

        if (isDragging) {
            drawContext.drawText(textRenderer, "Dragging...", hudX + PADDING, hudY + BOX_HEIGHT - 15, 0xFFFFFF00, true);
        }
    }

    private void handleMouseInput(MinecraftClient client) {
        if (client.mouse == null) return;
        double mouseX = client.mouse.getX() * client.getWindow().getScaledWidth() / client.getWindow().getWidth();
        double mouseY = client.mouse.getY() * client.getWindow().getScaledHeight() / client.getWindow().getHeight();
        boolean mousePressed = GLFW.glfwGetMouseButton(client.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;

        boolean mouseOverHUD = mouseX >= hudX && mouseX <= hudX + BOX_WIDTH && mouseY >= hudY && mouseY <= hudY + BOX_HEIGHT;

        if (mousePressed && !wasMousePressed && mouseOverHUD) {
            isDragging = true;
            dragStartX = mouseX - hudX;
            dragStartY = mouseY - hudY;
        }

        if (isDragging && mousePressed) {
            hudX = (int)(mouseX - dragStartX);
            hudY = (int)(mouseY - dragStartY);
        }

        if (isDragging && !mousePressed) {
            isDragging = false;
        }

        wasMousePressed = mousePressed;
    }
}