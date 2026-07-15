package de.omegazirkel.risingworld.landclaim;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import de.omegazirkel.risingworld.LandClaim;
// import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.landclaim.ui.LandClaimPlayerPluginSettings;
import net.risingworld.api.Server;
import net.risingworld.api.objects.Area;
import net.risingworld.api.objects.Player;
import net.risingworld.api.utils.Vector2i;
import net.risingworld.api.utils.Vector3i;
import net.risingworld.api.worldelements.Area3D;

public class Area3DUtils {
    // private static final I18n t = I18n.getInstance(LandClaim.name);
    private static final PluginSettings s = PluginSettings.getInstance();
    private static final Map<String, AreaColors> AREA_COLORS = new HashMap<>();
    private static final int SECTOR_SIZE_CHUNKS = 32;

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
            updateAreaFramesForPlayer(player);
        }
    }

    public static void updateAreaFramesForPlayer(Player player) {
        updateAreaFramesForPlayer(player, player.getChunkPosition());
    }

    public static void updateAreaFramesForPlayer(Player player, Vector3i playerChunk) {
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
        // remove frames not existing in the server db
        for (Map.Entry<Long, Area3D> entry : new ConcurrentHashMap<>(frames).entrySet()) {
            long areaId = entry.getKey();
            Area3D existing = entry.getValue();
            if (Server.getArea(areaId) == null) {
                player.removeGameObject(existing);
                frames.remove(areaId);
            }
        }

        for (Area area : Server.getAllAreas()) {
            if (area == null)
                continue;
            long areaId = area.getID();
            String areaPermission = area.getPlayerPermission(player);
            boolean isOwner = areaPermission != null && areaPermission.equals(s.ownerAreaPermission);
            boolean shouldShow = (isOwner ? Boolean.TRUE.equals(showOwned) : Boolean.TRUE.equals(showOther))
                    && (isOwner || specialAreaVisibleTo(player, playerChunk, area))
                    && isAreaInVisibleSectorNeighborhood(player, area);

            Area3D existing = frames.get(areaId);

            if (shouldShow) {
                AreaColors colors = colorsFor(area, isOwner);
                // if it should be visible but does not exist → create
                if (existing == null) {
                    Area3D a3d = new Area3D(area);
                    a3d.setColor(colors.border());
                    a3d.setFrameColor(colors.frame());
                    a3d.setFrameVisible(true);

                    frames.put(areaId, a3d);
                    player.addGameObject(a3d);
                } else {
                    existing.setArea(area);
                    existing.setColor(colors.border());
                    existing.setFrameColor(colors.frame());
                    existing.setFrameVisible(true);
                }
            } else {
                // if it should not be visible but exists → remove
                if (existing != null) {
                    player.removeGameObject(existing);
                    frames.remove(areaId);
                }
            }
        }
    }

    private static AreaColors colorsFor(Area area, boolean isOwner) {
        if (isListedForSale(area)) {
            return new AreaColors(s.forSaleAreaBorderColor, s.forSaleAreaFrameColor);
        }
        if (isOwner) {
            return new AreaColors(s.ownedAreaBorderColor, s.ownedAreaFrameColor);
        }
        String defaultAreaPermission = area.getDefaultPermission();
        return AREA_COLORS.getOrDefault(
                defaultAreaPermission,
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
        return s.allowClaimSale
                && area != null
                && LandClaim.claimSaleListingService() != null
                && LandClaim.claimSaleListingService().activeListing(area.getID()).isPresent();
    }

    private static boolean isAreaInVisibleSectorNeighborhood(Player player, Area area) {
        Vector2i playerSector = player.getSectorPosition();
        Vector3i playerChunk = player.getChunkPosition();
        Vector3i startChunk = area.getStartChunkPosition();
        Vector3i endChunk = area.getEndChunkPosition();
        if (playerSector == null || playerChunk == null || startChunk == null || endChunk == null) {
            return true;
        }

        int derivedPlayerSectorX = sectorCoordinate(playerChunk.x);
        int derivedPlayerSectorZ = sectorCoordinate(playerChunk.z);
        if (derivedPlayerSectorX != playerSector.x || derivedPlayerSectorZ != playerSector.y) {
            return true;
        }

        int minSectorX = sectorCoordinate(Math.min(startChunk.x, endChunk.x));
        int maxSectorX = sectorCoordinate(Math.max(startChunk.x, endChunk.x));
        int minSectorZ = sectorCoordinate(Math.min(startChunk.z, endChunk.z));
        int maxSectorZ = sectorCoordinate(Math.max(startChunk.z, endChunk.z));

        return maxSectorX >= playerSector.x - 1
                && minSectorX <= playerSector.x + 1
                && maxSectorZ >= playerSector.y - 1
                && minSectorZ <= playerSector.y + 1;
    }

    private static int sectorCoordinate(int chunkCoordinate) {
        return Math.floorDiv(chunkCoordinate, SECTOR_SIZE_CHUNKS);
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
