# Renew Zone Roadmap

## Objective
Add a configurable LandClaim special zone that periodically renatures its area by deleting/resetting the chunks in that zone.

## Ownership
Primary repository:
- `rw-plugin-oz-land-claim`

Supporting repositories:
- `rw-manager-backend` for map export consumption.
- Manager frontend/map renderer for visualizing renew zones and next renewal time.
- `rw-plugin-oz-discord-connect` only as optional message transport.

## Dependencies
- Rising World area and chunk reset APIs.
- Existing LandClaim special-zone permission and radial-menu behavior.
- Existing SQLite connection and plugin settings infrastructure.
- Existing permission copy flow from `src/main/resources/permissions`.
- A bridge/export contract that lets manager backend consume renew-zone metadata.

## Phases
- [x] Phase 1: Register the renew zone as a first-class special-zone type with permission file copy, menu entry, icon, i18n labels, and distinct overlay colors.
- [x] Phase 2: Add renew-zone persistence table with `areaId`, interval hours, and last reset timestamp plus migration-safe initialization.
- [x] Phase 3: Add `/lc config` and admin radial-menu access for zones with config UI, starting with renew-zone interval editing.
- [x] Phase 4: Implement hourly scheduler that checks renew zones on the full hour and resets due zone chunk columns.
- [x] Phase 5: Add configurable reset announcements: none by default, all players, or admins only.
- [x] Phase 6: Add optional Discord reset event logging through a configurable channel id where `0` disables logging.
- [x] Phase 7: Expose renew-zone bridge data for manager backend, including zone color and next renewal time.
- [x] Phase 8: Update manager backend/frontend map handling so renew zones display with the correct color and next renewal time.
- [x] Phase 9: Complete documentation, release notes, and end-to-end validation.

## Risks
- Chunk reset is destructive and must not affect unrelated claimed columns.
- Existing cleanup reset behavior resets whole chunk columns by `x:z`; renew zones need the same API limitation reviewed before enabling automatic execution.
- Area IDs can disappear or be recreated; stale renew-zone rows need cleanup behavior.
- Manager bridge changes cross repository ownership and must be kept contract-compatible.
- Full-hour scheduling must avoid duplicate execution after reloads or long server stalls.

## Validation Strategy
- Run `mvn -B -DskipTests package` for each build-impacting LandClaim phase.
- Run `mvn -B test` when persistence or export tests are added/changed.
- Verify permission files are copied and `reloadpermissions` is triggered only when a missing file is copied.
- Verify renew-zone creation, color rendering, and special-zone cleanup behavior in-game or with API-level smoke checks.
- Verify manager map output contains renew-zone metadata and frontend displays next renewal time.

## Affected Repositories/Plugins
- `rw-plugin-oz-land-claim`
- `rw-manager-backend`
- Manager frontend/map renderer
- `rw-plugin-oz-discord-connect` optional integration

## Rollback Considerations
- Phase 1 can be rolled back by removing the renew special-zone menu/settings/color entries while leaving existing areas as generic special areas.
- Persistence phases must tolerate stale table rows and should not delete server areas during rollback.
- Scheduler execution must be gated by persisted renew-zone config; disabling/removing rows stops automatic resets without removing areas.

## Current Status
Phases 1 through 9 are complete. In-game validation confirmed renew-zone chunk reset behavior; the scheduler now starts after the first player joins a server session to avoid pre-world-load reset checks.
