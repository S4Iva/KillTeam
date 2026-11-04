# Konzept: Weiterentwicklung Kill Team Companion

Grundlage: [ideen.md](ideen.md). Stand: 2026-11-04.

## 1. Rahmen

- **1.3.0.b** läuft im Test bei Usern und wird nur noch für Bugs angefasst (dritte Stelle).
- Alle neuen Features entstehen in einer **Alpha** (`1.4.0.a`). Die Version wird erst beim Release zur Beta festgelegt (`1.4.0.b`).
- Ablauf: Branch `alpha` (oder `feature/*`) -> Entwicklung -> Test -> Beta-Release mit Versionsnummer, neuem Schlüsselpaar und neuem Datenstand.

### Versionsschema

| Stelle | Bedeutung | Beispiel |
| --- | --- | --- |
| `v` | Präfix | `v` |
| 1. Ziffer | Feature | `1` |
| 2. Ziffer | Erweiterung | `3` |
| 3. Ziffer | Bug | `0` |
| Suffix | `a` = Alpha, `b` = Beta | `b` |

Anzeige: ganz unten unter **More**, nach dem letzten Feld, als Text `v1.3.0.b - Daten: 2026-11-02` (Datum der letzten Datenaktualisierung). Die Version kommt aus einer Konstante im `shared`-Modul, das Datenstand-Datum schreibt der Import in `killteams.json`/`rules.json` (Feld `dataDate`).

## 2. Features im Überblick

| Nr. | Thema | Aufwand | Abhängigkeit |
| --- | --- | --- | --- |
| A | UI-Design (Kill-Team-Orange) | mittel | Mock-ups |
| B | Game Wizard | hoch | A (Mock-ups) |
| C | Regeln zusammenführen (pausiert) | niedrig | keine |
| D | Rules-Tab mit Favoriten-Schalter | niedrig | keine |
| E | Versionsanzeige | niedrig | keine |
| F | Rechtliches | niedrig | keine |
| G | Update-Skripte (inkrementell, PDF) | hoch | keine |
| H | Schlüsselrotation pro Feature | niedrig | Release-Prozess |
| I | Kameramessung ausblenden | niedrig | keine |
| J | Button-System | mittel | A (Palette) |

Empfohlene Reihenfolge: I, E, F, C -> A (Mock-ups) -> J -> D -> B -> G -> H beim Release.

## 3. Konzepte im Detail

### A. UI-Design: Kill-Team-Orange

- `Palette` in `ui/Components.kt` bekommt ein neues Farbschema: dunkler Grund (Anthrazit/Schwarz), Akzent **Orange** (Richtwert `#E8650A`, am offiziellen Kill-Team-Look abgleichen), helle Schrift.
- Farben nur an einer Stelle (`Palette`) ändern, damit das Theme ohne Umbau der Screens wechselt. Optional kurz per Einstellung zwischen Teal (alt) und Orange (neu) umschaltbar, bis die Alpha fertig ist.
- Buttons: primär = gefüllt orange, sekundär = Outline, Mindestgröße 48 dp, klare Beschriftung und Icon.
- **Hintergrundbilder:** nur eigene oder lizenzfreie, dezente Texturen (Metall, Raster, Rauschen) mit geringer Deckkraft. Offizielle GW-Artworks nur nutzen, wenn die App privat/im Testkreis bleibt (siehe F).
- Vorgehen: erst 3 Mock-ups (Home/Generator, Game-Phase, Team-Detail), dann Freigabe, dann Umsetzung.

### B. Game Wizard

Ziel: Der Spieler wird durch jede Phase geführt, ohne zu suchen. Aufbauend auf dem bestehenden Game-Tab (fünf Schritte, Phasenübersicht).

**Prinzipien**

