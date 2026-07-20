# History / Changelog / Commitlog

<https://www.conventionalcommits.org/en/v1.0.0/>

## [0.13.4] - 2026-07-20 | Advanced button controls

- change: use the stable shared OZ button controls in claim and administration overlays

## [0.13.3] - 2026-07-20 | Settings completion

- change: Discord announcements use their configured channel IDs directly
- feat: expose decimal claim timing and sale settings in the admin UI

## [0.13.2] - 2026-07-20 | Update metadata

- change: publish the canonical GitHub release source for OZ Tools update management

## [0.13.1] - 2026-07-17 | Localized economy integration

- feat: expose localized Shop offer data through the updated Wallet integration

## [0.13.0] - 2026-07-15 | Special-zone visibility controls

- feat: add per-zone frame visibility controls for neutral, static, PvP, rest, trap, and renew areas
- feat: keep the frame of a player's current special zone visible and allow an explicit administrator override
- fix: refresh special-zone frames when settings change and when players enter or leave an area

## [0.12.1] - 2026-07-14 | Icon set and renew-zone stability

- change: use shared OZTools ownership for debug and tools menu icons
- change: rename LandClaim icon keys to their final semantic names
- change: regenerate all Classic Land Claim icons with brighter fills and high-contrast navy contours for dark in-game backgrounds
- fix: process due renew zones one at a time and delay subsequent resets based on the number of reset chunk columns

## [0.12.0] - 2026-07-06 | Renew zones

- feat: add configurable renew zones with persisted interval, hourly reset scheduler, announcements, Discord logging, and manager map export metadata
- fix: start the renew-zone hourly scheduler after the first player joins a server session to avoid pre-world-load reset checks
- feat: add route-ready active claim-sale export DTOs, service, and exposure setting for manager bridges

## [0.11.2] - 2026-06-13 | Permission UI and claim icon

- refactor: align area permissions with the shared plugin overlay layout
- fix: move area owner status to the footer and use the inherited top-right close button
- fix: improve special-area name contrast on the dark chunk-info overlay
- fix: use the available owned-claim icon for the extra-claim Shop offer

## [0.11.1] - 2026-06-11 | Restore deleted icon

- fix: restore deleted icon

## [0.11.0] - 2026-06-10 | Static area support and new icons

- feat: new special area "static" added, like "visitors only" area
- change: exchanged all flat icons with AI created icons to align with other plugins

## [0.10.0] - 2026-06-08 | Shortcut visibility and shutdown cleanup

- feat: add LandClaim player shortcut visibility setting for `/ozt` and inventory shortcuts
- change: remove obsolete shared escape-close registrations pending future API support
- change: close LandClaim SQLite connection on plugin shutdown

## [0.9.0] - 2026-05-26 | Claim economy and shared indicators

- feat: add a shared Tools indicator for active claim-sale areas
- feat: limit area-frame rendering to the current and neighboring sectors when sector/chunk coordinates are safely derivable
- feat: refresh LandClaim area frames when players enter a new sector
- feat: add LandClaim radial Info/Status menu action with the shared Tools info icon
- feat: add shared Tools Info/Status panel content for LandClaim and route `/lc status` to it
- feat: complete grouped admin settings metadata and i18n labels for LandClaim settings
- refactor: keep LandClaim settings reload logging on the main `OZ.LandClaim` logger only
- feat: add optional Wallet/Shop economy integration detection for claim economy work
- feat: persist purchased extra-claim capacity and include it in player claim limits
- feat: register an extra-claim capacity Shop offer with per-player linear pricing
- feat: add additive claim-sale listing persistence with active, withdrawn, and sold status tracking
- feat: add owner radial actions to list an owned area for sale or withdraw its active sale listing
- feat: show listed-for-sale areas to buyers and add a buyer radial entry for the upcoming purchase flow
- feat: implement Wallet-backed claim purchases with ownership transfer, permission reset, seller credit, and rollback handling
- feat: show listed-for-sale areas with configurable sale frame colors
- docs: document claim economy settings, install scope, and automatic migration behavior

## [0.8.0] - 2026-05-18 | Player data tab and claim visibility refresh

- feat: add a special properties tab to admin cleanup for non-claim server areas
- fix: shorten admin cleanup owner rows without narrowing the table
- fix: restore colored one-line plugin welcome message
- feat: add admin claim cleanup overlay with owner and area cleanup tabs
- fix: rename and tighten the admin property management UI, action colors, table labels, and teleport targets
- feat: add optional delayed auto-removal for claims owned by inactive players
- fix: show the selected player name in the fixed permission dropdown and increase its height so all options fit
- fix: align the area permission overlay frame with wallet styling, reduce the permission table height, and keep permission dropdowns above table rows
- fix: anchor permission dropdowns at a fixed scroll-independent Y position so they no longer drift down after scrolling
- fix: align confirm and text-input dialogs with the LandClaim panel styling
- feat: restyle the current-chunk overlay as a top-screen permission-style panel with area names and inventory-open hiding
- fix: make the current-chunk overlay more compact and move it closer to the top edge
- build: require OZTools `0.18.0`
- feat: show persisted player visibility and overlay settings in the shared player plugin data tab
- fix: persist shared player plugin settings changes through the same PlayerSettings table as the radial visibility menu
- fix: refresh existing visible area frames when a claimed area is expanded

