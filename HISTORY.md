# History / Changelog / Commitlog

<https://www.conventionalcommits.org/en/v1.0.0/>

## [unreleased/patches]

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

## [0.1.0] - 2025-12-18 | Initial release