- Pro Phase eine Seite mit: Titel, Kurzbeschreibung, Checkliste "Was jetzt zu tun ist", relevanten Regeln (einklappbar), Aktionsbuttons.
- Die nächste Aktion ist immer der auffälligste Button (orange gefüllt).
- Verworfen nach dem Test: der runde Weiter-/Zurück-Pfeil unten rechts und die Haken in den Checklisten (siehe `GW-2`, `GW-3` in anforderungs.md). Die Liste "Was jetzt zu tun ist" bleibt eine reine Textliste.
- Zurück-Pfeil links daneben (kleiner, Outline).
- Oben: Fortschrittsleiste (Schritt 1-5, Turning Point 1-4).

**Wireframe (Mock-up-Entwurf)**

```
+--------------------------------------+
| Step 4: Play the Battle      TP 2 / 4 |
| [=====|=====|=====|-----]            |
+--------------------------------------+
| Strategy Phase                       |
|  [x] Initiative bestimmen            |
|  [ ] Initiative-Karte vergeben       |
|  [ ] CP generieren                   |
|                                      |
|  > Regel: Initiative (einklappbar)   |
|                                      |
|  [ Initiative würfeln ]              |
|                                      |
|                              ( -> )  |
+--------------------------------------+
```

**Umsetzung**

1. Mock-ups (Figma, Papier oder statische Compose-Previews) für Phase-Seite, Score-Leiste und Wechsel zwischen Runden.
2. Phasenseiten behalten den bisherigen Aufbau (Phasenkarte mit Fortschrittsleiste, Textliste, Regeln, Aktionen).
3. Bestehende Phasen schrittweise darauf umstellen.
4. Fortschritt pro Phase im bestehenden Spielzustand speichern (bleibt nach Neustart erhalten).

### C. Regeln zusammenführen (pausiert)

Status 2026-11-04: pausiert. Prüfung und Vorschlag stehen in anforderungs.md bei `RG-1` (u. a. "Disable Ads" als Importfehler auf 15 Seiten, Grundaktionen auf 9 bis 15 Seiten, doppelte IDs bei Tomb World und Typhon).

- Mehrfach auftretende Regeln auf eine zentrale Regel reduzieren, z. B. "Fight (1 AP)" als eine Regel statt pro Team/Operative wiederholt.
- Vorgehen: `rules.json`/`lookup.json` und `killteams.json` nach identischen Regeltiteln und -texten auswerten (Skript-Report), Duplikate auf eine ID mappen, Verweise (Keyword-Popup, Team-Detail) auf die zentrale Regel zeigen lassen.
- Nur identische oder rein formal abweichende Texte zusammenführen. Abweichende Team-Varianten bleiben getrennt und verweisen auf die Grundregel.
- Wird im Importskript (G) umgesetzt, damit es bei jedem Update erhalten bleibt.

### D. Rules-Tab mit Favoriten-Schalter

Die ursprüngliche Idee "Referenz statt Rules" (Gruppen, Missionspakete) wurde nach dem Test verworfen. Der Tab bleibt **Rules** und zeigt nur Regeln. Die Missionspakete stehen im Generator. Der Schalter ALL / Favoriten bleibt.

```
Rules                    [ ALL | Favorites ]
-------------------------------------------
[Suche]
Core Rules            ☆
Killzones             ★
...
Universal map key
```

- Schalter **ALL / Favorites** ganz oben. Im Favoriten-Modus erscheinen die markierten Einträge (Regelseiten, Teams, Crit Ops, Tac Ops) als antippbare Liste.
- Regelseiten werden mit dem Stern auf ihrer Karte markiert.
- Der Favoriten-Bereich unter **More** entfällt, die Favoriten-Daten bleiben unverändert (Migration nicht nötig). Notizen bleiben unter More.
- Die Suche arbeitet über alle Regelseiten.


### E. Versionsanzeige

Siehe Abschnitt 1. Letzter Eintrag unter **More**: `v1.3.0.b - Daten: 2026-11-02`. Ein Alpha-Build zeigt `a`, so ist im Test sofort erkennbar, welcher Stand läuft.

### F. Rechtliches

