# Chunk Info Refresh Stability

## Objective
Stabilize the upper claim-information overlay when its countdown changes or a
player enters another claim, including servers where the periodic refresh path
fails for one player.

## Ownership
Owning repository/plugin: `rw-plugin-oz-land-claim`

Supporting repositories/plugins: none

## Dependencies
- Runtime: existing Rising World PluginAPI `Timer`, player events, and `Plugin.enqueue`
- Build: Java 20 and the existing Maven configuration
- Optional integrations: none

## Risks
- Refreshing during `PlayerEnterChunkEvent` can still observe the old current
  area; defer the targeted refresh to the next server-thread cycle.
- A failing player update must not stop refreshes for other players; isolate and
  rate-limit failure logging per controller.
- Avoid repeated remove/add operations for an already visible overlay, because
  they create unnecessary structural UI synchronization.

## Validation Strategy
- [x] Add focused state-transition regression tests for overlay visibility and text updates.
- [x] Run `mvn -B test`.
- [x] Run `mvn -B -DskipTests package`.
- [x] Run entry-point architecture and PluginAPI verification scripts.
- [x] Run `git diff --check`.
- [x] Deploy Land Claim through `dev-upload.sh` and verify plugin reload/start logs.
- [x] Confirm countdown and claim-transition refreshes on a development server.

## Affected Repositories/Plugins
- `rw-plugin-oz-land-claim`

## Rollback Considerations
No database, configuration, or public API migration is involved. The change can
be rolled back by restoring the previous chunk-info manager, controller, and
overlay implementations.

## Implementation Checklist
- [x] Keep the overlay mounted while visible and update only changed label text.
- [x] Hide the overlay idempotently for inventory and disconnect cleanup.
- [x] Refresh immediately after connect, spawn, and chunk transitions.
- [x] Initialize already connected players when the manager starts.
- [x] Isolate timer failures per player and log only the first consecutive failure.
- [x] Update `HISTORY.md`.
