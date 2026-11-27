# Claim acquisition, protection and area permissions
<!-- codemap-id: rw-plugin-oz-land-claim/claims -->

## Responsibility

Claim acquisition, protection and area permissions. Claim records and permission behavior belong to Land Claim. Verify protection event paths and claim boundaries in source.

## Source and tests

- [`LandClaimRuntime`](../../src/de/omegazirkel/risingworld/LandClaimRuntime.java)
- [`ChunkClaimUtil`](../../src/de/omegazirkel/risingworld/landclaim/ChunkClaimUtil.java)
- [`ClaimModePolicy`](../../src/de/omegazirkel/risingworld/landclaim/ClaimModePolicy.java)
- [`LandClaimChunkStore`](../../src/de/omegazirkel/risingworld/landclaim/db/LandClaimChunkStore.java)
- [`AreaPermissionPanel`](../../src/de/omegazirkel/risingworld/landclaim/ui/AreaPermissionPanel.java)
- [`PermissionFileUtil`](../../src/de/omegazirkel/risingworld/landclaim/PermissionFileUtil.java) delegates copying to Tools. Land Claim owns the packaged templates and selects which files to install.

## Dependencies

- `rw-plugin-oz-tools/shared-api` — shared area permission template copying. [Evidence](../../src/de/omegazirkel/risingworld/landclaim/PermissionFileUtil.java)

## See also

See the [repository index](../README.md) for lifecycle, build and cross-repository dependencies.