- **Nutzungsbedingungen** und **Datenschutz** als Seiten unter More (Text lokal gebündelt, offline lesbar).
- Beim ersten Start (nach Zugangscode) Zustimmung einholen, Zustimmung samt Version der Texte speichern. Bei geänderten Texten erneut fragen.
- Inhalt Datenschutz: App arbeitet offline, keine Konten, keine Weitergabe von Daten, lokale Speicherung (Notizen, Rosters, Verlauf). Kamera-Berechtigung nur, falls die Messung aktiv ist.
- Inhalt Nutzungsbedingungen: private, nicht kommerzielle Nutzung, Zugangscode nicht weitergeben, Inhalte gehören Games Workshop bzw. Wahapedia, kein Anspruch auf Verfügbarkeit.
- Die Texte sind Entwürfe und sollten vor einer Verbreitung über den Testkreis hinaus rechtlich geprüft werden.

### G. Update-Skripte (inkrementell + PDF)

Problem: `import-wahapedia.ps1` und `import-rules.ps1` überschreiben alles.

**Ziel:** nur neue oder geänderte Objekte schreiben.

- Jedes Objekt (Team, Operative, Regel, Ploy, Equipment) bekommt eine stabile ID und einen Inhalts-Hash.
- Ablauf je Lauf: Quelle lesen -> je Objekt ID suchen -> nicht vorhanden: **anlegen**, Hash anders: **aktualisieren**, sonst unverändert lassen.
- Bereichsparameter: `-Teams`, `-Rules`, `-Team "Name"`, `-Section "Core Rules"`, sodass nur der gewünschte Bereich läuft.
- Ergebnis als Änderungsbericht (neu / geändert / unverändert / nicht mehr in der Quelle), zuvor Backup der JSON-Dateien. Gelöschte Objekte nur markieren, nicht automatisch entfernen.
- Datenstand (`dataDate`) wird nur bei tatsächlichen Änderungen gesetzt.

**PDF-Reader**

- Neues Skript liest die offiziellen Team- und Regel-PDFs, extrahiert den Text und mappt ihn auf dasselbe Datenmodell wie der Wahapedia-Import.
- Technik: PowerShell kann PDFs nicht zuverlässig parsen. Vorschlag: `pdftotext -layout` (Poppler) oder ein kleines Python-/Kotlin-Tool für die Extraktion, die Skripte übernehmen den Merge.
- Layout-Erkennung je PDF-Typ (Team, Regeln) in einer Konfiguration, Treffer mit geringer Sicherheit im Bericht markieren und nicht automatisch übernehmen.
- Gleiche Upsert-Logik wie oben: fehlende Objekte neu anlegen, geänderte aktualisieren.

### H. Verschlüsselung/Schlüsselrotation

- Mit jedem neuen **Feature-Release** (erste Ziffer) wird ein neues Ed25519-Schlüsselpaar erzeugt (`tools\generate-access-code.ps1 -NewKey -Force`). Der öffentliche Schlüssel steckt in `AccessConfig.PUBLIC_KEY`.
- Wirkung: Codes der neuen Version funktionieren nicht in alten Versionen, alte Codes nicht in der neuen.
- Praxis:
  - Private Schlüssel pro Version sichern, z. B. `tools/keys/access-private-key-1.4.txt` (gitignored, Backup extern).
  - Nach dem Release neue Codes an die Tester verteilen. Vorher ankündigen, da ein Update ohne neuen Code sperrt.
  - Rotation nur bei Beta-Release, nicht bei jedem Alpha-Build. Alpha-Builds behalten den Schlüssel der letzten Beta. Sie sind nicht parallel zur Beta installierbar (gleiche Application-ID) und ersetzen sie.
- Bug-Releases (dritte Ziffer) behalten den Schlüssel, damit Tester nicht neu freigeschaltet werden müssen.

### I. Kameramessung

- Feature wird **herausgenommen, aber nicht gelöscht**.
- Umsetzung: Feature-Flag `FeatureFlags.MEASURE = false` in `shared`. Bei `false` ist `MorePage.Measure` nicht sichtbar, `CameraSource` wird nicht initialisiert, die CAMERA-Berechtigung entfällt im Android-Manifest (oder per Build-Variante).
- Code (`logic/Measure.kt`, `ui/MeasureScreen.kt`, `ui/CameraSource.kt`) bleibt im Repo. Eintrag in der Doku, dass das Feature pausiert ist.
- Später prüfen, ob es sinnvoll zurückkommt.

