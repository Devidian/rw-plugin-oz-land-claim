package de.omegazirkel.risingworld;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
import de.omegazirkel.risingworld.landclaim.db.ClaimSaleListingService;
import de.omegazirkel.risingworld.landclaim.db.ExtraClaimCapacityService;
import de.omegazirkel.risingworld.landclaim.db.LandClaimChunkService;
import de.omegazirkel.risingworld.landclaim.db.LandClaimChunkStore;
import de.omegazirkel.risingworld.landclaim.db.RenewZoneConfigService;
import de.omegazirkel.risingworld.landclaim.ui.ClaimSaleIndicatorProvider;
import de.omegazirkel.risingworld.landclaim.ui.ChunkInfoManager;
import de.omegazirkel.risingworld.landclaim.ui.LandClaimPlayerPluginData;
import de.omegazirkel.risingworld.landclaim.ui.LandClaimPlayerPluginSettings;
import de.omegazirkel.risingworld.tools.Colors;
import de.omegazirkel.risingworld.tools.FileChangeListener;
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
import net.risingworld.api.events.EventMethod;
import net.risingworld.api.events.Listener;
import net.risingworld.api.events.player.PlayerCommandEvent;
import net.risingworld.api.events.player.PlayerConnectEvent;
import net.risingworld.api.events.player.PlayerDeathEvent;
import net.risingworld.api.events.player.PlayerDisconnectEvent;
import net.risingworld.api.events.player.PlayerEnterChunkEvent;
import net.risingworld.api.events.player.PlayerEnterSectorEvent;
import net.risingworld.api.events.player.PlayerSpawnEvent;
import net.risingworld.api.objects.Area;
import net.risingworld.api.objects.Player;
import net.risingworld.api.utils.Vector3i;
import net.risingworld.api.worldelements.Area3D;

