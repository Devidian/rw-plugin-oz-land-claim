# Claim-time placeholder response

## Objective

Render the remaining claim time in the callback-based claim validation path.

## Ownership, compatibility, and rollback

Land Claim owns the message. This aligns the callback path with the existing
expansion path without changing the i18n key or claim-time policy.

## Checklist

- [x] Replace `PH_TIME_LEFT` before returning the validation callback.
- [x] Package and verify the Development runtime reload.
