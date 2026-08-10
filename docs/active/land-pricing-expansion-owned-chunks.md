# Land-pricing expansion with already-owned chunks

## Objective

Allow a player claim to expand across adjacent chunks already owned by the same player without consuming claim capacity or charging them again. The expansion must still charge and consume capacity for newly acquired chunks only.

## Ownership

Owning repository/plugin: `rw-plugin-oz-land-claim`

Supporting repositories/plugins: `none` (Wallet is used through the existing integration only).

## Dependencies

- Runtime: Rising World area and chunk ownership APIs.
- Build: existing OZ Tools dependency.
- Optional integrations: Wallet in land-pricing mode.

## Risks

- An already-owned adjacent chunk may belong to a separate player area; the existing merge path must remain intact.
- Capacity, claim-time, and price calculations must all use the same set of newly acquired chunks.

## Validation Strategy

- [x] `mvn -B -Dmaven.repo.local=/tmp/next-080826-m2 test package` (the fresh task cache could not resolve the published OZ Tools artifact).
- [x] Verify plugin API and sole-listener architecture.
- [x] Deploy only Land Claim to `rw-server-dev` and inspect post-reload logs.

## Affected Repositories/Plugins

- `rw-plugin-oz-land-claim`

## Rollback Considerations

Revert the expansion delta calculation; no persistence or configuration migration is introduced.

## Implementation Checklist

- [x] Count only non-owned perimeter chunks toward capacity and claim-time expansion work.
- [x] Preserve the existing ownership merge and land-price calculation.
- [x] Update history.
- [x] Validate and deploy to Dev.
- [x] Release `0.16.1` with player-facing forum entry and portfolio update.
