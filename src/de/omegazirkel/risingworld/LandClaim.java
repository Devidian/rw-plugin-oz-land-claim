package de.omegazirkel.risingworld;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import de.omegazirkel.risingworld.landclaim.ChunkClaimUtil;
import de.omegazirkel.risingworld.landclaim.DiscordConnect;
import de.omegazirkel.risingworld.landclaim.LandClaimChunkDatabase;
import de.omegazirkel.risingworld.landclaim.LandClaimGUI;
import de.omegazirkel.risingworld.landclaim.PermissionFileUtil;
import de.omegazirkel.risingworld.landclaim.PluginSettings;
import de.omegazirkel.risingworld.landclaim.ui.ChunkInfoManager;
import de.omegazirkel.risingworld.tools.Colors;
import de.omegazirkel.risingworld.tools.FileChangeListener;
import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.OZLogger;
import de.omegazirkel.risingworld.tools.db.SQLite;
import de.omegazirkel.risingworld.tools.ui.AssetManager;
import de.omegazirkel.risingworld.tools.ui.MenuItem;
import de.omegazirkel.risingworld.tools.ui.PluginMenuManager;
import net.risingworld.api.Plugin;
import net.risingworld.api.Server;
import net.risingworld.api.events.EventMethod;
import net.risingworld.api.events.Listener;
import net.risingworld.api.events.player.PlayerCommandEvent;
import net.risingworld.api.events.player.PlayerConnectEvent;
import net.risingworld.api.events.player.PlayerDeathEvent;
import net.risingworld.api.events.player.PlayerDisconnectEvent;
import net.risingworld.api.events.player.PlayerEnterChunkEvent;
import net.risingworld.api.events.player.PlayerSpawnEvent;
import net.risingworld.api.objects.Area;
import net.risingworld.api.objects.Player;
import net.risingworld.api.utils.Vector3i;
import net.risingworld.api.worldelements.Area3D;

public class LandClaim extends Plugin implements Listener, FileChangeListener {
    static final String pluginCMD = "lc";
    private ChunkInfoManager chunkInfoManager;

    static final Colors c = Colors.getInstance();
    private static I18n t = null;
    private static PluginSettings s;
    private static LandClaimGUI gui;
    private static ChunkClaimUtil chunkClaimUtil;
    public static String name;

    private LandClaimChunkDatabase database;

    public static OZLogger logger() {
        return OZLogger.getInstance("OZ.LandClaim");
    }

    public static OZLogger eventLogger() {
        return OZLogger.getInstance("OZ.LandClaim.Events");
    }

    @Override
    public void onEnable() {
        name = this.getDescription("name");
        t = I18n.getInstance(this);
        registerEventListener(this);
        s = PluginSettings.getInstance(this);
        database = new LandClaimChunkDatabase(new SQLite(this));
        chunkClaimUtil = new ChunkClaimUtil(database);
        gui = LandClaimGUI.getInstance(chunkClaimUtil, this);
        s.initSettings();
        logger().setLevel(s.logLevel);
        ensureDefaultPermissionFiles();

        // Load Plugin Menu into Main Plugin Menu
        PluginMenuManager
                .registerPluginMenu(new MenuItem(AssetManager.getIcon("oz-lc-logo"), "Land Claim", (Player p) -> {
                    gui.openMainMenu(p);
                }));
        // connect plugins
        DiscordConnect.init(this);

        // init Chunk Info overlay timer
        chunkInfoManager = new ChunkInfoManager(chunkClaimUtil);
        registerEventListener(chunkInfoManager);
        chunkInfoManager.start();

        logger().info("✅ " + this.getName() + " Plugin is enabled version:" + this.getDescription("version"));
    }

    @Override
    public void onDisable() {
        logger().warn("⚠️ Disabling " + this.getName() + " ...");
        if (chunkInfoManager != null)
            chunkInfoManager.stop();
        logger().warn("❌ " + this.getName() + " disabled.");
    }

    @Override
    public void onSettingsChanged(Path settingsPath) {
        s.initSettings(settingsPath.toString());
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
            // Invalid number of arguments (0)
            if (cmdParts.length < 2) {
                return;
            }
            String option = cmdParts[1].toLowerCase();
            switch (option) {
                case "status":
                    String statusMessage = t.get("TC_CMD_STATUS", player)
                            .replace("PH_VERSION", c.okay + this.getDescription("version") + c.endTag)
                            .replace("PH_LANGUAGE",
                                    c.info + player.getLanguage() + " / " + player.getSystemLanguage() + c.endTag)
                            .replace("PH_USEDLANG", c.okay + t.getLanguageUsed(lang) + c.endTag)
                            .replace("PH_LANG_AVAILABLE", c.warning + t.getLanguageAvailable() + c.endTag);
                    player.sendTextMessage(c.okay + this.getName() + ":> " + c.endTag + statusMessage);
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
                        player.setAttribute("developerMode",
                                player.hasAttribute("developerMode") ? !(Boolean) player.getAttribute("developerMode")
                                        : true);
                    }
                    break;
                case "open":
                    gui.openMainMenu(player);
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
        gui.updateCurrentChunkFrameForPlayer(player, area);
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

        // if player has "showCurrentChunkFrame" enabled, show chunk area
        if (player.hasAttribute("showCurrentChunkFrame") && (Boolean) player.getAttribute("showCurrentChunkFrame")) {
            Area area = ChunkClaimUtil.getVirtualAreaFromChunkVector(chunkPos);
            gui.updateCurrentChunkFrameForPlayer(player, area);
        }
    }

    @EventMethod
    public void onPlayerDisconnectEvent(PlayerDisconnectEvent event) {
        Player player = event.getPlayer();
        Vector3i chunkPos = player.getChunkPosition();
        chunkClaimUtil.leaveChunk(player, chunkPos);
        if (player.hasAttribute("areaFrames")) {

            @SuppressWarnings("unchecked")
            Map<Long, Area3D> frames = (Map<Long, Area3D>) player.getAttribute("areaFrames");
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
        // ensure values are set
        if (!player.hasAttribute("developerMode"))
            player.setAttribute("developerMode", false);
        if (!player.hasAttribute("showCurrentChunkFrame"))
            player.setAttribute("showCurrentChunkFrame", false);
        if (!player.hasAttribute("showOwnedAreaFrames"))
            player.setAttribute("showOwnedAreaFrames", false);
        if (!player.hasAttribute("showOtherAreaFrames"))
            player.setAttribute("showOtherAreaFrames", false);
        if (!player.hasAttribute("areaFrames"))
            player.setAttribute("areaFrames", new ConcurrentHashMap<Long, Area3D>());
        if (!player.hasAttribute("enableClaimInfoOverlay"))
            player.setAttribute("enableClaimInfoOverlay", true);
        if (!player.hasAttribute("currentAreaFrame"))
            player.setAttribute("currentAreaFrame", null);

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
                "ozlc-special-trap.json",
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
