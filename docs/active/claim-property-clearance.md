# Grundstück bereinigen

## Ziel

Grundstückseigentümer können, nur bei aktivierter Plugin-Einstellung, den Inhalt
einer Land-Claim-Area anhand des Ankerpunkts prüfen und anschließend entweder
entsorgen, gegen Wallet-Gebühr als Mail-Anhänge an den Grundstückseigentümer
demontieren oder zum konfigurierten Anteil der Shop-Basispreise recyceln.

## Zuständigkeiten und Grenzen

- `rw-plugin-oz-land-claim`: Area-Scan, Vorschau, Bestätigung, Gebühren,
  Weltmutation und Audit.
- `rw-plugin-oz-mail`: atomare Plugin-Mail-Anhänge sowie einzelne Entnahme und
  keine Rücksende-Schaltfläche für Plugin-Absender.
- Wallet bleibt die alleinige Instanz für Belastungen.
- `rw-plugin-oz-shop` liefert den zwischengespeicherten, vollständigen
  Katalog-Basispreis je Item und Variante; nicht bepreiste Materialien haben
  beim Recycling keinen Gegenwert.
- Alle optionalen Plugin-Grenzen laufen über Consumer-Bridges: Land Claim nutzt
  `WalletBridge`, `MailBridge` und `ShopBridge`; die Fachlogik kennt keine
  fremde Plugin-Hauptklasse.
- Die Materialliste ist eine Rekonstruktion aus den aktuell geladenen
  Definitionen/Rezepten; Container-Inhalte werden unverändert übernommen.

## Ablauf

1. Der Eigentümer öffnet in den Zoneneinstellungen den neuen Bereinigungsdialog.
2. Der Dienst ermittelt in den betroffenen X/Z-Chunks Objekte und
   Construction-Elemente, deren `getWorldPosition()` in der Area liegt.
3. Die Vorschau fasst Bau-Materialien und Container-Inhalte zusammen und zeigt
   Item, Menge und Gebühr. Nicht eindeutig auflösbare Rezeptzutaten blockieren
   die Demontage, aber nicht die Auflösung.
4. Alle Aktionen erzeugen nach einer zweiten Bestätigung einen neuen Snapshot.
   Gebühren werden idempotent vor der Mutation eingezogen.
5. `Auflösen` entfernt Elemente ohne Rückgabe. `Demontieren` reserviert zuerst
   die vollständige Mail-Zustellung und entfernt erst danach die Weltobjekte.
6. `Recyceln` zahlt vor der Weltmutation den auf volle Währungseinheiten
   abgerundeten Gegenwert der auflösbaren Vorschau aus. Der Standardanteil ist
   20 Prozent und wird auf höchstens 100 Prozent begrenzt.
7. Nicht auflösbare Vorschauzeilen benennen ihr Host-Objekt bzw. Bauteil.
   Dessen Host kann nach einer separaten Sicherheitsabfrage kostenlos und ohne
   Rückgabe entfernt werden; andere Area-Elemente bleiben unverändert.
8. Der administrative Bereinigungsdialog bietet dieselben sicheren
   Demontage-, Recycling- und Host-Entfernungsoptionen. Die Demontage bleibt
   kostenlos; Recycling zahlt dem ermittelten Claim-Besitzer aus und fällt nur
   bei unbekanntem Besitzer an den ausführenden Admin.

## Sicherheitsregeln

- Standard: deaktiviert; die Aktivierung und Gebühren sind PluginSettings für
  Administratoren, der Einstieg liegt in den Zoneneinstellungen des Eigentümers.
- Ankerpunkt definiert Area-Zugehörigkeit.
- Der Scan begrenzt Chunk- und Elementanzahlen und protokolliert Actor, Area,
  Modus, Menge, Gebühr und Korrelations-ID.
- Fehlende Wallet-/Mail-Verfügbarkeit, volle Mailbox oder nicht auflösbare
  Rückgabe verhindern die Demontage ohne Weltänderung.
- Fehlender Shop-/Wallet-Zugriff, nicht auflösbare Bauteile oder ein
  Gegenwert von null sperren Recycling ohne Weltänderung.

## Validierung

- Unit-Tests für Area-Filter, Rezeptaggregation, Container-Snapshot und
  Sicherheitsentscheidungen.
- OZ-Mail-Tests für Einzelentnahme und ausgeblendete Rückgabe bei Plugin-Mail.
- Maven-Test/Package beider Plugins, API- und ZIP-Prüfung.
- Scoped Development-Upload von Land Claim und OZ Mail; Reload- und
  Startprotokoll prüfen.