### J. Button-System

Ziel: Die vorhandenen Buttons bleiben als Arten erhalten, werden aber vereinheitlicht und fest definiert. Heute werden `Button`, `OutlinedButton`, `TextButton`, `FilterChip` und Eigenbauten (`Stepper`, `NavButton`, `FavoriteButton`) über rund 100 Stellen einzeln mit eigenen Modifiern und Höhen gesetzt. Das führt zu unterschiedlichen Höhen, Breiten und Beschriftungen. Mock-up: [mockups/buttons.html](mockups/buttons.html).

**1. Arten (Rolle statt Aussehen)**

| Art | Aussehen | Wofür | Heutige Beispiele |
| --- | --- | --- | --- |
| Primary | orange gefüllt, weiße Schrift | die eine Hauptaktion pro Karte oder Seite | Generate Mission, Next: <Phase>, Next turning point, Resolve roll-off, Do the Ready step, Save result and close game, Unlock, Start game |
| Secondary | orange Outline, orange Schrift | weitere Aktionen, Ein-/Ausblenden, Unterflüsse öffnen | Show/Hide crit op and map, Change mission, Secret selection, Redo roll-off, Rosters, More-Menüeinträge |
| Text | nur orange Schrift | Abbrechen, Zurück, Schließen, Link-Aktionen | "< Back", Cancel, Close, Remove: <Ausrüstung> |
| Danger | rote Outline und Schrift (im Bestätigungsdialog rot gefüllt) | Aktionen, die Daten löschen oder verwerfen | Discard game, Delete (Verlauf, Turnier), Clear all, Out |
| Choice (Chip) | Chip mit 8 dp Rundung, gewählt orange gefüllt | Auswahl aus Optionen und Filter | Archetyp-Filter, Phase, Würfelwerte 1-6, Einheiten |
| Segmented | gleich breite Gruppe, gewählt orange gefüllt, andere Outline | genau eine von 2 bis 3 Optionen, immer sichtbar | Player 1/2 has initiative, ALL / Favoriten |
| Icon | nur Symbol, Touch-Fläche 48 dp | Favorit, Select/Remove (Fadenkreuz) | Stern, Fadenkreuz |

Zusätzliche feste Bausteine (keine eigenen Arten): **Stepper** (Beschriftung links, `-` Wert `+` rechts, Buttons Größe S) und **Nav-Pille** (schwebend: Index, Cards, Top).

**2. Größen**

| Größe | Höhe | Schrift | Einsatz |
| --- | --- | --- | --- |
| S | 40 dp | `labelLarge` | Zeilen mit mehreren Aktionen (-1 W, +1 W, Engage, Ready, Out), Stepper |
| M (Standard) | 48 dp | `labelLarge` | alle normalen Aktionen, auch Next: <Phase> |
| L | 56 dp | `titleSmall` | nur Weitergabe- und Abschlussaktionen: Secret selection, Primary op wählen, Save result and close game, "I am <Spieler>" |

Touch-Fläche ist immer mindestens 48 dp, auch bei S (die Fläche ragt über die sichtbare Höhe hinaus).

**3. Form, Farben, Zustände**

- Form: Primary, Secondary, Text und Danger sind Pillen (volle Rundung). Chips 8 dp. Icon-Buttons rund.
- Farben nur aus `Palette`: Primary = `Accent` mit weißer Schrift, Pressed = dunkleres Orange, Danger = neues `Palette.Danger` (rot), Text/Secondary = `Accent`.
- Deaktiviert: 38 % Deckkraft, keine Interaktion. Es gibt keinen eigenen Ladezustand (alles läuft lokal).
- Auf dem Desktop zeigt ein Fokusrahmen (2 dp, hell) die Tastaturauswahl.

