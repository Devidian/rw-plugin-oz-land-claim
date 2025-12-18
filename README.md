# OmegaZirkel Land Claim Plugin for Rising World

With this plugin, players can secure and manage their own areas in Rising World. The plugin is operated mainly via intuitive UI elements to ensure easy handling.

Server administrators can configure almost all aspects of the plugin to their liking via the `settings.properties` file. Admins can also import existing zones, although compatibility with other plugins is not guaranteed.

## Prerequisites

This plugin requires the **OmegaZirkel Tools** plugin to be installed.

1. Download the latest `oz-tools-....zip` from the [OZ-Tools](https://github.com/Devidian/rw-plugin-oz-tools/releases) Releases Page.
2. Place the downloaded `.zip` file into your server's `Plugins` folder and extract it.

## Installation

1. Download the latest `oz-land-claim-....zip` from the GitHub Releases page.
2. Stop your Rising World server.
3. Place the downloaded `.zip` file into your server's `Plugins` folder and extract it.
4. Start the server. The plugin will generate its default configuration files during the first run.
5. Adjust the settings in `Plugins/OZLandClaim/settings.properties` to your needs and restart the server or use `/lc reload`.

## Features

- **Easy to use:** Players can manage their claims through an intuitive UI.
- **Highly Configurable:** Server admins can tweak almost every aspect of the plugin.
- **Claim Limits:** Control how many chunks a player can claim based on playtime.
- **Claim Protection:** Protect inactive players' areas from being claimed by others for a configurable amount of time.
- **Admin Tools:** Includes commands for repairing and managing zones.
- **UI-based Permissions:** Manage who has access to your claimed areas directly in-game.

## Commands

The main command is `/lc`. You can also use the alias `/landclaim`.

| Command           | Description                                     | Permission           |
| :---------------- | :---------------------------------------------- | :------------------- |
| `/lc open`        | Opens the main radial menu for land management. | (everyone)           |
| `/lc status`      | Shows the current status of the plugin.         | (everyone)           |
| `/lc repairareas` | (Admin) Scans and repairs all claim areas.      | `oz.landclaim.admin` |

## Configuration

All settings can be adjusted in the `settings.properties` file located in the plugin's directory.

| Setting                         | Default | Description                                                                                                    |
| :------------------------------ | :------ | :------------------------------------------------------------------------------------------------------------- |
| `logLevel`                      | `ALL`   | Sets the logging verbosity. Possible values: `OFF`, `FATAL`, `ERROR`, `WARN`, `INFO`, `DEBUG`, `TRACE`, `ALL`. |
| `minutesToClaim`                | `10`    | The minimum time in minutes a player must stay in a chunk before they are allowed to claim it.                 |
| `basicClaimLimit`               | `1`     | The initial number of chunks a player can claim.                                                               |
| `playTimeHoursExtraClaimFactor` | `0.6`   | A factor to calculate additional claim slots based on playtime. `HoursPlayed * Factor = ExtraClaims`.          |
| `claimProtectionBaseTimeDays`   | `7`     | The minimum number of offline days before another player can claim a chunk.                                    |
| `claimProtectionExtraTimeScale` | `5`     | A multiplier for playtime to extend claim protection. `DaysOffline > BaseTime + (PlayTimeDays * Scale)`.       |
| `enableClaimAnnouncement`       | `true`  | If `true`, a message is broadcast to all players when a chunk is claimed.                                      |
| `enableWelcomeMessage`          | `true`  | If `true`, players receive a welcome message from the plugin upon login.                                       |

### Color Configuration

You can override the default colors for the chunk visualizations by uncommenting and changing the hex values in the settings file.

- `currentChunkBorderColor` / `currentChunkFrameColor`
- `ownedChunkBorderColor` / `ownedChunkFrameColor`
- `otherChunkBorderColor` / `otherChunkFrameColor`
- `forSaleChunkBorderColor` / `forSaleChunkFrameColor`
- `specialChunkBorderColor` / `specialChunkFrameColor`

### Future Settings (Not yet implemented)

The settings file contains commented-out options for upcoming features like claim costs and selling.

- `claimBaseCost`
- `claimSaleFee`
- `allowClaimSale`
- `allowClaimBuyExceedLimit`

## Future plans / ideas

- team-mode? Claim areas by teams of players, players in the same group count together their playtime.

## Attribution

Uicons by [Flaticon]("https://www.flaticon.com/uicons")

---

_This README was partially generated and improved by Gemini Code Assist._
