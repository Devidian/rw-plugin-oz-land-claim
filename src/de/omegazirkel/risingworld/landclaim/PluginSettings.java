package de.omegazirkel.risingworld.landclaim;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import de.omegazirkel.risingworld.LandClaim;
import de.omegazirkel.risingworld.tools.OZLogger;
import de.omegazirkel.risingworld.tools.settings.AdminSettingsEntry;
import de.omegazirkel.risingworld.tools.settings.AdminSettingsType;
import de.omegazirkel.risingworld.tools.settings.SettingsFileEditor;
import net.risingworld.api.Plugin;
import net.risingworld.api.utils.ColorRGBA;

public class PluginSettings {
        private static PluginSettings instance = null;

        private static Plugin plugin;

        private static OZLogger logger() {
                return LandClaim.logger();
        }

        // Public settings
        public String logLevel = "ALL";
        public Boolean reloadOnChange = true;
        public Integer minutesToClaim = 10;
        public double claimTimeScaleFactor = 1.01;
        public Integer basicClaimLimit = 5;
        public double playTimeHoursExtraClaimFactor = 0.6;
        public Integer claimProtectionBaseTimeDays = 7;
        public double claimProtectionExtraTimeScale = 5;
        public Boolean enableWelcomeMessage = false;
        public Integer recentlyOnlinePermissionListHours = 24;

        public Integer claimBaseCost = 100;
        public double claimSaleFee = 0.01;
        public Boolean adminIgnoreLimit = true;
        public Boolean adminIgnoreTime = true;
        public Boolean enableAutoClaimRemoval = false;
        public Integer autoClaimRemovalInactiveDays = 90;
        public Integer autoClaimRemovalDelaySeconds = 60;
        public Boolean allowClaimSale = false;
        public Boolean allowClaimBuyExceedLimit = false;
        public Boolean enableExtraClaimShopOffer = true;
        public Integer extraClaimBasePrice = 200;
        public Integer extraClaimPriceIncreasePercent = 10;
        public String extraClaimShopCurrencyIdentifier = "";
        // Discord announcements
        public long discordClaimAnnouncementChannelId = 0;
        public long discordExpandAnnouncementChannelId = 0;
        public long discordReleaseAccouncementChannelId = 0;
        public long discordForSaleAccouncementChannelId = 0;
        public long discordBuyAccouncementChannelId = 0;
        public Boolean enableDiscordClaimAnnouncement = true;
        public Boolean enableDiscordExpandAnnouncement = true;
        public Boolean enableDiscordReleaseAccouncement = true;
        public Boolean enableDiscordForSaleAccouncement = true;
        public Boolean enableDiscordBuyAccouncement = true;
        // Ingame event announcements
        public Boolean enableIngameClaimAnnouncement = true;
        public Boolean enableIngameExpandAnnouncement = true;
        public Boolean enableIngameReleaseAccouncement = true;
        public Boolean enableIngameForSaleAccouncement = true;
        public Boolean enableIngameBuyAccouncement = true;
        // permissions to use
        public String specialRestAreaPermission = "ozlc-special-rest";
        public String specialPvPAreaPermission = "ozlc-special-pvp";
        public String specialTrapAreaPermission = "ozlc-special-trap";
        public String specialAreaPermission = "ozlc-special";
        public String defaultAreaPermission = "ozlc-guest";
        public String ownerAreaPermission = "ozlc-owner";
        public String residentAreaPermission = "ozlc-resident";
        public String prisonerAreaPermission = "ozlc-prisoner";
        public String exiledAreaPermission = "ozlc-exiled";
        public String friendAreaPermission = "ozlc-friend";
        // Colors
        // -- Border colors --
        // color="#FFFF0015"
        public ColorRGBA currentChunkBorderColor = new ColorRGBA(0xFFFF0010);
        // color="#10E00015"
        public ColorRGBA ownedAreaBorderColor = new ColorRGBA(0x10E00010);
        // color="#0010E015"
        public ColorRGBA otherAreaBorderColor = new ColorRGBA(0x0010E010);
        // color="#00FFFF15"
        public ColorRGBA forSaleAreaBorderColor = new ColorRGBA(0x00FFFF10);
        // color="#FFFFFF15"
        public ColorRGBA specialAreaBorderColor = new ColorRGBA(0xFFFFFF10);
        // color="#FF000015"
        public ColorRGBA pvpAreaBorderColor = new ColorRGBA(0xFF000010);
        // color="#6fff8215"
        public ColorRGBA restAreaBorderColor = new ColorRGBA(0x6fff829c);
        // color="#ff910015"
        public ColorRGBA trapAreaBorderColor = new ColorRGBA(0xff91009c);
        // -- Frame colors --
        // color="#FFFF0050"
        public ColorRGBA currentChunkFrameColor = new ColorRGBA(0xFFFF0050);
        // color="#10E00050"
        public ColorRGBA ownedAreaFrameColor = new ColorRGBA(0x10E00050);
        // color="#0010E050"
        public ColorRGBA otherAreaFrameColor = new ColorRGBA(0x0010E050);
        // color="#00FFFF50"
        public ColorRGBA forSaleAreaFrameColor = new ColorRGBA(0x00FFFF50);
        // color="#FFFFFF50"
        public ColorRGBA specialAreaFrameColor = new ColorRGBA(0xFFFFFF50);
        // color="#FF000050"
        public ColorRGBA pvpAreaFrameColor = new ColorRGBA(0xFF000050);
        // color="#6fff82AA"
        public ColorRGBA restAreaFrameColor = new ColorRGBA(0x6fff82AA);
        // color="#ff9100AA"
        public ColorRGBA trapAreaFrameColor = new ColorRGBA(0xff9100AA);

