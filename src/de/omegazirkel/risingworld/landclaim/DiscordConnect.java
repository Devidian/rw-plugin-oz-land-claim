package de.omegazirkel.risingworld.landclaim;

import java.lang.reflect.Method;

import de.omegazirkel.risingworld.LandClaim;
import net.risingworld.api.Plugin;

public class DiscordConnect {

    private static Plugin pluginRef = null;
    private static final PluginSettings s = PluginSettings.getInstance();
    public static final String botLang(){
        String lang = (String) callPluginMethod("getBotLanguage", null, null);
        return lang != null ?  lang : "en";
    }

    public static void init(Plugin plugin) {
        pluginRef = plugin.getPluginByName("OZ - Discord Connect");
        if (pluginRef != null)
            LandClaim.logger().info("✅ " + pluginRef.getName() + " found! ID: " + pluginRef.getID());
        else
            LandClaim.logger().warn("⚠️ OZ - Discord Connect not available!");
    }

    private static boolean isPluginAvailable() {
        try {
            Class.forName("de.omegazirkel.risingworld.DiscordConnect");
            return pluginRef != null;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static Object callPluginMethod(String methodName, Class<?>[] paramTypes, Object[] args) {
        if (!isPluginAvailable()) {
            return null;
        }

        try {
            Object plugin = pluginRef;
            Class<?> clazz = plugin.getClass();
            Method method = clazz.getMethod(methodName, paramTypes);
            return method.invoke(plugin, args);
        } catch (Exception e) {
            LandClaim.logger().error("Error while calling DiscordConnect Method");
            e.printStackTrace();
            return null;
        }
    }

    public static void sendDiscordMessage(String message, long channelId) {
        sendDiscordMessage(message, channelId, null);
    }

    public static void sendDiscordMessage(String message, long channelId, byte[] image) {
        callPluginMethod("sendDiscordMessageToTextChannel",
                new Class<?>[] { String.class, long.class, byte[].class },
                new Object[] { message, channelId, image });
    }

    public static void sendDiscordClaimAnnouncement(String message) {
        if (s.enableDiscordClaimAnnouncement)
            sendDiscordMessage(message, s.discordClaimAnnouncementChannelId);
    }

    public static void sendDiscordClaimAnnouncement(String message, byte[] image) {
        if (s.enableDiscordClaimAnnouncement)
            sendDiscordMessage(message, s.discordClaimAnnouncementChannelId);
    }

    public static void sendDiscordExpandAnnouncement(String message) {
        if (s.enableDiscordExpandAnnouncement)
            sendDiscordMessage(message, s.discordExpandAnnouncementChannelId);
    }

    public static void sendDiscordExpandAnnouncement(String message, byte[] image) {
        if (s.enableDiscordExpandAnnouncement)
            sendDiscordMessage(message, s.discordExpandAnnouncementChannelId, image);
    }

    public static void sendDiscordReleaseAccouncement(String message) {
        if (s.enableDiscordReleaseAccouncement)
            sendDiscordMessage(message, s.discordReleaseAccouncementChannelId);
    }

    public static void sendDiscordReleaseAccouncement(String message, byte[] image) {
        if (s.enableDiscordReleaseAccouncement)
            sendDiscordMessage(message, s.discordReleaseAccouncementChannelId);
    }

    public static void sendDiscordForSaleAccouncement(String message) {
        if (s.enableDiscordForSaleAccouncement)
            sendDiscordMessage(message, s.discordForSaleAccouncementChannelId);
    }

    public static void sendDiscordForSaleAccouncement(String message, byte[] image) {
        if (s.enableDiscordForSaleAccouncement)
            sendDiscordMessage(message, s.discordForSaleAccouncementChannelId);
    }

    public static void sendDiscordBuyAccouncement(String message) {
        if (s.enableDiscordBuyAccouncement)
            sendDiscordMessage(message, s.discordBuyAccouncementChannelId);
    }

    public static void sendDiscordBuyAccouncement(String message, byte[] image) {
        if (s.enableDiscordBuyAccouncement)
            sendDiscordMessage(message, s.discordBuyAccouncementChannelId);
    }

}
