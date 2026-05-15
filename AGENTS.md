# AGENTS.md

## Repository Purpose
This repository owns land ownership, claim protection, zones, and area permission workflows for Rising World Unity.

It must remain usable standalone. Workspace-root orchestration is optional and must never be required for build, release, or local agent operation.

## Ownership
Owns:
- claim creation, limits, protection, and repair behavior
- claimed-area permissions and special admin zones
- land-claim-specific commands, settings, UI, and persistence

Does not own:
- generic shared helpers that belong in `rw-plugin-oz-tools`
- Discord bridge behavior
- GPS, intercom, or admin utility domain logic

## Mandatory Workflow Rules
- Preserve the Java 20 baseline.
- Preserve Maven build and GitHub tag-release behavior.
- Keep dependencies minimal and runtime-safe.
- Use `rw-plugin-oz-tools` for reusable infrastructure.
- Follow `.codex/agents.toml` for local agent roles, task classes, context loading, and escalation.
- Follow `docs/policies/repository-policy.md` for reusable governance rules.
- Keep `README.md`, `HISTORY.md`, and `PLANS.md` aligned with behavior or structure changes.

## Validation
- Run `mvn -B -DskipTests package` for build-impacting changes.
- Run `mvn -B test` when tests exist.
- Verify new Rising World API usage before relying on it.
- Review claim protection, permission, zone, and persistence impact for user-visible changes.
