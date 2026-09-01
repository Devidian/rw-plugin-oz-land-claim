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

    public synchronized RenewZoneResetResult resetNextDueZone(long nowMs) {
        if (configService == null) {
            return new RenewZoneResetResult(0, 0, 0, 0);
        }
        RenewZoneConfig config = configService.dueAt(nowMs).stream().findFirst().orElse(null);
        if (config == null) {
            return new RenewZoneResetResult(0, 0, 0, 0);
        }
        Area area = Server.getArea(config.areaId());
        if (area == null || !settings.specialRenewAreaPermission.equals(area.getDefaultPermission())) {
            boolean removed = configService.delete(config.areaId());
            if (removed) {
                Area3DUtils.updateAreaFramesForAllPlayers();
            }
            return new RenewZoneResetResult(1, 0, 0, removed ? 1 : 0);
        }
        int resetCount = resetAreaColumns(area);
        configService.markReset(config.areaId(), nowMs);
        LandClaim.logger().info("Renew zone reset completed for area " + areaName(area)
                + " (#" + area.getID() + "), reset chunk columns: " + resetCount);
        announceReset(area, resetCount);
        Area3DUtils.updateAreaFramesForAllPlayers();
        return new RenewZoneResetResult(1, 1, resetCount, 0);
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
        String discordMessage = t.get("tc.discord.renew.zone.reset", DiscordConnect.botLang())
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
            player.sendTextMessage(t.get("tc.announcement.renew.zone.reset", player)
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
