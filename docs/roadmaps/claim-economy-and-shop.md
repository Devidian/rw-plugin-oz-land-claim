# Claim Economy And Shop Roadmap

## Objective
Add optional economy support for extra claims and claim sales without making Land Claim require Wallet or Shop for its core claim protection behavior.

## Ownership
Primary repository: `rw-plugin-oz-land-claim`

Supporting repositories:
- `rw-plugin-oz-wallet` for optional currency transactions
- future `rw-plugin-oz-shop` for purchasable extra-claim capacity
- `rw-plugin-oz-tools` for shared UI, settings reload, admin settings tab, area indicator coordination, and reusable helper support

## Dependencies
- `rw-plugin-oz-tools` remains required.
- `rw-plugin-oz-wallet` must stay optional for claim buy/sell flows.
- `rw-plugin-oz-shop` is required for extra-claim purchases.

## Confirmed Decisions
- All prices are whole integers.
- Extra-claim price increases are linear from the base price.
- Discord event messages are desired through a dedicated event channel id when claim sale events are implemented.
- Claim purchases may exceed normal claim limits only when an admin enables that global setting.
- Claim sale ownership transfer clears all old player rights and assigns the buyer as the only owner.
- Claim sellers receive the full listed sale price; no claim-sale fee/tax is planned for v1.

## Work Packages
- [x] Package 1: Adopt shared Tools settings reload/admin settings tab metadata after the Tools baseline exists.
- [x] Package 2: Add optional Wallet and Shop integration detection without hard runtime failure when absent.
- [x] Package 3: Implement extra-claim purchase registration through Shop with default price `200` and default linear per-purchase increase `10%`.
- [x] Package 4: Persist purchased claim capacity and include it in player max-claim calculations and admin claim-management views.
- [x] Package 5: Implement claim sale listing persistence for owner, area, price, listing date, buyer, purchase date, and sale status.
- [x] Package 6: Add owner radial action to list or withdraw a claim sale from inside the owned area.
- [x] Package 7: Add buyer indicator and radial action for areas listed for sale.
- [x] Package 8: Implement purchase flow with Wallet withdrawal, ownership/permission reset, seller credit, and rollback on failure.
- [x] Package 9: Add distinct frame color for areas listed for sale and make it configurable.
- [x] Package 10: Update README, HISTORY, i18n, settings defaults, and migration notes.

## Risks
- Economy logic must not make Land Claim depend hard on Wallet.
- Claim ownership transfer must remain consistent with existing area permissions and chunk persistence.
- Purchased claim capacity must remain separate from playtime-derived limits but included in all limit checks.
- Area sale indicators must coexist with Shop and Marketplace indicators.

## Validation Strategy
- Verify behavior with Wallet installed and absent.
- Verify behavior with Shop installed and absent.
- Verify claim ownership transfer, permission transfer, and database consistency after successful and failed purchases.
- Verify capacity upgrades affect max claim calculations without breaking playtime-based limits.
- Verify listed-for-sale frame color refreshes when entering/leaving areas and after sale withdrawal.

## Affected Repositories/Plugins
- `rw-plugin-oz-land-claim`
- `rw-plugin-oz-wallet`
- future `rw-plugin-oz-shop`
- `rw-plugin-oz-tools`

## Rollback Considerations
Keep all economy behavior gated behind configuration and optional plugin detection so servers can disable the feature without removing existing claims.

## Open Questions
- None.