        // settings end

        private PluginSettings() {
        }

        public static PluginSettings getInstance(Plugin p) {
                plugin = p;
                return getInstance();
        }

        public static PluginSettings getInstance() {

                if (instance == null) {
                        instance = new PluginSettings();
                }
                return instance;
        }

        public void initSettings() {
                initSettings((plugin.getPath() != null ? plugin.getPath() : ".") + "/settings.properties");
        }

        public void initSettings(String filePath) {
                Path settingsFile = Paths.get(filePath);
                Path defaultSettingsFile = settingsFile.resolveSibling("settings.default.properties");

                try {
                        if (Files.notExists(settingsFile) && Files.exists(defaultSettingsFile)) {
                                logger().info("settings.properties not found, copying from settings.default.properties...");
                                Files.copy(defaultSettingsFile, settingsFile);
                        }

                        Properties settings = new Properties();
                        if (Files.exists(settingsFile)) {
                                try (FileInputStream in = new FileInputStream(settingsFile.toFile())) {
                                        settings.load(new InputStreamReader(in, "UTF8"));
                                }
                        } else {
                                logger().warn(
                                                "⚠️ Neither settings.properties nor settings.default.properties found. Using default values.");
                        }
                        // fill properties
                        logLevel = settings.getProperty("logLevel", "ALL");
                        reloadOnChange = settings.getProperty("reloadOnChange", "true").contentEquals("true");
                        enableWelcomeMessage = settings.getProperty("enableWelcomeMessage", "false")
                                        .contentEquals("true");
                        recentlyOnlinePermissionListHours = Integer
                                        .parseInt(settings.getProperty("recentlyOnlinePermissionListHours", "24"));
                        // admin only settings
                        adminIgnoreLimit = settings.getProperty("adminIgnoreLimit", "false").contentEquals("true");
                        adminIgnoreTime = settings.getProperty("adminIgnoreTime", "false").contentEquals("true");
                        enableAutoClaimRemoval = settings.getProperty("enableAutoClaimRemoval", "false")
                                        .contentEquals("true");
                        autoClaimRemovalInactiveDays = Integer
                                        .parseInt(settings.getProperty("autoClaimRemovalInactiveDays", "90"));
                        autoClaimRemovalDelaySeconds = Integer
                                        .parseInt(settings.getProperty("autoClaimRemovalDelaySeconds", "60"));
                        // trade settings
                        allowClaimSale = settings.getProperty("allowClaimSale", "false").contentEquals("true");
                        allowClaimBuyExceedLimit = settings.getProperty("allowClaimBuyExceedLimit", "false")
                                        .contentEquals("true");
                        enableExtraClaimShopOffer = settings.getProperty("enableExtraClaimShopOffer", "true")
                                        .contentEquals("true");
                        extraClaimBasePrice = Integer.parseInt(settings.getProperty("extraClaimBasePrice", "200"));
                        extraClaimPriceIncreasePercent = Integer
                                        .parseInt(settings.getProperty("extraClaimPriceIncreasePercent", "10"));
                        extraClaimShopCurrencyIdentifier = settings.getProperty("extraClaimShopCurrencyIdentifier", "");
                        // claim settings
                        minutesToClaim = Integer.parseInt(settings.getProperty("minutesToClaim", "10"));
                        claimTimeScaleFactor = Double.parseDouble(settings.getProperty("claimTimeScaleFactor", "1.01"));
                        basicClaimLimit = Integer.parseInt(settings.getProperty("basicClaimLimit", "5"));
                        playTimeHoursExtraClaimFactor = Double
                                        .parseDouble(settings.getProperty("playTimeHoursExtraClaimFactor", "0.6"));
                        // protection settings
                        claimProtectionBaseTimeDays = Integer
                                        .parseInt(settings.getProperty("claimProtectionBaseTimeDays", "7"));
                        claimProtectionExtraTimeScale = Double
                                        .parseDouble(settings.getProperty("claimProtectionExtraTimeScale", "5"));

                        // Discord announcements
                        discordClaimAnnouncementChannelId = Long
                                        .parseLong(settings.getProperty("discordClaimAnnouncementChannelId", "0"));
                        discordExpandAnnouncementChannelId = Long
                                        .parseLong(settings.getProperty("discordExpandAnnouncementChannelId", "0"));
                        discordReleaseAccouncementChannelId = Long
                                        .parseLong(settings.getProperty("discordReleaseAccouncementChannelId", "0"));
                        discordForSaleAccouncementChannelId = Long
                                        .parseLong(settings.getProperty("discordForSaleAccouncementChannelId", "0"));
                        discordBuyAccouncementChannelId = Long
                                        .parseLong(settings.getProperty("discordBuyAccouncementChannelId", "0"));
                        enableDiscordClaimAnnouncement = settings.getProperty("enableDiscordClaimAnnouncement", "false")
                                        .contentEquals("true");
                        enableDiscordExpandAnnouncement = settings
                                        .getProperty("enableDiscordExpandAnnouncement", "false")
                                        .contentEquals("true");
                        enableDiscordReleaseAccouncement = settings
                                        .getProperty("enableDiscordReleaseAccouncement", "false")
                                        .contentEquals("true");
                        enableDiscordForSaleAccouncement = settings
                                        .getProperty("enableDiscordForSaleAccouncement", "false")
                                        .contentEquals("true");
                        enableDiscordBuyAccouncement = settings.getProperty("enableDiscordBuyAccouncement", "false")
                                        .contentEquals("true");
                        // Ingame announcements
                        enableIngameClaimAnnouncement = settings.getProperty("enableIngameClaimAnnouncement", "false")
                                        .contentEquals("true");
                        enableIngameExpandAnnouncement = settings.getProperty("enableIngameExpandAnnouncement", "false")
                                        .contentEquals("true");
                        enableIngameReleaseAccouncement = settings
                                        .getProperty("enableIngameReleaseAccouncement", "false")
                                        .contentEquals("true");
                        enableIngameForSaleAccouncement = settings
                                        .getProperty("enableIngameForSaleAccouncement", "false")
                                        .contentEquals("true");
                        enableIngameBuyAccouncement = settings.getProperty("enableIngameBuyAccouncement", "false")
                                        .contentEquals("true");

                        // Area permissions
                        specialRestAreaPermission = settings.getProperty("specialRestAreaPermission",
                                        "ozlc-special-rest");
                        specialPvPAreaPermission = settings.getProperty("specialPvPAreaPermission", "ozlc-special-pvp");
                        specialTrapAreaPermission = settings.getProperty("specialTrapAreaPermission",
                                        "ozlc-special-trap");
                        specialAreaPermission = settings.getProperty("specialAreaPermission", "ozlc-special");
                        defaultAreaPermission = settings.getProperty("defaultAreaPermission", "ozlc-guest");
                        ownerAreaPermission = settings.getProperty("ownerAreaPermission", "ozlc-owner");
                        friendAreaPermission = settings.getProperty("friendAreaPermission", "ozlc-friend");
                        residentAreaPermission = settings.getProperty("residentAreaPermission", "ozlc-resident");
                        prisonerAreaPermission = settings.getProperty("prisonerAreaPermission", "ozlc-prisoner");
                        exiledAreaPermission = settings.getProperty("exiledAreaPermission", "ozlc-exiled");

                        claimBaseCost = Integer.parseInt(settings.getProperty("claimBaseCost", "100"));
                        claimSaleFee = Double.parseDouble(settings.getProperty("claimSaleFee", "0.01"));

                        currentChunkBorderColor = new ColorRGBA((int) Long.parseUnsignedLong(
                                        settings.getProperty("currentChunkBorderColor", "0xFFFF0010").replace("0x", ""),
                                        16));
                        ownedAreaBorderColor = new ColorRGBA((int) Long.parseUnsignedLong(
                                        settings.getProperty("ownedAreaBorderColor", "0x10E00010").replace("0x", ""),
                                        16));
                        otherAreaBorderColor = new ColorRGBA((int) Long.parseUnsignedLong(
                                        settings.getProperty("otherAreaBorderColor", "0x0010E010").replace("0x", ""),
                                        16));
                        forSaleAreaBorderColor = new ColorRGBA((int) Long.parseUnsignedLong(
                                        settings.getProperty("forSaleAreaBorderColor", "0x00FFFF10").replace("0x", ""),
                                        16));
                        specialAreaBorderColor = new ColorRGBA((int) Long.parseUnsignedLong(
                                        settings.getProperty("specialAreaBorderColor", "0xFFFFFF10").replace("0x", ""),
                                        16));
                        pvpAreaBorderColor = new ColorRGBA((int) Long.parseUnsignedLong(
                                        settings.getProperty("pvpAreaBorderColor", "0xFF000010").replace("0x", ""),
                                        16));
                        restAreaBorderColor = new ColorRGBA((int) Long.parseUnsignedLong(
                                        settings.getProperty("restAreaBorderColor", "0x6fff829c").replace("0x", ""),
                                        16));
                        trapAreaBorderColor = new ColorRGBA((int) Long.parseUnsignedLong(
                                        settings.getProperty("trapAreaBorderColor", "0xff91009c").replace("0x", ""),
                                        16));

                        currentChunkFrameColor = new ColorRGBA((int) Long.parseUnsignedLong(
                                        settings.getProperty("currentChunkFrameColor", "0xFFFF0050").replace("0x", ""),
                                        16));
                        ownedAreaFrameColor = new ColorRGBA((int) Long.parseUnsignedLong(
                                        settings.getProperty("ownedAreaFrameColor", "0x10E00050").replace("0x", ""),
                                        16));
                        otherAreaFrameColor = new ColorRGBA((int) Long.parseUnsignedLong(
                                        settings.getProperty("otherAreaFrameColor", "0x0010E050").replace("0x", ""),
                                        16));
                        forSaleAreaFrameColor = new ColorRGBA((int) Long.parseUnsignedLong(
                                        settings.getProperty("forSaleAreaFrameColor", "0x00FFFF50").replace("0x", ""),
                                        16));
                        specialAreaFrameColor = new ColorRGBA((int) Long.parseUnsignedLong(
                                        settings.getProperty("specialAreaFrameColor", "0xFFFFFF50").replace("0x", ""),
                                        16));
                        pvpAreaFrameColor = new ColorRGBA((int) Long.parseUnsignedLong(
                                        settings.getProperty("pvpAreaFrameColor", "0xFF000050").replace("0x", ""), 16));
                        restAreaFrameColor = new ColorRGBA((int) Long.parseUnsignedLong(
                                        settings.getProperty("restAreaFrameColor", "0x6fff82AA").replace("0x", ""),
                                        16));
                        trapAreaFrameColor = new ColorRGBA((int) Long.parseUnsignedLong(
                                        settings.getProperty("trapAreaFrameColor", "0xff9100AA").replace("0x", ""),
                                        16));

                        logger().info(plugin.getName() + " Plugin settings loaded");

                } catch (IOException ex) {
                        logger().error("IOException on initSettings: " + ex.getMessage());
                        ex.printStackTrace();
                } catch (NumberFormatException ex) {
                        logger().error("NumberFormatException on initSettings: " + ex.getMessage());
                        ex.printStackTrace();
                }
        }

