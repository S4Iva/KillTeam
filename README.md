# Kill Team Companion

Private Companion-App für Kill Team (3rd Edition) mit dem Mission Generator aus Approved Ops 2025 als Kernfunktion. Läuft auf Android, iOS und Desktop (JVM), vollständig offline.

Regeltexte, Daten und Bilder stammen von [Wahapedia](https://wahapedia.ru/kill-team3/) und gehören Games Workshop bzw. Wahapedia (© Vyacheslav Maltsev). Die App ist nur für den privaten, nicht kommerziellen Gebrauch gedacht und wird nicht weitergegeben. Die Anforderungen stehen in [anforderungs.md](anforderungs.md).

## Funktionen

Die App hat fünf Tabs:

- **Generator:** zufällige oder manuell gewählte Mission (Crit Op), Map und Killzone, Kill-Op-Regeln mit Kill-Grade-Tabelle. Die Tac Ops sind nach Archetyp filterbar, ausgewählt werden sie aber erst im Spiel. Mission und Auswahl bleiben nach dem Neustart erhalten.
- **Game:** Assistent durch die fünf Schritte eines Approved-Ops-Spiels mit Phasenübersicht, Score-Tabelle und Initiative-Verwaltung. Jede neue Phase beginnt oben im Bild. Bei der Teamwahl ergeben sich Teamgröße und wählbare Tac Ops aus den Team-Regeln. In "Select Operatives" wählt jeder Spieler geheim eine Tac Op (Karten wie im Generator) und bis zu 4 Ausrüstungen aus Team- und allgemeiner Ausrüstung. Die Zahl der Incapacitated ist auf die Teamgröße begrenzt. Dazu Spielverlauf, Turniere und Statistiken.
- **Teams:** alle 48 Kill Teams alphabetisch, mit Wortsuche über Teams, Operatives, Regeln, Ploys und Ausrüstung. Zusammensetzung (Pfeile, Optionen und Auswahllisten, Ausrüstungsoptionen eingerückt), Datacards, Fraktionsregeln, Ploys, Equipment und Marker. Dazu Favoriten, Notizen und Rosters.
- **Rules:** Regelseiten von Wahapedia mit Suche.
- **More:** weitere Hilfen und Einstellungen.

Schlüsselbegriffe in den Texten sind antippbar. Ein Popup zeigt die Definition und führt zur passenden Regel.

Das Design ist bewusst eigenständig (petrolfarbenes Dunkel-Theme mit Teal-Akzent in `ui/Components.kt`, Objekt `Palette`) und übernimmt nur die Inhalte von Wahapedia, nicht deren Aussehen.

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

### Windows-App zum Weitergeben an eigene Geräte

`./gradlew :desktopApp:createDistributable` erzeugt unter `desktopApp/build/compose/binaries/main/app/com.example.killteam` einen eigenständigen Ordner mit `com.example.killteam.exe` und mitgelieferter Java-Laufzeit (rund 140 MB). Den ganzen Ordner auf das Zielgerät kopieren, dort ist keine Installation nötig. Der Build braucht ein vollständiges JDK mit `jpackage` (z. B. die JetBrains Runtime 21 unter `~/.jdks`), das JDK von Android Studio enthält es nicht. Einen MSI-Installer erzeugt `:desktopApp:packageMsi`.

## Tests

- Desktop (JVM): `./gradlew :shared:jvmTest`
- Android: `./gradlew :shared:testAndroidHostTest`
- iOS: `./gradlew :shared:iosSimulatorArm64Test` (nur unter macOS)

## Zugangspasswort

Beim Start fragt die App nach einem Passwort. Das Passwort lässt sich beim Eingeben anzeigen. Es soll verhindern, dass die APK unbedacht weitergegeben wird. Es gibt zwei Passwörter:

- **Unbegrenzt:** wird einmalig abgefragt und gilt danach dauerhaft.
- **Einmalig, 7 Tage:** startet bei der ersten Eingabe eine Frist von 7 Tagen. Danach ist es abgelaufen und lässt sich nicht erneut starten. Die Frist endet auch, wenn die Systemuhr zurückgestellt wird. Nach einer Neuinstallation oder dem Löschen der App-Daten beginnt sie von vorn, das ist offline nicht zu verhindern.

Nach der Eingabe des 7-Tage-Passworts zeigt die App einmal die verbleibende Zeit an. Unter **More** steht dauerhaft, ob der Zugang unbegrenzt ist oder wie lange die App noch nutzbar ist.

Setzen oder ändern, danach jeweils neu bauen:

- Unbegrenzt: `powershell -ExecutionPolicy Bypass -File tools\set-access-password.ps1`
- 7 Tage: `powershell -ExecutionPolicy Bypass -File tools\set-access-password.ps1 -Temp`

Das Skript schreibt nur Salt und Hash nach `logic/AccessGate.kt`. Ein leeres Passwort entfernt das jeweilige Passwort, sind beide leer, entfällt die Abfrage. Ein geändertes Passwort wird auf allen Geräten erneut abgefragt.

Das ist kein Kopierschutz: Die Daten liegen unverschlüsselt in der APK, und das Passwort lässt sich weitergeben.

## Daten aktualisieren

Die Daten werden einmalig importiert und mit der App ausgeliefert. Wenn Wahapedia einen neuen Stand zeigt:

1. Prüfen: `powershell -ExecutionPolicy Bypass -File tools\check-updates.ps1`
2. Teams neu importieren: `powershell -ExecutionPolicy Bypass -File tools\import-wahapedia.ps1`
3. Regeln und Keywords neu importieren: `powershell -ExecutionPolicy Bypass -File tools\import-rules.ps1`
4. App neu bauen.

Die Skripte laden schonend und mit Wiederholungen. Ein Lauf dauert einige Minuten. Schlägt ein Team fehl, bricht `import-wahapedia.ps1` ab, ohne etwas zu schreiben.

Kill-Grade-Tabelle, Tac Ops, die Liste der allgemeinen Ausrüstung und weitere Approved-Ops-Daten stehen fest im Code in `data/ApprovedOpsData.kt`.

## Technik

Kotlin Multiplatform und Compose Multiplatform, kotlinx-serialization für die JSON-Daten, multiplatform-settings für die lokale Persistenz. Die Passwortprüfung nutzt eine eigene SHA-256-Implementierung ohne zusätzliche Abhängigkeit.
