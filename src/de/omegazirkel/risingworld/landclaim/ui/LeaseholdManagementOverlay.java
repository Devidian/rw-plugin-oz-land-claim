package de.omegazirkel.risingworld.landclaim.ui;

import java.util.UUID;

import de.omegazirkel.risingworld.LandClaim;
import de.omegazirkel.risingworld.landclaim.EconomyIntegration;
import de.omegazirkel.risingworld.landclaim.db.CityService;
import de.omegazirkel.risingworld.landclaim.db.LeaseholdRecord;
import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.ui.AdvancedButton;
import de.omegazirkel.risingworld.tools.ui.AdvancedButtonFactory;
import de.omegazirkel.risingworld.tools.ui.BasePluginOverlayWithTabs;
import de.omegazirkel.risingworld.tools.ui.SwitchButton;
import net.risingworld.api.Server;
import net.risingworld.api.callbacks.Callback;
import net.risingworld.api.objects.Area;
import net.risingworld.api.objects.Player;
import net.risingworld.api.ui.UIElement;
import net.risingworld.api.ui.UILabel;
import net.risingworld.api.ui.UITarget;
import net.risingworld.api.ui.style.Pivot;
import net.risingworld.api.ui.style.TextAnchor;

public final class LeaseholdManagementOverlay extends BasePluginOverlayWithTabs {
    public static final String ATTRIBUTE_KEY = "landclaim-leasehold-management-overlay";
    private final Player player;
    private final Area area;
    private final CityService cities;
    private final EconomyIntegration economy;

    public LeaseholdManagementOverlay(Player player, Area area, CityService cities, EconomyIntegration economy,
            Callback<Player> onClose) {
        super(player, onClose);
        this.player = player;
        this.area = area;
        this.cities = cities;
        this.economy = economy;
        rebuild();
    }

    @Override protected I18n t() { return I18n.getInstance(LandClaim.name); }
    @Override protected String titleText() { return t().get("tc.ui.lease.admin.title", player); }
    @Override protected String descriptionText() { return t().get("tc.ui.lease.admin.subtitle", player); }
    @Override protected String legendText() { return t().get("tc.ui.lease.admin.legend", player); }

    @Override protected void setupTabs() {
        setupTabContainer();
        addTab(t().get("tc.ui.lease.admin.tab", player), 220, true, true, () -> { });
        setupLease();
    }

    private void setupLease() {
        LeaseholdRecord lease = cities.findLeasehold(area.getID()).orElse(null);
        if (lease == null) return;
        String owner = lease.occupied() ? Server.getLastKnownPlayerName(lease.ownerDbId())
                : t().get("tc.ui.lease.unoccupied", player);
        body.addChild(label(t().get("tc.ui.lease.overview", player)
                .replace("PH_AREA_NAME", area.getName() == null ? String.valueOf(area.getID()) : area.getName())
                .replace("PH_OWNER", owner == null ? String.valueOf(lease.ownerDbId()) : owner)
                .replace("PH_PURCHASE_PRICE", String.valueOf(lease.purchasePrice()))
                .replace("PH_RENT", String.valueOf(lease.dailyRent())), 20, 20, 720, 110));
        AdvancedButton price = AdvancedButtonFactory.defaultButton(t().get("tc.ui.lease.edit.price", player),
                event -> editAmount(lease, false));
        position(price, 20, 155, 220, 42); body.addChild(price);
        AdvancedButton rent = AdvancedButtonFactory.defaultButton(t().get("tc.ui.lease.edit.rent", player),
                event -> editAmount(lease, true));
        position(rent, 260, 155, 220, 42); body.addChild(rent);
        body.addChild(label(t().get("tc.ui.lease.purchase.allowed", player), 20, 220, 200, 28));
        SwitchButton buyFlag = new SwitchButton(lease.purchaseAllowed(), enabled -> {
            cities.configureLeasehold(area.getID(), lease.purchasePrice(), lease.dailyRent(),
                    enabled, lease.rentAllowed()); rebuild();
        });
        position(buyFlag, 210, 223, 60, 22); body.addChild(buyFlag);
        body.addChild(label(t().get("tc.ui.lease.rent.allowed", player), 300, 220, 200, 28));
        SwitchButton rentFlag = new SwitchButton(lease.rentAllowed(), enabled -> {
            cities.configureLeasehold(area.getID(), lease.purchasePrice(), lease.dailyRent(),
                    lease.purchaseAllowed(), enabled); rebuild();
        });
        position(rentFlag, 490, 223, 60, 22); body.addChild(rentFlag);
    }

