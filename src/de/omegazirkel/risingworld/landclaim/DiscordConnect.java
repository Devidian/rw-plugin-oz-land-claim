package de.omegazirkel.risingworld.landclaim;

import de.omegazirkel.risingworld.LandClaim;
import de.omegazirkel.risingworld.tools.bridge.DiscordBridge;
import net.risingworld.api.Plugin;

public class DiscordConnect extends DiscordBridge {

    private static DiscordConnect bridge;
    private static final PluginSettings s = PluginSettings.getInstance();

    private DiscordConnect(Plugin owner) {
        super(owner);
    }
    public static final String botLang(){
        return bridge == null ? "en" : bridge.getBotLanguage();
    }

    public static void init(Plugin plugin) {
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

    public static void sendDiscordClaimAnnouncement(String message) {
        if (s.discordClaimAnnouncementChannelId > 0)
            sendDiscordMessage(message, s.discordClaimAnnouncementChannelId);
    }

    public static void sendDiscordClaimAnnouncement(String message, byte[] image) {
        if (s.discordClaimAnnouncementChannelId > 0)
            sendDiscordMessage(message, s.discordClaimAnnouncementChannelId);
    }

    public static void sendDiscordExpandAnnouncement(String message) {
        if (s.discordExpandAnnouncementChannelId > 0)
            sendDiscordMessage(message, s.discordExpandAnnouncementChannelId);
    }

    public static void sendDiscordExpandAnnouncement(String message, byte[] image) {
        if (s.discordExpandAnnouncementChannelId > 0)
            sendDiscordMessage(message, s.discordExpandAnnouncementChannelId, image);
    }

    public static void sendDiscordReleaseAccouncement(String message) {
        if (s.discordReleaseAccouncementChannelId > 0)
            sendDiscordMessage(message, s.discordReleaseAccouncementChannelId);
    }

    public static void sendDiscordReleaseAccouncement(String message, byte[] image) {
        if (s.discordReleaseAccouncementChannelId > 0)
            sendDiscordMessage(message, s.discordReleaseAccouncementChannelId);
    }

    public static void sendDiscordForSaleAccouncement(String message) {
        if (s.discordForSaleAccouncementChannelId > 0)
            sendDiscordMessage(message, s.discordForSaleAccouncementChannelId);
    }

    public static void sendDiscordForSaleAccouncement(String message, byte[] image) {
        if (s.discordForSaleAccouncementChannelId > 0)
            sendDiscordMessage(message, s.discordForSaleAccouncementChannelId);
    }

    public static void sendDiscordBuyAccouncement(String message) {
        if (s.discordBuyAccouncementChannelId > 0)
            sendDiscordMessage(message, s.discordBuyAccouncementChannelId);
    }

    public static void sendDiscordBuyAccouncement(String message, byte[] image) {
        if (s.discordBuyAccouncementChannelId > 0)
            sendDiscordMessage(message, s.discordBuyAccouncementChannelId);
    }

    public static void sendDiscordRenewZoneLog(String message) {
        if (s.discordRenewZoneLogChannelId > 0)
            sendDiscordMessage(message, s.discordRenewZoneLogChannelId);
    }

}
