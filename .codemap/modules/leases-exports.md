# Lease, sale, city and map export services
<!-- codemap-id: rw-plugin-oz-land-claim/leases-exports -->

## Responsibility

Lease, sale, city and map export services. Land Claim owns lease and sale records. Export exposure and external consumers must be checked when changing data contracts.

## Source and tests

- [City lease acquisition](../../src/de/omegazirkel/risingworld/landclaim/CityLeaseAcquisition.java) reserves payment and verifies the displayed lease before assignment. [SQLite tests](../../src-test/de/omegazirkel/risingworld/landclaim/db/CityLeaseAcquisitionTest.java) cover stale terms and payment outcomes.
- [`PlayerLeaseService`](../../src/de/omegazirkel/risingworld/landclaim/db/PlayerLeaseService.java)
- [`ClaimSaleListingService`](../../src/de/omegazirkel/risingworld/landclaim/db/ClaimSaleListingService.java)
- [`LandClaimExportRoute`](../../src/de/omegazirkel/risingworld/landclaim/web/LandClaimExportRoute.java)
- [`ClaimSaleExportService`](../../src/de/omegazirkel/risingworld/landclaim/exports/ClaimSaleExportService.java)
- [`PlayerLeaseServiceTest`](../../src-test/de/omegazirkel/risingworld/landclaim/db/PlayerLeaseServiceTest.java)

See the [repository index](../README.md) for lifecycle, build and cross-repository dependencies.
