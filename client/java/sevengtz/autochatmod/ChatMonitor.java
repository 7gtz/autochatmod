package sevengtz.autochatmod;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.Style;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChatMonitor {
    private final Queue<MessageEntry> messageHistory = new ConcurrentLinkedQueue<>();
    private final DiscordWebhook webhook;
    private static final Logger LOGGER = LoggerFactory.getLogger("AutoChatMod");
    private static final Pattern REALNAME_RESPONSE_PATTERN = Pattern.compile("^\\[\\*\\]\\s+(\\w{2,})\\s+is nicknamed as\\s+(\\w{2,})$");
    private static final Pattern COLOR_CODE_PATTERN = Pattern.compile("(?i)Â§.");
    private static final Pattern COREPROTECT_PATTERN = Pattern.compile("^\\d+\\.\\d{2}/[dmh]\\s+ago.*");
    private static final Pattern FILTERED_PRIVATE_MESSAGE_PATTERN = Pattern.compile("^\\[S] \\[[^\\]]+] \\[Filtered] (?:\\[[^\\]]+] )?((?:\\* )?(\\w{2,})) Â» (?:\\[[^\\]]+] )?(?:\\* )?(\\w{2,}): (.*)$");
    private static final Pattern FILTERED_PATTERN = Pattern.compile(".*\\[Filtered]\\s+(\\w{2,})");
    private static final Pattern REPORT_PATTERN = Pattern.compile(".*reported\\s+(\\w{2,})\\s+for.*");
    private static final Pattern SERVER_FILTERED_PATTERN = Pattern.compile("^\\[S] \\[\\w+] \\[Filtered] (\\w{2,})");

    private static final Pattern wordPattern = Pattern.compile("\\b\\w+\\b");

    private record PendingNickResolution(String originalMessage, String nick, boolean isSpam, long timestamp,
                                         List<MessageEntry> similarMessages,
                                         boolean openActionOnResolve) {}
    private final Map<String, PendingNickResolution> pendingNickResolutions = new ConcurrentHashMap<>();
    private final Map<String, String> pendingDiscordNotifications = new ConcurrentHashMap<>();
    private final Set<String> flaggedMessages = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public ChatMonitor() {
        this.webhook = new DiscordWebhook();
    }

    /**
     * Modify the vanilla message to make it clickable instead of sending a new message
     */
    public Text makeMessageClickable(Text originalMessage) {
        if (originalMessage == null) return originalMessage;

        String plainText = originalMessage.getString();
        String cleanText = stripColorCodes(plainText);

        UsernameInfo userInfo = extractUsernameInfo(cleanText);
        if (userInfo == null) {
            return originalMessage;
        }

        if (userInfo.isNick) {
            pendingNickResolutions.put(userInfo.username.toLowerCase(),
                    new PendingNickResolution(cleanText, userInfo.username, false, Instant.now().toEpochMilli(), null, true));
            // Corrected Line
            ClickEvent click = ClickEvent.runCommand("/realname " + userInfo.username);
            // Corrected Line
            HoverEvent hover = HoverEvent.showText(Text.literal("Click to resolve and open actions for " + userInfo.username));
            return originalMessage.copy().setStyle(originalMessage.getStyle().withClickEvent(click).withHoverEvent(hover));
        } else {
            // Corrected Line
            ClickEvent click = ClickEvent.runCommand("/autochatmod action " + userInfo.username);
            // Corrected Line
            HoverEvent hover = HoverEvent.showText(Text.literal("Click for actions on " + userInfo.username));
            return originalMessage.copy().setStyle(originalMessage.getStyle().withClickEvent(click).withHoverEvent(hover));
        }
    }

    /**
     * Container for username information
     */
    private static class UsernameInfo {
        final String username;
        final boolean isNick;
        final String messageContent;

        UsernameInfo(String username, boolean isNick, String messageContent) {
            this.username = username;
            this.isNick = isNick;
            this.messageContent = messageContent;
        }
    }

    private UsernameInfo extractUsernameInfo(String message) {

        Matcher privateMatcher = FILTERED_PRIVATE_MESSAGE_PATTERN.matcher(message);
        if (privateMatcher.find()) {
            String senderWithPossibleNick = privateMatcher.group(1);
            String actualSender = privateMatcher.group(2);
            String messageContent = privateMatcher.group(3);

            boolean senderIsNick = senderWithPossibleNick.startsWith("* ");

            LOGGER.debug("[AutoChatMod]: Extracted sender [{}] (nick: {}) from private message: {}",
                    actualSender, senderIsNick, message);
            return new UsernameInfo(actualSender, senderIsNick, messageContent);
        }

        Matcher serverFilteredMatcher = SERVER_FILTERED_PATTERN.matcher(message);
        if (serverFilteredMatcher.find()) {
            String username = serverFilteredMatcher.group(1);
            String messageContent = message.substring(serverFilteredMatcher.end()).trim();
            LOGGER.debug("[AutoChatMod]: Extracted username [{}] from server filtered pattern: {}", username, message);
            return new UsernameInfo(username, false, messageContent);
        }

        for (int i = 2; i < message.length(); i++) {
            if (message.charAt(i) == ':') {
                String beforeColon = message.substring(0, i);
                String afterColon = message.substring(i + 1).trim();

                if (afterColon.isEmpty()) {
                    continue;
                }

                UsernameInfo userInfo = parseUsernameFromBeforeColon(beforeColon, afterColon);
                if (userInfo != null) {
                    return userInfo;
                }
            }
        }

        Matcher reportMatcher = REPORT_PATTERN.matcher(message);
        if (reportMatcher.find()) {
            String username = reportMatcher.group(1);
            return new UsernameInfo(username, false, message);
        }

        Matcher filteredMatcher = FILTERED_PATTERN.matcher(message);
        if (filteredMatcher.find()) {
            String username = filteredMatcher.group(1);
            if (username.length() >= 2) {
                return new UsernameInfo(username, false, message);
            }
        }

        return null;
    }

    private UsernameInfo parseUsernameFromBeforeColon(String beforeColon, String messageContent) {
        String[] words = beforeColon.split("\\s+");
        if (words.length == 0) {
            return null;
        }

        String lastWord = words[words.length - 1].trim();

        if (lastWord.length() < 2 || !lastWord.matches("\\w{2,}")) {
            return null;
        }

        int lastWordStart = beforeColon.lastIndexOf(lastWord);
        if (lastWordStart > 0) {
            String beforeWord = beforeColon.substring(0, lastWordStart).trim();
            if (beforeWord.endsWith("[")) {
                int lastWordEnd = lastWordStart + lastWord.length();
                if (lastWordEnd < beforeColon.length()) {
                    String afterWord = beforeColon.substring(lastWordEnd).trim();
                    if (afterWord.startsWith("]")) {
                        return null;
                    }
                }
            }
        }

        boolean isNick = false;
        if (words.length >= 2) {
            String secondLastWord = words[words.length - 2];
            if (secondLastWord.equals("*")) {
                isNick = true;
            }
        }

        return new UsernameInfo(lastWord, isNick, messageContent);
    }

    public void processMessage(String message) {
        ConfigManager.Config config = ConfigManager.getConfig();
        if (!config.enabled) return;

        if (shouldIgnoreMessage(message)) {
            LOGGER.debug("[AutoChatMod]: Message ignored: {}", message);
            return;
        }

        String cleanMessage = stripColorCodes(message);
        if (cleanMessage.trim().isEmpty()) return;

        if (handleRealnameResponse(cleanMessage)) {
            return;
        }

        boolean isSpam = config.spamDetectionEnabled && checkForSpam(cleanMessage);
        boolean isFlaggedPhrase = !isSpam && config.phraseDetectionEnabled && checkFlaggedPhrases(cleanMessage);
        boolean isFlaggedTerm = !isSpam && !isFlaggedPhrase && config.termDetectionEnabled && checkFlaggedTerms(cleanMessage);

        if (isSpam || isFlaggedPhrase || isFlaggedTerm) {
            handleFlaggedMessage(cleanMessage, isSpam);
        }
    }

    private boolean handleRealnameResponse(String message) {
        Matcher matcher = REALNAME_RESPONSE_PATTERN.matcher(message);
        if (matcher.find()) {
            String realUsername = matcher.group(1);
            String nick = matcher.group(2).toLowerCase();

            PendingNickResolution pending = pendingNickResolutions.remove(nick);
            if (pending != null) {
                LOGGER.info("[AutoChatMod]: Resolved {} -> {}. Opening action menu.", nick, realUsername);

                // Execute detection actions for spam if needed
                if (pending.isSpam && pending.similarMessages != null) {
                    executeSpamActions(realUsername, pending.originalMessage, pending.similarMessages);
                }

                // Directly open the action menu for the resolved username
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.player != null && pending.openActionOnResolve) {
                    client.execute(() -> {
                        client.player.networkHandler.sendChatCommand("autochatmod action " + realUsername);
                    });
                }

                // Only execute flagging actions if the message was actually flagged (not just clicked)
                if (pending.isSpam || !pending.openActionOnResolve) {
                    executeActions(realUsername, pending.originalMessage, pending.isSpam);
                }
            }

            // Handle pending Discord notifications that were waiting for nick resolution
            String pendingDiscordMessage = pendingDiscordNotifications.remove(nick);
            if (pendingDiscordMessage != null) {
                // Replace the nickname with the real username in the Discord message
                String resolvedDiscordMessage = pendingDiscordMessage.replace(nick, realUsername);
                webhook.sendMessage(resolvedDiscordMessage);
                LOGGER.info("[AutoChatMod]: Sent resolved Discord notification: {}", resolvedDiscordMessage);
            }

            return true;
        }
        return false;
    }

    private void handleFlaggedMessage(String message, boolean isSpam) {
        UsernameInfo userInfo = extractUsernameInfo(message);
        if (userInfo == null) {
            LOGGER.warn("[AutoChatMod]: Could not extract username from flagged message: {}", message);
            return;
        }

        if (userInfo.isNick) {
            String nick = userInfo.username;
            LOGGER.info("[AutoChatMod]: Flagged message from nick '{}'. Will resolve on user click.", nick);

            List<MessageEntry> similarMessages = null;
            if (isSpam) {
                similarMessages = collectSimilarMessages(message);
            }

            pendingNickResolutions.put(nick.toLowerCase(),
                    new PendingNickResolution(message, nick, isSpam, Instant.now().toEpochMilli(), similarMessages, false));

            ConfigManager.Config config = ConfigManager.getConfig();
            if (config.enableDiscordPing) {
                String alertType = isSpam ? "Spam detected" : "Flagged message";
                String discordMessage = String.format("`%s from %s: %s`", alertType, nick, userInfo.messageContent);

                pendingDiscordNotifications.put(nick.toLowerCase(), discordMessage);

                MinecraftClient client = MinecraftClient.getInstance();
                if (client.player != null) {
                    client.execute(() -> {
                        client.player.networkHandler.sendChatCommand("realname " + nick);
                    });
                }
            }
        } else {
            String username = userInfo.username;
            executeActions(username, message, isSpam);
        }
    }

    private List<MessageEntry> collectSimilarMessages(String message) {
        ConfigManager.Config config = ConfigManager.getConfig();
        List<MessageEntry> similarMessages = new ArrayList<>();
        messageHistory.stream()
                .filter(entry -> calculateSimilarity(message, entry.message) >= config.spamSimilarityThreshold)
                .forEach(similarMessages::add);
        return similarMessages;
    }

    private void executeSpamActions(String username, String originalMessage, List<MessageEntry> similarMessages) {
        ConfigManager.Config config = ConfigManager.getConfig();

        if (config.enableDiscordPing) {
            StringBuilder spamMessages = new StringBuilder("`Spam detected:\n");
            for (MessageEntry entry : similarMessages) {
                String cleanedMessage = extractUsernameAndMessage(entry.message, username);
                spamMessages.append("- ").append(cleanedMessage).append("\n");
            }
            String cleanedOriginal = extractUsernameAndMessage(originalMessage, username);
            spamMessages.append("- ").append(cleanedOriginal).append("`");
            webhook.sendMessage(spamMessages.toString());
        }

        for (MessageEntry entry : similarMessages) {
            displayFlaggedMessage(username, entry.message, true);
        }
    }

    private void executeActions(String username, String originalMessage, boolean isSpam) {
        ConfigManager.Config config = ConfigManager.getConfig();
        LOGGER.info("[AutoChatMod]: Executing actions for user '{}' on message: {}", username, originalMessage);

        if (config.enableDiscordPing && !isSpam) {
            String extractedContent = extractUsernameAndMessage(originalMessage, username);
            String alertType = isSpam ? "Spam detected" : "Flagged message";
            webhook.sendMessage(String.format("`%s from %s: %s`", alertType, username, extractedContent.split(":", 2)[1].trim()));
        }

        displayFlaggedMessage(username, originalMessage, isSpam);
        playAlertSound();
    }

    private boolean shouldIgnoreMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return true;
        }

        String cleanMessage = stripColorCodes(message);

        if (cleanMessage.startsWith("[SPAM]") || cleanMessage.startsWith("[FLAGGED]")) {
            LOGGER.debug("[AutoChatMod]: Ignoring already flagged message: {}", message);
            return true;
        }

        if (COREPROTECT_PATTERN.matcher(cleanMessage).find()) {
            LOGGER.debug("[AutoChatMod]: Ignoring CoreProtect message: {}", message);
            return true;
        }

        ConfigManager.Config config = ConfigManager.getConfig();

        for (String prefix : config.spamWhitelistPrefixes) {
            if (prefix != null && !prefix.trim().isEmpty() && message.startsWith(prefix)) {
                LOGGER.debug("[AutoChatMod]: Ignoring whitelisted prefix [{}] in message: {}", prefix, message);
                return true;
            }
        }

        return false;
    }

    private boolean checkForSpam(String message) {
        ConfigManager.Config config = ConfigManager.getConfig();
        long currentTime = Instant.now().toEpochMilli();
        if(message.startsWith("[*]")) {
            return false;
        }

        messageHistory.removeIf(entry ->
                currentTime - entry.timestamp > config.spamTimeWindowSeconds * 1000L);

        List<MessageEntry> similarMessages = new ArrayList<>();
        long similarCount = messageHistory.stream()
                .filter(entry -> {
                    double sim = calculateSimilarity(message, entry.message);
                    boolean isSimilar = sim >= config.spamSimilarityThreshold;
                    if (isSimilar) {
                        similarMessages.add(entry);
                        LOGGER.debug("[AutoChatMod]: Similar message found [{}] ~ sim={}", entry.message, sim);
                    }
                    return isSimilar;
                })
                .count();
        similarCount++;

        long shortIdenticalCount = 0;
        if (message.length() <= 2) {
            shortIdenticalCount = messageHistory.stream()
                    .filter(entry -> entry.message.equals(message))
                    .count();
            shortIdenticalCount++;
        }

        boolean isSpamBySimilarity = similarCount >= config.spamMessageCount;
        boolean isSpamByShortMessages = (message.length() <= 2) && (shortIdenticalCount >= config.spamMessageCount);

        messageHistory.add(new MessageEntry(currentTime, message));

        if (isSpamBySimilarity) {
            LOGGER.info("[AutoChatMod]: Spam detected due to similarity. SimilarCount={}, Threshold={}",
                    similarCount, config.spamMessageCount);

            UsernameInfo userInfo = extractUsernameInfo(message);
            if (userInfo == null || !userInfo.isNick) {
                handleDirectSpam(message, similarMessages);
            }
            return true;
        }

        if (isSpamByShortMessages) {
            LOGGER.info("[AutoChatMod]: Spam detected due to short, identical messages. ShortIdenticalCount={}, Threshold={}",
                    shortIdenticalCount, config.spamMessageCount);

            UsernameInfo userInfo = extractUsernameInfo(message);
            if (userInfo == null || !userInfo.isNick) {
                handleShortSpam(message);
            }
            return true;
        }

        return false;
    }

    private void handleDirectSpam(String message, List<MessageEntry> similarMessages) {
        ConfigManager.Config config = ConfigManager.getConfig();

        for (MessageEntry entry : similarMessages) {
            flaggedMessages.add(entry.message);
        }
        flaggedMessages.add(message);

        if (config.enableDiscordPing) {
            StringBuilder spamMessages = new StringBuilder("`Spam detected:\n");
            for (MessageEntry entry : similarMessages) {
                String cleanedMessage = extractUsernameAndMessage(entry.message, null);
                spamMessages.append("- ").append(cleanedMessage).append("\n");
            }
            String cleanedCurrent = extractUsernameAndMessage(message, null);
            spamMessages.append("- ").append(cleanedCurrent).append("`");
            webhook.sendMessage(spamMessages.toString());
        }

        messageHistory.clear();
        playAlertSound();
    }

    private void handleShortSpam(String message) {
        ConfigManager.Config config = ConfigManager.getConfig();

        for (MessageEntry entry : messageHistory) {
            if (entry.message.equals(message)) {
                flaggedMessages.add(entry.message);
            }
        }
        flaggedMessages.add(message);

        if (config.enableDiscordPing) {
            String cleanedMessage = extractUsernameAndMessage(message, null);
            webhook.sendMessage("`Short spam detected: " + cleanedMessage + "`");
        }

        messageHistory.clear();
        playAlertSound();
    }

    private boolean checkFlaggedPhrases(String message) {
        if (message.startsWith("[Auth]")) {
            LOGGER.debug("[AutoChatMod]: Skipping phrase check for Auth message: {}", message);
            return false;
        }

        ConfigManager.Config config = ConfigManager.getConfig();
        String lowerMessage = message.toLowerCase();

        for (String phrase : config.whitelistedPhrases) {
            if (phrase != null && !phrase.trim().isEmpty() && lowerMessage.contains(phrase.toLowerCase())) {
                LOGGER.debug("[AutoChatMod]: Phrase [{}] whitelisted in message: {}", phrase, message);
                return false;
            }
        }

        for (String phrase : config.flaggedPhrases) {
            if (phrase != null && !phrase.trim().isEmpty() && lowerMessage.contains(phrase.toLowerCase())) {
                LOGGER.info("[AutoChatMod]: Phrase [{}] matched in message: {}", phrase, message);
                flagMessage(message);
                return true;
            }
        }

        return false;
    }

    private boolean checkFlaggedTerms(String message) {
        String[] words = wordPattern.matcher(message).results()
                .map(match -> match.group())
                .toArray(String[]::new);

        for (String word : words) {
            if (isTermSimilarToFlagged(word)) {
                LOGGER.info("[AutoChatMod]: Word [{}] matched flagged terms in message: {}", word, message);
                flagMessage(message);
                return true;
            }
        }

        return false;
    }

    private boolean isTermSimilarToFlagged(String word) {
        ConfigManager.Config config = ConfigManager.getConfig();
        String lowerWord = word.toLowerCase();

        for (String whitelisted : config.whitelistedTerms) {
            if (whitelisted != null && !whitelisted.trim().isEmpty() && lowerWord.equals(whitelisted.toLowerCase())) {
                LOGGER.debug("[AutoChatMod]: Word [{}] is whitelisted", word);
                return false;
            }
        }

        if (lowerWord.equals("discord.gg/")) {
            LOGGER.info("[AutoChatMod]: Word [{}] flagged as discord link", word);
            return true;
        }

        for (String flaggedTerm : config.flaggedTerms) {
            if (flaggedTerm != null && !flaggedTerm.trim().isEmpty()) {
                double sim = calculateSimilarity(lowerWord, flaggedTerm.toLowerCase());
                if (sim >= config.similarityThreshold) {
                    LOGGER.info("[AutoChatMod]: Word [{}] similar to flagged term [{}] with sim={}", word, flaggedTerm, sim);
                    return true;
                }
            }
        }

        return false;
    }

    private void flagMessage(String message) {
        ConfigManager.Config config = ConfigManager.getConfig();
        LOGGER.info("[AutoChatMod]: Flagging message: {}", message);

        if (config.enableDiscordPing) {
            String cleanedMessage = extractUsernameAndMessage(message, null);
            webhook.sendMessage("`Flagged message: " + cleanedMessage + "`");
        }

        flaggedMessages.add(message);
        playAlertSound();
    }

    private void displayFlaggedMessage(String finalUsername, String originalMessage, boolean isSpam) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        String cleanMessage = stripColorCodes(originalMessage);
        String prefix = isSpam ? "[SPAM] " : "[FLAGGED] ";

        MutableText prefixText = Text.literal(prefix).setStyle(Style.EMPTY.withColor(Formatting.RED).withBold(true));

        int usernameStart = cleanMessage.indexOf(finalUsername);
        if (usernameStart != -1) {
            String beforeUsername = cleanMessage.substring(0, usernameStart);
            String afterUsername = cleanMessage.substring(usernameStart + finalUsername.length());

            MutableText beforeText = Text.literal(beforeUsername);
            MutableText usernameText = Text.literal(finalUsername)
                    .setStyle(Style.EMPTY
                            .withColor(Formatting.YELLOW)
                            .withBold(true)
                            // Corrected Line
                            .withHoverEvent(HoverEvent.showText(Text.literal("Click for actions on " + finalUsername)))
                            // Corrected Line
                            .withClickEvent(ClickEvent.runCommand("/autochatmod action " + finalUsername)));
            MutableText afterText = Text.literal(afterUsername);

            MutableText fullMessage = prefixText.append(beforeText).append(usernameText).append(afterText);
            client.execute(() -> client.player.sendMessage(fullMessage, false));
        } else {
            MutableText messageText = Text.literal(cleanMessage)
                    .setStyle(Style.EMPTY
                            .withColor(Formatting.YELLOW)
                            // Corrected Line
                            .withHoverEvent(HoverEvent.showText(Text.literal("Click for actions on " + finalUsername)))
                            // Corrected Line
                            .withClickEvent(ClickEvent.runCommand("/autochatmod action " + finalUsername)));

            MutableText fullMessage = prefixText.append(messageText);
            client.execute(() -> client.player.sendMessage(fullMessage, false));
        }
    }

    private String extractSender(String fullMessage) {
        String cleanMessage = stripColorCodes(fullMessage);

        UsernameInfo userInfo = extractUsernameInfo(cleanMessage);
        if (userInfo == null) {
            Matcher reportMatcher = REPORT_PATTERN.matcher(cleanMessage);
            if (reportMatcher.find()) {
                return reportMatcher.group(1);
            }

            Matcher filteredMatcher = FILTERED_PATTERN.matcher(cleanMessage);
            if (filteredMatcher.find()) {
                String candidate = filteredMatcher.group(1);
                if (candidate.length() >= 2) {
                    return candidate;
                }
            }

            return null;
        }

        if (userInfo.isNick) {
            return null;
        }

        return userInfo.username;
    }

    private void playAlertSound() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.getSoundManager().play(
                    new net.minecraft.client.sound.PositionedSoundInstance(
                            SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                            net.minecraft.sound.SoundCategory.PLAYERS,
                            1.0F, 1.0F,
                            net.minecraft.util.math.random.Random.create(),
                            client.player.getBlockPos()
                    )
            );
            LOGGER.debug("[AutoChatMod]: Played alert sound for flagged message");
        }
    }

    private String stripColorCodes(String input) {
        if (input == null) return "";
        return COLOR_CODE_PATTERN.matcher(input).replaceAll("");
    }

    private double calculateSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) return 0.0;
        if (s1.equals(s2)) return 1.0;

        int maxLen = Math.max(s1.length(), s2.length());
        if (maxLen == 0) return 1.0;

        return (maxLen - levenshteinDistance(s1, s2)) / (double) maxLen;
    }

    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost);
            }
        }

        return dp[s1.length()][s2.length()];
    }

    private String extractUsernameAndMessage(String fullMessage, String resolvedUsername) {
        String cleanMessage = stripColorCodes(fullMessage);

        UsernameInfo userInfo = extractUsernameInfo(cleanMessage);
        if (userInfo != null) {
            String displayName = resolvedUsername != null ? resolvedUsername : userInfo.username;
            return displayName + ": " + userInfo.messageContent;
        }

        return cleanMessage;
    }

    private String extractUsernameAndMessage(String fullMessage) {
        return extractUsernameAndMessage(fullMessage, null);
    }

    private static class MessageEntry {
        final long timestamp;
        final String message;

        MessageEntry(long timestamp, String message) {
            this.timestamp = timestamp;
            this.message = message;
        }
    }
}