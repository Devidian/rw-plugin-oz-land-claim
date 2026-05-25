# Roadmap Plan 02 Plugin Standardization

## Objective
Adopt Roadmap Plan 02 portfolio standards for logger naming, admin settings visibility, localized settings text, and standardized plugin info/status panels.

## Ownership
Primary repository: `rw-plugin-oz-land-claim`.

Supporting repository: `rw-plugin-oz-tools`.

## Work Packages
- [x] Package 1: Collapse specialized loggers into one main LandClaim logger.
- [x] Package 2: Verify every safe `settings.default.properties` key appears in the admin `PluginSettings` tab.
- [x] Package 3: Mark list/enum settings as read-only where editing is not yet supported.
- [x] Package 4: Add missing English and German i18n labels/descriptions for settings.
- [x] Package 5: Group related settings with labeled separators.
- [x] Package 6: Add LandClaim info/status panel content and redirect existing info/status commands to the shared Tools panel.

## Validation Strategy
- Run Maven package and tests.
- Verify normal players cannot see admin settings.
- Verify info/status panel opens from radial menu and commands.

## Progress Notes
- Package 1 is complete: LandClaim utility logger helpers already route to `OZ.LandClaim`, and settings reload now sets the main logger level once.
- Packages 2-5 are complete for Root Step 9: LandClaim admin settings cover every safe default key, grouped separators are present, decimal settings without validated editing are read-only, and English/German setting labels are available.
- Package 6 is complete for Root Step 10: LandClaim now registers a shared Tools Info/Status provider and routes `/lc status` to the shared panel.

## Affected Repositories/Plugins
- `rw-plugin-oz-land-claim`
- `rw-plugin-oz-tools`
