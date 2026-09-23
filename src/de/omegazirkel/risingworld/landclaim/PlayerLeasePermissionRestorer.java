package de.omegazirkel.risingworld.landclaim;

import java.util.List;
import java.util.Map;

import de.omegazirkel.risingworld.landclaim.db.PlayerLeaseService;
import net.risingworld.api.objects.Area;

/** Restores the exact permissions that existed before a player rental began. */
public final class PlayerLeasePermissionRestorer {
    private PlayerLeasePermissionRestorer() { }

    public static void restore(Area area, PlayerLeaseService leases, int landlordDbId, PluginSettings settings) {
        if (area == null || leases == null || settings == null) return;
        Map<Integer, String> original = leases.permissionSnapshot(area.getID());
        Map<Integer, String> current = area.getAllPlayerPermissions();
        if (current != null) for (Integer dbId : List.copyOf(current.keySet())) area.removePlayerPermission(dbId);
        if (original.isEmpty()) {
            area.setPlayerPermission(landlordDbId, settings.ownerAreaPermission);
        } else {
            for (Map.Entry<Integer, String> entry : original.entrySet()) {
                area.setPlayerPermission(entry.getKey(), entry.getValue());
            }
        }
        leases.removePermissionSnapshot(area.getID());
    }
}
