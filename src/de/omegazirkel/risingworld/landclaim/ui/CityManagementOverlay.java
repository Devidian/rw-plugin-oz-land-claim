package de.omegazirkel.risingworld.landclaim.ui;

import java.util.Arrays;
import java.util.List;

import de.omegazirkel.risingworld.LandClaim;
import de.omegazirkel.risingworld.landclaim.EconomyIntegration;
import de.omegazirkel.risingworld.landclaim.PluginSettings;
import de.omegazirkel.risingworld.landclaim.db.CityRecord;
import de.omegazirkel.risingworld.landclaim.db.CityService;
import de.omegazirkel.risingworld.landclaim.db.LandPriceService;
import de.omegazirkel.risingworld.landclaim.db.LeaseholdSummary;
import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.ui.AdvancedButton;
import de.omegazirkel.risingworld.tools.ui.AdvancedButtonFactory;
import de.omegazirkel.risingworld.tools.ui.AdvancedButtonState;
import de.omegazirkel.risingworld.tools.ui.AdvancedBaseButton.State;
import de.omegazirkel.risingworld.tools.ui.BasePluginOverlayWithTabs;
import de.omegazirkel.risingworld.tools.ui.OZUIElement;
import de.omegazirkel.risingworld.tools.ui.table.TableCell;
import de.omegazirkel.risingworld.tools.ui.table.TableRow;
import de.omegazirkel.risingworld.tools.ui.table.TableScrollView;
import net.risingworld.api.Server;
import net.risingworld.api.callbacks.Callback;
import net.risingworld.api.objects.Area;
import net.risingworld.api.objects.Player;
import net.risingworld.api.ui.UIElement;
import net.risingworld.api.ui.UILabel;
import net.risingworld.api.ui.UITextField;
import net.risingworld.api.ui.UITarget;
import net.risingworld.api.ui.style.Pivot;
import net.risingworld.api.ui.style.TextAnchor;
import net.risingworld.api.utils.Vector3f;

public final class CityManagementOverlay extends BasePluginOverlayWithTabs {
    public static final String ATTRIBUTE_KEY = "landclaim-city-management-overlay";
    private static final int PAGE_SIZE = 50;
    private enum Tab { CURRENT, OTHERS }

    private final Player player;
    private final CityService cities;
    private final EconomyIntegration economy;
    private final PluginSettings settings = PluginSettings.getInstance();
    private final long currentCityAreaId;
    private Tab activeTab = Tab.CURRENT;
    private String search = "";
    private int offset;

    public CityManagementOverlay(Player player, CityService cities, EconomyIntegration economy,
            long currentCityAreaId, Callback<Player> onClose) {
        super(player, onClose);
        this.player = player;
        this.cities = cities;
        this.economy = economy;
        this.currentCityAreaId = currentCityAreaId;
        rebuild();
    }

    @Override protected I18n t() { return I18n.getInstance(LandClaim.name); }
    @Override protected String titleText() { return t().get("tc.ui.city.title", player); }
    @Override protected String descriptionText() { return t().get("tc.ui.city.subtitle", player); }
    @Override protected String legendText() { return t().get("tc.ui.city.legend", player); }

    @Override
    protected void setupTabs() {
        setupTabContainer();
        addTab(t().get("tc.ui.city.tab.current", player), 200, activeTab == Tab.CURRENT, true, () -> {
            activeTab = Tab.CURRENT; rebuild();
        });
        addTab(t().get("tc.ui.city.tab.others", player), 200, activeTab == Tab.OTHERS, true, () -> {
            activeTab = Tab.OTHERS; rebuild();
        });
        if (activeTab == Tab.CURRENT) setupCurrent(); else setupOthers();
    }

