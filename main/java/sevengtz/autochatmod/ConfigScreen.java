package sevengtz.autochatmod;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.StringListEntry;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Optional;

public class ConfigScreen {

    public static Screen create(Screen parent) {
        ConfigManager.Config config = ConfigManager.getConfig();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.literal("AutoChatMod Configuration"))
                .setSavingRunnable(() -> {
                    ConfigManager.saveConfig();
                });

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        // General Settings
        ConfigCategory general = builder.getOrCreateCategory(Text.literal("General"));

        general.addEntry(entryBuilder.startBooleanToggle(Text.literal("Enabled"), config.enabled)
                .setDefaultValue(true)
                .setTooltip(Text.literal("Enable/disable the entire mod"))
                .setSaveConsumer(newValue -> config.enabled = newValue)
                .build());

        general.addEntry(entryBuilder.startStrField(Text.literal("Discord Webhook URL"), config.webhookUrl)
                .setDefaultValue("")
                .setTooltip(Text.literal("Your Discord webhook URL for notifications"))
                .setSaveConsumer(newValue -> config.webhookUrl = newValue)
                .build());

        general.addEntry(entryBuilder.startStrField(Text.literal("User Mention ID"), config.userMentionId)
                .setDefaultValue("")
                .setTooltip(Text.literal("Discord user ID to mention in alerts"))
                .setSaveConsumer(newValue -> config.userMentionId = newValue)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Text.literal("Ping on Discord Alert"), config.enableDiscordPing)
                .setDefaultValue(true)
                .setTooltip(Text.literal("Mention you in Discord alerts"))
                .setSaveConsumer(newValue -> config.enableDiscordPing = newValue)
                .build());

        // Detection Settings
        ConfigCategory detection = builder.getOrCreateCategory(Text.literal("Detection"));

        detection.addEntry(entryBuilder.startBooleanToggle(Text.literal("Auto-Open Overlay on Flag"), config.autoOpenOverlayOnFlag)
                .setDefaultValue(true)
                .setTooltip(Text.literal("Automatically open the action overlay when a message is flagged."))
                .setSaveConsumer(newValue -> config.autoOpenOverlayOnFlag = newValue)
                .build());

        detection.addEntry(entryBuilder.startBooleanToggle(Text.literal("Auto-Open Punish GUI on Flag"), config.autoOpenPunishGuiOnFlag)
                .setDefaultValue(false)
                .setTooltip(Text.literal("Automatically run /punish when a message is flagged."), Text.literal("Requires 'Auto-Open Overlay' to be on."))
                .setSaveConsumer(newValue -> config.autoOpenPunishGuiOnFlag = newValue)
                .build());

        detection.addEntry(entryBuilder.startBooleanToggle(Text.literal("Instant Punish for Spam"), config.instantPunishForSpam)
                .setDefaultValue(false)
                .setTooltip(Text.literal("If enabled, pressing the Punish keybind on a spammer will run /punish <user> i:1."), Text.literal("The overlay will NOT auto-open for spammers if this is on."))
                .setSaveConsumer(newValue -> config.instantPunishForSpam = newValue)
                .build());

        detection.addEntry(entryBuilder.startBooleanToggle(Text.literal("Spam Detection"), config.spamDetectionEnabled)
                .setDefaultValue(true)
                .setSaveConsumer(newValue -> config.spamDetectionEnabled = newValue)
                .build());

        detection.addEntry(entryBuilder.startBooleanToggle(Text.literal("Term Detection"), config.termDetectionEnabled)
                .setDefaultValue(true)
                .setSaveConsumer(newValue -> config.termDetectionEnabled = newValue)
                .build());

        detection.addEntry(entryBuilder.startBooleanToggle(Text.literal("Phrase Detection"), config.phraseDetectionEnabled)
                .setDefaultValue(true)
                .setSaveConsumer(newValue -> config.phraseDetectionEnabled = newValue)
                .build());

        detection.addEntry(entryBuilder.startDoubleField(Text.literal("Similarity Threshold"), config.similarityThreshold)
                .setDefaultValue(0.8)
                .setMin(0.0)
                .setMax(1.0)
                .setTooltip(Text.literal("How similar words need to be to flagged terms (0.0-1.0)"))
                .setSaveConsumer(newValue -> config.similarityThreshold = newValue)
                .build());

        // Spam Settings
        ConfigCategory spam = builder.getOrCreateCategory(Text.literal("Spam Detection"));

        spam.addEntry(entryBuilder.startDoubleField(Text.literal("Spam Similarity Threshold"), config.spamSimilarityThreshold)
                .setDefaultValue(0.9)
                .setMin(0.0)
                .setMax(1.0)
                .setTooltip(Text.literal("How similar messages need to be to count as spam"))
                .setSaveConsumer(newValue -> config.spamSimilarityThreshold = newValue)
                .build());

        spam.addEntry(entryBuilder.startIntField(Text.literal("Spam Message Count"), config.spamMessageCount)
                .setDefaultValue(3)
                .setMin(2)
                .setMax(10)
                .setTooltip(Text.literal("Number of similar messages to trigger spam detection"))
                .setSaveConsumer(newValue -> config.spamMessageCount = newValue)
                .build());

        spam.addEntry(entryBuilder.startIntField(Text.literal("Spam Time Window (seconds)"), config.spamTimeWindowSeconds)
                .setDefaultValue(15)
                .setMin(5)
                .setMax(300)
                .setTooltip(Text.literal("Time window for spam detection"))
                .setSaveConsumer(newValue -> config.spamTimeWindowSeconds = newValue)
                .build());

        spam.addEntry(entryBuilder.startStrList(Text.literal("Spam Whitelist Prefixes"), config.spamWhitelistPrefixes)
                .setDefaultValue(ConfigManager.getDefaultSpamWhitelistPrefixes())
                .setTooltip(Text.literal("Messages starting with these prefixes won't trigger spam detection"))
                .setSaveConsumer(newValue -> config.spamWhitelistPrefixes = newValue)
                .build());

        // Terms & Phrases
        ConfigCategory terms = builder.getOrCreateCategory(Text.literal("Flagged Terms"));

        terms.addEntry(entryBuilder.startStrList(Text.literal("Flagged Terms"), config.flaggedTerms)
                .setDefaultValue(ConfigManager.getDefaultFlaggedTerms())
                .setTooltip(Text.literal("Words that will trigger alerts"))
                .setSaveConsumer(newValue -> config.flaggedTerms = newValue)
                .build());

        terms.addEntry(entryBuilder.startStrList(Text.literal("Whitelisted Terms"), config.whitelistedTerms)
                .setDefaultValue(ConfigManager.getDefaultWhitelistedTerms())
                .setTooltip(Text.literal("Words that won't trigger alerts even if similar to flagged terms"))
                .setSaveConsumer(newValue -> config.whitelistedTerms = newValue)
                .build());

        ConfigCategory phrases = builder.getOrCreateCategory(Text.literal("Flagged Phrases"));

        phrases.addEntry(entryBuilder.startStrList(Text.literal("Flagged Phrases"), config.flaggedPhrases)
                .setDefaultValue(ConfigManager.getDefaultFlaggedPhrases())
                .setTooltip(Text.literal("Phrases that will trigger alerts"))
                .setSaveConsumer(newValue -> config.flaggedPhrases = newValue)
                .build());

        phrases.addEntry(entryBuilder.startStrList(Text.literal("Whitelisted Phrases"), config.whitelistedPhrases)
                .setDefaultValue(ConfigManager.getDefaultWhitelistedPhrases())
                .setTooltip(Text.literal("Phrases that won't trigger alerts"))
                .setSaveConsumer(newValue -> config.whitelistedPhrases = newValue)
                .build());



        ConfigCategory sound = builder.getOrCreateCategory(Text.literal("Sounds"));

        sound.addEntry(entryBuilder.startEnumSelector(Text.literal("Alert Sound"), SoundOption.class, config.alertSound)
                .setDefaultValue(SoundOption.EXPERIENCE_ORB)
                .setTooltip(Text.literal("The sound to play when a message is flagged."))
                .setEnumNameProvider(option -> ((SoundOption) option).toText())
                .setSaveConsumer(newValue -> config.alertSound = newValue)
                .build());
        sound.addEntry(entryBuilder.startFloatField(Text.literal("Alert Sound Volume"), config.alertSoundVolume)
                .setDefaultValue(1.0F)
                .setMin(0.0F).setMax(2.0F)
                .setTooltip(Text.literal("The volume of the alert sound (0.0 to 2.0)"))
                .setSaveConsumer(newValue -> config.alertSoundVolume = newValue)
                .build());
        
        sound.addEntry(entryBuilder.startFloatField(Text.literal("Alert Sound Pitch"), config.alertSoundPitch)
                .setDefaultValue(1.0F)
                .setMin(0.1F).setMax(2.0F)
                .setTooltip(Text.literal("The pitch of the alert sound (0.1 to 2.0)"))
                .setSaveConsumer(newValue -> config.alertSoundPitch = newValue)
                .build());
        sound.addEntry(entryBuilder.startBooleanToggle(Text.literal("Alert Sound Enabled"), config.alertSoundEnabled)
                .setDefaultValue(true)
                .setTooltip(Text.literal("Enable/disable the alert sound"))
                .setSaveConsumer(newValue -> config.alertSoundEnabled = newValue)
                .build());


        ConfigCategory evidence = builder.getOrCreateCategory(Text.literal("Evidence"));

        evidence.addEntry(entryBuilder.startBooleanToggle(Text.literal("Enable Evidence Screenshots"), config.evidenceScreenshotEnabled)
                        .setDefaultValue(true)
                        .setTooltip(Text.literal("Automatically take a screenshot when you mute a player."))
                        .setSaveConsumer(newValue -> config.evidenceScreenshotEnabled = newValue)
                        .build());
                
        evidence.addEntry(entryBuilder.startStrField(Text.literal("Your Moderator Name"), config.evidenceModeratorName)
                        .setDefaultValue("")
                        .setTooltip(Text.literal("Your in-game name. Screenshots will only be taken for mutes you perform."))
                        .setSaveConsumer(newValue -> config.evidenceModeratorName = newValue)
                        .build());

        return builder.build();
    }
}