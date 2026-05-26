# Roadmap Plan 03 Sector Visibility

## Objective
Limit visible claim areas to the player's current sector and neighboring sectors so area visibility transitions remain stable near sector borders.

## Ownership
Primary repository: `rw-plugin-oz-land-claim`

Supporting repository:
- `rw-plugin-oz-tools` for shared runtime helpers only if existing helpers are needed.

## Dependencies
- Hard runtime dependency: `rw-plugin-oz-tools`.
- Claim protection and permission behavior must remain unchanged; this is a visibility/rendering concern.

## Phases
- [x] Phase 1: Identify the current claim visibility refresh path and the coordinate/sector model it uses.
- [x] Phase 2: Filter visible claims to current sector plus neighboring sectors before rendering or sending UI updates.
- [x] Phase 3: Verify border behavior so adjacent-sector areas are visible before the player crosses a sector boundary.
- [x] Phase 4: Add a radial-menu Info/Status button in the LandClaim main menu.
- [x] Phase 5: Update README/HISTORY and validate.

## Risks
- Incorrect sector math could hide claims too aggressively and confuse players near borders.
- Visibility filtering must not weaken actual protection checks.
- Large claim datasets may need efficient lookup rather than scanning all claims per refresh.

## Validation Strategy
- Run `mvn -B -DskipTests package`.
- Run `mvn -B test`.
- Runtime-smoke current-sector, neighboring-sector, diagonal-neighbor, and far-sector claim visibility.
- Verify protection still applies to hidden far-sector claims when relevant server events occur.

## Affected Repositories/Plugins
- `rw-plugin-oz-land-claim`
- `rw-plugin-oz-tools`

## Rollback Considerations
Keep filtering at the display layer where possible. If visibility behavior is wrong, disable the sector filter without changing claim persistence.

## Progress Notes
- Phase 1 complete: claim area-frame visibility is rendered in `Area3DUtils.updateAreaFramesForPlayer`, which previously iterated every `Server.getAllAreas()` result for each player.
- Phase 2 complete: `Area3DUtils` now filters frame rendering to the player's current sector plus neighboring sectors when the runtime sector position matches the chunk-derived sector model. If coordinates cannot be derived safely, it falls back to showing rather than hiding.
- Phase 3 complete at code level: the filter includes current sector, direct neighbors, and diagonal neighbors before a sector crossing; `PlayerEnterSectorEvent` refreshes frames after a sector transition.
- Phase 4 complete: the LandClaim radial menu includes an Info/Status action using the shared Tools `icon-ki-info-status` asset, and `/lc info` routes to the same panel as `/lc status`.
- Phase 5 complete: README/HISTORY were updated and validation passed with `mvn -B test` and `mvn -B -DskipTests package`.
