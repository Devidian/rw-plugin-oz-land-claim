package de.omegazirkel.risingworld.landclaim.ui;

import java.util.List;

import de.omegazirkel.risingworld.LandClaim;
import de.omegazirkel.risingworld.landclaim.Area3DUtils;
import de.omegazirkel.risingworld.landclaim.ChunkClaimUtil;
import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.ui.BasePlayerPluginSettingsPanel;
import de.omegazirkel.risingworld.tools.ui.Dropdown;
import de.omegazirkel.risingworld.tools.ui.DropdownOption;
import de.omegazirkel.risingworld.tools.ui.OZUIElement;
import de.omegazirkel.risingworld.tools.ui.PlayerPluginSettings;
import de.omegazirkel.risingworld.tools.ui.PluginShortcutVisibility;
import net.risingworld.api.objects.Area;
import net.risingworld.api.objects.Player;
import net.risingworld.api.ui.style.Position;
import net.risingworld.api.ui.style.Unit;

public class LandClaimPlayerPluginSettings extends PlayerPluginSettings {
    public static final String SHOW_CURRENT_CHUNK_FRAME_KEY = "oz.landclaim.showCurrentChunkFrame";
    public static final String SHOW_OWNED_AREA_FRAMES_KEY = "oz.landclaim.showOwnedAreaFrames";
    public static final String SHOW_OTHER_AREA_FRAMES_KEY = "oz.landclaim.showOtherAreaFrames";
    public static final String AREA_FRAME_CHUNK_RADIUS_KEY = "oz.landclaim.areaFrameChunkRadius";
    public static final String ENABLE_CLAIM_INFO_OVERLAY_KEY = "oz.landclaim.enableClaimInfoOverlay";
    public static final String DEVELOPER_MODE_KEY = "oz.landclaim.developerMode";
    public static final int DEFAULT_AREA_FRAME_CHUNK_RADIUS = 15;
    public static final int MIN_AREA_FRAME_CHUNK_RADIUS = 1;
    public static final int MAX_AREA_FRAME_CHUNK_RADIUS = 64;

    public LandClaimPlayerPluginSettings(String pluginVersion) {
        this.pluginLabel = LandClaim.name;
        this.pluginVersion = pluginVersion;
    }

    private final I18n t() {
        return I18n.getInstance(LandClaim.name);
    }

    @Override
    public BasePlayerPluginSettingsPanel createPlayerPluginSettingsUIElement(Player uiPlayer) {
        return new BasePlayerPluginSettingsPanel(uiPlayer, pluginLabel) {

            @Override
            protected void redrawContent() {
                flexWrapper.removeAllChilds();
                flexWrapper.addChild(booleanSetting(uiPlayer, shortcutKey(),
                        "TC_UI_LABEL_LANDCLAIM_SHORTCUT", true, null));
                flexWrapper.addChild(booleanSetting(uiPlayer, SHOW_CURRENT_CHUNK_FRAME_KEY,
                        "TC_UI_LABEL_SHOW_CURRENT_CHUNK_FRAME", false, () -> {
                            Area area = ChunkClaimUtil.getVirtualAreaFromChunkVector(uiPlayer.getChunkPosition());
                            Area3DUtils.updateCurrentChunkFrameForPlayer(uiPlayer,
                                    booleanValue(uiPlayer, SHOW_CURRENT_CHUNK_FRAME_KEY, false) ? area : null);
                        }));
                flexWrapper.addChild(booleanSetting(uiPlayer, SHOW_OWNED_AREA_FRAMES_KEY,
                        "TC_UI_LABEL_SHOW_OWNED_AREAS", false,
                        () -> Area3DUtils.updateAreaFramesForPlayer(uiPlayer)));
                flexWrapper.addChild(booleanSetting(uiPlayer, SHOW_OTHER_AREA_FRAMES_KEY,
                        "TC_UI_LABEL_SHOW_OTHER_AREAS", false,
                        () -> Area3DUtils.updateAreaFramesForPlayer(uiPlayer)));
                flexWrapper.addChild(areaFrameChunkRadiusSetting(uiPlayer));
                flexWrapper.addChild(booleanSetting(uiPlayer, ENABLE_CLAIM_INFO_OVERLAY_KEY,
                        "TC_UI_LABEL_ENABLE_CLAIM_INFO_OVERLAY", true, null));
                if (uiPlayer.isAdmin())
                    flexWrapper.addChild(booleanSetting(uiPlayer, DEVELOPER_MODE_KEY,
                            "TC_UI_LABEL_DEVELOPER_MODE", false, null));
            }

            protected OZUIElement booleanSetting(Player uiPlayer, String key, String labelKey, boolean defaultValue,
                    Runnable onChanged) {
                OZUIElement element = defaultSettingsContainer();
                element.addChild(defaultSettingsLabel(t().get(labelKey, uiPlayer)));

                boolean isEnabled = booleanValue(uiPlayer, key, defaultValue);
                element.addChild(switchButtons(uiPlayer, isEnabled, event -> {
                    setBooleanValue(uiPlayer, key, !isEnabled);
                    redrawContent();
                    if (onChanged != null) {
                        onChanged.run();
                    }
                }));
                return element;
            }

            private OZUIElement areaFrameChunkRadiusSetting(Player uiPlayer) {
                OZUIElement element = defaultSettingsContainer();
                element.addChild(defaultSettingsLabel(t().get("TC_UI_LABEL_AREA_FRAME_CHUNK_RADIUS", uiPlayer)));

                int currentRadius = areaFrameChunkRadius(uiPlayer);
                Dropdown radius = new Dropdown(
                        List.of(1, 5, 10, 15, 32, 64).stream()
                                .map(value -> new DropdownOption(String.valueOf(value), String.valueOf(value)))
                                .toList(),
                        String.valueOf(currentRadius),
                        selected -> {
                            setAreaFrameChunkRadius(uiPlayer, Integer.parseInt(selected));
                            Area3DUtils.updateAreaFramesForPlayer(uiPlayer);
                        });
                radius.style.position.set(Position.Absolute);
                radius.style.left.set(10, Unit.Pixel);
                radius.style.top.set(52, Unit.Pixel);
                radius.style.width.set(92, Unit.Percent);
                element.addChild(radius);
                return element;
            }
        };
    }

