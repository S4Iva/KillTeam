# Kill Team Companion

Private Companion-App für Kill Team (3rd Edition) mit dem Mission Generator aus Approved Ops 2025 als Kernfunktion. Läuft auf Android, iOS und Desktop (JVM), vollständig offline.

Regeltexte, Daten und Bilder stammen von [Wahapedia](https://wahapedia.ru/kill-team3/) und gehören Games Workshop bzw. Wahapedia (© Vyacheslav Maltsev). Die App ist nur für den privaten, nicht kommerziellen Gebrauch gedacht und wird nicht weitergegeben. Die Anforderungen stehen in [anforderungs.md](anforderungs.md).

## Funktionen

Die App hat fünf Tabs:

- **Generator:** zufällige oder manuell gewählte Mission (Crit Op), Map und Killzone, Kill-Op-Regeln mit Kill-Grade-Tabelle, Tac Ops nach Archetyp filterbar. Auswahl und Mission bleiben nach dem Neustart erhalten.
- **Game:** Assistent durch die fünf Schritte eines Approved-Ops-Spiels, mit Spielverlauf, Turnieren und Statistiken.
- **Teams:** alle 48 Kill Teams mit Zusammensetzung, Datacards, Fraktionsregeln, Ploys, Equipment und Markern. Dazu Favoriten, Notizen und Rosters.
- **Rules:** Regelseiten von Wahapedia mit Suche.
- **More:** weitere Hilfen und Einstellungen.

Schlüsselbegriffe in den Texten sind antippbar. Ein Popup zeigt die Definition und führt zur passenden Regel.

## Projektstruktur

- [shared](shared/src) enthält den gemeinsamen Code und die Ressourcen.
  - [commonMain/kotlin/com/example/killteam](shared/src/commonMain/kotlin/com/example/killteam): `data` (Modelle, Regeldaten), `logic` (Generator, Spiel, Verlauf, Persistenz), `ui` (Compose-Screens) und `App.kt`.
  - [commonMain/composeResources/files](shared/src/commonMain/composeResources/files): `killteams.json`, `rules.json`, `lookup.json` und Bilder.
- [androidApp](androidApp), [desktopApp](desktopApp) und [iosApp](iosApp) sind die Einstiegspunkte der Plattformen.
- [tools](tools) enthält die PowerShell-Skripte für den Datenimport.

## Bauen und Starten

- Android: `./gradlew :androidApp:assembleDebug`
- Desktop: `./gradlew :desktopApp:run`, mit Hot Reload `./gradlew :desktopApp:hotRun --auto`
- iOS: Ordner [iosApp](iosApp) in Xcode öffnen (nur unter macOS)

## Tests

- Desktop (JVM): `./gradlew :shared:jvmTest`
- Android: `./gradlew :shared:testAndroidHostTest`
- iOS: `./gradlew :shared:iosSimulatorArm64Test` (nur unter macOS)

## Daten aktualisieren

Die Daten werden einmalig importiert und mit der App ausgeliefert. Wenn Wahapedia einen neuen Stand zeigt:

1. Prüfen: `powershell -ExecutionPolicy Bypass -File tools\check-updates.ps1`
2. Teams neu importieren: `powershell -ExecutionPolicy Bypass -File tools\import-wahapedia.ps1`
3. Regeln und Keywords neu importieren: `powershell -ExecutionPolicy Bypass -File tools\import-rules.ps1`
4. App neu bauen.

Die Skripte laden schonend und mit Wiederholungen. Ein Lauf dauert einige Minuten. Schlägt ein Team fehl, bricht `import-wahapedia.ps1` ab, ohne etwas zu schreiben.

Kill-Grade-Tabelle, Tac Ops und weitere Approved-Ops-Daten stehen fest im Code in `data/ApprovedOpsData.kt`.

## Technik

Kotlin Multiplatform und Compose Multiplatform, kotlinx-serialization für die JSON-Daten, multiplatform-settings für die lokale Persistenz.
