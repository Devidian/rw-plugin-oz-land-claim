package de.omegazirkel.risingworld.landclaim.ui;

import java.util.Arrays;
import java.util.List;

import de.omegazirkel.risingworld.LandClaim;
import de.omegazirkel.risingworld.landclaim.Area3DUtils;
import de.omegazirkel.risingworld.landclaim.ClaimCleanupService;
import de.omegazirkel.risingworld.landclaim.ClaimCleanupService.AreaSummary;
import de.omegazirkel.risingworld.landclaim.ClaimCleanupService.CleanupResult;
import de.omegazirkel.risingworld.landclaim.ClaimCleanupService.OwnerSummary;
import de.omegazirkel.risingworld.landclaim.ClaimCleanupService.SpecialAreaSummary;
import de.omegazirkel.risingworld.landclaim.DiscordConnect;
import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.ui.AdvancedButtonFactory;
import de.omegazirkel.risingworld.tools.ui.CursorManager;
import de.omegazirkel.risingworld.tools.ui.AdvancedButton;
import de.omegazirkel.risingworld.tools.ui.OZUIElement;
import de.omegazirkel.risingworld.tools.ui.table.TableCell;
import de.omegazirkel.risingworld.tools.ui.table.TableRow;
import de.omegazirkel.risingworld.tools.ui.table.TableScrollView;
import net.risingworld.api.callbacks.Callback;
import net.risingworld.api.objects.Player;
import net.risingworld.api.ui.UIElement;
import net.risingworld.api.ui.UILabel;
import net.risingworld.api.ui.UITarget;
import net.risingworld.api.ui.style.Font;
import net.risingworld.api.ui.style.Pivot;
import net.risingworld.api.ui.style.Position;
import net.risingworld.api.ui.style.TextAnchor;
import net.risingworld.api.ui.style.Unit;

public class AdminCleanupOverlay extends OZUIElement {
    public static final String ATTRIBUTE_KEY = "landclaim-admin-cleanup-overlay";

    private enum Tab {
        OWNERS, AREAS, SPECIAL_AREAS
    }

    private static final float PANEL_WIDTH_PERCENT = 88f;
    private static final float PANEL_HEIGHT_PIXELS = 620f;
    private static final float BODY_HEIGHT_PIXELS = 438f;
    private static final float TABLE_SCROLL_BODY_HEIGHT = 398f;

    private static I18n t() {
        return I18n.getInstance(LandClaim.name);
    }

    private final Player player;
    private final ClaimCleanupService cleanupService;
    private final Callback<Player> onClose;
    private Tab activeTab = Tab.OWNERS;
    private OZUIElement panel;
    private OZUIElement body;
    private OZUIElement ownersTab;
    private OZUIElement areasTab;
    private OZUIElement specialAreasTab;

    public AdminCleanupOverlay(Player player, ClaimCleanupService cleanupService, Callback<Player> onClose) {
        this.player = player;
        this.cleanupService = cleanupService;
        this.onClose = onClose;
        setClickable(false);
        setPivot(Pivot.UpperLeft);
        setSize(100, 100, true);
        setBackgroundColor(0, 0, 0, 0.4f);
        rebuild();
    }

    private void rebuild() {
        removeAllChilds();
        panel = new OZUIElement();
        panel.setPivot(Pivot.MiddleCenter);
        panel.setPosition(50f, 50f, true);
        panel.style.width.set(PANEL_WIDTH_PERCENT, Unit.Percent);
        panel.style.height.set(PANEL_HEIGHT_PIXELS, Unit.Pixel);
        panel.setBackgroundColor(0, 0, 0, 0.86f);
        panel.setBorderColor(0.95f, 0.75f, 0.25f, 0.6f);
        panel.setBorder(1);
        panel.setBorderEdgeRadius(6, false);
        addChild(panel);

        setupHeader();
        setupTabs();
        setupBody();
        setupFooter();
    }

