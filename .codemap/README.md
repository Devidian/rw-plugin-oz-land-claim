# rw-plugin-oz-land-claim
<!-- codemap-id: rw-plugin-oz-land-claim -->

## Purpose

Claims, area permissions, zones, leases and land transactions. The [plugin descriptor](../src/resources/plugin.yml) names [LandClaim](../src/de/omegazirkel/risingworld/LandClaim.java) as its lifecycle entry.

## Modules

- [claims](modules/claims.md) — Claim acquisition, protection and area permissions.
- [leases-exports](modules/leases-exports.md) — Lease, sale, city and map export services.

## Build and tests

[Maven](../pom.xml) defines the Java build. Follow [repository validation](../AGENTS.md) for packaging, API checks and runtime validation. Relevant automated tests are under [src-test](../src-test/).

## Dependencies

- `rw-plugin-oz-tools/shared-api` — build and runtime shared infrastructure. [Evidence](../pom.xml)
- `rw-plugin-oz-wallet/ledger` — optional economy integration. [Evidence](../src/de/omegazirkel/risingworld/landclaim/EconomyIntegration.java)
- `rw-plugin-oz-shop/api` — optional offer integration. [Evidence](../src/de/omegazirkel/risingworld/landclaim/ShopBridge.java)
- `rw-plugin-oz-discord-connect/discord` — optional notification integration. [Evidence](../src/de/omegazirkel/risingworld/landclaim/DiscordConnect.java)
