package de.omegazirkel.risingworld.landclaim;

import java.util.HashSet;
import java.util.Set;

import de.omegazirkel.risingworld.LandClaim;
import de.omegazirkel.risingworld.landclaim.db.RenewZoneConfig;
import de.omegazirkel.risingworld.landclaim.db.RenewZoneConfigService;
import de.omegazirkel.risingworld.tools.I18n;
import net.risingworld.api.Server;
import net.risingworld.api.World;
import net.risingworld.api.objects.Area;
import net.risingworld.api.objects.Player;
import net.risingworld.api.utils.Vector3i;

public class RenewZoneResetService {

    private final RenewZoneConfigService configService;
    private final PluginSettings settings;

    public RenewZoneResetService(RenewZoneConfigService configService, PluginSettings settings) {
        this.configService = configService;
        this.settings = settings;
    }

    public RenewZoneResetResult resetDueZones(long nowMs) {
        if (configService == null) {
            return new RenewZoneResetResult(0, 0, 0, 0);
        }
        int zonesChecked = 0;
        int zonesReset = 0;
        int chunksReset = 0;
        int staleConfigsRemoved = 0;
        for (RenewZoneConfig config : configService.dueAt(nowMs)) {
            zonesChecked++;
            Area area = Server.getArea(config.areaId());
            if (area == null || !settings.specialRenewAreaPermission.equals(area.getDefaultPermission())) {
                if (configService.delete(config.areaId())) {
                    staleConfigsRemoved++;
                }
                continue;
            }
            int resetCount = resetAreaColumns(area);
            configService.markReset(config.areaId(), nowMs);
            zonesReset++;
            chunksReset += resetCount;
            LandClaim.logger().info("Renew zone reset completed for area " + areaName(area)
                    + " (#" + area.getID() + "), reset chunk columns: " + resetCount);
            announceReset(area, resetCount);
        }
        if (zonesChecked > 0) {
            Area3DUtils.updateAreaFramesForAllPlayers();
        }
        return new RenewZoneResetResult(zonesChecked, zonesReset, chunksReset, staleConfigsRemoved);
    }

    private int resetAreaColumns(Area area) {
        Set<String> resetColumns = new HashSet<>();
        for (Vector3i chunk : ChunkClaimUtil.areaToChunks(area)) {
            resetColumns.add(chunk.x + ":" + chunk.z);
        }
        int resetCount = 0;
        for (String key : resetColumns) {
            String[] parts = key.split(":");
            if (parts.length == 2 && World.resetChunk(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]))) {
                resetCount++;
            }
        }
        return resetCount;
    }

    private void announceReset(Area area, int resetCount) {
        I18n t = I18n.getInstance(LandClaim.name);
        String discordMessage = t.get("TC_DISCORD_RENEW_ZONE_RESET", DiscordConnect.botLang())
                .replace("PH_AREA_NAME", areaName(area))
                .replace("PH_AREA_ID", String.valueOf(area.getID()))
                .replace("PH_RESET_COUNT", String.valueOf(resetCount));
        DiscordConnect.sendDiscordRenewZoneLog(discordMessage);

        String target = settings.renewZoneResetAnnouncementTarget == null
                ? "none"
                : settings.renewZoneResetAnnouncementTarget.trim().toLowerCase();
        if (!target.equals("all") && !target.equals("admins")) {
            return;
        }
        for (Player player : Server.getAllPlayers()) {
            if (target.equals("admins") && !player.isAdmin()) {
                continue;
            }
            player.sendTextMessage(t.get("TC_ANNOUNCEMENT_RENEW_ZONE_RESET", player)
                    .replace("PH_AREA_NAME", areaName(area))
                    .replace("PH_RESET_COUNT", String.valueOf(resetCount)));
        }
    }

    private String areaName(Area area) {
        return area.getName() == null || area.getName().isBlank() ? "Area #" + area.getID() : area.getName();
    }

    public record RenewZoneResetResult(
            int zonesChecked,
            int zonesReset,
            int chunksReset,
            int staleConfigsRemoved) {
    }
}