    public static boolean booleanValue(Player player, String key, boolean defaultValue) {
        if (LandClaim.ps == null) {
            return player.hasAttribute(key) ? (Boolean) player.getAttribute(key) : defaultValue;
        }
        return LandClaim.ps.getBoolean(player.getDbID(), key).orElse(defaultValue);
    }

    public static void setBooleanValue(Player player, String key, boolean value) {
        player.setAttribute(key, value);
        if (LandClaim.ps != null) {
            LandClaim.ps.setBoolean(player.getDbID(), key, value);
        }
    }

    public static int areaFrameChunkRadius(Player player) {
        if (player.hasAttribute(AREA_FRAME_CHUNK_RADIUS_KEY)) {
            Object value = player.getAttribute(AREA_FRAME_CHUNK_RADIUS_KEY);
            if (value instanceof Number number) {
                int radius = clampAreaFrameChunkRadius(number.intValue());
                player.setAttribute(AREA_FRAME_CHUNK_RADIUS_KEY, radius);
                return radius;
            }
        }

        int radius = LandClaim.ps == null
                ? DEFAULT_AREA_FRAME_CHUNK_RADIUS
                : LandClaim.ps.getInt(player.getDbID(), AREA_FRAME_CHUNK_RADIUS_KEY)
                        .map(LandClaimPlayerPluginSettings::clampAreaFrameChunkRadius)
                        .orElse(DEFAULT_AREA_FRAME_CHUNK_RADIUS);
        player.setAttribute(AREA_FRAME_CHUNK_RADIUS_KEY, radius);
        return radius;
    }

    public static void setAreaFrameChunkRadius(Player player, int radius) {
        int normalizedRadius = clampAreaFrameChunkRadius(radius);
        player.setAttribute(AREA_FRAME_CHUNK_RADIUS_KEY, normalizedRadius);
        if (LandClaim.ps != null) {
            LandClaim.ps.setInt(player.getDbID(), AREA_FRAME_CHUNK_RADIUS_KEY, normalizedRadius);
        }
    }

    public static int clampAreaFrameChunkRadius(int radius) {
        return Math.max(MIN_AREA_FRAME_CHUNK_RADIUS, Math.min(MAX_AREA_FRAME_CHUNK_RADIUS, radius));
    }

    public static boolean shortcutVisible(Player player) {
        return booleanValue(player, shortcutKey(), true);
    }

    private static String shortcutKey() {
        return PluginShortcutVisibility.playerSettingKey(LandClaim.name);
    }

}
