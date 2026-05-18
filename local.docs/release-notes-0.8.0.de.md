# OZ Land Claim 0.8.0

Dieses Release aktualisiert Land Claim fuer OZTools 0.18.0 und verbessert Verwaltung, Overlays und die Sichtbarkeit von Spieler-Einstellungen.

## Highlights

- Admins erhalten eine Grundstuecksverwaltung fuer Besitzer, beanspruchte Zonen und Spezialgrundstuecke.
- Eine optionale automatische Claim-Entfernung kann Besitzer nach konfigurierter Inaktivitaet nach dem Serverstart aufraeumen.
- Das aktuelle Chunk-Overlay wurde als kompaktes Panel am oberen Bildschirmrand ueberarbeitet und wird bei geoeffnetem Inventar ausgeblendet.
- Spieler-Sichtbarkeits- und Overlay-Einstellungen sind jetzt im gemeinsamen Plugin-Daten-Tab sichtbar.
- Gemeinsame Spieler-Plugin-Einstellungen werden nun ueber dieselbe PlayerSettings-Datenbank gespeichert wie das radiale Sichtbarkeitsmenue.
- Sichtbare Flaechenrahmen aktualisieren sich jetzt sofort, wenn eine beanspruchte Zone erweitert wird.
- Dialoge, Berechtigungstabellen, Dropdowns und Zeilen der Verwaltungsuebersicht erhielten Layout-Korrekturen.

## Installation

Bitte beide Plugins aktualisieren:

- `OZTools` `0.18.0`
- `OZLandClaim` `0.8.0`

Eine manuelle Datenbankmigration ist nicht notwendig. Vorhandene gespeicherte Spieler-Einstellungen bleiben in den OZTools-PlayerSettings-Tabellen erhalten.

## Roadmap

Claim-Kauf und -Verkauf, kaufbare zusaetzliche Claim-Kapazitaet und ein moegliches gemeinsames Shop-Plugin/Interface sind als zukuenftige Arbeit in `docs/roadmaps/claim-economy-and-shop.md` dokumentiert.