**4. Beschriftung**

- Englisch, Satzanfang groß, Verb zuerst ("Show crit op and map", "Save result and close game").
- Schalter zeigen die Aktion des nächsten Zustands ("Show" / "Hide").
- Keine Symbole im Text außer "< Back"; kein Punkt am Ende, höchstens etwa 28 Zeichen, sonst umbrechen (zentriert).
- Nach Rolle benennen: Abbrechen immer "Cancel", Schließen immer "Close", Bestätigen mit konkretem Verb statt "OK" (außer reinen Hinweisdialogen).

**5. Anordnung**

- Pro Karte und Seite höchstens eine Primary.
- Einzelne Aktion in einer Karte: volle Breite. Mehrere gestapelt: Primary oben, Secondary darunter, Danger zuletzt und optisch abgesetzt.
- Zwei Aktionen nebeneinander: gleich breit (`weight(1f)`), Primary rechts.
- Dialoge: Bestätigen rechts (Primary, bei Löschen Danger), Abbrechen links (Text).
- Aktionen in Zeilen mit S-Buttons laufen in einer `FlowRow` und umbrechen.
- Das Seitenende gilt für die Aktion zum Weiterkommen (Next) und darunter Danger (Discard game).

**6. Umsetzung im Code**

- Neue Datei `ui/Buttons.kt` mit:
  - `AppButton(text, onClick, kind = Primary, size = M, enabled, fullWidth)` und `enum ButtonKind { Primary, Secondary, Text, Danger }`, `enum ButtonSize { S, M, L }`
  - `SegmentedChoice(options, selected, label, onPick)`
  - verschobene Bausteine `ChipRow`, `Stepper`, `FavoriteButton` (Icon), `NavButton`
- Direkte Aufrufe von `Button`, `OutlinedButton`, `TextButton` außerhalb von `Buttons.kt` entfallen. Prüfung beim Review per Suche nach `OutlinedButton(` und `TextButton(`.
- `Palette` bekommt `Danger`; der gedrückte Zustand kommt vom Ripple des Systems.
- Migration in Etappen, jede Etappe baut und testet:
  1. `Buttons.kt` und `Palette`
  2. Game (`GameScreen`, `GameSetup`) mit den meisten Buttons
  3. Teams, Generator, More, Rules
  4. Dialoge (`ConfirmDialog`, Keywords, Image-Viewer)
  5. Messfunktion zuletzt (pausiert, Flag aus)

**7. Abgrenzung**

- Karten-Titelleisten und Tabs der unteren Navigation sind keine Buttons dieses Systems.
- Die Kamera-Toolbar der Messfunktion nutzt vorerst weiter eigene Farben auf Kamerabild.

## 4. Roadmap

| Phase | Inhalt |
| --- | --- |
| 0 | Branch `alpha`, Version `1.4.0.a`, Feature-Flag Kameramessung aus (I), Versionsanzeige (E) |
| 1 | Rechtliches (F), Regeln zusammenführen (C) |
| 2 | Design-Mock-ups (A, B), Freigabe |
| 3 | Orange-Theme (A), Rules-Tab mit Favoriten-Schalter (D) |
| 4 | Game Wizard (B) |
| 5 | Inkrementelle Updates + PDF-Reader (G) |
| 6 | Beta-Release `1.4.0.b`: neuer Schlüssel (H), neue Codes, Datenstand setzen |

## 5. Offene Fragen

Offen:

- Welche offiziellen Farbwerte und Schriften sollen für Orange als Referenz dienen (eigene Screenshots der offiziellen App)?
- Darf Artwork von Games Workshop als Hintergrund verwendet werden, solange die App nur im Testkreis bleibt?
- Welche PDFs (Quelle, Format) sollen für den PDF-Reader gelesen werden und liegen sie in einem festen Layout vor?

Entschieden:

- Der Alpha-Build ist nicht parallel zur Beta installierbar (gleiche Application-ID).
- Favoriten gelten für Regelseiten, Teams, Crit Ops und Tac Ops.
