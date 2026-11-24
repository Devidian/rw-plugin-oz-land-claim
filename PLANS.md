# PLANS.md

- [x] Bound time-based claim and expansion waits with configurable factors and targeted tests.
- [ ] Validate the next-300926 change on Development with a controlled player check.

Planning is stored in repository-local docs.

- Active implementation tasks: [docs/active/](docs/active/)
- Roadmaps and larger plans: [docs/roadmaps/](docs/roadmaps/)
- Completed phase summaries: [docs/phase-archive.md](docs/phase-archive.md)
- Planning and documentation standards: [docs/policies/repository-policy.md](docs/policies/repository-policy.md)

Every implementation plan must include objective, ownership, dependencies, risks, validation strategy, affected repositories/plugins, rollback considerations, and a markdown checkbox checklist.

## Player erasure

- [x] Add local deletion of player claim indexes, map visits, and purchased claim capacity.
- [x] Convert owned native areas to special zones without deleting other player permissions.
- [ ] Remove or anonymize the player's sale, lease, city, notification, and economy links; then connect an idempotent coordinator.
- [ ] Validate native area conversion and claim access on Development with a backup and rollback point.

The conversion uses the configured special-zone permission. Area changes are native game state; the Java suite validates compilation only, so a runtime check is required before enabling player deletion.

Discord event thread routing is installed on Development. A controlled Discord event test and the separately authorized release remain open. Configuration is documented in README.md.