    private void setupHeader() {
        UILabel title = new UILabel(t().get("TC_UI_ADMIN_CLEANUP_TITLE", player));
        title.setPivot(Pivot.UpperLeft);
        title.setPosition(24, 18, false);
        title.setFont(Font.DefaultBold);
        title.setFontSize(24);
        panel.addChild(title);

        UILabel subtitle = new UILabel(t().get("TC_UI_ADMIN_CLEANUP_SUBTITLE", player));
        subtitle.setPivot(Pivot.UpperLeft);
        subtitle.setPosition(24, 52, false);
        subtitle.setFont(Font.Default);
        subtitle.setFontSize(12);
        panel.addChild(subtitle);

        OZUIElement closeButton = new OZUIElement();
        closeButton.setPivot(Pivot.UpperRight);
        closeButton.style.position.set(Position.Absolute);
        closeButton.style.right.set(0, Unit.Pixel);
        closeButton.style.top.set(20, Unit.Pixel);
        closeButton.setSize(34, 34, false);
        closeButton.setBorder(1);
        closeButton.setBorderColor(0.95f, 0.75f, 0.25f, 0.54f);
        closeButton.setBorderEdgeRadius(4, false);
        closeButton.setBackgroundColor(0.12f, 0.10f, 0.08f, 0.9f);
        closeButton.setHoverBackgroundColor(0x611F1AF2);
        closeButton.setClickable(true);
        closeButton.setClickAction(event -> close());
        UILabel closeLabel = new UILabel("X");
        closeLabel.setPivot(Pivot.MiddleCenter);
        closeLabel.setPosition(50, 50, true);
        closeLabel.setSize(100, 100, true);
        closeLabel.setFont(Font.DefaultBold);
        closeLabel.setFontSize(18);
        closeLabel.setTextAlign(TextAnchor.MiddleCenter);
        closeButton.addChild(closeLabel);
        panel.addChild(closeButton);
    }

    private void setupTabs() {
        ownersTab = tab(t().get("TC_UI_ADMIN_CLEANUP_TAB_OWNERS", player), 24, 86, 180, () -> {
            activeTab = Tab.OWNERS;
            rebuild();
        });
        panel.addChild(ownersTab);

        areasTab = tab(t().get("TC_UI_ADMIN_CLEANUP_TAB_AREAS", player), 204, 86, 180, () -> {
            activeTab = Tab.AREAS;
            rebuild();
        });
        panel.addChild(areasTab);

        specialAreasTab = tab(t().get("TC_UI_ADMIN_CLEANUP_TAB_SPECIAL_AREAS", player), 384, 86, 220, () -> {
            activeTab = Tab.SPECIAL_AREAS;
            rebuild();
        });
        panel.addChild(specialAreasTab);

        applyTabStyles();
    }

    private OZUIElement tab(String text, float x, float y, float width, Runnable action) {
        OZUIElement tab = new OZUIElement();
        tab.setPivot(Pivot.UpperLeft);
        tab.setPosition(x, y, false);
        tab.setSize(width, 38, false);
        tab.setBorder(1);
        tab.setBorderEdgeRadius(4, false);
        tab.setClickable(true);
        tab.setClickAction(event -> action.run());

        UILabel label = new UILabel(text);
        label.setPivot(Pivot.MiddleCenter);
        label.setPosition(50, 50, true);
        label.setSize(100, 100, true);
        label.setFont(Font.DefaultBold);
        label.setFontSize(15);
        label.setTextAlign(TextAnchor.MiddleCenter);
        tab.addChild(label);
        return tab;
    }

    private void applyTabStyles() {
        styleTab(ownersTab, activeTab == Tab.OWNERS);
        styleTab(areasTab, activeTab == Tab.AREAS);
        styleTab(specialAreasTab, activeTab == Tab.SPECIAL_AREAS);
    }

    private void styleTab(OZUIElement tab, boolean active) {
        if (tab == null) {
            return;
        }
        if (active) {
            tab.setBackgroundColor(0.08f, 0.08f, 0.08f, 0.82f);
            tab.setBorderColor(0.95f, 0.75f, 0.25f, 0.74f);
        } else {
            tab.setBackgroundColor(0.10f, 0.10f, 0.10f, 0.38f);
            tab.setBorderColor(0.95f, 0.75f, 0.25f, 0.24f);
        }
    }

