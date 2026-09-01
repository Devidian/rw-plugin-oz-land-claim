package de.omegazirkel.risingworld;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import de.omegazirkel.risingworld.landclaim.Area3DUtils;
import de.omegazirkel.risingworld.landclaim.ClaimCleanupService;
import de.omegazirkel.risingworld.landclaim.ChunkClaimUtil;
import de.omegazirkel.risingworld.landclaim.DiscordConnect;
import de.omegazirkel.risingworld.landclaim.EconomyIntegration;
import de.omegazirkel.risingworld.landclaim.LandClaimGUI;
import de.omegazirkel.risingworld.landclaim.LandClaimPluginInfoStatusProvider;
import de.omegazirkel.risingworld.landclaim.PermissionFileUtil;
import de.omegazirkel.risingworld.landclaim.PluginSettings;
import de.omegazirkel.risingworld.landclaim.RenewZoneResetService;
import de.omegazirkel.risingworld.landclaim.CityRentService;
import de.omegazirkel.risingworld.landclaim.db.ClaimSaleListingService;
import de.omegazirkel.risingworld.landclaim.db.ExtraClaimCapacityService;
import de.omegazirkel.risingworld.landclaim.db.LandClaimChunkService;
import de.omegazirkel.risingworld.landclaim.db.LandClaimChunkStore;
import de.omegazirkel.risingworld.landclaim.db.RenewZoneConfigService;
import de.omegazirkel.risingworld.landclaim.db.LandPriceService;
import de.omegazirkel.risingworld.landclaim.db.CityService;
import de.omegazirkel.risingworld.landclaim.ui.ClaimSaleIndicatorProvider;
import de.omegazirkel.risingworld.landclaim.ui.ChunkInfoManager;
import de.omegazirkel.risingworld.landclaim.ui.LandClaimPlayerPluginData;
import de.omegazirkel.risingworld.landclaim.ui.LandClaimPlayerPluginSettings;
import de.omegazirkel.risingworld.landclaim.ui.UIDialogFactory;
import net.risingworld.api.ui.UIElement;
import net.risingworld.api.ui.UITarget;
import de.omegazirkel.risingworld.tools.Colors;
import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.OZLogger;
import de.omegazirkel.risingworld.tools.PlayerSettings;
import de.omegazirkel.risingworld.tools.db.SQLiteConnectionFactory;
import de.omegazirkel.risingworld.tools.settings.PlayerPluginAdminSettings;
import de.omegazirkel.risingworld.tools.ui.AssetManager;
import de.omegazirkel.risingworld.tools.ui.MenuItem;
import de.omegazirkel.risingworld.tools.ui.PlayerPluginSettingsOverlay;
import de.omegazirkel.risingworld.tools.ui.PluginInfoStatusProviders;
import de.omegazirkel.risingworld.tools.ui.PluginMenuManager;
import de.omegazirkel.risingworld.tools.ui.PluginShortcutVisibility;
import de.omegazirkel.risingworld.tools.ui.SharedIndicators;
import net.risingworld.api.Timer;
import net.risingworld.api.Plugin;
import net.risingworld.api.Server;
import net.risingworld.api.events.player.PlayerCommandEvent;
import net.risingworld.api.events.player.PlayerConnectEvent;
import net.risingworld.api.events.player.PlayerDeathEvent;
import net.risingworld.api.events.player.PlayerDisconnectEvent;
import net.risingworld.api.events.player.PlayerEnterChunkEvent;
import net.risingworld.api.events.player.PlayerSpawnEvent;
import net.risingworld.api.events.player.ui.PlayerToggleInventoryEvent;
import net.risingworld.api.objects.Area;
import net.risingworld.api.objects.Player;
import net.risingworld.api.utils.Vector3i;
import net.risingworld.api.worldelements.Area3D;

class LandClaimRuntime extends Plugin {
    static final String pluginCMD = "lc";
    private static LandClaim instance;
    private ChunkInfoManager chunkInfoManager;

