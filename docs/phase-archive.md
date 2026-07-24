# Phase Archive

Summarize completed phases and closed task groups here.

## Template

```md
## YYYY-MM - <Phase Name>

Repositories affected:
- `<repo>`

Summary:
- <what was completed>

Validation:
- <commands/checks run>

Follow-ups:
- <remaining work or none>
```

## 2026-07 - Configurable area-frame visibility

Repositories affected:
- `rw-plugin-oz-land-claim`

Summary:
- Added a persisted per-player three-dimensional chunk radius for area frames,
  defaulting to 15 chunks with bounded settings choices.
- Replaced coarse sector filtering with inclusive chunk-AABB intersections and
  differential frame updates while rendering intersecting areas in full.

Validation:
- 13 automated tests passed, followed by a successful package build and
  `git diff --check`.
- Uploaded through the narrowed root `dev-upload.sh`; the development server
  completed `RELOADED ALL PLUGINS` without LandClaim errors.
- Player accepted the settings UI and runtime behavior on the development server.

Follow-ups:
- None.
