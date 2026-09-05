# OmegaZirkel Land Claim Plugin for Rising World

With this plugin, players can secure and manage their own areas in Rising World. The plugin is operated mainly via intuitive UI elements to ensure easy handling.

Server administrators can configure almost all aspects of the plugin to their liking via the `settings.<world>.json` file. Admins can also import existing zones, although compatibility with other plugins is not guaranteed.

## Prerequisites

This plugin requires the **OmegaZirkel Tools** plugin to be installed.
Use OZTools `0.23.11` or newer with this release.
Wallet and Shop are detected optionally for economy features; core claim protection remains usable without them. Claim sales require Wallet. Extra-claim purchases require Shop and Wallet.

1. Download the latest `oz-tools-....zip` from the [OZ-Tools](https://github.com/Devidian/rw-plugin-oz-tools/releases) Releases Page.
2. Place the downloaded `.zip` file into your server's `Plugins` folder and extract it.

## Installation

1. Download the latest `oz-land-claim-....zip` from the GitHub Releases page.
2. Stop your Rising World server.
3. Place the downloaded `.zip` file into your server's `Plugins` folder and extract it.
4. Start the server. The plugin will generate its default configuration files during the first run.
5. Adjust the settings in `Plugins/OZLandClaim/settings.<world>.json` to your needs and restart the server or use `/lc reload`.

## Features

- **Easy to use:** Players can manage their claims through an intuitive UI.
- **Highly Configurable:** Server admins can tweak almost every aspect of the plugin.
- **Claim Limits:** Control how many chunks a player can claim based on playtime.
- **Global Claim Modes:** Choose time-based, administrative, geometric land-pricing, or city rules without rewriting existing areas.
- **Geometric Land Pricing:** Wallet-backed chunk prices use 26-neighbor three-dimensional clusters, persisted additive surcharges, ceiling rounding, and a safe maximum.
- **Cities and Leaseholds:** Admins can create non-overlapping city cores and leaseholds, manage city treasuries and radius expansion, and offer property for purchase, rent, or rent-to-own.
- **Daily Rent:** Server-local daily billing is idempotent, does not accrue downtime debt, warns low-balance tenants, and returns unpaid property to its city with Mail or login-dialog fallback.
- **Extra Claim Capacity:** Purchased extra-claim capacity is stored separately from playtime-derived limits and is included in max-claim calculations. When Shop and Wallet are installed, LandClaim registers an extra-claim capacity offer with per-player linear pricing.
- **Claim Protection:** Protect inactive players' areas from being claimed by others for a configurable amount of time.
- **Admin Tools:** Includes commands for repairing and managing zones.
- **Admin Cleanup:** Admins can review active claim owners and claimed areas, delete claim records, clean up abandoned chunks, or teleport to listed areas.
- **Special Zones for Admins:** Admins can create special zones such as default special, PvP, rest, trap, and renew zones directly from the UI.
- **Renew Zones:** Renew zones are configurable special zones that reset their chunk columns on a persisted hourly interval. The hourly scheduler starts after the first player joins a server session, so world data is loaded before reset checks run. Admins can create them from the special-zone radial menu and edit the interval with `/lc config` or the admin radial zone configuration action.
- **UI-based Permissions:** Manage who has access to your claimed areas directly in-game.
- **Claim Sales:** Claim sale listings are stored with owner, area, price, listing time, buyer, purchase time, and status. When `allowClaimSale=true`, owners can list an owned area for sale or withdraw its active sale listing from the radial area menu. Active sale areas show a shared Tools indicator, and buyers see listed areas in the current chunk overlay and can buy them through Wallet-backed settlement. A completed purchase withdraws the buyer payment, clears old area permissions, assigns the buyer as the only owner, credits the seller, and rolls back on failure.
- **Recently Online Players:** The permission UI can also list players who were online recently, even if they are offline now.
- **Permission Status Column:** The permission UI shows whether listed players are currently online or offline.
- **Current Chunk Overlay:** A compact top-screen overlay shows whether the current chunk is claimable, part of one of your areas, owned by another player, or a special area. The overlay hides while the inventory is open; the current PluginAPI exposes a reliable inventory toggle event but no equivalent crafting/map visibility hook.
- **Area Frame Visibility:** Owned/other area frame rendering is limited to a persisted per-player three-dimensional chunk radius. The default radius is `15`; players can select `1`, `5`, `10`, `15`, `32`, or `64` chunks in the shared plugin settings. An area that intersects the radius is always rendered in full. Claim protection itself is not filtered by visibility.
- **Special-Zone Visibility:** Admins can independently show or hide neutral, static, PvP, rest, trap, and renew-zone frames for other players. A player always sees the frame of their current special zone; the optional administrator override bypasses these filters.

## Commands

The main command is `/lc`. You can also use the alias `/landclaim`.

| Command           | Description                                     | Permission           |
| :---------------- | :---------------------------------------------- | :------------------- |
| `/lc open`        | Opens the main radial menu for land management. | (everyone)           |
| `/lc config`      | Opens the configuration UI for the current configurable zone. | Admin zone context |
| `/lc status` or `/lc info` | Opens the shared Tools Info/Status panel. | (everyone)           |
| `/lc repairareas` | (Admin) Scans and repairs all claim areas.      | `oz.landclaim.admin` |

## Configuration

All settings can be adjusted in the `settings.<world>.json` file located in the plugin's directory.

| Setting                         | Default | Description                                                                                                    |
| :------------------------------ | :------ | :------------------------------------------------------------------------------------------------------------- |
| `logLevel`                      | `ALL`   | Sets the logging verbosity. Possible values: `OFF`, `FATAL`, `ERROR`, `WARN`, `INFO`, `DEBUG`, `TRACE`, `ALL`. |
| `claimMode`                     | `TIME_BASED` | Global mode: `TIME_BASED`, `ADMINISTRATIVE`, `LAND_PRICING`, or `CITY`. |
| `landPriceBase`                 | `1000` | Base chunk price in land-pricing mode. |
| `landPriceClusterIncrement`     | `0.05` | Additive price multiplier for every occupied chunk in adjacent geometric clusters. |
| `cityBaseRadius`                | `2` | Initial three-dimensional city radius in chunks. |
| `cityAllowPrivateClaims`        | `false` | Global default for private player claims inside cities. |
| `cityPrivateClaimPrice`         | `10000` | Global price per private city chunk. |
| `cityExpansionBasePrice`        | `50` | Price per newly covered chunk for city radius expansion. |
| `cityRentBillingHour`           | `0` | Server-local daily rent billing hour, from 0 through 23. |
| `minutesToClaim`                | `10`    | The minimum time in minutes a player must stay in a chunk before they are allowed to claim it.                 |
| `basicClaimLimit`               | `1`     | The initial number of chunks a player can claim.                                                               |
| `playTimeHoursExtraClaimFactor` | `0.6`   | A factor to calculate additional claim slots based on playtime. `HoursPlayed * Factor = ExtraClaims`.          |
| `claimProtectionBaseTimeDays`   | `7`     | The minimum number of offline days before another player can claim a chunk.                                    |
| `claimProtectionExtraTimeScale` | `5`     | A multiplier for playtime to extend claim protection. `DaysOffline > BaseTime + (PlayTimeDays * Scale)`.       |
| `enableClaimAnnouncement`       | `true`  | If `true`, a message is broadcast to all players when a chunk is claimed.                                      |
| `enableWelcomeMessage`          | `false` | If `true`, players receive a welcome message from the plugin upon login.                                       |
| `recentlyOnlinePermissionListHours` | `24` | Lists players in the area permission UI if they were online within the last configured number of hours. `0` disables this. |
| `enableAutoClaimRemoval`            | `false` | Runs one delayed server-start cleanup for owners inactive longer than the configured threshold. This removes claims and areas but does not reset chunks. |
| `autoClaimRemovalInactiveDays`      | `90` | Inactivity threshold in days for automatic claim removal. |
| `autoClaimRemovalDelaySeconds`      | `60` | Delay after server start before automatic claim removal runs. |
| `renewZoneDefaultIntervalHours`     | `24` | Default interval in hours for newly created renew zones. |
| `renewZoneResetAnnouncementTarget`  | `none` | Who receives renew-zone reset announcements: `none`, `all`, or `admins`. |
| `renewZoneResetBaseDelaySeconds`    | `2` | Minimum delay before processing the next due renew zone. |
| `renewZoneResetDelayPerChunkMillis` | `25` | Additional delay per reset chunk column before the next due renew zone. |
| `renewZoneResetMaxDelaySeconds`     | `60` | Upper limit for the delay between due renew zones. |
| `discordRenewZoneLogChannelId`      | `0` | Discord channel id for renew-zone reset logs. `0` disables logging. |
| `allowClaimSale`                    | `false` | Enables owner sale listings and Wallet-backed claim purchases when Wallet is installed. |
| `allowClaimBuyExceedLimit`          | `false` | Allows claim purchases to exceed the buyer's normal claim limit. Keep disabled unless this is intentional. |
| `exposeClaimSales`                  | `true`  | Exposes active claim-sale listing metadata at `/plugins/oz---land-claim/claim-sales`. |
| `exposeRenewZones`                  | `true`  | Exposes renew-zone metadata at `/plugins/oz---land-claim/renew-zones`. |
| `allowAdminOverride`                 | `false` | Lets administrators bypass special-zone frame visibility filters. |
| `showSpecialAreaFrames`              | `true`  | Shows neutral special-zone frames for other players. |
| `showStaticAreaFrames`               | `true`  | Shows static-zone frames for other players. |
| `showPvPAreaFrames`                  | `true`  | Shows PvP-zone frames for other players. |
| `showRestAreaFrames`                 | `true`  | Shows rest-zone frames for other players. |
| `showTrapAreaFrames`                 | `true`  | Shows trap-zone frames for other players. |
| `showRenewAreaFrames`                | `false` | Shows renew-zone frames for other players. |
| `enableExtraClaimShopOffer`         | `true` | Registers an extra-claim capacity offer in OZ Shop when Shop is installed. |
| `extraClaimBasePrice`               | `200` | Price for the first purchased extra-claim capacity. |
| `extraClaimPriceIncreasePercent`    | `10` | Linear percent increase per already purchased extra-claim capacity. |
| `extraClaimShopCurrencyIdentifier`  | empty | Currency identifier for the extra-claim Shop offer. Empty uses the Wallet default currency. |

### Color Configuration

You can override the default colors for the chunk visualizations by uncommenting and changing the hex values in the settings file.

- `currentChunkBorderColor` / `currentChunkFrameColor`
- `ownedAreaBorderColor` / `ownedAreaFrameColor`
- `otherAreaBorderColor` / `otherAreaFrameColor`
- `forSaleAreaBorderColor` / `forSaleAreaFrameColor`
- `specialAreaBorderColor` / `specialAreaFrameColor`
- `staticAreaBorderColor` / `staticAreaFrameColor`
- `pvpAreaBorderColor` / `pvpAreaFrameColor`
- `restAreaBorderColor` / `restAreaFrameColor`
- `trapAreaBorderColor` / `trapAreaFrameColor`
- `renewAreaBorderColor` / `renewAreaFrameColor`

### Migration Notes

Claim economy support adds SQLite tables for purchased capacity, sale listings, geometric price surcharges, cities, leaseholds, pending notices, and economy-operation reconciliation. They are created additively on startup; existing areas and claims are not rewritten. An absent `claimMode` remains `TIME_BASED`.

Route-ready claim-sale exports read active listings from `claimSaleListings` by `world` and use `listed_at` as the `lastChange` cursor. LandClaim exports only plugin-owned sale metadata; world area geometry remains owned by world/AdminUtils routes.

Route-ready renew-zone exports read `renewZoneConfigs` by `world` and use `updated_at` as the `lastChange` cursor. The payload includes `areaId`, `intervalHours`, `lastResetAt`, `nextRenewalAt`, and the configured renew-zone colors so manager map layers can decorate the matching area geometry.

### Future Settings (Not yet implemented)

The settings file contains commented-out options for upcoming claim costs and sale fees.

- `claimBaseCost`
- `claimSaleFee`

## Future plans / ideas

- team-mode? Claim areas by teams of players, players in the same group count together their playtime.

## Attribution

Uicons by [Flaticon]("https://www.flaticon.com/uicons")

---

_This README was partially generated and improved by Gemini Code Assist._

## Release Notes

- `0.7.1`: fixed admin special-zone creation when no current area existed in the selected chunk.

## Contributor Workflow

- Review `AGENTS.md`, `PLANS.md`, `.codex/agents.toml`, and `.codex/skills/` before making structural changes.
- Verify Rising World API usage with `scripts/verify-plugin-api.sh` when adding or changing API calls.
- Run `mvn -B -DskipTests package` and `mvn -B test` before release-facing changes are merged.
- Use `RUNTIME_TESTING.md` and `scripts/docker-runtime-smoke.sh <PluginFolderName>` for runtime smoke tests when behavior changes need server validation.
- Keep `README.md` and `HISTORY.md` current and use Conventional Commit titles for commits and PRs.

## JSON-only distribution

Settings defaults (`settings.default.json`) and translations (`i18n/*.json`)
are shipped only as JSON. Legacy default and translation `.properties` files
are no longer included. Runtime settings remain world-scoped as
`settings.<world>.json`; migration of an existing `settings.properties` and
its backup remains supported. Updating the package does not delete old files
already present on the server. Use `mvn clean package` for a fresh local
package; ZIP assembly also excludes stale legacy settings and translations.
