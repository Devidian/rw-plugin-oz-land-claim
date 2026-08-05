package de.omegazirkel.risingworld.landclaim;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import de.omegazirkel.risingworld.LandClaim;
// import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.landclaim.ui.LandClaimPlayerPluginSettings;
import de.omegazirkel.risingworld.landclaim.db.LeaseholdRecord;
import net.risingworld.api.Server;
import net.risingworld.api.objects.Area;
import net.risingworld.api.objects.Player;
import net.risingworld.api.utils.Vector3i;
import net.risingworld.api.worldelements.Area3D;

public class Area3DUtils {
    // private static final I18n t = I18n.getInstance(LandClaim.name);
    private static final PluginSettings s = PluginSettings.getInstance();
    private static final Map<String, AreaColors> AREA_COLORS = new HashMap<>();

    private static void fillColorMap(Boolean refresh) {
        if (!AREA_COLORS.isEmpty() && !refresh)
            return;
        // might be useful if colors were changed in settings but might never be
        // relevant
        if (refresh)
            AREA_COLORS.clear();

        AREA_COLORS.put(s.specialRestAreaPermission,
                new AreaColors(s.restAreaBorderColor, s.restAreaFrameColor));

        AREA_COLORS.put(s.specialPvPAreaPermission,
                new AreaColors(s.pvpAreaBorderColor, s.pvpAreaFrameColor));

        AREA_COLORS.put(s.specialStaticAreaPermission,
                new AreaColors(s.staticAreaBorderColor, s.staticAreaFrameColor));

        AREA_COLORS.put(s.specialTrapAreaPermission,
                new AreaColors(s.trapAreaBorderColor, s.trapAreaFrameColor));

        AREA_COLORS.put(s.specialRenewAreaPermission,
                new AreaColors(s.renewAreaBorderColor, s.renewAreaFrameColor));

        AREA_COLORS.put(s.specialAreaPermission,
                new AreaColors(s.specialAreaBorderColor, s.specialAreaFrameColor));

        AREA_COLORS.put(s.defaultAreaPermission,
                new AreaColors(s.otherAreaBorderColor, s.otherAreaFrameColor));
    }

    public static void updateAreaFramesForAllPlayers() {
        for (Player player : Server.getAllPlayers()) {
            refreshAreaFramesForPlayer(player);
        }
    }

    public static void updateAreaFramesForPlayer(Player player) {
        updateAreaFramesForPlayer(player, player.getChunkPosition());
    }

    public static void updateAreaFramesForPlayer(Player player, Vector3i playerChunk) {
        updateAreaFramesForPlayer(player, playerChunk, false);
    }

    public static void refreshAreaFramesForPlayer(Player player) {
        updateAreaFramesForPlayer(player, player.getChunkPosition(), true);
    }

    private static void updateAreaFramesForPlayer(Player player, Vector3i playerChunk, boolean refreshExisting) {
        fillColorMap(false);
        Boolean showOwned = (Boolean) player.getAttribute(LandClaimPlayerPluginSettings.SHOW_OWNED_AREA_FRAMES_KEY);
        Boolean showOther = (Boolean) player.getAttribute(LandClaimPlayerPluginSettings.SHOW_OTHER_AREA_FRAMES_KEY);

        // Get or create map
        if (!player.hasAttribute("oz.landclaim.areaFrames")) {
            player.setAttribute("oz.landclaim.areaFrames", new ConcurrentHashMap<Long, Area3D>());
        }

        @SuppressWarnings("unchecked")
        ConcurrentHashMap<Long, Area3D> frames = (ConcurrentHashMap<Long, Area3D>) player
                .getAttribute("oz.landclaim.areaFrames");
        if (frames == null) {
            frames = new ConcurrentHashMap<>();
            player.setAttribute("oz.landclaim.areaFrames", frames);
        }

        if (!Boolean.TRUE.equals(showOwned) && !Boolean.TRUE.equals(showOther)) {
            removeFramesOutside(player, frames, Set.of());
            return;
        }

        Area[] areas = Server.getAllAreas();
        if (areas == null) {
            return;
        }

        int chunkRadius = LandClaimPlayerPluginSettings.areaFrameChunkRadius(player);
        Set<Long> visibleAreaIds = new HashSet<>();
        for (Area area : areas) {
            if (area == null)
                continue;
            if (!AreaFrameVisibility.intersectsChunkRadius(
                    playerChunk,
                    area.getStartChunkPosition(),
                    area.getEndChunkPosition(),
                    chunkRadius)) {
                continue;
            }

            long areaId = area.getID();
            String areaPermission = area.getPlayerPermission(player);
            boolean isOwner = areaPermission != null && areaPermission.equals(s.ownerAreaPermission);
            boolean shouldShow = (isOwner ? Boolean.TRUE.equals(showOwned) : Boolean.TRUE.equals(showOther))
                    && (isOwner || specialAreaVisibleTo(player, playerChunk, area));

            Area3D existing = frames.get(areaId);

            if (shouldShow) {
                visibleAreaIds.add(areaId);
                // if it should be visible but does not exist → create
                if (existing == null) {
                    AreaColors colors = colorsFor(area, isOwner);
                    Area3D a3d = new Area3D(area);
                    a3d.setColor(colors.border());
                    a3d.setFrameColor(colors.frame());
                    a3d.setFrameVisible(true);

                    frames.put(areaId, a3d);
                    player.addGameObject(a3d);
                } else if (refreshExisting) {
                    AreaColors colors = colorsFor(area, isOwner);
                    existing.setArea(area);
                    existing.setColor(colors.border());
                    existing.setFrameColor(colors.frame());
                    existing.setFrameVisible(true);
                }
            }
        }
        removeFramesOutside(player, frames, visibleAreaIds);
    }