    static final Colors c = Colors.getInstance();
    private static I18n t = null;
    private static PluginSettings s;
    private static LandClaimGUI gui;
    private static ChunkClaimUtil chunkClaimUtil;
    private static ClaimCleanupService cleanupService;
    private static EconomyIntegration economyIntegration;
    private static ExtraClaimCapacityService extraClaimCapacityService;
    private static ClaimSaleListingService claimSaleListingService;
    private static RenewZoneConfigService renewZoneConfigService;
    private static RenewZoneResetService renewZoneResetService;
    private static LandPriceService landPriceService;
    private static CityService cityService;
    private Timer renewZoneTimer;
    private Timer cityRentTimer;
    private CityRentService cityRentService;
    private LocalDate nextCityRentBillingDate;
    private long nextRenewZoneCheckMs;
    public static String name;
    // only for workaround with area bugs
    // public static WorldDatabase wdbAreas;
    // public static WorldDatabase wdbPlayers;
    public static Connection sqliteCon;
    public static PlayerSettings ps;

    public static LandClaimChunkService llcs;
    public static LandClaimChunkStore lccStore;

    public static OZLogger logger() {
        return OZLogger.getInstance("OZ.LandClaim");
    }

    public static OZLogger eventLogger() {
        return logger();
    }

    public static LandClaim getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = (LandClaim) this;
        name = this.getDescription("name");
        t = I18n.getInstance(this);
        s = PluginSettings.getInstance(this);
        sqliteCon = SQLiteConnectionFactory.open(this);
        ps = new PlayerSettings(sqliteCon);
        try {
            lccStore = new LandClaimChunkStore(sqliteCon);
            extraClaimCapacityService = new ExtraClaimCapacityService(sqliteCon);
            claimSaleListingService = new ClaimSaleListingService(sqliteCon);
            renewZoneConfigService = new RenewZoneConfigService(sqliteCon);
            renewZoneResetService = new RenewZoneResetService(renewZoneConfigService, s);
            landPriceService = new LandPriceService(sqliteCon);
            cityService = new CityService(sqliteCon);
        } catch (Exception e) {
            logger().error(e.getMessage());
            e.printStackTrace();
            return; // we cant proceed without sqlite here
        }
        llcs = new LandClaimChunkService(lccStore);

        // wdbAreas = this.getWorldDatabase(Target.Areas);
        // wdbPlayers = this.getWorldDatabase(Target.Players);
        chunkClaimUtil = new ChunkClaimUtil(llcs);
        s.initSettings();
        cleanupService = new ClaimCleanupService(llcs, s);
        gui = LandClaimGUI.getInstance(chunkClaimUtil, cleanupService, this);
        ensureDefaultPermissionFiles();

        // Load Plugin Menu into Main Plugin Menu
        PluginMenuManager
                .registerPluginMenu(new MenuItem(name, "oz-land-claim", "Land Claim", (Player p) -> {
                    gui.openMainMenu(p);
                }));
        PluginShortcutVisibility.register(name, LandClaimPlayerPluginSettings::shortcutVisible);
        // connect plugins
        DiscordConnect.init(this);
        economyIntegration = new EconomyIntegration(this);
        economyIntegration.logStatus();
        economyIntegration.registerExtraClaimOffer(s);
        landPriceService.refresh();
        cityRentService = new CityRentService(cityService, economyIntegration, s);
        scheduleCityRentBilling();
        int unresolvedEconomyOperations = cityService.countUnresolvedEconomyOperations();
        if (unresolvedEconomyOperations > 0) logger().warn("LandClaim has " + unresolvedEconomyOperations
                + " unresolved economy operations requiring reconciliation.");

        // init Chunk Info overlay timer
        chunkInfoManager = new ChunkInfoManager(chunkClaimUtil);
        chunkInfoManager.start();

