package sevengtz.autochatmod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_FILE = FabricLoader.getInstance()
            .getConfigDir().resolve("autochatmod.json");

    private static Config config;

    public static void init() {
        loadConfig();
    }

    public static void loadConfig() {
        if (Files.exists(CONFIG_FILE)) {
            try {
                String content = Files.readString(CONFIG_FILE);
                config = GSON.fromJson(content, Config.class);
                if (config == null) {
                    config = createDefaultConfig();
                }
                // Ensure all lists are initialized
                if (config.flaggedTerms == null) config.flaggedTerms = getDefaultFlaggedTerms();
                if (config.enableDiscordPing == null) config.enableDiscordPing = true;
                if (config.flaggedPhrases == null) config.flaggedPhrases = getDefaultFlaggedPhrases();
                if (config.whitelistedTerms == null) config.whitelistedTerms = getDefaultWhitelistedTerms();
                if (config.whitelistedPhrases == null) config.whitelistedPhrases = getDefaultWhitelistedPhrases();
                if (config.spamWhitelistPrefixes == null) config.spamWhitelistPrefixes = getDefaultSpamWhitelistPrefixes();
            } catch (IOException | JsonSyntaxException e) {
                AutoChatMod.LOGGER.error("Failed to load config, using defaults", e);
                config = createDefaultConfig();
            }
        } else {
            config = createDefaultConfig();
        }
        saveConfig();
    }

    public static void saveConfig() {
        try {
            Files.createDirectories(CONFIG_FILE.getParent());
            String json = GSON.toJson(config);
            Files.writeString(CONFIG_FILE, json);
        } catch (IOException e) {
            AutoChatMod.LOGGER.error("Failed to save config", e);
        }
    }

    private static Config createDefaultConfig() {
        Config defaultConfig = new Config();
        defaultConfig.webhookUrl = "https://discord.com/api/webhooks/YOUR_WEBHOOK_URL_HERE";
        defaultConfig.userMentionId = "YOUR ID HERE";
        defaultConfig.enabled = true;
        defaultConfig.spamDetectionEnabled = true;
        defaultConfig.termDetectionEnabled = true;
        defaultConfig.phraseDetectionEnabled = true;
        defaultConfig.similarityThreshold = 0.8;
        defaultConfig.spamSimilarityThreshold = 0.9;
        defaultConfig.spamMessageCount = 3;
        defaultConfig.spamTimeWindowSeconds = 15;
        defaultConfig.flaggedTerms = getDefaultFlaggedTerms();
        defaultConfig.flaggedPhrases = getDefaultFlaggedPhrases();
        defaultConfig.whitelistedTerms = getDefaultWhitelistedTerms();
        defaultConfig.whitelistedPhrases = getDefaultWhitelistedPhrases();
        defaultConfig.spamWhitelistPrefixes = getDefaultSpamWhitelistPrefixes();
        defaultConfig.alertSound = SoundOption.EXPERIENCE_ORB;
        defaultConfig.alertSoundVolume = 1.0F;
        defaultConfig.alertSoundPitch = 1.0F;
        defaultConfig.enableDiscordPing = false;
        defaultConfig.alertSoundEnabled = true;
        defaultConfig.autoOpenOverlayOnFlag = true;
        defaultConfig.autoOpenPunishGuiOnFlag = false;
        defaultConfig.instantPunishForSpam = false;
        defaultConfig.evidenceScreenshotEnabled = true;
        defaultConfig.evidenceModeratorName = ""; // User needs to set this
        return defaultConfig;
    }

    public static List<String> getDefaultFlaggedTerms() {
        return new ArrayList<>(Arrays.asList(
                "nigger", "faggot", "fag", "chink", "tranny", "kys", "slit", "cum", "hitler",
                "stalin", "child", "doxx", "doxbin", "beaner", "paki", "negro", "queer", "dox",
                "ddos", "doxxed", "swatted", "ddosed", "cancer", "family", "niger", "frocio",
                "pd", "pede", "negger", "swat", "suicide"
        ));
    }

    public static List<String> getDefaultFlaggedPhrases() {
        return new ArrayList<>(Arrays.asList(
                "hang yourself", "kill yourself", "kill urself", "slit your wrists", "hang urself",
                "kill ur self", "kill your self", "get cancer", "slit ur wrists", "hang ur self",
                "neck urself", "hope you die", "ching chong", "ur ip", "ur address", "your ip",
                "your address", "black monkey"
        ));
    }

    public static List<String> getDefaultWhitelistedTerms() {
        return new ArrayList<>(Arrays.asList(
                "think", "never", "bigger", "digger", "nicer", "china", "pakistan", "rag",
                "nice", "sweat", "tiger", "chill", "chunk", "thang", "queen"
        ));
    }

    public static List<String> getDefaultWhitelistedPhrases() {
        return new ArrayList<>(Arrays.asList("Suicide Encouragement"));
    }

    public static List<String> getDefaultSpamWhitelistPrefixes() {
        return new ArrayList<>(Arrays.asList(
                "[Broadcast]", "[Crates]", "[Spy]", "[System]", "[Server]", "[Auth]", "[*]", "[S]", "[SPAM]", "[FLAGGED]", "X-Ray ▶"
        ));
    }

    public static Config getConfig() {
        return config;
    }

    public static void setConfig(Config newConfig) {
        config = newConfig;
        saveConfig();
    }

    public static class Config {
        public String webhookUrl = "";
        public String userMentionId = "";
        public Boolean enableDiscordPing = false;
        public boolean enabled = true;
        public boolean spamDetectionEnabled = true;
        public boolean termDetectionEnabled = true;
        public boolean phraseDetectionEnabled = true;
        public double similarityThreshold = 0.8;
        public double spamSimilarityThreshold = 0.9;
        public int spamMessageCount = 3;
        public int spamTimeWindowSeconds = 15;
        public List<String> flaggedTerms = new ArrayList<>();
        public List<String> flaggedPhrases = new ArrayList<>();
        public List<String> whitelistedTerms = new ArrayList<>();
        public List<String> whitelistedPhrases = new ArrayList<>();
        public List<String> spamWhitelistPrefixes = new ArrayList<>();
        public SoundOption alertSound = SoundOption.EXPERIENCE_ORB;
        public float alertSoundVolume = 1.0F;
        public float alertSoundPitch = 1.0F;
        public boolean alertSoundEnabled = true;
        public boolean autoOpenOverlayOnFlag = true;
        public boolean autoOpenPunishGuiOnFlag = false;
        public boolean instantPunishForSpam = false;
        public int hudX = -1;
        public int hudY = -1;
        public int hudWidth = 250;
        public int hudHeight = 100;
        public boolean evidenceScreenshotEnabled = true;
        public String evidenceModeratorName = "";
    }
}
 
