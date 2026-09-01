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
import de.omegazirkel.risingworld.tools.ui.BasePluginOverlayWithTabs;
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
import net.risingworld.api.ui.style.Pivot;
import net.risingworld.api.ui.style.TextAnchor;
import net.risingworld.api.ui.style.Unit;

public class AdminCleanupOverlay extends BasePluginOverlayWithTabs {
    public static final String ATTRIBUTE_KEY = "landclaim-admin-cleanup-overlay";

    private enum Tab {
        OWNERS, AREAS, SPECIAL_AREAS
    }

    private static final float TABLE_SCROLL_BODY_HEIGHT = 398f;

    private final Player player;
    private final ClaimCleanupService cleanupService;
    private Tab activeTab = Tab.OWNERS;

    public AdminCleanupOverlay(Player player, ClaimCleanupService cleanupService, Callback<Player> onClose) {
        super(player, onClose);
        this.player = player;
        this.cleanupService = cleanupService;
        rebuild();
    }

    @Override
    protected I18n t() {
        return I18n.getInstance(LandClaim.name);
    }

    @Override
    protected String titleText() {
        return t().get("tc.ui.admin.cleanup.title", player);
    }

    @Override
    protected String descriptionText() {
        return t().get("tc.ui.admin.cleanup.subtitle", player);
    }

    @Override
    protected String legendText() {
        return t().get("tc.ui.admin.cleanup.action.legend", player);
    }