    private void setupBody() {
        body = new OZUIElement();
        body.setPivot(Pivot.UpperLeft);
        body.setPosition(24, 124, false);
        body.style.width.set(96, Unit.Percent);
        body.style.height.set(BODY_HEIGHT_PIXELS, Unit.Pixel);
        body.setBackgroundColor(0.08f, 0.08f, 0.08f, 0.55f);
        body.setBorder(1);
        body.setBorderColor(0.95f, 0.75f, 0.25f, 0.48f);
        body.setBorderEdgeRadius(4, false);
        panel.addChild(body);

        if (activeTab == Tab.OWNERS) {
            setupOwnerTable();
        } else if (activeTab == Tab.AREAS) {
            setupAreaTable();
        } else {
            setupSpecialAreaTable();
        }
    }

    private void setupOwnerTable() {
        TableScrollView table = new TableScrollView(
                Arrays.asList(
                        t().get("TC_UI_ADMIN_CLEANUP_TH_NAME", player),
                        t().get("TC_UI_ADMIN_CLEANUP_TH_CLAIMS", player),
                        t().get("TC_UI_ADMIN_CLEANUP_TH_MAX", player),
                        t().get("TC_UI_ADMIN_CLEANUP_TH_LAST_SEEN", player),
                        t().get("TC_UI_ADMIN_CLEANUP_TH_ACTIONS", player)),
                Arrays.asList(30f, 12f, 13f, 25f, 20f));
        table.setScrollBodyHeight(TABLE_SCROLL_BODY_HEIGHT);

        List<OwnerSummary> owners = cleanupService.getOwnerSummaries();
        if (owners.isEmpty()) {
            table.addRow(shortenOwnerRow(textOnlyRow(t().get("TC_UI_ADMIN_CLEANUP_EMPTY", player), 100f)));
        } else {
            for (OwnerSummary owner : owners) {
                table.addRow(shortenOwnerRow(ownerRow(owner)));
            }
        }
        body.addChild(table);
    }

    private void setupSpecialAreaTable() {
        TableScrollView table = new TableScrollView(
                Arrays.asList(
                        t().get("TC_UI_ADMIN_CLEANUP_TH_AREA", player),
                        t().get("TC_UI_ADMIN_CLEANUP_TH_CHUNKS", player),
                        t().get("TC_UI_ADMIN_CLEANUP_TH_OWNER", player),
                        t().get("TC_UI_ADMIN_CLEANUP_TH_ACTIONS", player)),
                Arrays.asList(38f, 17f, 25f, 20f));
        table.setScrollBodyHeight(TABLE_SCROLL_BODY_HEIGHT);

        List<SpecialAreaSummary> areas = cleanupService.getSpecialAreaSummaries();
        if (areas.isEmpty()) {
            table.addRow(textOnlyRow(t().get("TC_UI_ADMIN_CLEANUP_SPECIAL_EMPTY", player), 100f));
        } else {
            for (SpecialAreaSummary area : areas) {
                table.addRow(specialAreaRow(area));
            }
        }
        body.addChild(table);
    }

    private void setupAreaTable() {
        TableScrollView table = new TableScrollView(
                Arrays.asList(
                        t().get("TC_UI_ADMIN_CLEANUP_TH_AREA", player),
                        t().get("TC_UI_ADMIN_CLEANUP_TH_CHUNKS", player),
                        t().get("TC_UI_ADMIN_CLEANUP_TH_OWNER", player),
                        t().get("TC_UI_ADMIN_CLEANUP_TH_INACTIVE", player),
                        t().get("TC_UI_ADMIN_CLEANUP_TH_ACTIONS", player)),
                Arrays.asList(30f, 12f, 25f, 13f, 20f));
        table.setScrollBodyHeight(TABLE_SCROLL_BODY_HEIGHT);

        List<AreaSummary> areas = cleanupService.getAreaSummaries();
        if (areas.isEmpty()) {
            table.addRow(textOnlyRow(t().get("TC_UI_ADMIN_CLEANUP_EMPTY", player), 100f));
        } else {
            for (AreaSummary area : areas) {
                table.addRow(areaRow(area));
            }
        }
        body.addChild(table);
    }

