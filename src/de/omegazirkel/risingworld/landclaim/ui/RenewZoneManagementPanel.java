package de.omegazirkel.risingworld.landclaim.ui;

import java.util.Arrays;
import java.util.List;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import de.omegazirkel.risingworld.LandClaim;
import de.omegazirkel.risingworld.landclaim.PluginSettings;
import de.omegazirkel.risingworld.landclaim.RenewZoneResetService;
import de.omegazirkel.risingworld.landclaim.db.RenewZoneConfig;
import de.omegazirkel.risingworld.landclaim.db.RenewZoneConfigService;
import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.ui.AdvancedButton;
import de.omegazirkel.risingworld.tools.ui.AdvancedButtonFactory;
import de.omegazirkel.risingworld.tools.ui.OZUIElement;
import de.omegazirkel.risingworld.tools.ui.SwitchButton;
import de.omegazirkel.risingworld.tools.ui.table.TableCell;
import de.omegazirkel.risingworld.tools.ui.table.TableRow;
import de.omegazirkel.risingworld.tools.ui.table.TableScrollView;
import net.risingworld.api.Server;
import net.risingworld.api.objects.Area;
import net.risingworld.api.objects.Player;
import net.risingworld.api.ui.UIElement;
import net.risingworld.api.ui.UILabel;
import net.risingworld.api.ui.UITarget;
import net.risingworld.api.ui.style.Pivot;
import net.risingworld.api.ui.style.TextAnchor;

/** Renew-zone tab content for the established land-management overlay. */
public final class RenewZoneManagementPanel extends UIElement {
    private static final DateTimeFormatter NEXT_RESET_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
            .withZone(ZoneId.systemDefault());
    private final Player player;
    private final RenewZoneConfigService configs;
    private final RenewZoneResetService resetter;
    private final PluginSettings settings;

    public RenewZoneManagementPanel(Player player, RenewZoneConfigService configs, RenewZoneResetService resetter,
            PluginSettings settings) {
        this.player = player; this.configs = configs; this.resetter = resetter; this.settings = settings;
        setSize(100, 100, true); rebuild();
    }
    private I18n t() { return I18n.getInstance(LandClaim.name); }
    private void rebuild() { removeAllChilds(); controls(); table(); }
    private void controls() {
        addChild(label(t().get("tc.renew.zone.management.enabled", player), 20, 18, 210, 28));
        SwitchButton enabled = new SwitchButton(Boolean.TRUE.equals(settings.enableRenewZones), value -> {
            if (settings.saveAdminSetting("enableRenewZones", String.valueOf(value))) settings.enableRenewZones = value;
            rebuild();
        }); position(enabled, 245, 20, 60, 22); addChild(enabled);
        AdvancedButton interval = AdvancedButtonFactory.defaultButton(t().get("tc.renew.zone.management.all.interval", player), e -> editAll());
        position(interval, 335, 15, 220, 36); addChild(interval);
        AdvancedButton reset = AdvancedButtonFactory.defaultButton(t().get("tc.renew.zone.management.all.reset", player), e -> confirmAll());
        position(reset, 575, 15, 200, 36); addChild(reset);
    }
    private void table() {
        TableScrollView table = new TableScrollView(Arrays.asList(t().get("tc.renew.zone.management.th.area", player),
                t().get("tc.renew.zone.management.th.interval", player), t().get("tc.renew.zone.management.th.next", player),
                t().get("tc.renew.zone.management.th.actions", player)), Arrays.asList(42f, 16f, 25f, 17f));
        table.setPosition(0, 62, false);
        table.setScrollBodyHeight(336f);
        List<RenewZoneConfig> entries = configs.list();
        if (entries.isEmpty()) table.addRow(new TableRow(Arrays.asList(cell(t().get("tc.renew.zone.management.empty", player), 100))));
        for (RenewZoneConfig config : entries) table.addRow(row(config)); addChild(table);
    }
    private TableRow row(RenewZoneConfig config) {
        Area area = Server.getArea(config.areaId());
        String name = area == null ? t().get("tc.renew.zone.management.missing", player) + " #" + config.areaId()
                : (area.getName() == null || area.getName().isBlank() ? "#" + area.getID() : area.getName());
        OZUIElement actions = new OZUIElement(); actions.setSize(100, 100, true);
        actions.addChild(button("✎", 0, () -> rename(area))); actions.addChild(button("◷", 34, () -> edit(config)));
        actions.addChild(button("▶", 68, () -> confirm(t().get("tc.renew.zone.management.confirm.one", player), () -> resetter.resetArea(config.areaId(), System.currentTimeMillis()))));
        long nextReset = config.lastResetAt() + config.intervalHours() * 3_600_000L;
        return new TableRow(Arrays.asList(cell(name, 42), cell(String.valueOf(config.intervalHours()), 16),
                cell(NEXT_RESET_FORMAT.format(Instant.ofEpochMilli(nextReset)), 25), new TableCell(actions, 17)));
    }
    private void rename(Area area) { if (area == null) return; input(t().get("tc.renew.zone.management.rename", player), area.getName(), value -> { if (!value.isBlank()) area.setName(value); rebuild(); }); }
    private void edit(RenewZoneConfig config) { input(t().get("tc.renew.zone.management.interval", player), String.valueOf(config.intervalHours()), value -> { int hours = positive(value); if (hours > 0) configs.save(config.areaId(), hours, config.lastResetAt()); rebuild(); }); }
    private void editAll() { input(t().get("tc.renew.zone.management.all.interval", player), String.valueOf(settings.renewZoneDefaultIntervalHours), value -> { int hours = positive(value); if (hours > 0) confirm(t().get("tc.renew.zone.management.confirm.interval", player), () -> configs.updateAllIntervals(hours)); }); }
    private void confirmAll() { confirm(t().get("tc.renew.zone.management.confirm.all", player), () -> resetter.resetAll(System.currentTimeMillis())); }
    private void input(String title, String current, net.risingworld.api.callbacks.Callback<String> ok) { player.addUIElement(UIDialogFactory.getTextInput(player, title, current, ok, p -> { }), UITarget.Modal); }
    private void confirm(String text, Runnable action) { player.addUIElement(UIDialogFactory.getConfirmDangerDialogText(player, t().get("tc.renew.zone.management.confirm.title", player), text, 120, accepted -> { if (accepted) { action.run(); rebuild(); } }, p -> { }), UITarget.Modal); }
    private int positive(String value) { try { return Math.max(0, Integer.parseInt(value.trim())); } catch (RuntimeException e) { return 0; } }
    private AdvancedButton button(String text, float x, Runnable action) { AdvancedButton b = AdvancedButtonFactory.defaultButton(text, e -> action.run()); position(b, x, 5, 28, 22); return b; }
    private TableCell cell(String text, float width) { UILabel l = label(text, 2, 50, 0, 0); l.setTextAlign(TextAnchor.MiddleLeft); l.setPivot(Pivot.MiddleLeft); return new TableCell(l, width); }
    private UILabel label(String text, float x, float y, float width, float height) { UILabel l = new UILabel(text); l.setFontSize(13); l.setPivot(Pivot.UpperLeft); l.setPosition(x, y, false); if (width > 0) l.setSize(width, height, false); return l; }
    private void position(UIElement e, float x, float y, float width, float height) { e.setPivot(Pivot.UpperLeft); e.setPosition(x, y, false); e.setSize(width, height, false); }
}
