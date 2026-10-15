package de.omegazirkel.risingworld.landclaim;

import de.omegazirkel.risingworld.LandClaim;
import de.omegazirkel.risingworld.tools.bridge.DiscordBridge;
import net.risingworld.api.Plugin;

public class DiscordConnect extends DiscordBridge {

    private static DiscordConnect bridge;
    private static final PluginSettings s = PluginSettings.getInstance();
    private static String displayName = "OZ - Land Claim";

    private DiscordConnect(Plugin owner) {
        super(owner);
    }
    public static final String botLang(){
        return bridge == null ? "en" : bridge.getBotLanguage();
    }

    public static void init(Plugin plugin) {
        displayName = plugin.getDescription("name");
        bridge = new DiscordConnect(plugin);
        if (bridge.isAvailable())
            LandClaim.logger().info("✅ OZ - Discord Connect found!");
        else
            LandClaim.logger().warn("⚠️ OZ - Discord Connect not available!");
    }

    public static void sendDiscordMessage(String message, long channelId) {
        sendDiscordMessage(message, channelId, null);
    }

    public static void sendDiscordMessage(String message, long channelId, byte[] image) {
        if (bridge != null) bridge.sendTextMessage(message, channelId, image);
    }

    private static void sendEvent(String message, long channelId, byte[] image, String topic) {
        if (bridge != null) bridge.sendTextMessage(message, channelId, image,
                topic == null || topic.isBlank() ? displayName : topic.trim());
    }

    public static void sendDiscordClaimAnnouncement(String message) {
        if (s.discordClaimAnnouncementChannelId > 0)
            sendEvent(message, s.discordClaimAnnouncementChannelId, null, s.discordClaimAnnouncementThreadTopic);
    }

    public static void sendDiscordClaimAnnouncement(String message, byte[] image) {
        if (s.discordClaimAnnouncementChannelId > 0)
            sendEvent(message, s.discordClaimAnnouncementChannelId, image, s.discordClaimAnnouncementThreadTopic);
    }

    public static void sendDiscordExpandAnnouncement(String message) {
        if (s.discordExpandAnnouncementChannelId > 0)
            sendEvent(message, s.discordExpandAnnouncementChannelId, null, s.discordExpandAnnouncementThreadTopic);
    }

    public static void sendDiscordExpandAnnouncement(String message, byte[] image) {
        if (s.discordExpandAnnouncementChannelId > 0)
            sendEvent(message, s.discordExpandAnnouncementChannelId, image, s.discordExpandAnnouncementThreadTopic);
    }

    public static void sendDiscordReleaseAccouncement(String message) {
        if (s.discordReleaseAccouncementChannelId > 0)
            sendEvent(message, s.discordReleaseAccouncementChannelId, null, s.discordReleaseAccouncementThreadTopic);
    }

    public static void sendDiscordReleaseAccouncement(String message, byte[] image) {
        if (s.discordReleaseAccouncementChannelId > 0)
            sendEvent(message, s.discordReleaseAccouncementChannelId, image, s.discordReleaseAccouncementThreadTopic);
    }

    public static void sendDiscordForSaleAccouncement(String message) {
        if (s.discordForSaleAccouncementChannelId > 0)
            sendEvent(message, s.discordForSaleAccouncementChannelId, null, s.discordForSaleAccouncementThreadTopic);
    }

    public static void sendDiscordForSaleAccouncement(String message, byte[] image) {
        if (s.discordForSaleAccouncementChannelId > 0)
            sendEvent(message, s.discordForSaleAccouncementChannelId, image, s.discordForSaleAccouncementThreadTopic);
    }

    public static void sendDiscordBuyAccouncement(String message) {
        if (s.discordBuyAccouncementChannelId > 0)
            sendEvent(message, s.discordBuyAccouncementChannelId, null, s.discordBuyAccouncementThreadTopic);
    }

    public static void sendDiscordBuyAccouncement(String message, byte[] image) {
        if (s.discordBuyAccouncementChannelId > 0)
            sendEvent(message, s.discordBuyAccouncementChannelId, image, s.discordBuyAccouncementThreadTopic);
    }

    public static void sendDiscordRenewZoneLog(String message) {
        if (s.discordRenewZoneLogChannelId > 0)
            sendEvent(message, s.discordRenewZoneLogChannelId, null, s.discordRenewZoneLogThreadTopic);
    }

    public static void sendDiscordCleanupEvent(String message) {
        if (s.discordCleanupEventChannelId > 0)
            sendEvent(message, s.discordCleanupEventChannelId, null, s.discordCleanupEventThreadTopic);
    }

}
