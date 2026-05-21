# OZ Land Claim 0.8.0

Dieses Release aktualisiert Land Claim für OZTools 0.18.0 und verbessert Verwaltung, Overlays und die Sichtbarkeit von Spieler-Einstellungen.

## Highlights

- Admins erhalten eine Grundstücksverwaltung für Besitzer, beanspruchte Zonen und Spezialgrundstücke.
- Eine optionale automatische Claim-Entfernung kann Besitzer nach konfigurierter Inaktivität nach dem Serverstart aufräumen.
- Das aktuelle Chunk-Overlay wurde als kompaktes Panel am oberen Bildschirmrand überarbeitet und wird bei geöffnetem Inventar ausgeblendet.
- Spieler-Sichtbarkeits- und Overlay-Einstellungen sind jetzt im gemeinsamen Plugin-Daten-Tab sichtbar.
- Gemeinsame Spieler-Plugin-Einstellungen werden nun über dieselbe PlayerSettings-Datenbank gespeichert wie das radiale Sichtbarkeitsmenü.
- Sichtbare Flächenrahmen aktualisieren sich jetzt sofort, wenn eine beanspruchte Zone erweitert wird.
- LandClaim hat jetzt optionale Economy-Unterstützung: zusätzliche Claim-Kapazität kann als Shop-Angebot registriert werden, Besitzer können Claims zum Verkauf anbieten, Käufer können gelistete Claims über Wallet kaufen, und Verkaufsflächen nutzen eine konfigurierbare Rahmenfarbe.
- Dialoge, Berechtigungstabellen, Dropdowns und Zeilen der Verwaltungsübersicht erhielten Layout-Korrekturen.

## Installation

Bitte beide Plugins aktualisieren:

- `OZTools` `0.18.0`
- `OZLandClaim` `0.8.0`

Optionale Economy-Funktionen verwenden zusätzlich:

- `OZWallet` für Claim-Verkäufe
- `OZShop` plus `OZWallet` für kaufbare zusätzliche Claim-Kapazität

Eine manuelle Datenbankmigration ist nicht notwendig. Neue Tabellen für gekaufte Claim-Kapazität und Claim-Verkaufsangebote werden automatisch angelegt. Vorhandene gespeicherte Spieler-Einstellungen bleiben in den OZTools-PlayerSettings-Tabellen erhalten. Claim-Verkäufe bleiben deaktiviert, bis `allowClaimSale=true` gesetzt wird.

## Roadmap

Die Claim-Economy-Roadmap ist bis zur ersten Claim-Verkaufs- und Extra-Claim-Kauf-Implementierung abgeschlossen. Weitere zukünftige Arbeiten werden separat verfolgt.