public class LandClaim extends Plugin implements Listener, FileChangeListener {
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
    private Timer renewZoneTimer;
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
        instance = this;
        name = this.getDescription("name");
        t = I18n.getInstance(this);
        registerEventListener(this);
        s = PluginSettings.getInstance(this);
        sqliteCon = SQLiteConnectionFactory.open(this);
        ps = new PlayerSettings(sqliteCon);
        try {
            lccStore = new LandClaimChunkStore(sqliteCon);
            extraClaimCapacityService = new ExtraClaimCapacityService(sqliteCon);
            claimSaleListingService = new ClaimSaleListingService(sqliteCon);
            renewZoneConfigService = new RenewZoneConfigService(sqliteCon);
            renewZoneResetService = new RenewZoneResetService(renewZoneConfigService, s);
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
        logger().setLevel(s.logLevel);
        ensureDefaultPermissionFiles();

        // Load Plugin Menu into Main Plugin Menu
        PluginMenuManager
                .registerPluginMenu(new MenuItem(name, "icon-ki-plugin-logo", "Land Claim", (Player p) -> {
                    gui.openMainMenu(p);
                }));
        PluginShortcutVisibility.register(name, LandClaimPlayerPluginSettings::shortcutVisible);
        // connect plugins
        DiscordConnect.init(this);
        economyIntegration = new EconomyIntegration(this);
        economyIntegration.logStatus();
        economyIntegration.registerExtraClaimOffer(s);

        // init Chunk Info overlay timer
        chunkInfoManager = new ChunkInfoManager(chunkClaimUtil);
        registerEventListener(chunkInfoManager);
        chunkInfoManager.start();

        // register plugin settings
        PlayerPluginSettingsOverlay.registerPlayerPluginSettings(new LandClaimPlayerPluginSettings(getDescription("version")));
        PlayerPluginSettingsOverlay.registerPlayerPluginData(new LandClaimPlayerPluginData(getDescription("version")));
        PlayerPluginSettingsOverlay.registerPlayerPluginAdminSettings(
                new PlayerPluginAdminSettings(name, getDescription("version"), () -> s.adminSettingsEntries(),
                        s::initSettings));
        PluginInfoStatusProviders
                .registerProvider(new LandClaimPluginInfoStatusProvider(this, getDescription("version")));
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

    @Override
    public void onSettingsChanged(Path settingsPath) {
        s.initSettings(settingsPath.toString());
        logger().setLevel(s.logLevel);
        if (economyIntegration != null) {
            economyIntegration.logStatus();
            economyIntegration.registerExtraClaimOffer(s);
        }
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
            String message = t.get("TC_DISCORD_AUTO_CLAIM_REMOVAL", DiscordConnect.botLang())
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
        renewZoneTimer = new Timer(3600f, delaySeconds, -1, () -> {
            if (renewZoneResetService == null) {
                return;
            }
            RenewZoneResetService.RenewZoneResetResult result = renewZoneResetService
                    .resetDueZones(System.currentTimeMillis());
            if (result.zonesChecked() > 0) {
                logger().info("Renew zone hourly check completed. Checked: " + result.zonesChecked()
                        + ", reset: " + result.zonesReset()
                        + ", chunk columns reset: " + result.chunksReset()
                        + ", stale configs removed: " + result.staleConfigsRemoved());
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
        long nextHourMs = ((nowMs / 3_600_000L) + 1L) * 3_600_000L;
        return Math.max(1f, (nextHourMs - nowMs) / 1000f);
    }

    @EventMethod
    public void onPlayerCommand(PlayerCommandEvent event) {
        Player player = event.getPlayer();
        String lang = player.getSystemLanguage();
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
                    String statsMessage = t.get("TC_CMD_STATS", player)
                            .replace("PH_PLAYER_CLAIMS", chunkClaimUtil.getPlayerClaimCount(player) + "")
                            .replace("PH_PLAYER_MAX_CLAIMS", chunkClaimUtil.getPlayerMaxClaims(player) + "")
                            .replace("PH_PLAYER_CLAIM_TIME", chunkClaimUtil.getPlayerNextClaimTime(player) + "");
                    player.sendTextMessage(c.okay + this.getName() + ":> " + c.endTag + statsMessage);
                    break;
                case "help":
                    String helpMessage = t.get("TC_CMD_HELP", player).replaceAll("PH_PLUGIN_CMD", pluginCMD);
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
                    player.sendTextMessage(t.get("TC_ERR_CMD_UNKNOWN").replace("PH_PLUGIN_CMD", pluginCMD));
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

    @EventMethod
    public void onPlayerEnterChunkEvent(PlayerEnterChunkEvent event) {
        Player player = event.getPlayer();
        Vector3i oldChunkPos = event.getOldChunkCoordinates();
        Vector3i chunkPos = event.getNewChunkCoordinates();

        logger().debug("Player " + player.getName() + " entered chunk " + chunkPos.toString()
                + " from chunk " + oldChunkPos.toString());

        chunkClaimUtil.leaveChunk(player, oldChunkPos);
        chunkClaimUtil.enterChunk(player, chunkPos);

        // if player has "oz.landclaim.showCurrentChunkFrame" enabled, show chunk area
        if (player.hasAttribute(LandClaimPlayerPluginSettings.SHOW_CURRENT_CHUNK_FRAME_KEY)
                && (Boolean) player.getAttribute(LandClaimPlayerPluginSettings.SHOW_CURRENT_CHUNK_FRAME_KEY)) {
            Area area = ChunkClaimUtil.getVirtualAreaFromChunkVector(chunkPos);
            Area3DUtils.updateCurrentChunkFrameForPlayer(player, area);
        }
    }

    @EventMethod
    public void onPlayerEnterSectorEvent(PlayerEnterSectorEvent event) {
        Area3DUtils.updateAreaFramesForPlayer(event.getPlayer());
    }

    @EventMethod
    public void onPlayerDisconnectEvent(PlayerDisconnectEvent event) {
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

    @EventMethod
    public void onPlayerConnectEvent(PlayerConnectEvent event) {
        Player player = event.getPlayer();
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
        if (!player.hasAttribute(LandClaimPlayerPluginSettings.ENABLE_CLAIM_INFO_OVERLAY_KEY))
            player.setAttribute(LandClaimPlayerPluginSettings.ENABLE_CLAIM_INFO_OVERLAY_KEY,
                    ps.getBoolean(dbId, LandClaimPlayerPluginSettings.ENABLE_CLAIM_INFO_OVERLAY_KEY).orElse(true));
        if (!player.hasAttribute("oz.landclaim.areaFrames"))
            player.setAttribute("oz.landclaim.areaFrames", new ConcurrentHashMap<Long, Area3D>());
        if (!player.hasAttribute("oz.landclaim.currentAreaFrame"))
            player.setAttribute("oz.landclaim.currentAreaFrame", null);

        // updated Area3D frames if needed
        if ((Boolean) player.getAttribute(LandClaimPlayerPluginSettings.SHOW_CURRENT_CHUNK_FRAME_KEY))
            updatecurrentAreaFrameForPlayer(player);
        Area3DUtils.updateAreaFramesForPlayer(player);
    }

    @EventMethod
    public void onPlayerSpawnEvent(PlayerSpawnEvent event) {
        Player player = event.getPlayer();
        Vector3i chunkPos = player.getChunkPosition();
        eventLogger().debug("Player " + player.getName() + " spawned. Current chunk position: "
                + chunkPos.toString());
        chunkClaimUtil.enterChunk(player, chunkPos);

        if (s.enableWelcomeMessage) {
            // Player player = event.getPlayer();
            String lang = player.getSystemLanguage();
            player.sendTextMessage(t.get("TC_MSG_PLUGIN_WELCOME", lang)
                    .replace("PH_PLUGIN_NAME", getDescription("name"))
                    .replace("PH_PLUGIN_CMD", pluginCMD)
                    .replace("PH_PLUGIN_VERSION", getDescription("version")));
        }
    }

    @EventMethod
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

}