    private void editAmount(LeaseholdRecord lease, boolean rent) {
        long current = rent ? lease.dailyRent() : lease.purchasePrice();
        UIElement input = UIDialogFactory.getTextInput(player,
                t().get(rent ? "tc.ui.lease.edit.rent" : "tc.ui.lease.edit.price", player), String.valueOf(current),
                text -> {
                    long value;
                    try { value = Long.parseLong(text.trim()); } catch (RuntimeException ex) { value = -1; }
                    if (value < 0) return;
                    if (rent && lease.occupied() && value > lease.dailyRent()) {
                        long newRent = value;
                        UIElement confirm = UIDialogFactory.getConfirmDangerDialog(player,
                                t().get("tc.ui.lease.rent.increase.title", player),
                                t().get("tc.ui.lease.rent.increase.confirm", player)
                                        .replace("PH_OLD_RENT", String.valueOf(lease.dailyRent()))
                                        .replace("PH_NEW_RENT", String.valueOf(newRent)), accepted -> {
                            if (accepted) saveAmount(lease, true, newRent);
                        }, p -> { });
                        player.addUIElement(confirm, UITarget.Modal);
                    } else saveAmount(lease, rent, value);
                }, p -> { });
        player.addUIElement(input, UITarget.Modal);
    }

    private void saveAmount(LeaseholdRecord lease, boolean rent, long value) {
        boolean saved = cities.configureLeasehold(area.getID(), rent ? lease.purchasePrice() : value,
                rent ? value : lease.dailyRent(), lease.purchaseAllowed(), lease.rentAllowed());
        if (saved && rent && lease.occupied() && value > lease.dailyRent()) notifyRentIncrease(lease, value);
        rebuild();
    }

    private void notifyRentIncrease(LeaseholdRecord lease, long newRent) {
        Player owner = Server.getPlayerByDbID(lease.ownerDbId());
        String ownerName = Server.getLastKnownPlayerName(lease.ownerDbId());
        String areaName = area.getName() == null ? String.valueOf(area.getID()) : area.getName();
        String language = owner == null ? cities.playerLanguage(lease.ownerDbId()).orElse("en")
                : de.omegazirkel.risingworld.OZTools.getPlayerLanguage(owner);
        String message = t().get("tc.city.rent.increased", language).replace("PH_AREA_NAME", areaName)
                .replace("PH_RENT", String.valueOf(newRent));
        long pendingId = cities.addPendingNotification(lease.ownerDbId(), "tc.city.rent.increased",
                areaName + "\t" + newRent);
        boolean sent = economy.sendMail(lease.ownerDbId(), ownerName == null ? "Player" : ownerName,
                t().get("tc.city.mail.subject", language), message,
                "city-rent-increase:" + area.getID() + ":" + UUID.randomUUID());
        if (sent && pendingId > 0) cities.deletePendingNotification(pendingId);
    }

    private UILabel label(String text, float x, float y, float width, float height) {
        UILabel label = new UILabel(text); label.setRichTextEnabled(true); label.setFontSize(16);
        label.setTextAlign(TextAnchor.UpperLeft); label.setPivot(Pivot.UpperLeft);
        label.setPosition(x, y, false); label.setSize(width, height, false); return label;
    }
    private void position(UIElement element, float x, float y, float width, float height) {
        element.setPivot(Pivot.UpperLeft); element.setPosition(x, y, false); element.setSize(width, height, false);
    }
    @Override protected void close() { player.deleteAttribute(ATTRIBUTE_KEY); super.close(); }
}
