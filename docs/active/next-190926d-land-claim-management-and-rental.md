# Land Claim: renew management and rental purchase

## Objective

Provide administrator renew-zone management, make administrative area removal
use the automatic-cleanup safety path, and add economy-mode rental purchase for
player-owned and unclaimed claims.

## Ownership and dependencies

- Owner: `rw-plugin-oz-land-claim`.
- Economy operations remain behind `EconomyIntegration`/OZ Wallet.
- Existing city leasehold persistence and permission conventions are the model;
  this work must not change city-lease semantics.

## Risks and rollback

- Claim ownership and permissions are persistent state. Transfers must remove
  mutable permissions atomically around a successfully reserved payment.
- A disabled renew scheduler must preserve due timestamps; reenabling processes
  overdue rows in the ordinary scheduler pass.
- Area/chunk removal is destructive. The existing same-column conflict guard
  and property-clearance route must run before a reset.
- Rollback is by disabling the new settings; records remain inert and must not
  delete areas or debit accounts.

## Validation

- [x] Unit tests cover renew configuration bulk behavior and world-scoped player
  and unclaimed rental persistence.
- [x] The complete source and affected tests compile; the complete Maven suite
  passes after declaring Hamcrest explicitly for the JUnit 4 test launcher.
- [x] The final Land Claim artifact hash is `9efbd000…1d8366dae` both locally
  and on Development; `rw-development` logged `RELOADED ALL PLUGINS` at 23:37:55.

## Implementation checklist

- [x] Add renewal scheduler enable gate and administrator renew-zone tab with
  per-zone and bulk actions.
- [x] Route reset/delete claim removal through the automatic cleanup workflow.
- [x] Add player and unclaimed rental-purchase persistence, guarded settings,
  permission groups, UI and daily billing.
- [x] Add translations, documentation/history/changelog fragment and validate.