    @Override
    protected void setupTabs() {
        setupTabContainer();
        addTab(t().get("tc.ui.admin.cleanup.tab.owners", player), 180, activeTab == Tab.OWNERS, true, () -> {
            activeTab = Tab.OWNERS;
            rebuild();
        });
        addTab(t().get("tc.ui.admin.cleanup.tab.areas", player), 180, activeTab == Tab.AREAS, true, () -> {
            activeTab = Tab.AREAS;
            rebuild();
        });
        addTab(t().get("tc.ui.admin.cleanup.tab.special.areas", player), 220, activeTab == Tab.SPECIAL_AREAS, true, () -> {
            activeTab = Tab.SPECIAL_AREAS;
            rebuild();
        });
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
                        t().get("tc.ui.admin.cleanup.th.name", player),
                        t().get("tc.ui.admin.cleanup.th.claims", player),
                        t().get("tc.ui.admin.cleanup.th.max", player),
                        t().get("tc.ui.admin.cleanup.th.last.seen", player),
                        t().get("tc.ui.admin.cleanup.th.actions", player)),
                Arrays.asList(30f, 12f, 13f, 25f, 20f));
        table.setScrollBodyHeight(TABLE_SCROLL_BODY_HEIGHT);

        List<OwnerSummary> owners = cleanupService.getOwnerSummaries();
        if (owners.isEmpty()) {
            table.addRow(shortenOwnerRow(textOnlyRow(t().get("tc.ui.admin.cleanup.empty", player), 100f)));
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
                        t().get("tc.ui.admin.cleanup.th.area", player),
                        t().get("tc.ui.admin.cleanup.th.chunks", player),
                        t().get("tc.ui.admin.cleanup.th.owner", player),
                        t().get("tc.ui.admin.cleanup.th.actions", player)),
                Arrays.asList(38f, 17f, 25f, 20f));
        table.setScrollBodyHeight(TABLE_SCROLL_BODY_HEIGHT);

        List<SpecialAreaSummary> areas = cleanupService.getSpecialAreaSummaries();
        if (areas.isEmpty()) {
            table.addRow(textOnlyRow(t().get("tc.ui.admin.cleanup.special.empty", player), 100f));
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
                        t().get("tc.ui.admin.cleanup.th.area", player),
                        t().get("tc.ui.admin.cleanup.th.chunks", player),
                        t().get("tc.ui.admin.cleanup.th.owner", player),
                        t().get("tc.ui.admin.cleanup.th.inactive", player),
                        t().get("tc.ui.admin.cleanup.th.actions", player)),
                Arrays.asList(30f, 12f, 25f, 13f, 20f));
        table.setScrollBodyHeight(TABLE_SCROLL_BODY_HEIGHT);

        List<AreaSummary> areas = cleanupService.getAreaSummaries();
        if (areas.isEmpty()) {
            table.addRow(textOnlyRow(t().get("tc.ui.admin.cleanup.empty", player), 100f));
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
                labelCell(t().get("tc.ui.admin.cleanup.owner.system", player), 25f),
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
                t().get("tc.dialog.admin.delete.owner.title", player),
                t().get("tc.dialog.admin.delete.owner.confirm", player)
                        .replace("PH_PLAYER_NAME", owner.ownerName())
                        .replace("PH_CLAIM_COUNT", String.valueOf(owner.claimCount())),
                () -> handleCleanupResult(cleanupService.deleteOwner(owner.ownerUid()), owner.ownerName(), false));
    }

    private void confirmOwnerCleanup(OwnerSummary owner) {
        showConfirm(
                t().get("tc.dialog.admin.cleanup.owner.title", player),
                t().get("tc.dialog.admin.cleanup.owner.confirm", player)
                        .replace("PH_PLAYER_NAME", owner.ownerName())
                        .replace("PH_CLAIM_COUNT", String.valueOf(owner.claimCount())),
                () -> handleCleanupResult(cleanupService.cleanupOwner(owner.ownerUid()), owner.ownerName(), true));
    }

    private void confirmAreaDelete(AreaSummary area) {
        showConfirm(
                t().get("tc.dialog.admin.delete.area.title", player),
                t().get("tc.dialog.admin.delete.area.confirm", player)
                        .replace("PH_AREA_NAME", area.areaName()),
                () -> handleCleanupResult(cleanupService.deleteArea(area.areaId()), area.areaName(), false));
    }

    private void confirmAreaCleanup(AreaSummary area) {
        showConfirm(
                t().get("tc.dialog.admin.cleanup.area.title", player),
                t().get("tc.dialog.admin.cleanup.area.confirm", player)
                        .replace("PH_AREA_NAME", area.areaName()),
                () -> handleCleanupResult(cleanupService.cleanupArea(area.areaId()), area.areaName(), true));
    }

    private void confirmSpecialAreaDelete(SpecialAreaSummary area) {
        showConfirm(
                t().get("tc.dialog.admin.delete.special.area.title", player),
                t().get("tc.dialog.admin.delete.special.area.confirm", player)
                        .replace("PH_AREA_NAME", area.areaName()),
                () -> handleSpecialAreaResult(cleanupService.deleteSpecialArea(area.areaId()), area.areaName(), false));
    }

    private void confirmSpecialAreaCleanup(SpecialAreaSummary area) {
        showConfirm(
                t().get("tc.dialog.admin.cleanup.special.area.title", player),
                t().get("tc.dialog.admin.cleanup.special.area.confirm", player)
                        .replace("PH_AREA_NAME", area.areaName()),
                () -> handleSpecialAreaResult(cleanupService.cleanupSpecialArea(area.areaId()), area.areaName(), true));
    }

    private void teleport(AreaSummary area) {
        if (cleanupService.teleportToArea(player, area.areaId())) {
            player.sendTextMessage(t().get("tc.admin.cleanup.teleport.success", player)
                    .replace("PH_AREA_NAME", area.areaName()));
        } else {
            player.sendTextMessage(t().get("tc.admin.cleanup.teleport.failed", player)
                    .replace("PH_AREA_NAME", area.areaName()));
        }
    }

    private void teleport(SpecialAreaSummary area) {
        if (cleanupService.teleportToArea(player, area.areaId())) {
            player.sendTextMessage(t().get("tc.admin.cleanup.teleport.success", player)
                    .replace("PH_AREA_NAME", area.areaName()));
        } else {
            player.sendTextMessage(t().get("tc.admin.cleanup.teleport.failed", player)
                    .replace("PH_AREA_NAME", area.areaName()));
        }
    }

    private void showConfirm(String title, String message, Runnable onConfirm) {
        UIElement dialog = UIDialogFactory.getConfirmDangerDialog(player, title, message, confirmed -> {
            if (confirmed) {
                onConfirm.run();
            }
        }, p -> {
        });
        player.addUIElement(dialog, UITarget.Modal);
    }

    private void handleCleanupResult(CleanupResult result, String targetName, boolean resetChunks) {
        if (!result.success()) {
            player.sendTextMessage(t().get("tc.admin.cleanup.blocked", player)
                    .replace("PH_CHUNK_POS", String.valueOf(result.conflictChunk())));
            DiscordConnect.sendDiscordReleaseAccouncement(t().get("tc.discord.admin.cleanup.blocked", DiscordConnect.botLang())
                    .replace("PH_TARGET", targetName)
                    .replace("PH_CHUNK_POS", String.valueOf(result.conflictChunk()))
                    .replace("PH_ADMIN_NAME", player.getName()));
            return;
        }
        Area3DUtils.updateAreaFramesForAllPlayers();
        String messageKey = resetChunks ? "tc.admin.cleanup.success" : "tc.admin.delete.success";
        player.sendTextMessage(t().get(messageKey, player)
                .replace("PH_TARGET", targetName)
                .replace("PH_CLAIM_COUNT", String.valueOf(result.claimsAffected()))
                .replace("PH_RESET_COUNT", String.valueOf(result.chunksReset())));
        String discordKey = resetChunks ? "tc.discord.admin.cleanup" : "tc.discord.admin.delete";
        DiscordConnect.sendDiscordReleaseAccouncement(t().get(discordKey, DiscordConnect.botLang())
                .replace("PH_TARGET", targetName)
                .replace("PH_CLAIM_COUNT", String.valueOf(result.claimsAffected()))
                .replace("PH_RESET_COUNT", String.valueOf(result.chunksReset()))
                .replace("PH_ADMIN_NAME", player.getName()));
        rebuild();
    }

    private void handleSpecialAreaResult(CleanupResult result, String targetName, boolean resetChunks) {
        Area3DUtils.updateAreaFramesForAllPlayers();
        String messageKey = resetChunks ? "tc.admin.cleanup.special.area.success" : "tc.admin.delete.special.area.success";
        player.sendTextMessage(t().get(messageKey, player)
                .replace("PH_TARGET", targetName)
                .replace("PH_RESET_COUNT", String.valueOf(result.chunksReset())));
        String discordKey = resetChunks ? "tc.discord.admin.cleanup.special.area" : "tc.discord.admin.delete.special.area";
        DiscordConnect.sendDiscordReleaseAccouncement(t().get(discordKey, DiscordConnect.botLang())
                .replace("PH_TARGET", targetName)
                .replace("PH_RESET_COUNT", String.valueOf(result.chunksReset()))
                .replace("PH_ADMIN_NAME", player.getName()));
        rebuild();
    }

    private String formatLastSeen(long lastSeenEpochSeconds, long inactiveDays) {
        if (lastSeenEpochSeconds <= 0) {
            return t().get("tc.ui.admin.cleanup.unknown", player);
        }
        if (inactiveDays == 0) {
            return t().get("tc.ui.admin.cleanup.today", player);
        }
        return t().get("tc.ui.admin.cleanup.days", player).replace("PH_DAYS", String.valueOf(inactiveDays));
    }

    @Override
    protected void close() {
        player.deleteAttribute(ATTRIBUTE_KEY);
        super.close();
    }
}
