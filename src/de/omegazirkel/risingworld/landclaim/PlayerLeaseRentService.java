package de.omegazirkel.risingworld.landclaim;

import java.time.LocalDate;

import de.omegazirkel.risingworld.landclaim.db.PlayerLeaseRecord;
import de.omegazirkel.risingworld.landclaim.db.PlayerLeaseService;
import net.risingworld.api.Server;
import net.risingworld.api.objects.Area;

/** Settles private leaseholds and performs their ownership transitions. */
public final class PlayerLeaseRentService {
    private final PlayerLeaseService leases;
    private final EconomyIntegration economy;
    private final ChunkClaimUtil claims;
    private final PluginSettings settings;

    public PlayerLeaseRentService(PlayerLeaseService leases, EconomyIntegration economy,
            ChunkClaimUtil claims, PluginSettings settings) {
        this.leases = leases;
        this.economy = economy;
        this.claims = claims;
        this.settings = settings;
    }

    public RentRunResult bill(LocalDate date) {
        if (ClaimModePolicy.current() != ClaimMode.LAND_PRICING || leases == null || economy == null
                || claims == null || !economy.hasPlayerTransferApi()) return new RentRunResult(0, 0, 0, 0);
        int checked = 0, paid = 0, evicted = 0, purchased = 0;
        for (PlayerLeaseRecord lease : leases.occupied()) {
            if (date.toString().equals(lease.lastBillingDate())) continue;
            checked++;
            Area area = Server.getArea(lease.areaId());
            if (area == null) continue;
            String correlation = "player-lease-rent:" + lease.areaId() + ":" + date;
            EconomyIntegration.WalletOperationResult result = lease.dailyRent() == 0
                    ? new EconomyIntegration.WalletOperationResult(true, "")
                    : economy.transferPlayerToPlayer(lease.tenantDbId(), lease.landlordDbId(), lease.dailyRent(),
                            "Land Claim rental payment for area #" + lease.areaId(), correlation);
            if (!result.success()) {
                if (leases.clearTenant(lease.areaId())) {
                    PlayerLeasePermissionRestorer.restore(area, leases, lease.landlordDbId(), settings);
                    evicted++;
                }
                continue;
            }
            long credit = lease.rentCredit() > Long.MAX_VALUE - lease.dailyRent()
                    ? Long.MAX_VALUE : lease.rentCredit() + lease.dailyRent();
            if (!leases.recordRent(lease.areaId(), credit, date.toString())) continue;
            paid++;
            if (lease.purchaseAllowed() && credit >= lease.purchasePrice()) {
                if (claims.transferAreaOwnership(area, lease.tenantUuid(), lease.tenantDbId())
                        && leases.completePurchase(lease.areaId())) purchased++;
            }
        }
        return new RentRunResult(checked, paid, evicted, purchased);
    }

    public record RentRunResult(int checked, int paid, int evicted, int purchased) { }
}
