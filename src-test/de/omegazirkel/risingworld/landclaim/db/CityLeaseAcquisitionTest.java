package de.omegazirkel.risingworld.landclaim.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import de.omegazirkel.risingworld.landclaim.CityLeaseAcquisition;
import de.omegazirkel.risingworld.landclaim.CityLeaseAcquisition.Status;
import de.omegazirkel.risingworld.landclaim.EconomyIntegration.WalletOperationResult;

public class CityLeaseAcquisitionTest {
    private static final WalletOperationResult OK = new WalletOperationResult(true, "");
    private static final WalletOperationResult UNKNOWN = new WalletOperationResult(false, "unknown");

    private CityService city(Connection db) throws Exception {
        CityService city = new CityService(db, "world");
        try (Statement statement = db.createStatement()) {
            statement.executeUpdate("INSERT INTO leaseholds VALUES('world',1,2,100,10,1,1,'',0,'NONE',0,'')");
        }
        return city;
    }

    private String status(Connection db) throws Exception {
        try (Statement query = db.createStatement();
                ResultSet rows = query.executeQuery("SELECT status FROM landClaimEconomyOperations")) {
            assertTrue(rows.next());
            return rows.getString(1);
        }
    }

    @Test
    public void staleConfirmationsNeverChargeOrChangeTheFirstOwner() throws Exception {
        try (Connection db = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            CityService city = city(db);
            LeaseholdRecord offer = city.findLeasehold(1).orElseThrow();
            AtomicInteger charges = new AtomicInteger();
            AtomicInteger permissions = new AtomicInteger();
            assertEquals(Status.COMPLETED, CityLeaseAcquisition.acquire(city, offer, "first", 11, true,
                    id -> { charges.incrementAndGet(); return OK; }, id -> { fail(); return OK; },
                    permissions::incrementAndGet).status());
            assertEquals(Status.STALE, CityLeaseAcquisition.acquire(city, offer, "second", 12, true,
                    id -> { charges.incrementAndGet(); return OK; }, id -> { fail(); return OK; },
                    permissions::incrementAndGet).status());
            assertEquals(1, charges.get());
            assertEquals(1, permissions.get());
            assertEquals(11, city.findLeasehold(1).orElseThrow().ownerDbId());
            assertEquals("COMPLETED", status(db));
        }
    }

    @Test
    public void changesDuringPaymentReverseOnlyTheConfirmedPayment() throws Exception {
        for (boolean reversalSucceeds : new boolean[] { true, false }) {
            try (Connection db = DriverManager.getConnection("jdbc:sqlite::memory:")) {
                CityService city = city(db);
                LeaseholdRecord offer = city.findLeasehold(1).orElseThrow();
                AtomicInteger reversals = new AtomicInteger();
                Status result = CityLeaseAcquisition.acquire(city, offer, "first", 11, true,
                        id -> { assertTrue(city.configureLeasehold(1, 101, 10, true, true)); return OK; },
                        id -> { reversals.incrementAndGet(); return reversalSucceeds ? OK : UNKNOWN; },
                        () -> fail("Stale offer must not change permissions")).status();
                assertEquals(reversalSucceeds ? Status.STALE : Status.RECONCILIATION_REQUIRED, result);
                assertEquals(1, reversals.get());
                assertEquals(0, city.findLeasehold(1).orElseThrow().ownerDbId());
                assertEquals(reversalSucceeds ? "REVERSED" : "RECONCILIATION_REQUIRED", status(db));
            }
        }
    }

    @Test
    public void unknownPaymentIsPreservedWithoutReversalOrAssignment() throws Exception {
        try (Connection db = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            CityService city = city(db);
            LeaseholdRecord offer = city.findLeasehold(1).orElseThrow();
            assertEquals(Status.RECONCILIATION_REQUIRED, CityLeaseAcquisition.acquire(city, offer, "first", 11, true,
                    id -> UNKNOWN, id -> { fail(); return OK; }, () -> fail()).status());
            assertEquals(0, city.findLeasehold(1).orElseThrow().ownerDbId());
            assertEquals("RECONCILIATION_REQUIRED", status(db));
            assertEquals(Status.RECONCILIATION_REQUIRED, CityLeaseAcquisition.acquire(city, offer, "first", 11, true,
                    id -> { fail("An unresolved transfer must not be retried"); return OK; },
                    id -> { fail(); return OK; }, () -> fail()).status());
        }
    }

    @Test
    public void confirmedInsufficientFundsCanBeRetriedAfterBalanceChanges() throws Exception {
        try (Connection db = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            CityService city = city(db);
            LeaseholdRecord offer = city.findLeasehold(1).orElseThrow();
            assertEquals(Status.PAYMENT_FAILED, CityLeaseAcquisition.acquire(city, offer, "first", 11, true,
                    id -> new WalletOperationResult(false, "Wallet balance is too low for this transfer."),
                    id -> { fail(); return OK; }, () -> fail()).status());
            assertEquals("FAILED", status(db));
            assertEquals(Status.COMPLETED, CityLeaseAcquisition.acquire(city, offer, "first", 11, true,
                    id -> OK, id -> { fail(); return OK; }, () -> { }).status());
        }
    }

    @Test
    public void onlyTenantCanConvertAndRentCreditReducesPurchase() throws Exception {
        try (Connection db = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            CityService city = city(db);
            LeaseholdRecord offer = city.findLeasehold(1).orElseThrow();
            assertEquals(Status.COMPLETED, CityLeaseAcquisition.acquire(city, offer, "tenant", 11, false,
                    id -> OK, id -> { fail(); return OK; }, () -> { }).status());
            LeaseholdRecord rented = city.findLeasehold(1).orElseThrow();
            assertEquals(10, rented.paidRentCredit());
            assertEquals(Status.STALE, CityLeaseAcquisition.acquire(city, rented, "other", 12, true,
                    id -> { fail(); return OK; }, id -> { fail(); return OK; }, () -> fail()).status());
            assertEquals(Status.COMPLETED, CityLeaseAcquisition.acquire(city, rented, "tenant", 11, true,
                    id -> OK, id -> { fail(); return OK; }, () -> { }).status());
            assertEquals("PURCHASED", city.findLeasehold(1).orElseThrow().ownershipType());
            try (Statement query = db.createStatement();
                    ResultSet rows = query.executeQuery("SELECT amount FROM landClaimEconomyOperations "
                            + "WHERE operation_type='LEASE_PURCHASE'")) {
                assertTrue(rows.next());
                assertEquals(90, rows.getLong(1));
            }
        }
    }

    @Test
    public void freeLeaseAndPermissionFailureAreJournaled() throws Exception {
        try (Connection db = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            CityService city = city(db);
            assertTrue(city.configureLeasehold(1, 0, 0, true, true));
            LeaseholdRecord offer = city.findLeasehold(1).orElseThrow();
            assertEquals(Status.RECONCILIATION_REQUIRED, CityLeaseAcquisition.acquire(city, offer, "first", 11, true,
                    id -> { fail(); return OK; }, id -> { fail(); return OK; },
                    () -> { throw new IllegalStateException("injected"); }).status());
            assertEquals(11, city.findLeasehold(1).orElseThrow().ownerDbId());
            assertEquals("RECONCILIATION_REQUIRED", status(db));
        }
    }
}
