# Claim Economy And Shop Roadmap

## Objective
Add optional economy support for claims without making Land Claim require Wallet or a future shop plugin.

## Ownership
Primary repository: `rw-plugin-oz-land-claim`

Supporting repositories:
- `rw-plugin-oz-wallet` for optional currency transactions
- possible future shop plugin for a shared item registration and purchase interface
- `rw-plugin-oz-tools` only if reusable UI or integration helpers are needed

## Dependencies
- `rw-plugin-oz-tools` remains required.
- `rw-plugin-oz-wallet` must stay optional for claim buy/sell flows.
- A shared shop plugin/interface should be evaluated before Land Claim grows a Land-Claim-only shop implementation.

## Phases
- [ ] Phase 1: Define optional Wallet integration for claim sale listings, claim purchases, and transaction rollback behavior.
- [ ] Phase 2: Add claim buy/sell flows when Wallet is available, with configuration for enabling sales, fees, and limits.
- [ ] Phase 3: Add purchasable claim-capacity upgrades that increase a player's maximum claim count.
- [ ] Phase 4: Evaluate a shared shop plugin/interface where multiple plugins can register purchasable items or upgrades.

## Risks
- Economy logic must not make Land Claim depend hard on Wallet.
- Claim ownership transfer must remain consistent with existing area permissions and chunk persistence.
- A shop interface could become shared infrastructure; avoid duplicating registration, pricing, and purchase UI across plugins.

## Validation Strategy
- Verify behavior with Wallet installed and absent.
- Verify claim ownership transfer, permission transfer, and database consistency after successful and failed purchases.
- Verify capacity upgrades affect max claim calculations without breaking playtime-based limits.

## Affected Repositories/Plugins
- `rw-plugin-oz-land-claim`
- `rw-plugin-oz-wallet`
- possible future shop plugin
- `rw-plugin-oz-tools` if shared helpers are introduced

## Rollback Considerations
Keep all economy behavior gated behind configuration and optional plugin detection so servers can disable the feature without removing existing claims.
