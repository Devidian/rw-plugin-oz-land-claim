# Claim modes and cities

Implementation ownership and the complete cross-plugin contract live in
`../../../docs/active/land-claim-modes-and-wallet-system-accounts.md` from the
workspace root.

Implemented in this repository:

- global `claimMode` policy with mode-safe sales and Wallet availability gates;
- geometric land-price surcharge persistence and 26-neighbor cluster pricing;
- city, leasehold, pending-notification, and economy-operation tables;
- city/private-claim/leasehold settlement through Wallet system accounts;
- daily idempotent rent billing without downtime catch-up;
- durable Mail fallback notices with persisted last-known player language;
- city and leasehold administration overlays and DE/EN UI;
- city-core and leasehold permissions and icon assets.

Validation requires unit tests, package validation, sole-listener verification,
and a development-server runtime reload. Player purchase, rent, eviction, mode
switching, and UI acceptance remain distinct manual checks.

## 2026-08-05 City-mode follow-up

Objective: complete the city-mode administration and player purchase feedback
without changing claim-mode persistence or Wallet settlement contracts.

- [x] Present city treasury, leasehold occupancy, and active daily rent income
      in the city overview; make unavailable city expansion visually disabled.
- [x] Correct the city list wording, sector/position columns, pagination footer,
      and action-cell layout.
- [x] Replace leasehold purchase/rent toggles with switches.
- [x] Give city cores and available/occupied leaseholds distinct configurable
      frame colours.
- [x] Localize insufficient-funds feedback, permit leasehold owners to rename,
      and add price-bearing confirmations for city private-claim purchase and
      expansion.
- [x] Validate focused tests and package build; retain manual game UI and
      Wallet transaction acceptance as follow-up checks.

## 2026-08-05 City-overlay and expansion follow-up

- [x] Identify city cores and free/occupied leaseholds in the claim-info overlay.
- [x] Disable impossible city expansions before confirmation; the existing
      settlement path debits the city account and credits the Wallet world
      treasury.
- [x] Validate tests and package build; confirm a funded in-game expansion and
      its resulting World-treasury audit row manually after deployment.
