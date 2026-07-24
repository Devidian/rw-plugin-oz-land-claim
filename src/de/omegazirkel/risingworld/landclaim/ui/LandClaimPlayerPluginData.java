package de.omegazirkel.risingworld.landclaim.ui;

import java.util.ArrayList;
import java.util.Arrays;

import de.omegazirkel.risingworld.LandClaim;
import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.ui.BasePlayerPluginDataPanel;
import de.omegazirkel.risingworld.tools.ui.PlayerPluginData;
import de.omegazirkel.risingworld.tools.ui.table.TableCell;
import de.omegazirkel.risingworld.tools.ui.table.TableRow;
import de.omegazirkel.risingworld.tools.ui.table.TableScrollView;
import net.risingworld.api.objects.Player;
import net.risingworld.api.ui.UILabel;
import net.risingworld.api.ui.style.Font;
import net.risingworld.api.ui.style.TextAnchor;
import net.risingworld.api.ui.style.Unit;

public class LandClaimPlayerPluginData extends PlayerPluginData {

    public LandClaimPlayerPluginData(String pluginVersion) {
        this.pluginLabel = LandClaim.name;
        this.pluginVersion = pluginVersion;
    }

    private I18n t() {
        return I18n.getInstance(LandClaim.name);
    }

    @Override
    public BasePlayerPluginDataPanel createPlayerPluginDataUIElement(Player uiPlayer) {
        return new BasePlayerPluginDataPanel(uiPlayer, pluginLabel) {
            @Override
            protected void redrawContent() {
                flexWrapper.removeAllChilds();

                TableScrollView table = new TableScrollView(
                        Arrays.asList(
                                t().get("TC_DATA_COL_DESCRIPTION", uiPlayer),
                                "key",
                                "value"),
                        Arrays.asList(38f, 42f, 20f));
                table.setPosition(0, 0, false);
                table.style.width.set(100, Unit.Percent);
                table.setScrollBodyHeight(320);

                addRow(table, t().get("TC_UI_LABEL_SHOW_CURRENT_CHUNK_FRAME", uiPlayer),
                        LandClaimPlayerPluginSettings.SHOW_CURRENT_CHUNK_FRAME_KEY,
                        booleanValue(uiPlayer, LandClaimPlayerPluginSettings.SHOW_CURRENT_CHUNK_FRAME_KEY, false));
                addRow(table, t().get("TC_UI_LABEL_SHOW_OWNED_AREAS", uiPlayer),
                        LandClaimPlayerPluginSettings.SHOW_OWNED_AREA_FRAMES_KEY,
                        booleanValue(uiPlayer, LandClaimPlayerPluginSettings.SHOW_OWNED_AREA_FRAMES_KEY, false));
                addRow(table, t().get("TC_UI_LABEL_SHOW_OTHER_AREAS", uiPlayer),
                        LandClaimPlayerPluginSettings.SHOW_OTHER_AREA_FRAMES_KEY,
                        booleanValue(uiPlayer, LandClaimPlayerPluginSettings.SHOW_OTHER_AREA_FRAMES_KEY, false));
                addRow(table, t().get("TC_UI_LABEL_AREA_FRAME_CHUNK_RADIUS", uiPlayer),
                        LandClaimPlayerPluginSettings.AREA_FRAME_CHUNK_RADIUS_KEY,
                        String.valueOf(LandClaimPlayerPluginSettings.areaFrameChunkRadius(uiPlayer)));
                addRow(table, t().get("TC_UI_LABEL_ENABLE_CLAIM_INFO_OVERLAY", uiPlayer),
                        LandClaimPlayerPluginSettings.ENABLE_CLAIM_INFO_OVERLAY_KEY,
                        booleanValue(uiPlayer, LandClaimPlayerPluginSettings.ENABLE_CLAIM_INFO_OVERLAY_KEY, true));
                if (uiPlayer.isAdmin()) {
                    addRow(table, t().get("TC_UI_LABEL_DEVELOPER_MODE", uiPlayer),
                            LandClaimPlayerPluginSettings.DEVELOPER_MODE_KEY,
                            booleanValue(uiPlayer, LandClaimPlayerPluginSettings.DEVELOPER_MODE_KEY, false));
                }

                flexWrapper.addChild(table.getRoot());
            }

            private void addRow(TableScrollView table, String description, String key, String value) {
                table.addRow(new TableRow(new ArrayList<>(Arrays.asList(
                        cell(description, 38f),
                        cell(key, 42f),
                        cell(value, 20f)))));
            }

            private String booleanValue(Player player, String key, boolean defaultValue) {
                return String.valueOf(LandClaimPlayerPluginSettings.booleanValue(player, key, defaultValue));
            }

            private TableCell cell(String text, float width) {
                UILabel label = new UILabel(text == null ? "" : text);
                label.setFont(Font.Default);
                label.setFontSize(13);
                label.setTextWrap(false);
                label.setTextAlign(TextAnchor.MiddleLeft);
                return new TableCell(label, width);
            }
        };
    }
}