    private void setupCurrent() {
        CityRecord city = cities.findCity(currentCityAreaId).orElse(null);
        if (city == null) { body.addChild(label(t().get("tc.ui.city.not.found", player), 20, 20, 700, 40)); return; }
        long nextChunks = cities.expansionChunkCount(city);
        long nextPrice;
        try { nextPrice = Math.multiplyExact(nextChunks, Math.max(0L, settings.cityExpansionBasePrice)); }
        catch (ArithmeticException ex) { nextPrice = Long.MAX_VALUE; }
        if (nextPrice > LandPriceService.MAX_SAFE_INTEGER) nextPrice = -1;
        CityService.ExpansionEligibility geometry = cities.expansionEligibility(city.areaId());
        LeaseholdSummary leaseholds = cities.leaseholdSummary(city.areaId());
        body.addChild(label(t().get("tc.ui.city.overview", player)
                .replace("PH_CITY_NAME", city.name()).replace("PH_RADIUS", String.valueOf(city.radius()))
                .replace("PH_BALANCE", String.valueOf(economy.cityBalance(city.areaId())))
                .replace("PH_NEXT_PRICE", nextPrice < 0 ? "N/A" : String.valueOf(nextPrice))
                .replace("PH_LEASE_TOTAL", String.valueOf(leaseholds.total()))
                .replace("PH_LEASE_OCCUPIED", String.valueOf(leaseholds.occupied()))
                .replace("PH_LEASE_UNOCCUPIED", String.valueOf(leaseholds.unoccupied()))
                .replace("PH_DAILY_RENT", String.valueOf(leaseholds.dailyRentIncome()))
                .replace("PH_CURRENCY", economy.defaultCurrencyIdentifier()), 20, 20, 720, 125));
        long expansionPrice = nextPrice;
        boolean canExpand = expansionPrice >= 0 && geometry.eligible()
                && economy.cityBalance(city.areaId()) >= expansionPrice;
        AdvancedButton expand = AdvancedButtonFactory.custom(
                new AdvancedButtonState(State.DEFAULT, 0x00000080, 0x269F59FF, 0xFFFFFFFF, 0x000000AA, 0x32A05AFF,
                        t().get("tc.ui.city.expand", player), event -> {
            if (expansionPrice < 0 || !cities.expansionEligibility(city.areaId()).eligible()
                    || economy.cityBalance(city.areaId()) < expansionPrice) return;
            UIElement dialog = UIDialogFactory.getConfirmDangerDialog(player, t().get("tc.dialog.city.expand.title", player),
                    t().get("tc.dialog.city.expand.confirm", player).replace("PH_PRICE", String.valueOf(expansionPrice)),
                    confirmed -> { if (confirmed) expand(city, expansionPrice); }, p -> { });
            player.addUIElement(dialog, UITarget.Modal);
        }), new AdvancedButtonState(State.DISABLED, 0x414141FF, 0x242424FF, 0x999999FF, 0x414141FF, 0x242424FF,
                t().get("tc.ui.city.expand", player), null));
        if (!canExpand) expand.setState(State.DISABLED);
        position(expand, 20, 170, 220, 42); body.addChild(expand);
        if (!canExpand) {
            body.addChild(label(expansionBlockerText(geometry, expansionPrice, economy.cityBalance(city.areaId())),
                    20, 218, 760, 28));
        }

        String privateState = city.allowPrivateClaimsOverride() == null ? t().get("tc.ui.city.global", player)
                : String.valueOf(city.allowPrivateClaimsOverride());
        AdvancedButton toggle = AdvancedButtonFactory.defaultButton(t().get("tc.ui.city.private", player)
                .replace("PH_VALUE", privateState), event -> {
            Boolean next = city.allowPrivateClaimsOverride() == null ? Boolean.TRUE
                    : city.allowPrivateClaimsOverride() ? Boolean.FALSE : null;
            cities.updateCityOverrides(city.areaId(), next, city.privateClaimPriceOverride()); rebuild();
        });
        position(toggle, 260, 170, 240, 42); body.addChild(toggle);

        String price = city.privateClaimPriceOverride() == null ? t().get("tc.ui.city.global", player)
                : String.valueOf(city.privateClaimPriceOverride());
        AdvancedButton priceButton = AdvancedButtonFactory.defaultButton(t().get("tc.ui.city.private.price", player)
                .replace("PH_VALUE", price), event -> editPrivatePrice(city));
        position(priceButton, 520, 170, 260, 42); body.addChild(priceButton);
    }

    private void setupOthers() {
        UITextField input = new UITextField();
        input.setText(search); input.setSize(420, 38, false); input.setPosition(20, 8, false);
        body.addChild(input);
        AdvancedButton find = AdvancedButtonFactory.defaultButton(t().get("tc.ui.city.search", player), event ->
                input.getCurrentText(player, text -> { search = text == null ? "" : text.trim(); offset = 0; rebuild(); }));
        position(find, 455, 8, 120, 38); body.addChild(find);

        TableScrollView table = new TableScrollView(Arrays.asList(
                t().get("tc.ui.city.th.name", player), t().get("tc.ui.city.th.sector", player),
                t().get("tc.ui.city.th.position", player), t().get("tc.ui.city.th.radius", player),
                t().get("tc.ui.city.th.balance", player), t().get("tc.ui.city.th.action", player)),
                Arrays.asList(23f, 14f, 16f, 10f, 21f, 16f));
        table.setPosition(0, 58, false); table.setScrollBodyHeight(325f);
        List<CityRecord> values = cities.listCities(search, offset, PAGE_SIZE);
        for (CityRecord city : values) table.addRow(cityRow(city));
        body.addChild(table);

        AdvancedButton previous = AdvancedButtonFactory.defaultButton("<", event -> { offset = Math.max(0, offset - PAGE_SIZE); rebuild(); });
        position(previous, 620, 395, 50, 34); body.addChild(previous);
        AdvancedButton next = AdvancedButtonFactory.defaultButton(">", event -> { if (values.size() == PAGE_SIZE) offset += PAGE_SIZE; rebuild(); });
        position(next, 730, 395, 50, 34); body.addChild(next);
        body.addChild(label(t().get("tc.ui.city.page", player).replace("PH_PAGE", String.valueOf(offset / PAGE_SIZE + 1)),
                675, 395, 50, 34));
    }

