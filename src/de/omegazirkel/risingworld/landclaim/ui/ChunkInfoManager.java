package de.omegazirkel.risingworld.landclaim.ui;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import de.omegazirkel.risingworld.LandClaim;
import de.omegazirkel.risingworld.landclaim.ChunkClaimUtil;
import net.risingworld.api.Server;
import net.risingworld.api.Timer;
import net.risingworld.api.events.player.PlayerConnectEvent;
import net.risingworld.api.events.player.PlayerDisconnectEvent;
import net.risingworld.api.events.player.ui.PlayerToggleInventoryEvent;
import net.risingworld.api.objects.Player;

public final class ChunkInfoManager {

    private final ChunkClaimUtil chunkClaimUtil;
    private final Map<Player, ChunkInfoController> controllers = new ConcurrentHashMap<>();
    private final Set<Player> failedControllers = ConcurrentHashMap.newKeySet();

    private Timer timer;
    private volatile boolean active;

    public ChunkInfoManager(ChunkClaimUtil util) {
        this.chunkClaimUtil = util;
    }

    public void onPlayerConnectEvent(PlayerConnectEvent event) {
        ensureController(event.getPlayer());
    }

    public void onPlayerDisconnectEvent(PlayerDisconnectEvent event) {
        Player p = event.getPlayer();
        ChunkInfoController controller = controllers.remove(p);
        if (controller != null) {
            hideSafely(p, controller);
        }
        failedControllers.remove(p);
    }

    public void onPlayerToggleInventoryEvent(PlayerToggleInventoryEvent event) {
        Player p = event.getPlayer();
        ChunkInfoController controller = ensureController(p);
        try {
            controller.setInventoryVisible(event.isVisible());
            failedControllers.remove(p);
        } catch (RuntimeException ex) {
            reportFailure(p, ex);
        }
    }

    public void refresh(Player player) {
        if (!active || player == null || !player.isConnected()) {
            return;
        }
        updateSafely(player, ensureController(player));
    }

    public void start() {
        stop();
        active = true;
        timer = new Timer(1, 0, -1, () -> updateLoop());
        timer.start();
        Player[] players = Server.getAllPlayers();
        if (players != null) {
            for (Player player : players) {
                refresh(player);
            }
        }
    }

    public void stop() {
        active = false;
        if (timer != null) {
            timer.kill();
            timer = null;
        }
        for (Map.Entry<Player, ChunkInfoController> entry : controllers.entrySet()) {
            hideSafely(entry.getKey(), entry.getValue());
        }
        controllers.clear();
        failedControllers.clear();
    }

    private void updateLoop() {
        for (Map.Entry<Player, ChunkInfoController> entry : controllers.entrySet()) {
            Player p = entry.getKey();
            ChunkInfoController controller = entry.getValue();
            try {
                if (!p.isConnected()) {
                    controllers.remove(p, controller);
                    failedControllers.remove(p);
                    controller.hide();
                    continue;
                }
                controller.update();
                if (failedControllers.remove(p)) {
                    LandClaim.logger().debug("Chunk info refresh recovered for player " + p.getName() + ".");
                }
            } catch (RuntimeException ex) {
                reportFailure(p, ex);
            }
        }
    }

    private ChunkInfoController ensureController(Player player) {
        return controllers.computeIfAbsent(player, p -> new ChunkInfoController(p, chunkClaimUtil));
    }

    private void updateSafely(Player player, ChunkInfoController controller) {
        try {
            controller.update();
            if (failedControllers.remove(player)) {
                LandClaim.logger().debug("Chunk info refresh recovered for player " + player.getName() + ".");
            }
        } catch (RuntimeException ex) {
            reportFailure(player, ex);
        }
    }

    private void hideSafely(Player player, ChunkInfoController controller) {
        try {
            controller.hide();
        } catch (RuntimeException ex) {
            reportFailure(player, ex);
        }
    }

    private void reportFailure(Player player, RuntimeException ex) {
        if (!failedControllers.add(player)) {
            return;
        }
        String playerName;
        try {
            playerName = player.getName();
        } catch (RuntimeException ignored) {
            playerName = "<unknown>";
        }
        LandClaim.logger().error("Chunk info refresh failed for player " + playerName
                + ": " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
    }
}
