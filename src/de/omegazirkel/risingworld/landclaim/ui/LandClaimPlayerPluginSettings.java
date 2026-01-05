package de.omegazirkel.risingworld.landclaim.ui;

import de.omegazirkel.risingworld.LandClaim;
import de.omegazirkel.risingworld.landclaim.Area3DUtils;
import de.omegazirkel.risingworld.landclaim.ChunkClaimUtil;
import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.ui.BasePlayerPluginSettingsPanel;
import de.omegazirkel.risingworld.tools.ui.OZUIElement;
import de.omegazirkel.risingworld.tools.ui.PlayerPluginSettings;
import net.risingworld.api.objects.Area;
import net.risingworld.api.objects.Player;

public class LandClaimPlayerPluginSettings extends PlayerPluginSettings {

    public LandClaimPlayerPluginSettings() {
        this.pluginLabel = LandClaim.name;
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
                flexWrapper.addChild(playerSettingShowCurrentChunk(uiPlayer));
                flexWrapper.addChild(playerSettingShowOwnedAreas(uiPlayer));
                flexWrapper.addChild(playerSettingShowOtherAreas(uiPlayer));
                flexWrapper.addChild(playerSettingEnableClaimInfoOverlay(uiPlayer));
                if (uiPlayer.isAdmin())
                    flexWrapper.addChild(playerSettingDeveloperMode(uiPlayer));
            }

            protected OZUIElement playerSettingShowCurrentChunk(Player uiPlayer) {
                OZUIElement element = defaultSettingsContainer();
                // label
                element.addChild(defaultSettingsLabel(t().get("TC_UI_LABEL_SHOW_CURRENT_CHUNK_FRAME", uiPlayer)));
                // current value
                String attributeKey = "oz.landclaim.showCurrentChunkFrame";
                Boolean isEnabled = uiPlayer.hasAttribute(attributeKey) ? (Boolean) uiPlayer.getAttribute(attributeKey)
                        : false;
                element.addChild(switchButtons(uiPlayer, isEnabled, event -> {
                    uiPlayer.setAttribute(attributeKey, !isEnabled);
                    redrawContent();
                    Area area = ChunkClaimUtil.getVirtualAreaFromChunkVector(uiPlayer.getChunkPosition());
                    Area3DUtils.updateCurrentChunkFrameForPlayer(uiPlayer, !isEnabled ? area : null);
                }));
                return element;
            }

            protected OZUIElement playerSettingShowOtherAreas(Player uiPlayer) {
                OZUIElement element = defaultSettingsContainer();
                // label
                element.addChild(defaultSettingsLabel(t().get("TC_UI_LABEL_SHOW_OTHER_AREAS", uiPlayer)));

                // toggle button
                String attributeKey = "oz.landclaim.showOtherAreaFrames";
                Boolean isEnabled = uiPlayer.hasAttribute(attributeKey) ? (Boolean) uiPlayer.getAttribute(attributeKey)
                        : false;
                element.addChild(switchButtons(uiPlayer, isEnabled, event -> {
                    uiPlayer.setAttribute(attributeKey, !isEnabled);
                    redrawContent();
                    Area3DUtils.updateAreaFramesForPlayer(uiPlayer);
                }));
                return element;
            }

            protected OZUIElement playerSettingShowOwnedAreas(Player uiPlayer) {
                OZUIElement element = defaultSettingsContainer();
                // label
                element.addChild(defaultSettingsLabel(t().get("TC_UI_LABEL_SHOW_OWNED_AREAS", uiPlayer)));

                // toggle button
                String attributeKey = "oz.landclaim.showOwnedAreaFrames";
                Boolean isEnabled = uiPlayer.hasAttribute(attributeKey) ? (Boolean) uiPlayer.getAttribute(attributeKey)
                        : false;
                element.addChild(switchButtons(uiPlayer, isEnabled, event -> {
                    uiPlayer.setAttribute(attributeKey, !isEnabled);
                    redrawContent();
                    Area3DUtils.updateAreaFramesForPlayer(uiPlayer);
                }));
                return element;
            }

            protected OZUIElement playerSettingEnableClaimInfoOverlay(Player uiPlayer) {
                OZUIElement element = defaultSettingsContainer();
                // label
                element.addChild(defaultSettingsLabel(t().get("TC_UI_LABEL_ENABLE_CLAIM_INFO_OVERLAY", uiPlayer)));

                // toggle button
                String attributeKey = "oz.landclaim.enableClaimInfoOverlay";
                Boolean isEnabled = uiPlayer.hasAttribute(attributeKey) ? (Boolean) uiPlayer.getAttribute(attributeKey)
                        : false;
                element.addChild(switchButtons(uiPlayer, isEnabled, event -> {
                    uiPlayer.setAttribute(attributeKey, !isEnabled);
                    redrawContent();
                }));
                return element;
            }

            protected OZUIElement playerSettingDeveloperMode(Player uiPlayer) {
                OZUIElement element = defaultSettingsContainer();
                // label
                element.addChild(defaultSettingsLabel(t().get("TC_UI_LABEL_DEVELOPER_MODE", uiPlayer)));

                // toggle button
                String attributeKey = "oz.landclaim.developerMode";
                Boolean isEnabled = uiPlayer.hasAttribute(attributeKey) ? (Boolean) uiPlayer.getAttribute(attributeKey)
                        : false;
                element.addChild(switchButtons(uiPlayer, isEnabled, event -> {
                    uiPlayer.setAttribute(attributeKey, !isEnabled);
                    redrawContent();
                }));
                return element;
            }
        };
    }

}