        public java.util.List<AdminSettingsEntry> adminSettingsEntries() {
                return java.util.List.of(
                                entry("logLevel", "Log level", "Controls LandClaim logging verbosity.", logLevel,
                                                "ALL", AdminSettingsType.STRING),
                                entry("reloadOnChange", "Reload on change",
                                                "Documents that LandClaim settings reload when settings.properties changes.",
                                                reloadOnChange, "true", AdminSettingsType.BOOLEAN),
                                entry("enableWelcomeMessage", "Welcome message",
                                                "Shows a short LandClaim message when a player joins.",
                                                enableWelcomeMessage, "false", AdminSettingsType.BOOLEAN),
                                entry("recentlyOnlinePermissionListHours", "Recently online hours",
                                                "Hours used for recently seen players in permission workflows.",
                                                recentlyOnlinePermissionListHours, "24", AdminSettingsType.INTEGER),
                                entry("adminIgnoreLimit", "Admin ignores claim limit",
                                                "Lets admins bypass claim limits.", adminIgnoreLimit, "true",
                                                AdminSettingsType.BOOLEAN),
                                entry("adminIgnoreTime", "Admin ignores claim time",
                                                "Lets admins bypass claim wait time.", adminIgnoreTime, "true",
                                                AdminSettingsType.BOOLEAN),
                                entry("enableAutoClaimRemoval", "Auto claim removal",
                                                "Removes claims from owners inactive longer than the configured days.",
                                                enableAutoClaimRemoval, "false", AdminSettingsType.BOOLEAN),
                                entry("autoClaimRemovalInactiveDays", "Inactive days",
                                                "Inactive owner age before auto claim removal.", autoClaimRemovalInactiveDays,
                                                "90", AdminSettingsType.INTEGER),
                                entry("enableExtraClaimShopOffer", "Extra claim shop offer",
                                                "Registers the extra-claim capacity offer in OZ Shop when available.",
                                                enableExtraClaimShopOffer, "true", AdminSettingsType.BOOLEAN),
                                entry("extraClaimBasePrice", "Extra claim base price",
                                                "Price for the first purchased extra claim capacity.",
                                                extraClaimBasePrice, "200", AdminSettingsType.INTEGER),
                                entry("extraClaimPriceIncreasePercent", "Extra claim price increase",
                                                "Linear percent increase per already purchased extra claim capacity.",
                                                extraClaimPriceIncreasePercent, "10", AdminSettingsType.INTEGER),
                                entry("extraClaimShopCurrencyIdentifier", "Extra claim currency",
                                                "Currency identifier for extra-claim Shop purchases. Empty uses Wallet default.",
                                                extraClaimShopCurrencyIdentifier, "", AdminSettingsType.STRING));
        }

        private AdminSettingsEntry entry(String key, String label, String description, Object value, String defaultValue,
                        AdminSettingsType type) {
                return new AdminSettingsEntry(
                                key,
                                label,
                                description,
                                String.valueOf(value),
                                defaultValue,
                                type,
                                false,
                                newValue -> SettingsFileEditor.writeValue(settingsPath(), key, newValue));
        }

        private Path settingsPath() {
                return Paths.get((plugin.getPath() != null ? plugin.getPath() : ".") + "/settings.properties");
        }
}