    private static void removeFramesOutside(
            Player player,
            ConcurrentHashMap<Long, Area3D> frames,
            Set<Long> visibleAreaIds) {
        for (Map.Entry<Long, Area3D> entry : new HashMap<>(frames).entrySet()) {
            if (visibleAreaIds.contains(entry.getKey())) {
                continue;
            }
            player.removeGameObject(entry.getValue());
            frames.remove(entry.getKey());
        }
    }

    private static AreaColors colorsFor(Area area, boolean isOwner) {
        String defaultPermission = area.getDefaultPermission();
        if (s.specialCityCorePermission.equals(defaultPermission)) {
            return new AreaColors(s.cityCoreBorderColor, s.cityCoreFrameColor);
        }
        if (s.specialCityLeaseholdPermission.equals(defaultPermission)) {
            LeaseholdRecord lease = LandClaim.cityService() == null ? null
                    : LandClaim.cityService().findLeasehold(area.getID()).orElse(null);
            return lease != null && lease.occupied()
                    ? new AreaColors(s.cityLeaseholdOccupiedBorderColor, s.cityLeaseholdOccupiedFrameColor)
                    : new AreaColors(s.cityLeaseholdAvailableBorderColor, s.cityLeaseholdAvailableFrameColor);
        }
        if (isListedForSale(area)) {
            return new AreaColors(s.forSaleAreaBorderColor, s.forSaleAreaFrameColor);
        }
        if (isOwner) {
            return new AreaColors(s.ownedAreaBorderColor, s.ownedAreaFrameColor);
        }
        return AREA_COLORS.getOrDefault(
                defaultPermission,
                new AreaColors(s.otherAreaBorderColor, s.otherAreaFrameColor));
    }

    private static boolean specialAreaVisibleTo(Player player, Vector3i playerChunk, Area area) {
        if (area == null) {
            return false;
        }
        if (isChunkInsideArea(playerChunk, area)) {
            return true;
        }
        if (Boolean.TRUE.equals(s.allowAdminOverride) && player.isAdmin()) {
            return true;
        }
        String permission = area.getDefaultPermission();
        if (s.specialAreaPermission.equals(permission)) {
            return Boolean.TRUE.equals(s.showSpecialAreaFrames);
        }
        if (s.specialStaticAreaPermission.equals(permission)) {
            return Boolean.TRUE.equals(s.showStaticAreaFrames);
        }
        if (s.specialPvPAreaPermission.equals(permission)) {
            return Boolean.TRUE.equals(s.showPvPAreaFrames);
        }
        if (s.specialRestAreaPermission.equals(permission)) {
            return Boolean.TRUE.equals(s.showRestAreaFrames);
        }
        if (s.specialTrapAreaPermission.equals(permission)) {
            return Boolean.TRUE.equals(s.showTrapAreaFrames);
        }
        if (s.specialRenewAreaPermission.equals(permission)) {
            return Boolean.TRUE.equals(s.showRenewAreaFrames);
        }
        return true;
    }

    private static boolean isChunkInsideArea(Vector3i chunk, Area area) {
        if (chunk == null || area == null || area.getStartChunkPosition() == null || area.getEndChunkPosition() == null) {
            return false;
        }
        Vector3i start = area.getStartChunkPosition();
        Vector3i end = area.getEndChunkPosition();
        return chunk.x >= Math.min(start.x, end.x)
                && chunk.x <= Math.max(start.x, end.x)
                && chunk.z >= Math.min(start.z, end.z)
                && chunk.z <= Math.max(start.z, end.z);
    }

    private static boolean isListedForSale(Area area) {
        boolean walletAvailable = LandClaim.economyIntegration() != null
                && LandClaim.economyIntegration().hasSystemAccountApi();
        return ClaimModePolicy.salesAvailable(s.allowClaimSale, walletAvailable)
                && area != null
                && LandClaim.claimSaleListingService() != null
                && LandClaim.claimSaleListingService().activeListing(area.getID()).isPresent();
    }

    public static void updateCurrentChunkFrameForPlayer(Player player, Area area) {

        Area3D chunkBorderArea = ((Area3D) player.getAttribute("oz.landclaim.currentAreaFrame"));
        if (area == null) {
            if (chunkBorderArea != null) {
                player.removeGameObject(chunkBorderArea);
                player.setAttribute("oz.landclaim.currentAreaFrame", null);
            }
            return;
        }
        if (chunkBorderArea == null) {
            chunkBorderArea = new Area3D(area);
            chunkBorderArea.setColor(s.currentChunkBorderColor);
            chunkBorderArea.setFrameColor(s.currentChunkFrameColor);
            chunkBorderArea.setFrameVisible(true);
            player.setAttribute("oz.landclaim.currentAreaFrame", chunkBorderArea);
            player.addGameObject(chunkBorderArea);
        } else {
            chunkBorderArea.setArea(area);
        }
    }
}
