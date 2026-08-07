# First included land-price claims are free

## Objective

Add a default-off administrator switch for land-price mode. A player below the configured base claim limit does not pay the base land price, but still pays the cluster surcharge.

## Ownership, compatibility, and rollback

Land Claim calculates the charge using persisted normal claim ownership. The additive setting defaults to `false`; disabling it restores current pricing. No data migration is needed.

## Risks and validation

Verify zero owned claims, a clustered first claim, the claim at the limit boundary, release/reclaim, and Wallet failure. Unit tests cover charge calculations; Dev runtime acceptance remains manual.

## Checklist

- [x] Add setting, admin editor entry, and default configuration.
- [x] Calculate waived base price from persisted normal ownership.
- [x] Preserve cluster surcharge and audit/payment behavior.
- [x] Test, package, and deploy to Dev.