    private TableRow ownerRow(OwnerSummary owner) {
        return new TableRow(Arrays.asList(
                labelCell(owner.ownerName(), 30f),
                labelCell(String.valueOf(owner.claimCount()), 12f),
                labelCell(String.valueOf(owner.maxClaims()), 13f),
                labelCell(formatLastSeen(owner.lastSeenEpochSeconds(), owner.inactiveDays()), 25f),
                new TableCell(ownerActions(owner), 20f)));
    }

    private TableRow areaRow(AreaSummary area) {
        return new TableRow(Arrays.asList(
                labelCell(area.areaName(), 30f),
                labelCell(String.valueOf(area.chunkCount()), 12f),
                labelCell(area.ownerName(), 25f),
                labelCell(formatLastSeen(area.lastSeenEpochSeconds(), area.inactiveDays()), 13f),
                new TableCell(areaActions(area), 20f)));
    }

    private TableRow specialAreaRow(SpecialAreaSummary area) {
        return new TableRow(Arrays.asList(
                labelCell(area.areaName(), 38f),
                labelCell(String.valueOf(area.chunkCount()), 17f),
                labelCell(t().get("TC_UI_ADMIN_CLEANUP_OWNER_SYSTEM", player), 25f),
                new TableCell(specialAreaActions(area), 20f)));
    }

    private TableRow textOnlyRow(String text, float width) {
        return new TableRow(Arrays.asList(labelCell(text, width)));
    }

    private TableRow shortenOwnerRow(TableRow row) {
        row.style.marginRight.set(8, Unit.Pixel);
        return row;
    }

    private TableCell labelCell(String text, float width) {
        UILabel label = new UILabel(text == null ? "" : text);
        label.setFontSize(13);
        label.setTextAlign(TextAnchor.MiddleLeft);
        label.setPivot(Pivot.MiddleLeft);
        label.setPosition(2, 50, true);
        return new TableCell(label, width);
    }

    private UIElement ownerActions(OwnerSummary owner) {
        OZUIElement actions = new OZUIElement();
        actions.setSize(100, 100, true);
        actions.addChild(actionButton("D", 0, () -> confirmOwnerDelete(owner)));
        actions.addChild(actionButton("C", 36, () -> confirmOwnerCleanup(owner)));
        return actions;
    }

    private UIElement areaActions(AreaSummary area) {
        OZUIElement actions = new OZUIElement();
        actions.setSize(100, 100, true);
        actions.addChild(actionButton("D", 0, () -> confirmAreaDelete(area)));
        actions.addChild(actionButton("C", 34, () -> confirmAreaCleanup(area)));
        actions.addChild(actionButton("T", 68, () -> teleport(area)));
        return actions;
    }

    private UIElement specialAreaActions(SpecialAreaSummary area) {
        OZUIElement actions = new OZUIElement();
        actions.setSize(100, 100, true);
        actions.addChild(actionButton("D", 0, () -> confirmSpecialAreaDelete(area)));
        actions.addChild(actionButton("C", 34, () -> confirmSpecialAreaCleanup(area)));
        actions.addChild(actionButton("T", 68, () -> teleport(area)));
        return actions;
    }

    private UIElement actionButton(String text, float leftPercent, Runnable action) {
        AdvancedButton button = AdvancedButtonFactory.defaultButton(text, event -> action.run());
        button.setPivot(Pivot.UpperLeft);
        button.setPosition(leftPercent, 10, true);
        button.setSize(28, 22, false);
        button.setBorderEdgeRadius(3, false);
        applyActionButtonColor(button, text);
        return button;
    }

    private void applyActionButtonColor(AdvancedButton button, String text) {
        if ("C".equals(text)) {
            button.setBackgroundColor(0.78f, 0.16f, 0.12f, 1f);
            button.setHoverBackgroundColor(0xDD3228FF);
        } else if ("D".equals(text)) {
            button.setBackgroundColor(0.95f, 0.52f, 0.12f, 1f);
            button.setHoverBackgroundColor(0xF47A1FFF);
        } else {
            button.setBackgroundColor(0.2f, 0.4f, 0.9f, 1f);
            button.setHoverBackgroundColor(0x3F70FFFF);
        }
        button.setBorderColor(0f, 0f, 0f, 0.4f);
    }

