package sevengtz.autochatmod;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ScreenshotEvidence {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");

    public static void takeScreenshot(String mutedPlayer, String reason, MinecraftClient client) {
        AutoChatMod.LOGGER.info("[ScreenshotEvidence]: takeScreenshot called for player: {}, reason: {}", mutedPlayer, reason);
        AutoChatMod.LOGGER.info("[ScreenshotEvidence]: Client run directory: {}", client.runDirectory.getAbsolutePath());
        // Run this on the client thread to be safe
        client.execute(() -> {
            AutoChatMod.LOGGER.info("[ScreenshotEvidence]: Executing screenshot on client thread");
            try {
                // 1. Create the custom evidence directory if it doesn't exist
                File evidenceDir = new File(client.runDirectory, "modassist-evidence");
                boolean dirCreated = evidenceDir.mkdirs();
                AutoChatMod.LOGGER.info("[ScreenshotEvidence]: Evidence directory created/exists: {}, Path: {}", dirCreated, evidenceDir.getAbsolutePath());

                // 2. Sanitize the reason to create a valid filename
                // This replaces any character that isn't a letter, number, dot, or dash with an underscore
                String sanitizedReason = reason.replaceAll("[^a-zA-Z0-9.-]", "_");
                
                // Truncate if the reason is too long to prevent extremely long filenames
                if (sanitizedReason.length() > 50) {
                    sanitizedReason = sanitizedReason.substring(0, 50);
                }

                // 3. Create the final filename
                String timestamp = DATE_FORMAT.format(new Date());
                String fileName = String.format("%s_%s_%s.png", mutedPlayer, sanitizedReason, timestamp);
                File screenshotFile = new File(evidenceDir, fileName);
                AutoChatMod.LOGGER.info("[ScreenshotEvidence]: Screenshot file path: {}", screenshotFile.getAbsolutePath());

                // 4. Capture the image data from the screen and save it
                AutoChatMod.LOGGER.info("[ScreenshotEvidence]: Calling ScreenshotRecorder.takeScreenshot");
                ScreenshotRecorder.takeScreenshot(client.getFramebuffer(), (nativeImage) -> {
                    AutoChatMod.LOGGER.info("[ScreenshotEvidence]: Screenshot callback executed");
                    try {
                        nativeImage.writeTo(screenshotFile);
                        nativeImage.close();
                        // Send a confirmation message to the player's chat
                        sendConfirmation(Text.literal("Screenshot saved: " + fileName).formatted(Formatting.GREEN), client);
                    } catch (Exception e) {
                        AutoChatMod.LOGGER.error("Failed to save evidence screenshot", e);
                        sendConfirmation(Text.literal("Failed to save screenshot!").formatted(Formatting.RED), client);
                    }
                });

            } catch (Exception e) {
                AutoChatMod.LOGGER.error("Failed to save evidence screenshot", e);
                sendConfirmation(Text.literal("Failed to save screenshot!").formatted(Formatting.RED), client);
            }
        });
    }

    private static void sendConfirmation(Text message, MinecraftClient client) {
        MutableText prefix = Text.literal("[").formatted(Formatting.DARK_GRAY)
                .append(Text.literal("Evidence").formatted(Formatting.AQUA))
                .append(Text.literal("] ").formatted(Formatting.DARK_GRAY));
        
        if (client.player != null) {
            client.player.sendMessage(prefix.append(message), false);
        }
    }
}