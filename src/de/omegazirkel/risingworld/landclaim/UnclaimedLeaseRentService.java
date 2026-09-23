package de.omegazirkel.risingworld.landclaim;

import java.time.LocalDate;

import de.omegazirkel.risingworld.landclaim.db.UnclaimedLeaseRecord;
import de.omegazirkel.risingworld.landclaim.db.UnclaimedLeaseService;
import net.risingworld.api.Server;
import net.risingworld.api.objects.Area;

/** Daily world-rental settlement. Rent and ancillary costs are separately auditable Wallet transfers. */
public final class UnclaimedLeaseRentService {
    private final UnclaimedLeaseService leases;
    private final EconomyIntegration economy;
    private final ChunkClaimUtil claims;
    private final PluginSettings settings;

    public UnclaimedLeaseRentService(UnclaimedLeaseService leases, EconomyIntegration economy,
            ChunkClaimUtil claims, PluginSettings settings) {
        this.leases = leases; this.economy = economy; this.claims = claims; this.settings = settings;
    }

    public RentRunResult bill(LocalDate date) {
        if (ClaimModePolicy.current() != ClaimMode.LAND_PRICING || !Boolean.TRUE.equals(settings.enableUnclaimedLeaseholds)
                || !economy.hasSystemAccountApi()) return new RentRunResult(0, 0, 0, 0);
        int checked = 0, paid = 0, revoked = 0, purchased = 0;
        for (UnclaimedLeaseRecord lease : leases.active()) {
            if (date.toString().equals(lease.lastBillingDate())) continue;
            checked++;
            Area area = Server.getArea(lease.areaId());
            if (area == null) { leases.remove(lease.areaId()); continue; }
            String prefix = "unclaimed-lease:" + lease.areaId() + ":" + date;
            EconomyIntegration.WalletOperationResult rent = lease.dailyRent() == 0
                    ? new EconomyIntegration.WalletOperationResult(true, "")
                    : economy.transferPlayerToWorld(lease.tenantDbId(), lease.dailyRent(),
                            "Land Claim rent for area #" + lease.areaId(), prefix + ":rent");
            EconomyIntegration.WalletOperationResult ancillary = rent.success() && lease.ancillaryCost() > 0
                    ? economy.transferPlayerToWorld(lease.tenantDbId(), lease.ancillaryCost(),
                            "Land Claim ancillary cost for area #" + lease.areaId(), prefix + ":ancillary")
                    : new EconomyIntegration.WalletOperationResult(rent.success(), "");
            if (!rent.success()) {
                if (claims.revokeUnclaimedRental(area) && leases.remove(lease.areaId())) revoked++;
                continue;
            }
            if (!ancillary.success()) {
                EconomyIntegration.WalletOperationResult reversal = economy.reverseTransfer(prefix + ":rent",
                        prefix + ":rent-reversal", "Reverse incomplete Land Claim world rental settlement");
                if (reversal.success() && claims.revokeUnclaimedRental(area) && leases.remove(lease.areaId())) revoked++;
                continue;
            }
            long credit = lease.rentCredit() > Long.MAX_VALUE - lease.dailyRent()
                    ? Long.MAX_VALUE : lease.rentCredit() + lease.dailyRent();
            if (!leases.recordRent(lease.areaId(), credit, date.toString())) continue;
            paid++;
            if (credit >= lease.purchasePrice()) {
                area.removePlayerPermission(lease.tenantDbId());
                area.setPlayerPermission(lease.tenantDbId(), settings.ownerAreaPermission);
                if (leases.remove(lease.areaId())) purchased++;
            }
        }
        return new RentRunResult(checked, paid, revoked, purchased);
    }

    public record RentRunResult(int checked, int paid, int revoked, int purchased) { }
}
