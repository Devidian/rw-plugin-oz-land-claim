# Roadmap Plan 04 Plugin Runtime Standards

## Objective
Apply Plan 04 portfolio runtime standards to LandClaim without changing claim ownership or protection rules.

## Ownership
Primary repository: `rw-plugin-oz-land-claim`

Supporting repositories:
- `rw-plugin-oz-tools` for shared settings, i18n, persistence, and overlay behavior.

## Dependencies
- Hard runtime dependency: `rw-plugin-oz-tools`.
- Existing claim persistence and area permission behavior must remain compatible.

## Phases
- [x] Phase 1: Audit for deprecated Tools `SQLite` usage and migrate to `SQLiteConnectionFactory` if needed.
- [x] Phase 2: Verify i18n files are loaded only once during `onEnable`.
- [x] Phase 3: Add PlayerPluginSettings shortcut visibility for `/ozt open` and inventory entry, defaulting to visible.
- [x] Phase 4: Document the Escape-close API limitation for open LandClaim panels.
- [x] Phase 5: Verify persisted runtime data remains SQLite/world-safe.
- [x] Phase 6: Update README/HISTORY and validate.

## Implementation Notes
- LandClaim already uses `SQLiteConnectionFactory` for claim, extra-capacity, sale-listing, and player-setting persistence.
- LandClaim loads i18n once through `I18n.getInstance(this)` during enable.
- The player settings panel now includes a default-visible LandClaim shortcut setting.
- Custom-overlay Escape behavior is deferred to the future Rising World API layer.

## Risks
- Persistence cleanup must not alter existing claim ownership, permissions, or special zone behavior.

## Validation Strategy
- Run `mvn -B test` and `mvn -B -DskipTests package`.
- Runtime-smoke claim panel open/close, shortcut visibility, and existing claim visibility/protection behavior.

## Affected Repositories/Plugins
- `rw-plugin-oz-land-claim`
- `rw-plugin-oz-tools`

## Rollback Considerations
Keep changes limited to runtime standards and UI behavior. Avoid persistence shape changes unless the audit finds deprecated APIs that must be migrated.