## [0.7.3] - 2026-05-10 | PluginAPI alignment and area workaround cleanup

- build: align bundled PluginAPI jar and Maven dependency version
- refactor: remove obsolete area update and direct SQL workarounds

## [0.7.2] - 2026-03-13 | Fix special areas and expanding areas

- fix: special zones are marked as owned for admins (they must not)
  - existing areas will be cleaned when expanded again.
- fix: expanding an area is not returning to the expand menu again (player.getCurrentArea()=null)

## [0.7.1] - 2026-03-11 | Patch release

- fix: admins can create special zones again even when no current area exists in the selected chunk

## [0.7.0] - 2026-03-11 | Area permission activity list

- docs: standardize agent prompts, PR checklist, and runtime smoke-test guidance
- build: add API verification helper and stricter CI/release validation flow
- feat: include recently online players in the area permission list via configurable Players-DB lookup
- feat: show online or offline status in the area permission table
- build: package only `README.md` and `HISTORY.md` into release artifacts

- refactor: used `player.getCurrentArea();` where possible [0.6.1]
  - this will reduce Server.getAllAreas calls
- fix: impossible to claim because `null` area results in false for singleChunkCheck [0.6.2]
- fix: wrong area used for claim method (`null` area from `uiPlayer.getCurrentArea();`)

BREAKING: this release requires OZTools `>= 0.17.0`

## [0.6.0] - 2026-02-05 | Moved utility methods to OZTools

- refactor: ChunkClaimUtil internally using `OZTools.AreaUtils`
  - `chunksToArea` redirected to `AreaUtils.chunksToArea`
  - `isAreaIntersecting` redirected to `AreaUtils.isAreaIntersecting`
  - `getVirtualAreaFromChunkVector` redirected to `AreaUtils.getVirtualAreaFromChunkVector`
  - this is done because these methods can now be used in other plugins
- refactor: changed default permissions [0.5.2]
  - ozlc-owner.general.editnpc is now `true`
  - ozlc-resident.general.editnpc is now `true`
  - ozlc-prisoner.general_ridemounts is now `false`
- fix: no db entry was created for new chunks [0.5.2]
- fix: cache was cleared before indexing [0.5.1]

BREAKING: due to refactoring it is mandatory to install OZTools `>= 0.16.0`

## [0.5.0] - 2026-01-27 | Database interface refactoring

- refactor: now using new database interface from OZTools with caching
  - to prevent sql exceptions due to closed statements for example
- fix: rs / result = null error message.
- fix: expand areas with negative x/z
- fix: prevent expand area to claim special areas

## [0.4.0] - 2026-01-05 | Player plugin settings implemented

- feat: player plugin settings implemented
- fix: botLang is null when Discord Connect is not found [0.3.1]

## [0.3.0] - 2025-12-28 | Changed Consumer to Callback

[BREAKING]

- build: needs OZ Tools v0.13.0+
- refactor: added PermissionOverlay (fixes issue with permission selector stay open)
- refactor: replaced Consumer with Callback (java -> rw api)

## [0.2.0] - 2025-12-24 | Persisting settings to SQLite

- feat: settings are now persisted to SQLite [0.2.0]
- feat: player gets message if area is renamed [0.1.4]
- refactor: now `/lc` opens the menu too (instead of doing nothing) [0.1.2]
- refactor: default area permissions adjusted [0.1.4]
- refactor: moved area3d methods to dedicated class [0.1.5]
- fix: missing translation for changing player permissions of a zone [0.1.1]
- fix: expanding area did not take origin permisssions into account [0.1.3]
- fix: Dropdown z-index issue workaround in PermissionPanel [0.1.3]
- fix: rename area now using direct SQL to Area db [0.1.5]
- fix: default-permissions in permissions panel from other area types are hidden now [0.1.5]
- fix: use `area.setName` before `Server.addArea`, `area.setPlayerPermission` after [0.1.5]
- fix: some more possible null pointer exceptions with area names [0.1.5]
- fix: if x or z were negative area was miscalculated [0.1.6]
- fix: possible null pointer exceptions [0.1.7]

## [0.1.0] - 2025-12-18 | Initial release