        // register plugin settings
        PlayerPluginSettingsOverlay.registerPlayerPluginSettings(new LandClaimPlayerPluginSettings(getDescription("version")));
        PlayerPluginSettingsOverlay.registerPlayerPluginData(new LandClaimPlayerPluginData(getDescription("version")));
        PlayerPluginSettingsOverlay.registerPlayerPluginAdminSettings(
                new PlayerPluginAdminSettings(name, getDescription("version"), () -> s.adminSettingsEntries(),
                        s::initSettings));
        PluginInfoStatusProviders
                .registerProvider(new LandClaimPluginInfoStatusProvider((LandClaim) this, getDescription("version")));
        SharedIndicators.registerProvider(name, new ClaimSaleIndicatorProvider());

        scheduleAutoClaimRemoval();

        logger().info("✅ " + this.getName() + " Plugin is enabled version:" + this.getDescription("version"));
    }

    @Override
    public void onDisable() {
        logger().warn("⚠️ Disabling " + this.getName() + " ...");
        if (chunkInfoManager != null)
            chunkInfoManager.stop();
        if (renewZoneTimer != null) {
            renewZoneTimer.kill();
            renewZoneTimer = null;
        }
        if (cityRentTimer != null) {
            cityRentTimer.kill();
            cityRentTimer = null;
        }
        if (name != null) {
            PluginShortcutVisibility.unregister(name);
            PluginInfoStatusProviders.unregisterProvider(name);
            SharedIndicators.unregisterProvider(name);
        }
        if (lccStore != null) {
            lccStore.shutdown();
        }
        if (sqliteCon != null) {
            try {
                sqliteCon.close();
            } catch (SQLException ex) {
                logger().warn("Failed to close Land Claim database connection: " + ex.getMessage());
            }
        }

        logger().warn("❌ " + this.getName() + " disabled.");
    }

    public void onSettingsChanged(Path settingsPath) {
        s.initSettings(settingsPath.toString());
        if (economyIntegration != null) {
            economyIntegration.logStatus();
            economyIntegration.registerExtraClaimOffer(s);
        }
        scheduleCityRentBilling();
        if (landPriceService != null) landPriceService.refresh();
        Area3DUtils.updateAreaFramesForAllPlayers();
    }

    public static ExtraClaimCapacityService extraClaimCapacityService() {
        return extraClaimCapacityService;
    }

    public static ClaimSaleListingService claimSaleListingService() {
        return claimSaleListingService;
    }

    public static RenewZoneConfigService renewZoneConfigService() {
        return renewZoneConfigService;
    }

    public static RenewZoneResetService renewZoneResetService() {
        return renewZoneResetService;
    }

    public static EconomyIntegration economyIntegration() {
        return economyIntegration;
    }

    public static LandPriceService landPriceService() {
        return landPriceService;
    }

    public static CityService cityService() {
        return cityService;
    }

    public int playerClaimCount(Player player) {
        return chunkClaimUtil == null ? 0 : chunkClaimUtil.getPlayerClaimCount(player);
    }

    public long playerMaxClaims(Player player) {
        return chunkClaimUtil == null ? 0 : chunkClaimUtil.getPlayerMaxClaims(player);
    }

    private void scheduleAutoClaimRemoval() {
        if (!s.enableAutoClaimRemoval) {
            return;
        }
        float delaySeconds = Math.max(0, s.autoClaimRemovalDelaySeconds);
        executeDelayed(delaySeconds, () -> {
            ClaimCleanupService.AutoRemovalResult result = cleanupService
                    .removeInactiveOwners(Math.max(0, s.autoClaimRemovalInactiveDays));
            if (result.ownersRemoved() > 0) {
                Area3DUtils.updateAreaFramesForAllPlayers();
            }
            logger().info("Auto claim removal checked inactive owners. Owners removed: "
                    + result.ownersRemoved() + ", claims removed: " + result.claimsRemoved());
            String message = t.get("tc.discord.auto.claim.removal", DiscordConnect.botLang())
                    .replace("PH_OWNER_COUNT", String.valueOf(result.ownersRemoved()))
                    .replace("PH_CLAIM_COUNT", String.valueOf(result.claimsRemoved()))
                    .replace("PH_DAYS", String.valueOf(result.inactiveDays()));
            DiscordConnect.sendDiscordReleaseAccouncement(message);
        });
    }

    private void scheduleRenewZoneReset() {
        if (renewZoneTimer != null) {
            renewZoneTimer.kill();
        }
        float delaySeconds = secondsUntilNextFullHour();
        nextRenewZoneCheckMs = System.currentTimeMillis() + Math.round(delaySeconds * 1000f);
        renewZoneTimer = new Timer(1f, 1f, -1, () -> {
            if (renewZoneResetService == null) {
                return;
            }
            long nowMs = System.currentTimeMillis();
            if (nowMs < nextRenewZoneCheckMs) {
                return;
            }
            RenewZoneResetService.RenewZoneResetResult result = renewZoneResetService
                    .resetNextDueZone(nowMs);
            if (result.zonesChecked() > 0) {
                logger().info("Renew zone reset check completed. Checked: " + result.zonesChecked()
                        + ", reset: " + result.zonesReset()
                        + ", chunk columns reset: " + result.chunksReset()
                        + ", stale configs removed: " + result.staleConfigsRemoved());
                nextRenewZoneCheckMs = nowMs + renewZoneResetDelayMs(result.chunksReset());
            } else {
                nextRenewZoneCheckMs = nextFullHourMs(nowMs);
            }
        });
        renewZoneTimer.start();
        logger().info("Renew zone hourly check scheduled in " + Math.round(delaySeconds) + " seconds.");
    }

    private void ensureRenewZoneResetScheduled() {
        if (renewZoneTimer != null) {
            return;
        }
        scheduleRenewZoneReset();
    }

    private float secondsUntilNextFullHour() {
        long nowMs = System.currentTimeMillis();
        long nextHourMs = nextFullHourMs(nowMs);
        return Math.max(1f, (nextHourMs - nowMs) / 1000f);
    }

    private long nextFullHourMs(long nowMs) {
        return ((nowMs / 3_600_000L) + 1L) * 3_600_000L;
    }

    private long renewZoneResetDelayMs(int chunksReset) {
        long baseDelayMs = Math.max(0, s.renewZoneResetBaseDelaySeconds) * 1000L;
        long chunkDelayMs = Math.max(0, s.renewZoneResetDelayPerChunkMillis) * (long) Math.max(0, chunksReset);
        long maxDelayMs = Math.max(baseDelayMs, Math.max(0, s.renewZoneResetMaxDelaySeconds) * 1000L);
        return Math.min(maxDelayMs, baseDelayMs + chunkDelayMs);
    }

    public void onPlayerCommand(PlayerCommandEvent event) {
        Player player = event.getPlayer();
        String lang = de.omegazirkel.risingworld.OZTools.getPlayerLanguage(player);
        String commandLine = event.getCommand();
        // Vector3f pos = player.getPosition();

        String[] cmdParts = commandLine.split(" ", 2);
        String command = cmdParts[0].toLowerCase();

        if (command.equals("/" + pluginCMD)) {
            // no arguments = fallback and open menu
            if (cmdParts.length < 2) {
                gui.openMainMenu(player);
                return;
            }
            String option = cmdParts[1].toLowerCase();
            switch (option) {
                case "info":
                case "status":
                    PluginInfoStatusProviders.show(player, name);
                    break;
                case "stats":
                    String statsMessage = t.get("tc.cmd.stats", player)
                            .replace("PH_PLAYER_CLAIMS", chunkClaimUtil.getPlayerClaimCount(player) + "")
                            .replace("PH_PLAYER_MAX_CLAIMS", chunkClaimUtil.getPlayerMaxClaims(player) + "")
                            .replace("PH_PLAYER_CLAIM_TIME", chunkClaimUtil.getPlayerNextClaimTime(player) + "");
                    player.sendTextMessage(c.okay + this.getName() + ":> " + c.endTag + statsMessage);
                    break;
                case "help":
                    String helpMessage = t.get("tc.cmd.help", player).replaceAll("PH_PLUGIN_CMD", pluginCMD);
                    player.sendTextMessage(c.okay + this.getName() + ":> " + c.endTag + helpMessage);
                    break;
                case "devmode":
                    if (player.isAdmin()) {
                        boolean currentValue = LandClaimPlayerPluginSettings.booleanValue(player,
                                LandClaimPlayerPluginSettings.DEVELOPER_MODE_KEY, false);
                        LandClaimPlayerPluginSettings.setBooleanValue(player,
                                LandClaimPlayerPluginSettings.DEVELOPER_MODE_KEY, !currentValue);
                    }
                    break;
                case "open":
                    gui.openMainMenu(player);
                    break;
                case "config":
                    gui.openCurrentAreaConfig(player, (Player p) -> gui.openMainMenu(p));
                    break;
                default:
                    player.sendTextMessage(t.get("tc.err.cmd.unknown").replace("PH_PLUGIN_CMD", pluginCMD));
                    break;
            }
        }
    }

    public void updatecurrentAreaFrameForPlayer(Player player) {
        updatecurrentAreaFrameForPlayer(player, player.getChunkPosition());
    }

    public void updatecurrentAreaFrameForPlayer(Player player, Vector3i chunkPosition) {
        Area area = ChunkClaimUtil.getVirtualAreaFromChunkVector(chunkPosition);
        Area3DUtils.updateCurrentChunkFrameForPlayer(player, area);
    }

    public void onPlayerEnterChunkEvent(PlayerEnterChunkEvent event) {
        Player player = event.getPlayer();
        Vector3i oldChunkPos = event.getOldChunkCoordinates();
        Vector3i chunkPos = event.getNewChunkCoordinates();

        logger().debug("Player " + player.getName() + " entered chunk " + chunkPos.toString()
                + " from chunk " + oldChunkPos.toString());

        chunkClaimUtil.leaveChunk(player, oldChunkPos);
        chunkClaimUtil.enterChunk(player, chunkPos);
        // Player#getCurrentArea() is updated after this event. Pass the event's
        // destination explicitly so entering a special zone refreshes its frame
        // immediately as well as leaving it.
        Area3DUtils.updateAreaFramesForPlayer(player, chunkPos);
        enqueue(() -> {
            if (chunkInfoManager != null) {
                chunkInfoManager.refresh(player);
            }
        });

        // if player has "oz.landclaim.showCurrentChunkFrame" enabled, show chunk area
        if (player.hasAttribute(LandClaimPlayerPluginSettings.SHOW_CURRENT_CHUNK_FRAME_KEY)
                && (Boolean) player.getAttribute(LandClaimPlayerPluginSettings.SHOW_CURRENT_CHUNK_FRAME_KEY)) {
            Area area = ChunkClaimUtil.getVirtualAreaFromChunkVector(chunkPos);
            Area3DUtils.updateCurrentChunkFrameForPlayer(player, area);
        }
    }

    public void onPlayerDisconnectEvent(PlayerDisconnectEvent event) {
        if (chunkInfoManager != null) {
            chunkInfoManager.onPlayerDisconnectEvent(event);
        }
        Player player = event.getPlayer();
        Vector3i chunkPos = player.getChunkPosition();
        chunkClaimUtil.leaveChunk(player, chunkPos);
        if (player.hasAttribute("oz.landclaim.areaFrames")) {

            @SuppressWarnings("unchecked")
            Map<Long, Area3D> frames = (Map<Long, Area3D>) player.getAttribute("oz.landclaim.areaFrames");
            if (frames != null) {
                for (Area3D a3d : frames.values()) {
                    player.removeGameObject(a3d);
                }
            }
        }

        eventLogger().debug("Player " + player.getName() + " disconnected from the server. Last chunk position: "
                + chunkPos.toString());
    }

    public void onPlayerConnectEvent(PlayerConnectEvent event) {
        if (chunkInfoManager != null) {
            chunkInfoManager.onPlayerConnectEvent(event);
        }
        Player player = event.getPlayer();
        if (cityService != null) {
            cityService.rememberPlayerLanguage(player.getDbID(),
                    de.omegazirkel.risingworld.OZTools.getPlayerLanguage(player));
        }
        showPendingCityNotifications(player);
        Vector3i chunkPos = player.getChunkPosition();
        eventLogger().debug("Player " + player.getName() + " connected to the server. Current chunk position: "
                + chunkPos.toString());
        ensureRenewZoneResetScheduled();
        Integer dbId = player.getDbID();
        // ensure values are set
        if (!player.hasAttribute(LandClaimPlayerPluginSettings.DEVELOPER_MODE_KEY))
            player.setAttribute(LandClaimPlayerPluginSettings.DEVELOPER_MODE_KEY,
                    ps.getBoolean(dbId, LandClaimPlayerPluginSettings.DEVELOPER_MODE_KEY).orElse(false));
        if (!player.hasAttribute(LandClaimPlayerPluginSettings.SHOW_CURRENT_CHUNK_FRAME_KEY))
            player.setAttribute(LandClaimPlayerPluginSettings.SHOW_CURRENT_CHUNK_FRAME_KEY,
                    ps.getBoolean(dbId, LandClaimPlayerPluginSettings.SHOW_CURRENT_CHUNK_FRAME_KEY).orElse(false));
        if (!player.hasAttribute(LandClaimPlayerPluginSettings.SHOW_OWNED_AREA_FRAMES_KEY))
            player.setAttribute(LandClaimPlayerPluginSettings.SHOW_OWNED_AREA_FRAMES_KEY,
                    ps.getBoolean(dbId, LandClaimPlayerPluginSettings.SHOW_OWNED_AREA_FRAMES_KEY).orElse(false));
        if (!player.hasAttribute(LandClaimPlayerPluginSettings.SHOW_OTHER_AREA_FRAMES_KEY))
            player.setAttribute(LandClaimPlayerPluginSettings.SHOW_OTHER_AREA_FRAMES_KEY,
                    ps.getBoolean(dbId, LandClaimPlayerPluginSettings.SHOW_OTHER_AREA_FRAMES_KEY).orElse(false));
        LandClaimPlayerPluginSettings.areaFrameChunkRadius(player);
        if (!player.hasAttribute(LandClaimPlayerPluginSettings.ENABLE_CLAIM_INFO_OVERLAY_KEY))
            player.setAttribute(LandClaimPlayerPluginSettings.ENABLE_CLAIM_INFO_OVERLAY_KEY,
                    ps.getBoolean(dbId, LandClaimPlayerPluginSettings.ENABLE_CLAIM_INFO_OVERLAY_KEY).orElse(true));
        if (!player.hasAttribute(LandClaimPlayerPluginSettings.ENABLE_TIME_MEASUREMENT_OVERLAY_KEY))
            player.setAttribute(LandClaimPlayerPluginSettings.ENABLE_TIME_MEASUREMENT_OVERLAY_KEY,
                    ps.getBoolean(dbId, LandClaimPlayerPluginSettings.ENABLE_TIME_MEASUREMENT_OVERLAY_KEY).orElse(false));
        if (!player.hasAttribute("oz.landclaim.areaFrames"))
            player.setAttribute("oz.landclaim.areaFrames", new ConcurrentHashMap<Long, Area3D>());
        if (!player.hasAttribute("oz.landclaim.currentAreaFrame"))
            player.setAttribute("oz.landclaim.currentAreaFrame", null);

        // updated Area3D frames if needed
        if ((Boolean) player.getAttribute(LandClaimPlayerPluginSettings.SHOW_CURRENT_CHUNK_FRAME_KEY))
            updatecurrentAreaFrameForPlayer(player);
        Area3DUtils.updateAreaFramesForPlayer(player);
        enqueue(() -> {
            if (chunkInfoManager != null) {
                chunkInfoManager.refresh(player);
            }
        });
    }

    private void scheduleCityRentBilling() {
        if (cityRentTimer != null) cityRentTimer.kill();
        int hour = Math.max(0, Math.min(23, s.cityRentBillingHour));
        LocalDateTime now = LocalDateTime.now();
        nextCityRentBillingDate = now.toLocalTime().isBefore(LocalTime.of(hour, 0))
                ? now.toLocalDate() : now.toLocalDate().plusDays(1);
        cityRentTimer = new Timer(30f, 60f, -1, () -> {
            LocalDateTime current = LocalDateTime.now();
            if (current.toLocalDate().isBefore(nextCityRentBillingDate)
                    || current.getHour() < Math.max(0, Math.min(23, s.cityRentBillingHour))) return;
            CityRentService.RentRunResult result = cityRentService.bill(nextCityRentBillingDate);
            logger().info("City rent billing checked " + result.checked() + " leaseholds, paid=" + result.paid()
                    + ", evicted=" + result.evicted() + ", warned=" + result.warned());
            nextCityRentBillingDate = current.toLocalDate().plusDays(1);
        });
        cityRentTimer.start();
    }

    private void showPendingCityNotifications(Player player) {
        if (cityService == null) return;
        var notices = cityService.pendingNotifications(player.getDbID());
        if (notices.isEmpty()) return;
        var notice = notices.get(0);
        String[] arguments = notice.arguments().split("\\t", 2);
        String message = t.get(notice.messageKey(), player).replace("PH_AREA_NAME", arguments[0]);
        if (arguments.length > 1) message = message.replace("PH_RENT", arguments[1]);
        UIElement dialog = UIDialogFactory.getWarningDialog(player, t.get("tc.city.notice.title", player), message,
                closed -> {
                    cityService.deletePendingNotification(notice.id());
                    showPendingCityNotifications(closed);
                });
        player.addUIElement(dialog, UITarget.Modal);
    }

    public void onPlayerSpawnEvent(PlayerSpawnEvent event) {
        Player player = event.getPlayer();
        Vector3i chunkPos = player.getChunkPosition();
        eventLogger().debug("Player " + player.getName() + " spawned. Current chunk position: "
                + chunkPos.toString());
        chunkClaimUtil.enterChunk(player, chunkPos);
        enqueue(() -> {
            if (chunkInfoManager != null) {
                chunkInfoManager.refresh(player);
            }
        });

        if (s.enableWelcomeMessage) {
            // Player player = event.getPlayer();
            String lang = de.omegazirkel.risingworld.OZTools.getPlayerLanguage(player);
            player.sendTextMessage(t.get("tc.msg.plugin.welcome", lang)
                    .replace("PH_PLUGIN_NAME", getDescription("name"))
                    .replace("PH_PLUGIN_CMD", pluginCMD)
                    .replace("PH_PLUGIN_VERSION", getDescription("version")));
        }
    }

    public void onPlayerDeathEvent(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        Vector3i chunkPos = player.getChunkPosition();
        eventLogger().debug("Player " + player.getName() + " died. Current chunk position: "
                + chunkPos.toString());
        chunkClaimUtil.leaveChunk(player, chunkPos);
    }

    public void ensureDefaultPermissionFiles() {
        String[] files = {
                "ozlc-owner.json",
                "ozlc-friend.json",
                "ozlc-guest.json",
                "ozlc-resident.json",
                "ozlc-prisoner.json",
                "ozlc-exiled.json",
                "ozlc-special-rest.json",
                "ozlc-special-pvp.json",
                "ozlc-special-static.json",
                "ozlc-special-trap.json",
                "ozlc-special-renew.json",
                "ozlc-special-city-core.json",
                "ozlc-special-city-leasehold.json",
                "ozlc-special.json"
        };

        PermissionFileUtil fileUtil = new PermissionFileUtil(this);
        if (fileUtil.copyPermissionFiles(false, files)) {
            Server.sendInputCommand("reloadpermissions");
            logger().info("Permission files reloaded.");
        } else {
            logger().warn("No permission files were copied, skipping reload.");
        }

    }

    public void onPlayerToggleInventoryEvent(PlayerToggleInventoryEvent event) {
        if (chunkInfoManager != null) {
            chunkInfoManager.onPlayerToggleInventoryEvent(event);
        }
    }

}
