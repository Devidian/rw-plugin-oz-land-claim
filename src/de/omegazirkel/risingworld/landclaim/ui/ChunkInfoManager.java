package de.omegazirkel.risingworld.landclaim.ui;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import de.omegazirkel.risingworld.landclaim.ChunkClaimUtil;
import net.risingworld.api.Timer;
import net.risingworld.api.events.EventMethod;
import net.risingworld.api.events.Listener;
import net.risingworld.api.events.player.PlayerConnectEvent;
import net.risingworld.api.events.player.PlayerDisconnectEvent;
import net.risingworld.api.events.player.ui.PlayerToggleInventoryEvent;
import net.risingworld.api.objects.Player;

public class ChunkInfoManager implements Listener {

    private final ChunkClaimUtil chunkClaimUtil;
    private final Map<Player, ChunkInfoController> controllers = new HashMap<>();

    private Timer timer;

    public ChunkInfoManager(ChunkClaimUtil util) {
        this.chunkClaimUtil = util;
    }

    @EventMethod
    public void onPlayerConnectEvent(PlayerConnectEvent event) {
        Player p = event.getPlayer();
        ChunkInfoController controller = new ChunkInfoController(p, chunkClaimUtil);
        controllers.put(p, controller);
    }

    @EventMethod
    public void onPlayerDisconnectEvent(PlayerDisconnectEvent event) {
        Player p = event.getPlayer();
        ChunkInfoController controller = controllers.remove(p);
        if (controller != null) {
            controller.update(); // sorgt für overlay.remove(), falls nötig
        }
    }

    @EventMethod
    public void onPlayerToggleInventoryEvent(PlayerToggleInventoryEvent event) {
        Player p = event.getPlayer();
        ChunkInfoController controller = controllers.get(p);
        if (controller == null) {
            controller = new ChunkInfoController(p, chunkClaimUtil);
            controllers.put(p, controller);
        }
        controller.setInventoryVisible(event.isVisible());
    }

    public void start() {
        stop(); // ensure old timer is killed before creating a new one
        timer = new Timer(1, 0, -1, () -> updateLoop());
        timer.start();
    }

    public void stop() {
        if (timer != null)
            timer.kill();
    }

    private void updateLoop() {
        Iterator<Map.Entry<Player, ChunkInfoController>> it = controllers.entrySet().iterator();

        while (it.hasNext()) {
            var entry = it.next();
            Player p = entry.getKey();
            ChunkInfoController controller = entry.getValue();

            // Player offline → remove
            if (!p.isConnected()) {
                controller.update(); // cleanup overlay
                it.remove();
                continue;
            }

            // update
            controller.update();

            // if (!keep) {
            //     it.remove();
            // }
        }
    }
}
