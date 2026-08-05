package de.omegazirkel.risingworld.landclaim;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.List;

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
        public ClaimMode claimMode = ClaimMode.TIME_BASED;
        public Integer minutesToClaim = 10;
        public double claimTimeScaleFactor = 1.01;
        public Integer basicClaimLimit = 5;
        public double playTimeHoursExtraClaimFactor = 0.6;
        public Integer claimProtectionBaseTimeDays = 7;
        public double claimProtectionExtraTimeScale = 5;
        public Boolean enableWelcomeMessage = false;
        public Integer recentlyOnlinePermissionListHours = 24;

        public Integer claimBaseCost = 100;
        public long landPriceBase = 1000;
        public double landPriceClusterIncrement = 0.05d;
        public Integer cityBaseRadius = 2;
        public Boolean cityAllowPrivateClaims = false;
        public long cityPrivateClaimPrice = 10000;
        public long cityExpansionBasePrice = 50;
        public Integer cityRentBillingHour = 0;
        public double claimSaleFee = 0.01;
        public Boolean adminIgnoreLimit = true;
        public Boolean adminIgnoreTime = true;
        public Boolean allowAdminOverride = false;
        public Boolean showSpecialAreaFrames = true;
        public Boolean showStaticAreaFrames = true;
        public Boolean showPvPAreaFrames = true;
        public Boolean showRestAreaFrames = true;
        public Boolean showTrapAreaFrames = true;
        public Boolean showRenewAreaFrames = false;
        public Boolean enableAutoClaimRemoval = false;
        public Integer autoClaimRemovalInactiveDays = 90;
        public Integer autoClaimRemovalDelaySeconds = 60;
        public Integer renewZoneDefaultIntervalHours = 24;
        public String renewZoneResetAnnouncementTarget = "none";
        public Integer renewZoneResetBaseDelaySeconds = 2;
        public Integer renewZoneResetDelayPerChunkMillis = 25;
        public Integer renewZoneResetMaxDelaySeconds = 60;
        public long discordRenewZoneLogChannelId = 0;
        public Boolean allowClaimSale = false;
        public Boolean allowClaimBuyExceedLimit = false;
        public Boolean exposeClaimSales = true;
        public Boolean exposeRenewZones = true;
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
        // Ingame event announcements
        public Boolean enableIngameClaimAnnouncement = true;
        public Boolean enableIngameExpandAnnouncement = true;
        public Boolean enableIngameReleaseAccouncement = true;
        public Boolean enableIngameForSaleAccouncement = true;
        public Boolean enableIngameBuyAccouncement = true;
        // permissions to use
        public String specialRestAreaPermission = "ozlc-special-rest";
        public String specialPvPAreaPermission = "ozlc-special-pvp";
        public String specialStaticAreaPermission = "ozlc-special-static";
        public String specialTrapAreaPermission = "ozlc-special-trap";
        public String specialRenewAreaPermission = "ozlc-special-renew";
        public String specialCityCorePermission = "ozlc-special-city-core";
        public String specialCityLeaseholdPermission = "ozlc-special-city-leasehold";
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
        // color="#81D4FA10"
        public ColorRGBA staticAreaBorderColor = new ColorRGBA(0x81D4FA10);
        // color="#FF000015"
        public ColorRGBA pvpAreaBorderColor = new ColorRGBA(0xFF000010);
        // color="#6fff8215"
        public ColorRGBA restAreaBorderColor = new ColorRGBA(0x6fff829c);
        // color="#ff910015"
        public ColorRGBA trapAreaBorderColor = new ColorRGBA(0xff91009c);
        // color="#00C2A815"
        public ColorRGBA renewAreaBorderColor = new ColorRGBA(0x00C2A89c);
        // color="#7B61FF15"
        public ColorRGBA cityCoreBorderColor = new ColorRGBA(0x7B61FF10);
        // color="#00BFA515"
        public ColorRGBA cityLeaseholdAvailableBorderColor = new ColorRGBA(0x00BFA510);
        // color="#FF980015"
        public ColorRGBA cityLeaseholdOccupiedBorderColor = new ColorRGBA(0xFF980010);
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
        // color="#81D4FA50"
        public ColorRGBA staticAreaFrameColor = new ColorRGBA(0x81D4FA50);
        // color="#FF000050"
        public ColorRGBA pvpAreaFrameColor = new ColorRGBA(0xFF000050);
        // color="#6fff82AA"
        public ColorRGBA restAreaFrameColor = new ColorRGBA(0x6fff82AA);
        // color="#ff9100AA"
        public ColorRGBA trapAreaFrameColor = new ColorRGBA(0xff9100AA);
        // color="#00C2A8AA"
        public ColorRGBA renewAreaFrameColor = new ColorRGBA(0x00C2A8AA);
        // color="#7B61FF50"
        public ColorRGBA cityCoreFrameColor = new ColorRGBA(0x7B61FF50);
        // color="#00BFA550"
        public ColorRGBA cityLeaseholdAvailableFrameColor = new ColorRGBA(0x00BFA550);
        // color="#FF980050"
        public ColorRGBA cityLeaseholdOccupiedFrameColor = new ColorRGBA(0xFF980050);

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
                        claimMode = ClaimMode.parse(settings.getProperty("claimMode", "TIME_BASED"));
                        enableWelcomeMessage = settings.getProperty("enableWelcomeMessage", "false")
                                        .contentEquals("true");
                        recentlyOnlinePermissionListHours = Integer
                                        .parseInt(settings.getProperty("recentlyOnlinePermissionListHours", "24"));
                        // admin only settings
                        adminIgnoreLimit = settings.getProperty("adminIgnoreLimit", "false").contentEquals("true");
                        adminIgnoreTime = settings.getProperty("adminIgnoreTime", "false").contentEquals("true");
                        allowAdminOverride = settings.getProperty("allowAdminOverride", "false").contentEquals("true");
                        showSpecialAreaFrames = settings.getProperty("showSpecialAreaFrames", "true").contentEquals("true");
                        showStaticAreaFrames = settings.getProperty("showStaticAreaFrames", "true").contentEquals("true");
                        showPvPAreaFrames = settings.getProperty("showPvPAreaFrames", "true").contentEquals("true");
                        showRestAreaFrames = settings.getProperty("showRestAreaFrames", "true").contentEquals("true");
                        showTrapAreaFrames = settings.getProperty("showTrapAreaFrames", "true").contentEquals("true");
                        showRenewAreaFrames = settings.getProperty("showRenewAreaFrames", "false").contentEquals("true");
                        enableAutoClaimRemoval = settings.getProperty("enableAutoClaimRemoval", "false")
                                        .contentEquals("true");
                        autoClaimRemovalInactiveDays = Integer
                                        .parseInt(settings.getProperty("autoClaimRemovalInactiveDays", "90"));
                        autoClaimRemovalDelaySeconds = Integer
                                        .parseInt(settings.getProperty("autoClaimRemovalDelaySeconds", "60"));
                        renewZoneDefaultIntervalHours = Integer
                                        .parseInt(settings.getProperty("renewZoneDefaultIntervalHours", "24"));
                        renewZoneResetAnnouncementTarget = settings
                                        .getProperty("renewZoneResetAnnouncementTarget", "none");
                        renewZoneResetBaseDelaySeconds = Integer
                                        .parseInt(settings.getProperty("renewZoneResetBaseDelaySeconds", "2"));
                        renewZoneResetDelayPerChunkMillis = Integer
                                        .parseInt(settings.getProperty("renewZoneResetDelayPerChunkMillis", "25"));
                        renewZoneResetMaxDelaySeconds = Integer
                                        .parseInt(settings.getProperty("renewZoneResetMaxDelaySeconds", "60"));
                        // trade settings
                        allowClaimSale = settings.getProperty("allowClaimSale", "false").contentEquals("true");
                        allowClaimBuyExceedLimit = settings.getProperty("allowClaimBuyExceedLimit", "false")
                                        .contentEquals("true");
                        exposeClaimSales = settings.getProperty("exposeClaimSales", "true").contentEquals("true");
                        exposeRenewZones = settings.getProperty("exposeRenewZones", "true").contentEquals("true");
                        enableExtraClaimShopOffer = settings.getProperty("enableExtraClaimShopOffer", "true")
                                        .contentEquals("true");
                        extraClaimBasePrice = Integer.parseInt(settings.getProperty("extraClaimBasePrice", "200"));
                        extraClaimPriceIncreasePercent = Integer
                                        .parseInt(settings.getProperty("extraClaimPriceIncreasePercent", "10"));
                        extraClaimShopCurrencyIdentifier = settings.getProperty("extraClaimShopCurrencyIdentifier", "");
                        // claim settings
                        minutesToClaim = Integer.parseInt(settings.getProperty("minutesToClaim", "10"));
                        claimTimeScaleFactor = Double.parseDouble(settings.getProperty("claimTimeScaleFactor", "1.01"));
                        landPriceBase = Long.parseLong(settings.getProperty("landPriceBase", "1000"));
                        landPriceClusterIncrement = Double
                                        .parseDouble(settings.getProperty("landPriceClusterIncrement", "0.05"));
                        cityBaseRadius = Integer.parseInt(settings.getProperty("cityBaseRadius", "2"));
                        cityAllowPrivateClaims = Boolean.parseBoolean(settings.getProperty("cityAllowPrivateClaims", "false"));
                        cityPrivateClaimPrice = Long.parseLong(settings.getProperty("cityPrivateClaimPrice", "10000"));
                        cityExpansionBasePrice = Long.parseLong(settings.getProperty("cityExpansionBasePrice", "50"));
                        cityRentBillingHour = Integer.parseInt(settings.getProperty("cityRentBillingHour", "0"));
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
                        discordRenewZoneLogChannelId = Long
                                        .parseLong(settings.getProperty("discordRenewZoneLogChannelId", "0"));
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
                        specialStaticAreaPermission = settings.getProperty("specialStaticAreaPermission",
                                        "ozlc-special-static");
                        specialPvPAreaPermission = settings.getProperty("specialPvPAreaPermission", "ozlc-special-pvp");
                        specialTrapAreaPermission = settings.getProperty("specialTrapAreaPermission",
                                        "ozlc-special-trap");
                        specialRenewAreaPermission = settings.getProperty("specialRenewAreaPermission",
                                        "ozlc-special-renew");
                        specialCityCorePermission = settings.getProperty("specialCityCorePermission",
                                        "ozlc-special-city-core");
                        specialCityLeaseholdPermission = settings.getProperty("specialCityLeaseholdPermission",
                                        "ozlc-special-city-leasehold");
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
                        renewAreaBorderColor = new ColorRGBA((int) Long.parseUnsignedLong(
                                        settings.getProperty("renewAreaBorderColor", "0x00C2A89c").replace("0x", ""),
                                        16));
                        cityCoreBorderColor = new ColorRGBA((int) Long.parseUnsignedLong(
                                        settings.getProperty("cityCoreBorderColor", "0x7B61FF10").replace("0x", ""), 16));
                        cityLeaseholdAvailableBorderColor = new ColorRGBA((int) Long.parseUnsignedLong(settings
                                        .getProperty("cityLeaseholdAvailableBorderColor", "0x00BFA510").replace("0x", ""), 16));
                        cityLeaseholdOccupiedBorderColor = new ColorRGBA((int) Long.parseUnsignedLong(settings
                                        .getProperty("cityLeaseholdOccupiedBorderColor", "0xFF980010").replace("0x", ""), 16));

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
                        renewAreaFrameColor = new ColorRGBA((int) Long.parseUnsignedLong(
                                        settings.getProperty("renewAreaFrameColor", "0x00C2A8AA").replace("0x", ""),
                                        16));
                        cityCoreFrameColor = new ColorRGBA((int) Long.parseUnsignedLong(
                                        settings.getProperty("cityCoreFrameColor", "0x7B61FF50").replace("0x", ""), 16));
                        cityLeaseholdAvailableFrameColor = new ColorRGBA((int) Long.parseUnsignedLong(settings
                                        .getProperty("cityLeaseholdAvailableFrameColor", "0x00BFA550").replace("0x", ""), 16));
                        cityLeaseholdOccupiedFrameColor = new ColorRGBA((int) Long.parseUnsignedLong(settings
                                        .getProperty("cityLeaseholdOccupiedFrameColor", "0xFF980050").replace("0x", ""), 16));

                        logger().info((plugin == null ? "OZLandClaim" : plugin.getName()) + " Plugin settings loaded");

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
                                AdminSettingsEntry.group("general", "General",
                                                "Logging, reload, welcome, and permission list behavior."),
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
                                AdminSettingsEntry.group("adminOverrides", "Admin overrides",
                                                "Admin bypass behavior for claim limits and claim time."),
                                entry("adminIgnoreLimit", "Admin ignores claim limit",
                                                "Lets admins bypass claim limits.", adminIgnoreLimit, "true",
                                                AdminSettingsType.BOOLEAN),
                                entry("adminIgnoreTime", "Admin ignores claim time",
                                                "Lets admins bypass claim wait time.", adminIgnoreTime, "true",
                                                AdminSettingsType.BOOLEAN),
                                entry("allowAdminOverride", "Allow admin override",
                                                "Lets admins see hidden special-zone frames.", allowAdminOverride, "false",
                                                AdminSettingsType.BOOLEAN),
                                AdminSettingsEntry.group("specialZoneVisibility", "Special-zone visibility",
                                                "Controls which special zones players see when showing other areas."),
                                entry("showSpecialAreaFrames", "Show special areas",
                                                "Shows neutral special-zone frames to players.", showSpecialAreaFrames, "true",
                                                AdminSettingsType.BOOLEAN),
                                entry("showStaticAreaFrames", "Show static areas",
                                                "Shows static special-zone frames to players.", showStaticAreaFrames, "true",
                                                AdminSettingsType.BOOLEAN),
                                entry("showPvPAreaFrames", "Show PvP areas",
                                                "Shows PvP special-zone frames to players.", showPvPAreaFrames, "true",
                                                AdminSettingsType.BOOLEAN),
                                entry("showRestAreaFrames", "Show rest areas",
                                                "Shows rest special-zone frames to players.", showRestAreaFrames, "true",
                                                AdminSettingsType.BOOLEAN),
                                entry("showTrapAreaFrames", "Show trap areas",
                                                "Shows trap special-zone frames to players.", showTrapAreaFrames, "true",
                                                AdminSettingsType.BOOLEAN),
                                entry("showRenewAreaFrames", "Show renew zones",
                                                "Shows renew-zone frames to players.", showRenewAreaFrames, "false",
                                                AdminSettingsType.BOOLEAN),
                                AdminSettingsEntry.group("claimRules", "Claim rules",
                                                "Global acquisition mode and base claim-limit behavior."),
                                selectEntry("claimMode", "Claim mode",
                                                "Global acquisition and ownership rules for all claim areas.",
                                                claimMode.name(), "TIME_BASED",
                                                List.of("TIME_BASED", "ADMINISTRATIVE", "LAND_PRICING", "CITY")),
                                entry("basicClaimLimit", "Basic claim limit",
                                                "Base number of claims a player can own.", basicClaimLimit, "1",
                                                AdminSettingsType.INTEGER),
                                AdminSettingsEntry.group("timeModeRules", "Time-mode rules",
                                                "Rules used by the TIME_BASED acquisition mode."),
                                entry("minutesToClaim", "Minutes to claim",
                                                "Minimum minutes a player must stay in a chunk before claiming.",
                                                minutesToClaim, "10", AdminSettingsType.INTEGER),
                                entry("claimTimeScaleFactor", "Claim time scale",
                                                "Decimal scale factor for increasing or decreasing claim wait time.",
                                                claimTimeScaleFactor, "1.01", AdminSettingsType.DECIMAL),
                                entry("playTimeHoursExtraClaimFactor", "Playtime extra-claim factor",
                                                "Decimal factor used to grant additional claims from playtime.",
                                                playTimeHoursExtraClaimFactor, "0.6", AdminSettingsType.DECIMAL),
                                AdminSettingsEntry.group("autoRemoval", "Auto removal",
                                                "Inactive-owner automatic claim removal behavior."),
                                entry("enableAutoClaimRemoval", "Auto claim removal",
                                                "Removes claims from owners inactive longer than the configured days.",
                                                enableAutoClaimRemoval, "false", AdminSettingsType.BOOLEAN),
                                entry("autoClaimRemovalInactiveDays", "Inactive days",
                                                "Inactive owner age before auto claim removal.", autoClaimRemovalInactiveDays,
                                                "90", AdminSettingsType.INTEGER),
                                entry("autoClaimRemovalDelaySeconds", "Removal delay",
                                                "Delay in seconds before the automatic removal check runs after startup.",
                                                autoClaimRemovalDelaySeconds, "60", AdminSettingsType.INTEGER),
                                AdminSettingsEntry.group("renewZones", "Renew zones",
                                                "Default interval for newly created renew zones."),
                                entry("renewZoneDefaultIntervalHours", "Default renew interval",
                                                "Default interval in hours for newly created renew zones.",
                                                renewZoneDefaultIntervalHours, "24", AdminSettingsType.INTEGER),
                                entry("renewZoneResetAnnouncementTarget", "Reset announcements",
                                        "Who receives renew-zone reset announcements: none, all, or admins.",
                                        renewZoneResetAnnouncementTarget, "none", AdminSettingsType.STRING),
                                entry("renewZoneResetBaseDelaySeconds", "Base reset delay",
                                                "Minimum delay in seconds before the next due renew zone is reset.",
                                                renewZoneResetBaseDelaySeconds, "2", AdminSettingsType.INTEGER),
                                entry("renewZoneResetDelayPerChunkMillis", "Reset delay per chunk",
                                                "Additional delay in milliseconds per reset chunk column before the next zone.",
                                                renewZoneResetDelayPerChunkMillis, "25", AdminSettingsType.INTEGER),
                                entry("renewZoneResetMaxDelaySeconds", "Maximum reset delay",
                                                "Maximum delay in seconds before the next due renew zone is reset.",
                                                renewZoneResetMaxDelaySeconds, "60", AdminSettingsType.INTEGER),
                                entry("discordRenewZoneLogChannelId", "Discord renew-zone log",
                                                "Discord channel id for renew-zone reset logs. 0 disables logging.",
                                                discordRenewZoneLogChannelId, "0", AdminSettingsType.INTEGER),
                                AdminSettingsEntry.group("landPricingRules", "Land-pricing rules",
                                                "Rules used by the LAND_PRICING acquisition mode."),
                                entry("landPriceBase", "Land base price",
                                                "Base price for one free chunk in LAND_PRICING mode.",
                                                landPriceBase, "1000", AdminSettingsType.INTEGER),
                                entry("landPriceClusterIncrement", "Cluster increment",
                                                "Additive price increment per occupied chunk in an adjacent cluster.",
                                                landPriceClusterIncrement, "0.05", AdminSettingsType.DECIMAL),
                                AdminSettingsEntry.group("cityModeRules", "City-mode rules",
                                                "Rules used by the CITY acquisition mode."),
                                entry("cityBaseRadius", "City base radius",
                                                "Initial three-dimensional city radius in chunks.",
                                                cityBaseRadius, "2", AdminSettingsType.INTEGER),
                                entry("cityAllowPrivateClaims", "Private city claims",
                                                "Allows private player claims inside city bounds by default.",
                                                cityAllowPrivateClaims, "false", AdminSettingsType.BOOLEAN),
                                entry("cityPrivateClaimPrice", "Private claim price",
                                                "Default price per private city claim chunk.",
                                                cityPrivateClaimPrice, "10000", AdminSettingsType.INTEGER),
                                entry("cityExpansionBasePrice", "City expansion base price",
                                                "Price per newly covered chunk when a city radius grows.",
                                                cityExpansionBasePrice, "50", AdminSettingsType.INTEGER),
                                entry("cityRentBillingHour", "Rent billing hour",
                                                "Server-local hour (0-23) for daily city rent billing.",
                                                cityRentBillingHour, "0", AdminSettingsType.INTEGER),
                                AdminSettingsEntry.group("claimSales", "Claim sales",
                                                "Wallet-backed claim sale and purchase behavior."),
                                entry("allowClaimSale", "Allow claim sale",
                                                "Allows players to list owned claims for Wallet-backed sale.",
                                                allowClaimSale, "false", AdminSettingsType.BOOLEAN),
                                entry("allowClaimBuyExceedLimit", "Buy above limit",
                                                "Allows claim purchases to exceed the buyer's normal claim limit.",
                                                allowClaimBuyExceedLimit, "false", AdminSettingsType.BOOLEAN),
                                entry("claimSaleFee", "Claim sale fee",
                                                "Decimal Wallet fee fraction charged for a claim sale.",
                                                claimSaleFee, "0.01", AdminSettingsType.DECIMAL),
                                AdminSettingsEntry.group("exportRoutes", "Export routes",
                                                "Route-ready read exposure for manager bridges."),
                                entry("exposeClaimSales", "Expose claim sales",
                                                "Allows bridge/native route layers to expose active claim-sale listings.",
                                                exposeClaimSales, "true", AdminSettingsType.BOOLEAN),
                                entry("exposeRenewZones", "Expose renew zones",
                                                "Allows bridge/native route layers to expose renew-zone metadata.",
                                                exposeRenewZones, "true", AdminSettingsType.BOOLEAN),
                                AdminSettingsEntry.group("extraClaims", "Extra claim shop",
                                                "Shop offer for purchasing additional claim capacity."),
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
                                                extraClaimShopCurrencyIdentifier, "", AdminSettingsType.STRING),
                                AdminSettingsEntry.group("unusedRules", "Currently unused rules",
                                                "Reserved settings without current gameplay effect."),
                                entry("claimProtectionBaseTimeDays", "Claim protection days",
                                                "Reserved base protection time; currently not used.",
                                                claimProtectionBaseTimeDays, "7", AdminSettingsType.INTEGER),
                                entry("claimProtectionExtraTimeScale", "Claim protection scale",
                                                "Reserved additional protection scale; currently not used.",
                                                claimProtectionExtraTimeScale, "5", AdminSettingsType.DECIMAL));
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

        private AdminSettingsEntry readOnlyEntry(String key, String label, String description, Object value,
                        String defaultValue, AdminSettingsType type) {
                return new AdminSettingsEntry(
                                key,
                                label,
                                description,
                                String.valueOf(value),
                                defaultValue,
                                type,
                                false,
                                null);
        }

        private AdminSettingsEntry selectEntry(String key, String label, String description, Object value,
                        String defaultValue, List<String> options) {
                return new AdminSettingsEntry(
                                key,
                                label,
                                description,
                                String.valueOf(value),
                                defaultValue,
                                AdminSettingsType.SELECT,
                                false,
                                newValue -> SettingsFileEditor.writeValue(settingsPath(), key, newValue),
                                options);
        }

        private Path settingsPath() {
                return Paths.get((plugin.getPath() != null ? plugin.getPath() : ".") + "/settings.properties");
        }
}