    private void confirmOwnerDelete(OwnerSummary owner) {
        showConfirm(
                t().get("TC_DIALOG_ADMIN_DELETE_OWNER_TITLE", player),
                t().get("TC_DIALOG_ADMIN_DELETE_OWNER_CONFIRM", player)
                        .replace("PH_PLAYER_NAME", owner.ownerName())
                        .replace("PH_CLAIM_COUNT", String.valueOf(owner.claimCount())),
                () -> handleCleanupResult(cleanupService.deleteOwner(owner.ownerUid()), owner.ownerName(), false));
    }

    private void confirmOwnerCleanup(OwnerSummary owner) {
        showConfirm(
                t().get("TC_DIALOG_ADMIN_CLEANUP_OWNER_TITLE", player),
                t().get("TC_DIALOG_ADMIN_CLEANUP_OWNER_CONFIRM", player)
                        .replace("PH_PLAYER_NAME", owner.ownerName())
                        .replace("PH_CLAIM_COUNT", String.valueOf(owner.claimCount())),
                () -> handleCleanupResult(cleanupService.cleanupOwner(owner.ownerUid()), owner.ownerName(), true));
    }

    private void confirmAreaDelete(AreaSummary area) {
        showConfirm(
                t().get("TC_DIALOG_ADMIN_DELETE_AREA_TITLE", player),
                t().get("TC_DIALOG_ADMIN_DELETE_AREA_CONFIRM", player)
                        .replace("PH_AREA_NAME", area.areaName()),
                () -> handleCleanupResult(cleanupService.deleteArea(area.areaId()), area.areaName(), false));
    }

    private void confirmAreaCleanup(AreaSummary area) {
        showConfirm(
                t().get("TC_DIALOG_ADMIN_CLEANUP_AREA_TITLE", player),
                t().get("TC_DIALOG_ADMIN_CLEANUP_AREA_CONFIRM", player)
                        .replace("PH_AREA_NAME", area.areaName()),
                () -> handleCleanupResult(cleanupService.cleanupArea(area.areaId()), area.areaName(), true));
    }

    private void confirmSpecialAreaDelete(SpecialAreaSummary area) {
        showConfirm(
                t().get("TC_DIALOG_ADMIN_DELETE_SPECIAL_AREA_TITLE", player),
                t().get("TC_DIALOG_ADMIN_DELETE_SPECIAL_AREA_CONFIRM", player)
                        .replace("PH_AREA_NAME", area.areaName()),
                () -> handleSpecialAreaResult(cleanupService.deleteSpecialArea(area.areaId()), area.areaName(), false));
    }

    private void confirmSpecialAreaCleanup(SpecialAreaSummary area) {
        showConfirm(
                t().get("TC_DIALOG_ADMIN_CLEANUP_SPECIAL_AREA_TITLE", player),
                t().get("TC_DIALOG_ADMIN_CLEANUP_SPECIAL_AREA_CONFIRM", player)
                        .replace("PH_AREA_NAME", area.areaName()),
                () -> handleSpecialAreaResult(cleanupService.cleanupSpecialArea(area.areaId()), area.areaName(), true));
    }

    private void teleport(AreaSummary area) {
        if (cleanupService.teleportToArea(player, area.areaId())) {
            player.sendTextMessage(t().get("TC_ADMIN_CLEANUP_TELEPORT_SUCCESS", player)
                    .replace("PH_AREA_NAME", area.areaName()));
        } else {
            player.sendTextMessage(t().get("TC_ADMIN_CLEANUP_TELEPORT_FAILED", player)
                    .replace("PH_AREA_NAME", area.areaName()));
        }
    }

    private void teleport(SpecialAreaSummary area) {
        if (cleanupService.teleportToArea(player, area.areaId())) {
            player.sendTextMessage(t().get("TC_ADMIN_CLEANUP_TELEPORT_SUCCESS", player)
                    .replace("PH_AREA_NAME", area.areaName()));
        } else {
            player.sendTextMessage(t().get("TC_ADMIN_CLEANUP_TELEPORT_FAILED", player)
                    .replace("PH_AREA_NAME", area.areaName()));
        }
    }

    private void showConfirm(String title, String message, Runnable onConfirm) {
        UIElement dialog = UIDialogFactory.getConfirmDangerDialog(player, title, message, confirmed -> {
            if (confirmed) {
                onConfirm.run();
            }
        }, p -> {
            CursorManager.show(p);
        });
        player.addUIElement(dialog, UITarget.HUD);
        CursorManager.show(player);
    }

