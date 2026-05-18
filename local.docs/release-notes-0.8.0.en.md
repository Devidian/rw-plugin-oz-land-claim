# OZ Land Claim 0.8.0

This release updates Land Claim for OZTools 0.18.0 and improves claim administration, overlays, and player setting visibility.

## Highlights

- Admins get a property management view for land owners, claimed areas, and special properties.
- Optional automatic claim removal can clean up owners inactive for a configured number of days after server start.
- The current chunk overlay was restyled as a compact top-screen panel and hides while the inventory is open.
- Player visibility and overlay settings are now visible in the shared plugin data tab.
- Shared player plugin settings now persist through the same player settings database used by the radial visibility menu.
- Visible area frames now refresh immediately after a claimed area is expanded.
- Dialogs, permission tables, dropdowns, and cleanup table rows received layout fixes.

## Installation

Update both plugins:

- `OZTools` `0.18.0`
- `OZLandClaim` `0.8.0`

No manual database migration is required. Existing persisted player settings remain in the OZTools player settings tables.

## Roadmap

Claim buying/selling, purchasable extra claim capacity, and a possible shared shop plugin/interface are documented as future work in `docs/roadmaps/claim-economy-and-shop.md`.
