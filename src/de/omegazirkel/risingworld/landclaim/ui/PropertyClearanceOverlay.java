package de.omegazirkel.risingworld.landclaim.ui;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import de.omegazirkel.risingworld.LandClaim;
import de.omegazirkel.risingworld.landclaim.PropertyClearanceService;
import de.omegazirkel.risingworld.landclaim.PropertyClearanceService.Preview;
import de.omegazirkel.risingworld.landclaim.PropertyClearanceService.ReturnItem;
import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.ui.AdvancedButton;
import de.omegazirkel.risingworld.tools.ui.AdvancedBaseButton;
import de.omegazirkel.risingworld.tools.ui.AdvancedButtonFactory;
import de.omegazirkel.risingworld.tools.ui.AdvancedButtonState;
import de.omegazirkel.risingworld.tools.ui.BasePluginOverlay;
import de.omegazirkel.risingworld.tools.ui.table.TableCell;
import de.omegazirkel.risingworld.tools.ui.table.TableRow;
import de.omegazirkel.risingworld.tools.ui.table.TableScrollView;
import net.risingworld.api.callbacks.Callback;
import net.risingworld.api.objects.Area;
import net.risingworld.api.objects.Player;
import net.risingworld.api.ui.UIElement;
import net.risingworld.api.ui.UILabel;
import net.risingworld.api.ui.UITarget;
import net.risingworld.api.ui.style.Pivot;
import net.risingworld.api.ui.style.TextAnchor;

/** Read-only clearance preview; mutations always use a fresh server snapshot. */
public final class PropertyClearanceOverlay extends BasePluginOverlay {
    public static final String ATTRIBUTE_KEY = "landclaim-property-clearance-overlay";
    private final Area area;
    private final PropertyClearanceService service;
    private final boolean administrator;
    private Preview preview;

    public PropertyClearanceOverlay(Player player, Area area, PropertyClearanceService service, Callback<Player> onClose) {
        this(player, area, service, onClose, false);
    }
    public PropertyClearanceOverlay(Player player, Area area, PropertyClearanceService service, Callback<Player> onClose,
            boolean administrator) {
        super(player, onClose);
        this.area = area;
        this.service = service;
        this.administrator = administrator;
        preview = service.preview(area, player.getLanguage());
        rebuild();
        populate();
    }

    @Override protected I18n t() { return I18n.getInstance(LandClaim.name); }
    @Override protected String titleText() { return t().get(administrator
            ? "tc.property.clearance.admin.title" : "tc.property.clearance.title", uiPlayer); }
    @Override protected String descriptionText() { return t().get("tc.property.clearance.subtitle", uiPlayer)
            .replace("PH_AREA_NAME", area.getName() == null ? "#" + area.getID() : area.getName()); }
    @Override protected String legendText() { return t().get("tc.property.clearance.legend", uiPlayer); }
    @Override protected void close() { uiPlayer.deleteAttribute(ATTRIBUTE_KEY); super.close(); }