    private void handleCleanupResult(CleanupResult result, String targetName, boolean resetChunks) {
        if (!result.success()) {
            player.sendTextMessage(t().get("TC_ADMIN_CLEANUP_BLOCKED", player)
                    .replace("PH_CHUNK_POS", String.valueOf(result.conflictChunk())));
            DiscordConnect.sendDiscordReleaseAccouncement(t().get("TC_DISCORD_ADMIN_CLEANUP_BLOCKED", DiscordConnect.botLang())
                    .replace("PH_TARGET", targetName)
                    .replace("PH_CHUNK_POS", String.valueOf(result.conflictChunk()))
                    .replace("PH_ADMIN_NAME", player.getName()));
            CursorManager.show(player);
            return;
        }
        Area3DUtils.updateAreaFramesForAllPlayers();
        String messageKey = resetChunks ? "TC_ADMIN_CLEANUP_SUCCESS" : "TC_ADMIN_DELETE_SUCCESS";
        player.sendTextMessage(t().get(messageKey, player)
                .replace("PH_TARGET", targetName)
                .replace("PH_CLAIM_COUNT", String.valueOf(result.claimsAffected()))
                .replace("PH_RESET_COUNT", String.valueOf(result.chunksReset())));
        String discordKey = resetChunks ? "TC_DISCORD_ADMIN_CLEANUP" : "TC_DISCORD_ADMIN_DELETE";
        DiscordConnect.sendDiscordReleaseAccouncement(t().get(discordKey, DiscordConnect.botLang())
                .replace("PH_TARGET", targetName)
                .replace("PH_CLAIM_COUNT", String.valueOf(result.claimsAffected()))
                .replace("PH_RESET_COUNT", String.valueOf(result.chunksReset()))
                .replace("PH_ADMIN_NAME", player.getName()));
        CursorManager.show(player);
        rebuild();
    }

    private void handleSpecialAreaResult(CleanupResult result, String targetName, boolean resetChunks) {
        Area3DUtils.updateAreaFramesForAllPlayers();
        String messageKey = resetChunks ? "TC_ADMIN_CLEANUP_SPECIAL_AREA_SUCCESS" : "TC_ADMIN_DELETE_SPECIAL_AREA_SUCCESS";
        player.sendTextMessage(t().get(messageKey, player)
                .replace("PH_TARGET", targetName)
                .replace("PH_RESET_COUNT", String.valueOf(result.chunksReset())));
        String discordKey = resetChunks ? "TC_DISCORD_ADMIN_CLEANUP_SPECIAL_AREA" : "TC_DISCORD_ADMIN_DELETE_SPECIAL_AREA";
        DiscordConnect.sendDiscordReleaseAccouncement(t().get(discordKey, DiscordConnect.botLang())
                .replace("PH_TARGET", targetName)
                .replace("PH_RESET_COUNT", String.valueOf(result.chunksReset()))
                .replace("PH_ADMIN_NAME", player.getName()));
        CursorManager.show(player);
        rebuild();
    }

    private String formatLastSeen(long lastSeenEpochSeconds, long inactiveDays) {
        if (lastSeenEpochSeconds <= 0) {
            return t().get("TC_UI_ADMIN_CLEANUP_UNKNOWN", player);
        }
        if (inactiveDays == 0) {
            return t().get("TC_UI_ADMIN_CLEANUP_TODAY", player);
        }
        return t().get("TC_UI_ADMIN_CLEANUP_DAYS", player).replace("PH_DAYS", String.valueOf(inactiveDays));
    }

    private void setupFooter() {
        UILabel legend = new UILabel(t().get("TC_UI_ADMIN_CLEANUP_ACTION_LEGEND", player));
        legend.setPivot(Pivot.LowerLeft);
        legend.setPosition(24, PANEL_HEIGHT_PIXELS - 18, false);
        legend.setFontSize(12);
        panel.addChild(legend);
    }

    private void close() {
        player.removeUIElement(this);
        CursorManager.hide(player);
        player.deleteAttribute(ATTRIBUTE_KEY);
        onClose.onCall(player);
    }
}
