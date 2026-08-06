package de.omegazirkel.risingworld.landclaim;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import de.omegazirkel.risingworld.LandClaim;
import de.omegazirkel.risingworld.landclaim.db.LeaseholdRecord;
import de.omegazirkel.risingworld.landclaim.db.CityService;
import de.omegazirkel.risingworld.tools.I18n;
import net.risingworld.api.Server;
import net.risingworld.api.objects.Area;
import net.risingworld.api.objects.Player;

public final class CityRentService {
    private final CityService cities;
    private final EconomyIntegration economy;
    private final PluginSettings settings;

    public CityRentService(CityService cities, EconomyIntegration economy, PluginSettings settings) {
        this.cities = cities;
        this.economy = economy;
        this.settings = settings;
    }

    public RentRunResult bill(LocalDate billingDate) {
        if (ClaimModePolicy.current() != ClaimMode.CITY || !economy.hasSystemAccountApi())
            return new RentRunResult(0, 0, 0, 0);
        int checked = 0, paid = 0, evicted = 0, warned = 0;
        for (LeaseholdRecord lease : cities.rentedLeaseholds()) {
            if (billingDate.toString().equals(lease.lastBillingDate())) continue;
            checked++;
            Area area = Server.getArea(lease.areaId());
            if (area == null) continue;
            String areaName = area.getName() == null ? String.valueOf(area.getID()) : area.getName();
            Player owner = Server.getPlayerByDbID(lease.ownerDbId());
            String reason = I18n.getInstance(LandClaim.name).get("TC_WALLET_CITY_LEASE_RENT",
                    economy.walletAuditLanguage())
                    .replace("PH_AREA_NAME", areaName).replace("PH_AREA_ID", String.valueOf(area.getID()));
            String correlation = "city-rent:" + area.getID() + ":" + billingDate;
            if (lease.dailyRent() > 0) cities.beginEconomyOperation(correlation, "DAILY_RENT", area.getID(),
                    lease.ownerDbId(), lease.dailyRent());
            EconomyIntegration.WalletOperationResult payment = lease.dailyRent() == 0
                    ? new EconomyIntegration.WalletOperationResult(true, "")
                    : economy.transferPlayerToCity(lease.ownerDbId(), lease.cityAreaId(), lease.dailyRent(), reason,
                            correlation);
            if (!payment.success()) {
                if (lease.dailyRent() > 0) cities.updateEconomyOperation(correlation, "FAILED", area.getID(),
                        payment.message());
                clearPermissions(area);
                cities.clearLeasehold(area.getID());
                notifyPlayer(lease.ownerDbId(), "TC_CITY_RENT_EVICTED", areaName, billingDate);
                evicted++;
                continue;
            }
            if (lease.dailyRent() > 0) cities.updateEconomyOperation(correlation, "PAID", area.getID(), "");
            long credit = lease.paidRentCredit() > Long.MAX_VALUE - lease.dailyRent()
                    ? Long.MAX_VALUE : lease.paidRentCredit() + lease.dailyRent();
            cities.recordRent(area.getID(), credit, billingDate.toString());
            if (lease.dailyRent() > 0) cities.updateEconomyOperation(correlation, "COMPLETED", area.getID(), "");
            paid++;
            if (lease.dailyRent() > 0 && economy.playerBalance(lease.ownerDbId()) / lease.dailyRent() < 7) {
                notifyPlayer(lease.ownerDbId(), "TC_CITY_RENT_WARNING", areaName, billingDate);
                warned++;
            }
        }
        return new RentRunResult(checked, paid, evicted, warned);
    }

    private void notifyPlayer(int playerDbId, String key, String areaName, LocalDate date) {
        String playerName = Server.getLastKnownPlayerName(playerDbId);
        Player online = Server.getPlayerByDbID(playerDbId);
        I18n translations = I18n.getInstance(LandClaim.name);
        String language = online == null ? cities.playerLanguage(playerDbId).orElse("en")
                : de.omegazirkel.risingworld.OZTools.getPlayerLanguage(online);
        String message = translations.get(key, language).replace("PH_AREA_NAME", areaName);
        long pendingId = cities.addPendingNotification(playerDbId, key, areaName);
        boolean mailed = economy.sendMail(playerDbId, playerName == null ? "Player" : playerName,
                translations.get("TC_CITY_MAIL_SUBJECT", language), message,
                "landclaim-notice:" + key + ":" + playerDbId + ":" + date);
        if (mailed && pendingId > 0) cities.deletePendingNotification(pendingId);
    }

    private void clearPermissions(Area area) {
        Map<Integer, String> permissions = area.getAllPlayerPermissions();
        if (permissions != null) for (Integer dbId : List.copyOf(permissions.keySet())) area.removePlayerPermission(dbId);
    }

    public record RentRunResult(int checked, int paid, int evicted, int warned) { }
}