    private void populate() {
        if (!preview.available()) { message(t().get("tc.property.clearance.unavailable", uiPlayer)); return; }
        TableScrollView table = new TableScrollView(Arrays.asList(t().get("tc.property.clearance.th.item", uiPlayer),
                t().get("tc.property.clearance.th.amount", uiPlayer)), Arrays.asList(76f, 24f));
        // Keep one complete table row clear of the fee summary below.
        table.setScrollBodyHeight(298f);
        for (ReturnItem item : preview.items()) table.addRow(new TableRow(Arrays.asList(
                label(item.key().displayName(uiPlayer.getLanguage()), 76f), label(String.valueOf(item.amount()), 24f))));
        Map<PropertyClearanceService.UnresolvedElement, Integer> unresolvedCounts = new LinkedHashMap<>();
        for (PropertyClearanceService.UnresolvedElement source : preview.unresolved()) unresolvedCounts.merge(source, 1, Integer::sum);
        for (Map.Entry<PropertyClearanceService.UnresolvedElement, Integer> unresolved : unresolvedCounts.entrySet()) table.addRow(new TableRow(Arrays.asList(
                label(t().get("tc.property.clearance.unresolved.item", uiPlayer)
                        .replace("PH_ITEM", unresolvedName(unresolved.getKey())), 76f),
                label(String.valueOf(unresolved.getValue()), 24f))));
        body.addChild(table);
        String feeKey = administrator ? "tc.property.clearance.admin.fees" : "tc.property.clearance.fees";
        UILabel fee = new UILabel(t().get(feeKey, uiPlayer).replace("PH_COUNT", String.valueOf(preview.elements().size()))
                .replace("PH_RESOURCES", String.valueOf(preview.resourceCount()))
                .replace("PH_DISPOSE", String.valueOf(fee(PropertyClearanceService.Mode.DISPOSE)))
                .replace("PH_DEMOLISH", String.valueOf(fee(PropertyClearanceService.Mode.DEMOLISH)))
                .replace("PH_RECYCLE", String.valueOf(service.recycleValue(preview)))
                .replace("PH_CURRENCY", service.defaultCurrencyIdentifier()));
        fee.setPivot(Pivot.UpperLeft); fee.setPosition(18, 352, false); fee.setSize(1_000, 32, false); fee.setTextWrap(true); body.addChild(fee);
        buttons();
    }
    private TableCell label(String text, float width) { UILabel label = new UILabel(text == null ? "" : text); label.setFontSize(13);
        label.setTextAlign(TextAnchor.MiddleLeft); label.setPivot(Pivot.MiddleLeft); label.setPosition(2, 50, true); return new TableCell(label, width); }
    private String unresolvedName(PropertyClearanceService.UnresolvedElement source) {
        if (source.container()) return t().get("tc.property.clearance.unresolved.container", uiPlayer);
        String name = source.name() == null || source.name().isBlank()
                ? t().get("tc.property.clearance.unresolved.unknown", uiPlayer)
                : new PropertyClearanceService.ItemKey(source.name(), 0, 0, (short) 0, "", 0).displayName(uiPlayer.getLanguage());
        String origin = new PropertyClearanceService.ItemKey(source.sourceName(), 0, 0, (short) 0, "", 0)
                .displayName(uiPlayer.getLanguage());
        if (source.construction()) return t().get("tc.property.clearance.unresolved.block", uiPlayer)
                .replace("PH_ITEM", name).replace("PH_SOURCE", origin).replace("PH_BLOCK_ID", String.valueOf(source.blockId()));
        return t().get("tc.property.clearance.unresolved.object", uiPlayer)
                .replace("PH_ITEM", name).replace("PH_SOURCE", origin).replace("PH_OBJECT_ID", String.valueOf(source.typeId()));
    }
    private void message(String value) { UILabel label = new UILabel(value); label.setPosition(18, 18, false); label.setSize(680, 60, false); label.setTextWrap(true); body.addChild(label); }
    private long fee(PropertyClearanceService.Mode mode) {
        if (administrator || mode == PropertyClearanceService.Mode.RECYCLE) return 0L;
        double rate = mode == PropertyClearanceService.Mode.DISPOSE
            ? de.omegazirkel.risingworld.landclaim.PluginSettings.getInstance().propertyClearanceDisposeFeePerItem
            : de.omegazirkel.risingworld.landclaim.PluginSettings.getInstance().propertyClearanceDemolishFeePerItem;
        return (long) Math.ceil(preview.resourceCount() * Math.max(0d, rate)); }
    private void buttons() {
        if (administrator) {
            button(18, 392, 105, AdvancedButtonFactory.defaultButton(t().get("tc.property.clearance.cancel", uiPlayer), event -> close()));
            boolean hasElements = preview.available() && !preview.elements().isEmpty();
            boolean resolved = hasElements && preview.unresolved().isEmpty();
            AdvancedButton recycle = actionButton(t().get("tc.property.clearance.recycle", uiPlayer), false,
                    event -> confirm(PropertyClearanceService.Mode.RECYCLE));
            if (!resolved || !service.isRecyclingAvailable() || service.recycleValue(preview) <= 0)
                recycle.setState(AdvancedBaseButton.State.DISABLED);
            button(130, 392, 115, recycle);
            AdvancedButton removeUnresolved = actionButton(t().get("tc.property.clearance.remove.unresolved", uiPlayer), true,
                    event -> confirm(PropertyClearanceService.Mode.REMOVE_UNRESOLVED));
            if (preview.unresolvedHostCount() == 0) removeUnresolved.setState(AdvancedBaseButton.State.DISABLED);
            button(252, 392, 270, removeUnresolved);
            AdvancedButton demolish = actionButton(t().get("tc.property.clearance.demolish", uiPlayer), false,
                    event -> confirm(PropertyClearanceService.Mode.DEMOLISH));
            if (!resolved) demolish.setState(AdvancedBaseButton.State.DISABLED);
            button(529, 392, 145, demolish);
            return;
        }
        button(18, 392, 105, AdvancedButtonFactory.defaultButton(t().get("tc.property.clearance.cancel", uiPlayer), event -> close()));
        boolean hasElements = preview.available() && !preview.elements().isEmpty();
        AdvancedButton dispose = actionButton(t().get("tc.property.clearance.dispose", uiPlayer), true,
                event -> confirm(PropertyClearanceService.Mode.DISPOSE));
        if (!hasElements) dispose.setState(AdvancedBaseButton.State.DISABLED);
        button(130, 392, 105, dispose);
        AdvancedButton recycle = actionButton(t().get("tc.property.clearance.recycle", uiPlayer), false,
                event -> confirm(PropertyClearanceService.Mode.RECYCLE));
        if (!hasElements || !preview.unresolved().isEmpty() || !service.isRecyclingAvailable() || service.recycleValue(preview) <= 0)
            recycle.setState(AdvancedBaseButton.State.DISABLED);
        button(242, 392, 105, recycle);
        AdvancedButton demolish = actionButton(t().get("tc.property.clearance.demolish", uiPlayer), false,
                event -> confirm(PropertyClearanceService.Mode.DEMOLISH));
        if (!hasElements || !preview.unresolved().isEmpty()) demolish.setState(AdvancedBaseButton.State.DISABLED);
        button(354, 392, 125, demolish);
        AdvancedButton removeUnresolved = actionButton(t().get("tc.property.clearance.remove.unresolved", uiPlayer), true,
                event -> confirm(PropertyClearanceService.Mode.REMOVE_UNRESOLVED));
        if (preview.unresolvedHostCount() == 0) removeUnresolved.setState(AdvancedBaseButton.State.DISABLED);
        button(486, 392, 195, removeUnresolved);
    }
    private AdvancedButton actionButton(String text, boolean danger, net.risingworld.api.callbacks.Callback<net.risingworld.api.events.player.ui.PlayerUIElementClickEvent> callback) {
        int color = danger ? 0xCC3333FF : 0x269F59FF;
        int hover = danger ? 0xDD4444FF : 0x32A05AFF;
        return AdvancedButtonFactory.custom(
                new AdvancedButtonState(AdvancedBaseButton.State.DEFAULT, 0x00000080, color, 0xFFFFFFFF, 0x000000AA, hover, text, callback),
                new AdvancedButtonState(AdvancedBaseButton.State.DISABLED, 0x47433CFF, 0x282724FF, 0x8B887FFF, 0x47433CFF, 0x282724FF, text, null));
    }
    private void button(String ignored, int x, AdvancedButton button) { button(x, 392, 145, button); }
    private void button(int x, int y, int width, AdvancedButton button) { button.setPivot(Pivot.UpperLeft); button.setPosition(x, y, false); button.setSize(width, 30, false); body.addChild(button); }
    private void confirm(PropertyClearanceService.Mode mode) {
        if (preview.elements().isEmpty() || ((mode == PropertyClearanceService.Mode.DEMOLISH || mode == PropertyClearanceService.Mode.RECYCLE)
                && !preview.unresolved().isEmpty()) || (mode == PropertyClearanceService.Mode.RECYCLE
                && (!service.isRecyclingAvailable() || service.recycleValue(preview) <= 0)) || (mode == PropertyClearanceService.Mode.REMOVE_UNRESOLVED
                && preview.unresolvedHostCount() == 0)) {
            uiPlayer.sendTextMessage(t().get("tc.property.clearance.unresolved", uiPlayer));
            return;
        }
        String key = administrator && mode == PropertyClearanceService.Mode.RECYCLE ? "tc.property.clearance.admin.recycle.confirm"
                : administrator && mode == PropertyClearanceService.Mode.REMOVE_UNRESOLVED ? "tc.property.clearance.admin.remove.unresolved.confirm"
                : administrator ? "tc.property.clearance.admin.confirm"
                : mode == PropertyClearanceService.Mode.DISPOSE ? "tc.property.clearance.dispose.confirm"
                : mode == PropertyClearanceService.Mode.RECYCLE ? "tc.property.clearance.recycle.confirm"
                : mode == PropertyClearanceService.Mode.REMOVE_UNRESOLVED ? "tc.property.clearance.remove.unresolved.confirm"
                : "tc.property.clearance.demolish.confirm";
        UIElement dialog = UIDialogFactory.getConfirmDangerDialog(uiPlayer, t().get(administrator
                ? "tc.property.clearance.admin.title" : "tc.property.clearance.confirm.title", uiPlayer),
                t().get(key, uiPlayer).replace("PH_COUNT", String.valueOf(mode == PropertyClearanceService.Mode.RECYCLE
                        ? preview.resourceCount() : mode == PropertyClearanceService.Mode.REMOVE_UNRESOLVED
                        ? preview.unresolvedHostCount() : preview.elements().size()))
                        .replace("PH_FEE", String.valueOf(fee(mode))).replace("PH_RECYCLE", String.valueOf(service.recycleValue(preview)))
                        .replace("PH_CURRENCY", service.defaultCurrencyIdentifier()),
                t().get(actionKey(mode), uiPlayer), accepted -> {
                    if (!accepted) return;
                    PropertyClearanceService.Result result = administrator && mode == PropertyClearanceService.Mode.RECYCLE ? service.adminRecycle(uiPlayer, area)
                            : administrator && mode == PropertyClearanceService.Mode.REMOVE_UNRESOLVED ? service.adminRemoveUnresolved(uiPlayer, area)
                            : administrator ? service.adminDemolish(uiPlayer, area)
                            : mode == PropertyClearanceService.Mode.DISPOSE ? service.dispose(uiPlayer, area)
                            : mode == PropertyClearanceService.Mode.RECYCLE ? service.recycle(uiPlayer, area)
                            : mode == PropertyClearanceService.Mode.REMOVE_UNRESOLVED ? service.removeUnresolved(uiPlayer, area) : service.demolish(uiPlayer, area);
                    if (result.success()) {
                        uiPlayer.sendTextMessage(t().get(mode == PropertyClearanceService.Mode.RECYCLE ? "tc.property.clearance.recycle.success"
                                : mode == PropertyClearanceService.Mode.REMOVE_UNRESOLVED ? "tc.property.clearance.remove.unresolved.success"
                                : "tc.property.clearance.success", uiPlayer)
                                .replace("PH_COUNT", String.valueOf(result.removed())).replace("PH_FEE", String.valueOf(result.fee()))
                                .replace("PH_RECYCLE", String.valueOf(result.recycledValue()))
                                .replace("PH_CURRENCY", service.defaultCurrencyIdentifier()));
                        close();
                    } else {
                        uiPlayer.showWarningMessageBox(t().get("tc.property.clearance.title", uiPlayer), failureMessage(result.reason()));
                        preview = service.preview(area, uiPlayer.getLanguage()); rebuild(); populate();
                    }
                }, ignored -> { });
        uiPlayer.addUIElement(dialog, UITarget.Modal);
    }
    private String actionKey(PropertyClearanceService.Mode mode) {
        return mode == PropertyClearanceService.Mode.DISPOSE ? "tc.property.clearance.dispose"
                : mode == PropertyClearanceService.Mode.RECYCLE ? "tc.property.clearance.recycle"
                : mode == PropertyClearanceService.Mode.REMOVE_UNRESOLVED ? "tc.property.clearance.remove.unresolved"
                : "tc.property.clearance.demolish";
    }
    private String failureMessage(String reason) {
        String normalized = reason == null ? "" : reason;
        String key = normalized.startsWith("UNRESOLVED_MATERIALS") ? "tc.property.clearance.reason.unresolved"
                : normalized.startsWith("MAIL_") ? "tc.property.clearance.reason.mail"
                : normalized.startsWith("PAYMENT_") ? "tc.property.clearance.reason.payment"
                : normalized.startsWith("SHOP_") ? "tc.property.clearance.reason.shop"
                : "tc.property.clearance.failed";
        return t().get(key, uiPlayer);
    }
}
