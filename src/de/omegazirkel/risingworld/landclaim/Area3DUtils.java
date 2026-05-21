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

        AREA_COLORS.put(s.specialTrapAreaPermission,
                new AreaColors(s.trapAreaBorderColor, s.trapAreaFrameColor));

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
            boolean shouldShow = isOwner ? Boolean.TRUE.equals(showOwned) : Boolean.TRUE.equals(showOther);

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

    private static boolean isListedForSale(Area area) {
        return s.allowClaimSale
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
