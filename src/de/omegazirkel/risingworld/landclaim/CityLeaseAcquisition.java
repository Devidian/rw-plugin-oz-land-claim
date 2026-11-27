package de.omegazirkel.risingworld.landclaim;

import java.time.LocalDate;
import java.util.UUID;
import java.util.function.Function;

import de.omegazirkel.risingworld.landclaim.db.CityService;
import de.omegazirkel.risingworld.landclaim.db.CityService.LeaseAssignment;
import de.omegazirkel.risingworld.landclaim.db.LeaseholdRecord;

/** Coordinates one city lease confirmation across the journal, Wallet and area permissions. */
public final class CityLeaseAcquisition {
    public enum Status { COMPLETED, STALE, PAYMENT_FAILED, RECONCILIATION_REQUIRED }

    public record Result(Status status, String message) { }

    private CityLeaseAcquisition() { }

    public static Result acquire(CityService city, LeaseholdRecord offer, String ownerUuid, int ownerDbId,
            boolean purchase, Function<String, EconomyIntegration.WalletOperationResult> charge,
            Function<String, EconomyIntegration.WalletOperationResult> reverse, Runnable applyPermissions) {
        if (offer == null || city.findLeasehold(offer.areaId()).filter(offer::equals).isEmpty()
                || (offer.occupied() && !(purchase && offer.rented() && offer.ownerDbId() == ownerDbId))
                || (purchase && !offer.purchaseAllowed()) || (!purchase && !offer.rentAllowed())) {
            return new Result(Status.STALE, "");
        }

        long amount = purchase ? Math.max(0L, offer.purchasePrice() - offer.paidRentCredit()) : offer.dailyRent();
        String correlation = "city-lease-" + (purchase ? "buy:" : "rent:") + UUID.randomUUID();
        if (!city.tryBeginLeaseOperation(correlation,
                purchase ? "LEASE_PURCHASE" : "LEASE_RENT", offer.areaId(), ownerDbId, amount)) {
            return new Result(Status.RECONCILIATION_REQUIRED, "");
        }
        if (city.findLeasehold(offer.areaId()).filter(offer::equals).isEmpty()) {
            city.updateEconomyOperation(correlation, "FAILED", offer.areaId(), "Offer changed before charging.");
            return new Result(Status.STALE, "");
        }
        if (amount > 0) {
            EconomyIntegration.WalletOperationResult payment;
            try {
                payment = charge.apply(correlation);
            } catch (RuntimeException ex) {
                city.updateEconomyOperation(correlation, "RECONCILIATION_REQUIRED", offer.areaId(), ex.toString());
                return new Result(Status.RECONCILIATION_REQUIRED, "");
            }
            if (!payment.success()) {
                // The current Wallet provider emits this result before any debit. An empty or unknown
                // reflective response leaves the same operation quarantined.
                boolean rejected = "Wallet balance is too low for this transfer.".equals(payment.message())
                        || "Wallet default currency is unavailable.".equals(payment.message());
                city.updateEconomyOperation(correlation, rejected ? "FAILED" : "RECONCILIATION_REQUIRED",
                        offer.areaId(), payment.message());
                return new Result(rejected ? Status.PAYMENT_FAILED : Status.RECONCILIATION_REQUIRED,
                        payment.message());
            }
            city.updateEconomyOperation(correlation, "PAID", offer.areaId(), "");
        }

        LeaseAssignment assignment = city.assignLeaseholdIfUnchanged(
                offer, ownerUuid, ownerDbId, purchase, LocalDate.now().toString());
        if (assignment == LeaseAssignment.UNCERTAIN) {
            city.updateEconomyOperation(correlation, "RECONCILIATION_REQUIRED", offer.areaId(),
                    "Lease assignment outcome is uncertain.");
            return new Result(Status.RECONCILIATION_REQUIRED, "");
        }
        if (assignment == LeaseAssignment.STALE) {
            if (amount > 0) {
                try {
                    EconomyIntegration.WalletOperationResult reversal = reverse.apply(correlation);
                    city.updateEconomyOperation(correlation, reversal.success() ? "REVERSED" : "RECONCILIATION_REQUIRED",
                            offer.areaId(), reversal.message());
                    if (!reversal.success()) return new Result(Status.RECONCILIATION_REQUIRED, reversal.message());
                } catch (RuntimeException ex) {
                    city.updateEconomyOperation(correlation, "RECONCILIATION_REQUIRED", offer.areaId(), ex.toString());
                    return new Result(Status.RECONCILIATION_REQUIRED, "");
                }
            }
            if (amount == 0) city.updateEconomyOperation(correlation, "FAILED", offer.areaId(), "Offer changed.");
            return new Result(Status.STALE, "");
        }
        try {
            applyPermissions.run();
        } catch (RuntimeException ex) {
            city.updateEconomyOperation(correlation, "RECONCILIATION_REQUIRED", offer.areaId(),
                    "Permissions update failed: " + ex);
            return new Result(Status.RECONCILIATION_REQUIRED, "");
        }
        city.updateEconomyOperation(correlation, "COMPLETED", offer.areaId(), "");
        return new Result(Status.COMPLETED, "");
    }
}