    private TableRow cityRow(CityRecord city) {
        AdvancedButton teleport = AdvancedButtonFactory.defaultButton(t().get("tc.ui.city.teleport", player), event -> teleport(city));
        // TableCell positions its content relative to the cell.  Pixel sizing
        // keeps the action inside its cell and gives it a usable row height.
        teleport.setSize(100, 24, false);
        return new TableRow(Arrays.asList(cell(city.name(), 23f),
                cell(Math.floorDiv(city.center().x, CityService.CHUNKS_PER_SECTOR) + ", "
                        + Math.floorDiv(city.center().z, CityService.CHUNKS_PER_SECTOR), 14f),
                cell(city.center().x + ", " + city.center().z, 16f), cell(String.valueOf(city.radius()), 10f),
                cell(String.valueOf(economy.cityBalance(city.areaId())), 21f), new TableCell(teleport, 16f)));
    }

    private void expand(CityRecord city, long price) {
        String correlation = "city-expand:" + city.areaId() + ":" + (city.radius() + 1);
        if (price > 0) cities.beginEconomyOperation(correlation, "CITY_EXPANSION", city.areaId(),
                player.getDbID(), price);
        EconomyIntegration.WalletOperationResult payment = price == 0
                ? new EconomyIntegration.WalletOperationResult(true, "")
                : economy.transferCityToWorld(city.areaId(), price,
                        t().get("tc.wallet.city.expansion", economy.walletAuditLanguage())
                                .replace("PH_CITY_NAME", city.name()), correlation);
        if (!payment.success()) {
            if (price > 0) cities.updateEconomyOperation(correlation, "FAILED", city.areaId(), payment.message());
            player.sendTextMessage(t().get("tc.city.expand.payment.failed", player)
                    .replace("PH_REASON", payment.message()));
        } else if (!cities.expandCity(city.areaId())) {
            if (price > 0) {
                cities.updateEconomyOperation(correlation, "PAID", city.areaId(), "");
                EconomyIntegration.WalletOperationResult reversal = economy.reverseTransfer(correlation,
                        correlation + ":reversal", t().get("tc.wallet.city.expansion.rollback", economy.walletAuditLanguage()));
                cities.updateEconomyOperation(correlation,
                        reversal.success() ? "REVERSED" : "RECONCILIATION_REQUIRED", city.areaId(), reversal.message());
            }
            CityService.ExpansionEligibility geometry = cities.expansionEligibility(city.areaId());
            player.sendTextMessage(expansionBlockerText(geometry, price, economy.cityBalance(city.areaId())));
        } else {
            if (price > 0) cities.updateEconomyOperation(correlation, "COMPLETED", city.areaId(), "");
            player.sendTextMessage(t().get("tc.city.expanded", player)
                    .replace("PH_RADIUS", String.valueOf(city.radius() + 1)));
        }
        rebuild();
    }

    private String expansionBlockerText(CityService.ExpansionEligibility geometry, long price, long balance) {
        if (price < 0) return t().get("tc.ui.city.expand.blocked.price", player);
        if (balance < price) return t().get("tc.ui.city.expand.blocked.funds", player)
                .replace("PH_PRICE", String.valueOf(price)).replace("PH_BALANCE", String.valueOf(balance));
        if (geometry == null || geometry.eligible()) return t().get("tc.city.expand.failed", player);
        return t().get("TC_UI_CITY_EXPAND_BLOCKED_" + geometry.blocker().name(), player);
    }

    private void editPrivatePrice(CityRecord city) {
        UIElement dialog = UIDialogFactory.getTextInput(player, t().get("tc.ui.city.private.price.edit", player),
                city.privateClaimPriceOverride() == null ? "" : String.valueOf(city.privateClaimPriceOverride()), text -> {
            Long value = null;
            if (text != null && !text.isBlank()) try { value = Long.valueOf(text.trim()); } catch (NumberFormatException ignored) { }
            cities.updateCityOverrides(city.areaId(), city.allowPrivateClaimsOverride(), value);
            rebuild();
        }, p -> { });
        player.addUIElement(dialog, UITarget.Modal);
    }

    private void teleport(CityRecord city) {
        Area area = Server.getArea(city.areaId());
        if (area == null) return;
        Vector3f start = area.getStartPosition(), end = area.getEndPosition();
        player.setPosition((start.x + end.x) / 2f, (start.y + end.y) / 2f, (start.z + end.z) / 2f);
    }

    private TableCell cell(String text, float width) { return new TableCell(label(text, 2, 0, 300, 30), width); }
    private UILabel label(String text, float x, float y, float width, float height) {
        UILabel label = new UILabel(text == null ? "" : text); label.setFontSize(15);
        label.setTextAlign(TextAnchor.MiddleLeft); label.setPivot(Pivot.UpperLeft);
        label.setPosition(x, y, false); label.setSize(width, height, false); return label;
    }
    private void position(UIElement element, float x, float y, float width, float height) {
        element.setPivot(Pivot.UpperLeft); element.setPosition(x, y, false); element.setSize(width, height, false);
    }
    @Override protected void close() { player.deleteAttribute(ATTRIBUTE_KEY); super.close(); }
}
