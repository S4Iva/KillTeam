# Anforderungen: Kill Team Companion App

Stand: 2026-11-04 | Status: Entwurf (Alpha 1.4.0.a in Planung, Konzept in [konzept.md](konzept.md), Mock-up in [mockups/game-wizard.html](mockups/game-wizard.html))

## 1. Ziel

Eine Kill-Team-App (3rd Edition) mit dem **Mission Generator aus Approved Ops 2025** als Kernfunktion. Alle Regeltexte, Daten und Bilder stammen von [Wahapedia](https://wahapedia.ru/kill-team3/the-rules/approved-ops-2025/).

## 2. Rahmenbedingungen

| Thema | Entscheidung |
| --- | --- |
| Plattformen | Android, iOS, Desktop (JVM) |
| Technik | Kotlin Multiplatform + Compose Multiplatform (bestehendes Projekt, Modul `shared`) |
| UI-Sprache | Englisch (Regelbegriffe wie auf Wahapedia) |
| Nutzung | Rein privat, nicht kommerziell, keine Store-Veröffentlichung |
| Datenquelle | Einmaliger Import von Wahapedia, Auslieferung mit der App |
| Konnektivität | Vollständig offline nutzbar |

### Rechtliches

- Inhalte und Bilder sind Eigentum von Games Workshop bzw. Wahapedia (© Vyacheslav Maltsev). Die App bleibt privat und wird nicht weitergegeben.
- Die Quelle ist in der App ("About") anzugeben.
- Der Import erfolgt einmalig und schonend (kein Dauer-Scraping, keine Last auf Wahapedia).
- Bei späterer Veröffentlichung müssen Daten, Bilder und Lizenzfragen neu bewertet werden.

## 3. Datenmodell (Import aus Wahapedia)

### 3.1 Approved Ops 2025

- **Crit Ops (9):** Secure, Loot, Transmission, Orb, Stake Claim, Energy Cells, Download, Data, Reboot. Je Crit Op: Name, Mission Action (AP-Kosten, Bedingungen), Victory Points, zugehörige Maps.
- **Tac Ops (12)** in 4 Archetypen:
  - Infiltration: Plant Devices, Steal Intelligence, Track Enemy
  - Recon: Flank, Retrieval, Scout Enemy Movement
  - Security: Plant Banner, Martyrs, Envoy
  - Seek & Destroy: Rout, Sweep & Clear, Dominate
  - Je Tac Op: Archetyp, Reveal-Bedingung, Additional Rules, Mission Action, Victory Points, Max-VP pro Turning Point.
- **Kill Op:** Kill-Grade-Tabelle (Bild `KillGrade.png`), Wertung pro neuem Kill Grade und Vergleich am Ende.
- **Maps:** Je Killzone 6 empfohlene Layouts, Gallowdark-Maps und unspezifische Maps (z. B. "Non-specific 5").
- **Universal Map Key:** Drop Zones, Territory, Killzone Edge, Objective Marker, Centre, Centre Line (Spieler 1, Spieler 2, neutral).
- **Initiative-Karten:** Re-roll, +1/-1 bis +3/-3 (nach Turning Point).

### 3.2 Weitere Daten

- Kill Teams mit Operatives, Waffenprofilen, Fähigkeiten, Ploys, Equipment, Archetypen.
- Core Rules, Killzones, Terrain-Typen, Glossar, Appendix.
- Classified-Rotation (Kill-Team-Liste nach Season).

### 3.3 Bilder

- Maps, Map-Key-Symbole, Kill-Grade-Tabelle, Kill-Team-Bilder werden einmalig heruntergeladen und als Ressourcen gebündelt (`composeResources`).
- Nicht gefundene oder fehlende Bilder zeigen einen Platzhalter.

## 4. Funktionale Anforderungen

Priorität: **M** = Muss, **S** = Soll, **K** = Kann.

### 4.1 Mission Generator (Kernfunktion)

| ID | Anforderung | Prio |
| --- | --- | --- |
| MG-1 | Button "Generate Mission" wählt nach der Methode RANDOM Mission (Crit Op) und Map. | M |
| MG-2 | Dropdowns für Mission, Map und Killzone erlauben manuelle Auswahl statt Zufall. | M |
| MG-3 | Anzeige der Crit Op mit Mission Action, Victory Points und Map-Bild samt Universal Map Key. | M |
| MG-4 | Anzeige der Kill-Op-Regeln inklusive Kill-Grade-Tabelle. | M |
| MG-5 | Liste aller Tac Ops, filterbar nach Archetyp (Infiltration, Recon, Security, Seek & Destroy). | M |
| MG-6 | Tac Ops werden nicht im Generator, sondern im Spiel (Schritt 2 "Select Operatives") geheim ausgewählt, mit "Select" / "Remove" (eigenes Fadenkreuz-Symbol, siehe DS-6) an den Tac-Op-Karten; wählbar sind die Archetypen des Kill Teams. Der Generator zeigt die Tac Ops nur zum Nachlesen. | M |
| MG-7 | Auswahl und generierte Mission bleiben nach App-Neustart erhalten (lokale Persistenz, Ersatz für die Cookies von Wahapedia). | M |
| MG-8 | Crit-Op-Gruppen laut Tournament Companion für rundenweise Zufallsauswahl: (Secure, Loot, Transmission), (Orb, Energy Cells, Download), (Stake Claim, Data, Reboot). | S |

### 4.2 Spielablauf-Assistent

Führt durch die fünf Schritte des Approved-Ops-Spiels:

1. **Set up the Battle:** Kill Teams, Killzone und Terrain, Crit Op, Objective Marker, Roll-off um Initiative, Drop-Zone-Wahl (Gegner erhält Re-roll-Initiative-Karte).
2. **Select Operatives:** Operatives und bis zu 4 verschiedene Equipment-Optionen, 2 CP je Spieler, Tac-Op-Wahl.
3. **Set up Operatives:** Equipment im Wechsel aufstellen, danach je ein Drittel (aufgerundet) des Teams mit Conceal-Order.
4. **Play the Battle:** 4 Turning Points mit Initiative-Roll-off, Primary-Op-Wahl als Strategic Gambit im 1. Turning Point.
5. **End the Battle:** Primary-Op-Reveal, Bonus-VP, Gewinner oder Unentschieden.

| ID | Anforderung | Prio |
| --- | --- | --- |
| AS-1 | Schritt-für-Schritt-Ansicht mit Checkliste je Phase und Anzeige des aktuellen Turning Points (1-4). | M |
| AS-2 | Initiative-Verwaltung: Roll-off-Ergebnis, Gewinner entscheidet Initiative, bei Gleichstand gewinnt der Spieler ohne Initiative. | M |
| AS-3 | Initiative-Karten: Verlierer erhält Karte passend zum Turning Point (nicht im 4.), Re-roll-Karte des Gegners, Modifikation auch über 6 und unter 1, Re-roll überschreibt Modifikationen. | M |
| AS-4 | Würfel-Tracker für Initiative-Würfe je Turning Point. | S |
| AS-5 | Kill-Team-Auswahl pro Spieler als Voraussetzung (Verknüpfung mit Roster, siehe 4.4). | S |

### 4.3 Punkte- und Ressourcen-Tracker

| ID | Anforderung | Prio |
| --- | --- | --- |
| VP-1 | VP pro Spieler getrennt nach Crit Op, Kill Op, Tac Op (Maximum 6 VP je Op). | M |
| VP-2 | Primary Op pro Spieler geheim wählen, am Ende gleichzeitig aufdecken, Bonus = halbe VP dieser Op (aufgerundet). | M |
| VP-3 | Automatische Endwertung, Gewinner oder Draw. | M |
| VP-4 | Kill-Grade-Berechnung aus Anzahl gegnerischer Operatives und Ausschaltungen (1 VP je neuer Stufe bis 5, 1 VP am Ende bei höherem Grade). | M |
| VP-5 | CP-Zähler (Start 2 CP) pro Spieler. | M |
| VP-6 | Tac-Op-spezifische Hilfen, z. B. Dominate-Token, Martyr-Token, Device-Token, Intelligence-Marker, VP-Limit pro Turning Point. | S |

### 4.4 Geheime Auswahl (Pass-and-Play)

| ID | Anforderung | Prio |
| --- | --- | --- |
| GA-1 | Verdeckte Eingabe von Tac Op und Primary Op je Spieler auf einem Gerät mit Sichtschutz-Bildschirm zwischen den Spielern. Das Equipment ist nicht mehr verdeckt (siehe RW-13, RW-14). | M |
| GA-2 | Gleichzeitiges Aufdecken der Auswahl (Tac Op, Primary Op). | M |
| GA-3 | Reveal-Bedingungen der Tac Op als Erinnerung (z. B. "first time a friendly operative performs the Scout action"). | S |

### 4.5 Kill-Team-Browser und Regel-Nachschlagewerk

| ID | Anforderung | Prio |
| --- | --- | --- |
| KB-1 | Liste aller Kill Teams mit Suche und Filter nach Season und Classified-Status. | M |
| KB-2 | Detailansicht: Operatives, Datenblätter (APL, Move, Save, Wounds), Waffenprofile, Fähigkeiten, Ploys, Equipment. | M |
| KB-3 | Core Rules, Killzones, Terrain-Typen und Glossar mit Volltextsuche. | S |
| KB-4 | Favoriten und eigene Notizen zu Missionen, Tac Ops, Teams und Regeln. | S |

### 4.6 Roster-Builder

| ID | Anforderung | Prio |
| --- | --- | --- |
| RB-1 | Kill Team zusammenstellen gemäß Auswahlregeln des Teams (Operative-Anzahl, Limits). | M |
| RB-2 | Rosters lokal speichern, benennen, bearbeiten, löschen. | M |
| RB-3 | Archetypen des Teams bestimmen die wählbaren Tac Ops. | S |
| RB-4 | Teilen/Exportieren als Text. | K |

### 4.7 Datenblatt-Tracker (im Spiel)

| ID | Anforderung | Prio |
| --- | --- | --- |
| DT-1 | Pro Operative: aktuelle Wounds, Aktivierungsstatus (ready/expended), Incapacitated. Die Anzeige und die Buttons für die Order (Engage/Conceal) entfallen (2026-11-04); ein vorhandenes Feld in gespeicherten Spielständen bleibt erhalten, wird aber nicht mehr angezeigt. | M |
| DT-2 | Tokens und Marker pro Operative (z. B. Dominate, Intelligence-Marker). | S |
| DT-3 | Ausschaltungen fließen automatisch in die Kill-Grade-Berechnung ein. | S |

### 4.8 Karten- und Killzone-Ansicht

| ID | Anforderung | Prio |
| --- | --- | --- |
| KZ-1 | Map-Bilder in hoher Auflösung mit Zoom und Pan. | M |
| KZ-2 | Legende (Universal Map Key) einblendbar. | M |
| KZ-3 | Killzone-Infos (z. B. Bheta-Decima: Objective Marker nicht nur am Boden) und Terrain-Typen. | S |
| KZ-4 | Auswahl von Drop Zone je Spieler, Spiegelung/Drehung der Ansicht für Spieler 2. | K |

### 4.9 Turnier-Modus (Tournament Companion)

| ID | Anforderung | Prio |
| --- | --- | --- |
| TM-1 | Turnier mit Spielern und Runden anlegen. | S |
| TM-2 | Crit Op pro Runde für alle Spieler gleich, zufällig oder vorgegeben (siehe MG-8). | S |
| TM-3 | Pairings: Runde 1 zufällig; danach nach Record, dann Win Path, dann zufällig; bei ungerader Zahl Paarung des niedrigsten Spielers mit dem höchsten der nächsten Gruppe. | S |
| TM-4 | Ranking: Record (2 Unentschieden = 1 Sieg + 1 Niederlage), dann Strength of Schedule, dann Primary-Op-VP. | S |
| TM-5 | Zuordnung der Terrain-Layouts pro Runde (Rotation). | K |
| TM-6 | Classified-Format-Filter für erlaubte Kill Teams. | K |

### 4.10 Spielhistorie und Statistiken

| ID | Anforderung | Prio |
| --- | --- | --- |
| SH-1 | Abgeschlossene Spiele speichern: Teams, Mission, Map, VP je Op, Primary Op, Ergebnis, Datum. | S |
| SH-2 | Statistiken: Winrate pro Team, Mission, Crit Op und Tac Op, Durchschnitts-VP. | K |
| SH-3 | Spielstand laufender Partien wiederherstellbar. | S |

### 4.11 Würfelsimulator und Schadensrechner

| ID | Anforderung | Prio |
| --- | --- | --- |
| WS-1 | Würfelsimulator (W6, mehrere Würfel, Re-roll). | K |
| WS-2 | Schadensrechner: Wahrscheinlichkeiten für Angriff und Verteidigung anhand von Waffenprofil und Save. | K |

### 4.12 Zugangscodes für Alpha-Tests

Ersetzt die zwei festen Passwörter aus AT-13 (unbegrenzt, 7 Tage ab erster Eingabe) durch Codes, die ein externer Generator erzeugt. Die App prüft die Codes offline.

| ID | Anforderung | Prio |
| --- | --- | --- |
| ZC-1 | **Format:** Der Code hat 109 Zeichen (Base32 ohne verwechselbare Zeichen wie 0/O und 1/I/L) in 5er-Gruppen mit Bindestrichen und ist zum Einfügen gedacht. Bei der Eingabe sind Groß-/Kleinschreibung, Bindestriche und Leerzeichen egal. | M |
| ZC-2 | **Inhalt:** Ein Code enthält den Ablaufzeitpunkt (in vollen Stunden, UTC; Wert 0 = unbegrenzt), eine Zufallszahl, damit jeder Code einmalig ist, und die Ed25519-Signatur darüber (64 Byte). Die App liest daraus ab, ob der Code unbegrenzt ist oder bis wann er gilt. | M |
| ZC-3 | **Frist ab Generierung:** Der Ablauf ist fest im Code eingetragen und gilt ab dem Zeitpunkt der Erzeugung, nicht ab der ersten Eingabe. Ein Code lässt sich nach Ablauf nicht mehr verwenden, auch nicht nach Neuinstallation oder Löschen der App-Daten. | M |
| ZC-4 | **Generator (extern):** Das Skript `tools/generate-access-code.ps1` erzeugt einen Code und gibt Code und Ablaufzeitpunkt aus. Die Dauer ab Generierung wird mit `-d` (Tage) und `-h` (Stunden) angegeben, beides ist kombinierbar (z. B. `-d 7 -h 12`). Alternativ legt `-Until yyyy-MM-dd[ HH:mm]` ein festes Ablaufdatum in UTC fest (nur Datum = bis Ende dieses Tages, auf volle Stunden aufgerundet). Die App rechnet den Zeitpunkt auf die Zeitzone des Geräts um (siehe ZC-9). `-Unlimited` erzeugt einen unbegrenzten Code. Genau eine dieser drei Angaben ist nötig. | M |
| ZC-5 | **Schlüssel:** Die App enthält nur den öffentlichen Ed25519-Schlüssel (`AccessConfig.PUBLIC_KEY` in `logic/AccessGate.kt`); den privaten hält nur der Generator (`tools/access-private-key.txt`, nicht im Repository). `-NewKey` erzeugt ein neues Paar; ein Wechsel macht alle bisherigen Codes ungültig. Ohne öffentlichen Schlüssel ist die Abfrage abgeschaltet. | M |
| ZC-6 | **Prüfung beim Start:** Die App fragt beim Start nach einem Code, solange kein gültiger gespeichert ist. Ein gültiger Code wird lokal gespeichert, bis er abläuft. Ungültige oder manipulierte Codes werden mit "Wrong code" abgelehnt, abgelaufene mit "Code expired". | M |
| ZC-7 | **Ablauf im Betrieb:** Läuft der Code ab (auch bei geöffneter App, Prüfung mindestens minütlich), sperrt die App und fragt nach einem neuen Code. Ein neuer Code ersetzt den alten. Die Spielstände bleiben erhalten. | M |
| ZC-8 | **Uhr-Schutz:** Wird die Systemuhr hinter den zuletzt gesehenen Zeitpunkt zurückgestellt (Toleranz 5 Minuten), sperrt die App, bis die Uhr wieder stimmt. | S |
| ZC-9 | **Anzeige:** Nach der Eingabe zeigt ein Dialog einmalig "Unlimited" oder den Ablaufzeitpunkt (aus UTC in die Zeitzone des Geräts umgerechnet) und die Restzeit. Unter More steht dauerhaft der Status: unbegrenzt oder Restzeit und Ablaufzeitpunkt. | M |
| ZC-10 | **Tests:** Unit-Tests für Erzeugen und Prüfen (Round-Trip), Tippfehler und manipulierte Zeichen, unbegrenzt, abgelaufen, Uhr zurückgestellt und ein fester Testvektor, damit PowerShell-Generator und App-Prüfung übereinstimmen. | M |
| ZC-11 | **Nutzungsbedingungen:** Das Login-Fenster zeigt unter "Unlock" den Hinweis "By entering the code you accept the terms of use." und einen Schalter "Show terms of use", der die Bedingungen aufklappt (zugeklappt als Standard, Fenster scrollbar). Inhalt: privater, nicht kommerzieller Test; keine Weitergabe von App, APK, Code oder größeren Inhalten; Code nur für den Empfänger, keine Umgehung und keine eigenen Codes; Urheberrecht und Marken von Games Workshop und Wahapedia, inoffizielle App; keine Rückentwicklung; Haftungsbeschränkung; Offline-Betrieb ohne Konten und Telemetrie; Zugang jederzeit beendbar. Die Eingabe des Codes gilt als Zustimmung, eine Checkbox gibt es nicht. | M |
| ZC-12 | **Schlüsselrotation pro Feature-Release:** Mit jedem Beta-Release eines neuen Features (erste Ziffer der Version, siehe VR-1) wird ein neues Ed25519-Schlüsselpaar erzeugt (`-NewKey -Force`). Codes der neuen Version laufen nicht in alten Versionen, alte Codes nicht in der neuen. Der private Schlüssel je Version wird gesichert (z. B. `tools/keys/access-private-key-1.4.txt`, nicht im Repository, externes Backup). Erweiterungs- und Bug-Releases sowie Alpha-Builds behalten den Schlüssel der letzten Beta. Vor dem Release sind die Tester zu informieren, da sie für das Update einen neuen Code brauchen. | S |

Grenzen: Wer die APK zerlegt, kann die Abfrage überspringen (Patchen), aber keine gültigen Codes erzeugen. Ein Code ist nicht an ein Gerät gebunden und lässt sich weitergeben. Die Frist startet bei der Erzeugung, ein spät eingegebener Code hat entsprechend weniger Restzeit.

Entscheidung: Die zwei festen Passwörter (`tools/set-access-password.ps1`, `HASH`/`HASH_TEMP`) sind nach der Umstellung entfallen.

Status 2026-11-02: ZC-1 bis ZC-10 sind umgesetzt (`tools/generate-access-code.ps1`, `logic/AccessGate.kt`).

### 4.13 Icons in der Tab-Leiste

Die untere Navigationsleiste (`NavigationBar` in `App.kt`) zeigt bisher nur Text. Jeder Tab (Generator, Game, Teams, Rules, More) bekommt ein Icon über ein eigenes Vektor-Drawable als Compose-Resource, ohne neue Abhängigkeit (kein `material-icons-extended`).

| ID | Anforderung | Prio |
| --- | --- | --- |
| TI-1 | **Ressourcen:** Pro Tab liegt ein Icon unter `shared/src/commonMain/composeResources/drawable/` (`ic_generator`, `ic_game`, `ic_teams`, `ic_rules`, `ic_more`). Format: Android-Vektor-Drawable (XML) mit 24x24 dp, einfarbig, aus Material Symbols (Apache-2.0) oder selbst gezeichnet. Die Quelle der Icons steht in der README. | M |
| TI-2 | **Zuordnung:** Das Enum `Tab` (`ui/UiState.kt`) trägt zusätzlich ein `icon: DrawableResource`. `App.kt` setzt es im `NavigationBarItem` über `Icon(painterResource(t.icon), contentDescription = t.label)`. Neue Tabs müssen ein Icon angeben, sonst kompiliert der Code nicht. | M |
| TI-3 | **Farben:** Das aktive Icon nutzt `Palette.Accent`, inaktive `Palette.Text`, analog zu den Beschriftungen (`selectedIconColor`, `unselectedIconColor`). Die Icons sind dafür einfarbig und werden vom Theme eingefärbt. | M |
| TI-4 | **Label bleibt:** Die Textbeschriftung bleibt unter dem Icon sichtbar, damit die Tabs eindeutig bleiben. Der aktive Tab behält den Indikator (`0xFF3C3C3C`). | M |
| TI-5 | **Plattformen:** Die Icons erscheinen unverändert auf Android, iOS und Desktop. Die Leiste bleibt in Hoch- und Querformat bedienbar (NF-4), Touch-Fläche mindestens 48 dp. | M |
| TI-6 | **Motive:** Alle Icons haben einen achteckigen Rahmen (Oktagon) mit einem Symbol darin: Generator = Würfelaugen, Game = Play, Teams = drei Figuren, Rules = offenes Buch, More = drei Punkte. Die Icons sind einfarbig und eigenständig gezeichnet, nicht die Wahapedia-Grafiken (siehe Abschnitt 2, Rechtliches). | S |
| TI-7 | **Zurück zum Start des Tabs:** Ein Tipp auf einen Tab in der Leiste bringt den Nutzer immer an den Anfang dieses Tabs, auch wenn der Tab schon aktiv ist. Ist er in einer Unterseite (z. B. Team-Details, Regelseite, Unterseite unter More, offene Suche), schließt die App sie und zeigt die Startansicht des Tabs (Teams: Teamliste, Rules: Regelübersicht, More: Hauptmenü, Generator und Game: Hauptansicht). Die Startansicht wird ganz nach oben gescrollt. | M |
| TI-8 | **Daten bleiben erhalten:** Das Zurücksetzen betrifft nur Navigation und Scrollposition. Laufende Partie, Spielstand, Generator-Auswahl, Rosters, Favoriten und Notizen bleiben unverändert. Ein Tipp auf den aktiven Tab, der schon in der Startansicht oben steht, ändert nichts. | M |
| TI-9 | **Abnahme:** Alle fünf Tabs zeigen auf Android und Desktop ein Icon in beiden Zuständen (aktiv/inaktiv), kein Icon ist abgeschnitten oder falsch eingefärbt. Der Tipp auf jeden Tab führt wie in TI-7 zur Startansicht, oben. `./gradlew :shared:jvmTest` und `:androidApp:assembleDebug` laufen weiterhin fehlerfrei. | M |

Nicht Teil dieser Anforderung: Badges auf Tabs, Animationen beim Tabwechsel, ein Wechsel auf `material-icons-extended`.

Status 2026-11-02: TI-1 bis TI-8 sind umgesetzt (`ui/UiState.kt` `goHome`, `App.kt`), TI-9 ist auf dem Gerät noch zu prüfen.

### 4.14 Kamera-Messfunktion (Distanzen am Spieltisch)

Die App misst Abstände auf dem Spieltisch über die Kamera, z. B. zwischen zwei Operatives, einem Operative und einem Objective Marker oder entlang eines Bewegungswegs. Die Messpunkte wählt der Nutzer frei im Kamerabild. Der Ersatz für Maßband und Mess-Schablonen (Kreis, Dreieck, Quadrat, Fünfeck) ist eine Hilfe und kein Regelschiedsrichter.

| ID | Anforderung | Prio |
| --- | --- | --- |
| KM-1 | **Zugang:** Die Messfunktion ist ausschließlich über More (Eintrag "Measure") erreichbar, nicht über andere Tabs oder den Spielablauf. Der Eintrag öffnet die Kamera im Vollbild. Beim ersten Aufruf fragt die App die Kamera-Berechtigung mit kurzer Erklärung ab. Wird sie verweigert, zeigt die App einen Hinweis mit Weg in die Einstellungen; der Rest der App bleibt nutzbar. | M |
| KM-2 | **Freie Punktwahl:** Ein Tipp ins Kamerabild setzt einen Messpunkt an beliebiger Stelle. Es sind mindestens zwei und beliebig viele Punkte (mindestens 10) möglich. Die Punkte sind nummeriert (1, 2, 3, ...) und über Linien verbunden. | M |
| KM-3 | **Anzeige:** Für jede Strecke zwischen zwei aufeinanderfolgenden Punkten zeigt die App die Länge direkt an der Linie, zusätzlich die Gesamtlänge des Linienzugs. Die Werte aktualisieren sich sofort bei jeder Änderung. | M |
| KM-4 | **Punkte bearbeiten:** Ein gesetzter Punkt lässt sich per Ziehen verschieben (Fadenkreuz am Punkt zur genauen Platzierung), einzeln löschen und über "Undo" und "Clear all" zurücknehmen bzw. komplett entfernen. Die Touch-Fläche pro Punkt beträgt mindestens 48 dp. | M |
| KM-5 | **Messmodi:** (a) "Path": Linienzug über alle Punkte in Reihenfolge, Gesamtlänge; (b) "Between points": Abstand zwischen zwei frei gewählten Punkten (Tipp auf Punkt A, dann Punkt B), ohne dass die Reihenfolge der Punkte eine Rolle spielt. Der Nutzer kann jederzeit zwischen den Modi wechseln, ohne die Punkte zu verlieren. | M |
| KM-6 | **Kalibrierung:** Da die Kamera allein keine Entfernungen kennt, kalibriert der Nutzer den Maßstab einmalig pro Messung: Er markiert zwei Punkte einer bekannten Länge und gibt diese ein (z. B. Lineal, bekannte Basegröße 25/28,5/32/40 mm, Spielfeldkante, Objective Marker mit 40 mm). Es gibt Voreinstellungen für die gängigen Base-Größen und Objective Marker. Kalibrierung und Messung gelten für die Ebene des Spieltischs. | M |
| KM-7 | **Perspektive:** Die Kalibrierung berücksichtigt die Tischebene. Optional markiert der Nutzer vier Ecken eines bekannten Rechtecks (z. B. Killzone-Kante, Spielfeld 30" x 22" bzw. 60 x 60 cm) und die App rechnet das Bild per Homographie auf eine Draufsicht um, damit auch Schrägaufnahmen brauchbare Werte liefern. Ohne diese Entzerrung weist die App darauf hin, dass die Kamera möglichst senkrecht über dem Tisch gehalten werden soll. | S |
| KM-8 | **Einheiten:** Anzeige wahlweise in Zoll (Standard, Kill-Team-Regel), Zentimeter und Millimeter. Die Einheit ist umschaltbar und wird gespeichert. | M |
| KM-9 | **Kill-Team-Distanzen:** Zusätzlich zur Zahl zeigt die App für jede Strecke, ob sie innerhalb der Regel-Distanzen liegt: Kreis (1"), Dreieck (2"), Quadrat (3"), Fünfeck (6") (siehe Core Rules). Eine Strecke wird farblich markiert, wenn sie innerhalb der gewählten Distanz liegt ("within X"). Die Referenzdistanz ist wählbar. | S |
| KM-10 | **Standbild:** Mit "Freeze" friert die App das Kamerabild ein, damit der Nutzer Punkte ohne Handzittern setzen kann; "Live" kehrt zur Live-Ansicht zurück. Gesetzte Punkte bleiben an ihrer Bildschirmposition und folgen einer Kamerabewegung nicht (kein AR-Tracking in der Basisversion); nach einer Bewegung ist neu zu kalibrieren oder ein Standbild zu verwenden. | M |
| KM-11 | **Zoom:** Pinch-Zoom und Verschieben im Kamerabild, um Punkte präzise zu setzen. Zoom verändert den Maßstab der Messung nicht (Werte bleiben gleich). | S |
| KM-12 | **Optionale AR-Messung:** Auf Geräten mit ARCore (Android) bzw. ARKit (iOS) kann alternativ die Entfernung über Tiefenerkennung gemessen werden. Die Punkte haften dann an der Tischfläche und folgen der Kamerabewegung. Ohne AR-Unterstützung fällt die App auf die Kalibrierung aus KM-6 zurück. | K |
| KM-13 | **Genauigkeit:** Die App weist beim ersten Aufruf und in der Hilfe auf die Genauigkeit hin (typisch ± 2-3 mm bei sorgfältiger Kalibrierung und senkrechter Aufnahme) und darauf, dass bei Streitfällen das Maßband gilt. Optional zeigt sie eine Warnung, wenn Kalibrierlänge sehr kurz ist (< 5 % der Bildbreite) oder die Aufnahme stark schräg ist. | S |
| KM-14 | **Ergebnisse übernehmen:** Eine Messung lässt sich als Notiz am aktuellen Spiel speichern (z. B. "Operative A to Objective: 4.5\""). Optional wird sie als Bild mit eingeblendeten Punkten und Werten in die Galerie gespeichert oder geteilt. Standardmäßig werden keine Kamerabilder gespeichert. | K |
| KM-15 | **Plattformen:** Android (CameraX) und iOS (AVFoundation) nutzen die Gerätekamera über `expect/actual` in `shared`. Der Desktop (JVM) nutzt eine angeschlossene Webcam oder bietet alternativ den Import eines Fotos; ist beides nicht möglich, ist die Funktion ausgeblendet. Die Mess- und Rechenlogik (Kalibrierung, Homographie, Einheiten, Distanzprüfung) liegt plattformunabhängig in `commonMain`. | M |
| KM-16 | **Datenschutz:** Das Kamerabild wird nur lokal verarbeitet, nicht gespeichert und nicht übertragen (NF-1, NF-7). Die Kamera ist nur aktiv, solange die Messansicht sichtbar ist, und wird beim Verlassen oder Ausblenden der App freigegeben. | M |
| KM-17 | **Bedienung:** Die Messansicht funktioniert im Hoch- und Querformat (NF-4) und im Dunkelmodus (NF-5). Eine kurze Einblendung erklärt die Schritte (1. Kalibrieren, 2. Punkte setzen, 3. Ergebnis lesen). Der Bildschirm bleibt wach. | S |
| KM-18 | **Tests:** Unit-Tests in `commonTest` für die Umrechnung Pixel zu Zoll/cm/mm, Kalibrierung mit zwei Punkten, Homographie mit bekanntem Rechteck, Länge von Linienzügen mit mehreren Punkten, Abstand zwischen beliebigen Punkten und Distanzprüfung (Kreis, Dreieck, Quadrat, Fünfeck, inklusive Grenzwerten). | M |
| KM-19 | **Abnahme:** Auf Android und Desktop lassen sich nach Kalibrierung mindestens drei frei gewählte Punkte setzen, verschieben und löschen; die angezeigten Strecken weichen bei senkrechter Aufnahme eines 30"-Referenzmaßes um höchstens 3 mm je 10 cm ab; Zugang über More (KM-1) und Berechtigungsablauf sind geprüft. | M |
| KM-20 | **Pausiert (ab 1.4.0.a):** Die Messfunktion wird herausgenommen, aber nicht gelöscht. Ein Feature-Flag `FeatureFlags.MEASURE = false` blendet den Eintrag "Measure" unter More aus und initialisiert die Kamera nicht. Die CAMERA-Berechtigung entfällt im Android-Manifest, solange das Flag aus ist. Der Code (`logic/Measure.kt`, `ui/MeasureScreen.kt`, `ui/CameraSource*.kt`, Tests) bleibt im Repository. Die Funktion kann in einer späteren Version wieder aktiviert werden, wenn sie sich als sinnvoll erweist. Die Datenschutztexte (RC-3) erwähnen die Kamera nur, wenn das Flag aktiv ist. | M |

Nicht Teil dieser Anforderung (Basisversion): automatische Erkennung von Operatives oder Terrain per Bilderkennung, Sichtlinien- und Deckungsprüfung, Aufzeichnung von Videos, Cloud-Auswertung.

Status 2026-11-03: KM-1 bis KM-11, KM-13, KM-15 bis KM-18 sind umgesetzt (`logic/Measure.kt`, `ui/MeasureScreen.kt`, `ui/CameraSource*.kt`, Tests in `MeasureTest`). Offen: KM-12 und KM-14 (Prio K), iOS-Kamera (der Eintrag ist dort ausgeblendet, braucht macOS zum Bauen und Prüfen), Desktop-Webcam (dort nur Foto-Import), KM-19 (Abnahme auf dem Gerät).

### 4.15 Versionierung und Release-Prozess (Alpha 1.4.0.a)

Die Version 1.3.0.b läuft im Test bei Usern. Neue Features entstehen ausschließlich in der Alpha (`1.4.0.a`). Die Versionsnummer wird erst beim nächsten Release zur Beta festgelegt. Bis dahin wird nur in der Alpha entwickelt, in der Beta nur Bugs behoben (dritte Ziffer).

| ID | Anforderung | Prio |
| --- | --- | --- |
| VR-1 | **Schema:** `v<Feature>.<Erweiterung>.<Bug>.<Stufe>`: erste Ziffer = Feature, zweite = Erweiterung, dritte = Bug, Suffix `b` = Beta, `a` = Alpha (z. B. `v1.3.0.b`). | M |
| VR-2 | **Anzeige:** Unter More steht ganz unten, nach dem letzten Feld, als Text die Version mit Datenstand: `v1.4.0.a - Daten: 2026-11-02` (Datum der letzten Aktualisierung der Daten). Die Version stammt aus einer Konstante in `shared`, der Datenstand aus dem Feld `dataDate` in `killteams.json` und `rules.json` (wird vom Import gesetzt). Betrifft NF-6. | M |
| VR-3 | **Ablauf:** Branch `alpha` (oder `feature/*`) für neue Features; Beta-Release mit Versionsnummer, neuem Schlüsselpaar (ZC-12) und neuem Datenstand. Bug-Fixes der Beta laufen getrennt vom Alpha-Stand. | S |
| VR-4 | **Installation:** Alpha und Beta verwenden dieselbe Application-ID. Ein Alpha-Build ist nicht parallel zur Beta installierbar und ersetzt sie auf dem Gerät. | M |

Status 2026-11-04: VR-1, VR-2 und VR-4 sind umgesetzt (`logic/AppInfo.kt`, Fußzeile unter More, `versionName` 1.4.0.a; der Datenstand kommt aus `importedAt` der `killteams.json`). VR-3 ist organisatorisch.

### 4.16 Design: Kill-Team-Orange

Betrifft AT-14 (eigenständiges Design, bisher Petrol/Teal in `Palette`, `ui/Components.kt`). Das neue Design lehnt sich an die offiziellen Kill-Team-Farben und das Design der offiziellen App an, bleibt aber eigenständig gezeichnet.

| ID | Anforderung | Prio |
| --- | --- | --- |
| DS-1 | **Farbschema:** Dunkler Grund (Anthrazit/Schwarz), Akzent **Kill-Team-Orange** (Richtwert `#E8650A`), helle Schrift. Alle Farben liegen nur in `Palette`, sodass ein Wechsel ohne Umbau der Screens möglich ist. | M |
| DS-2 | **Buttons:** Gut erkennbar und einfach zu bedienen: primär = gefüllt orange, sekundär = Outline, Mindestgröße 48 dp, klare Beschriftung. Die nächste Aktion ist immer der auffälligste Button. | M |
| DS-3 | **Hintergrundbilder:** Dezente Hintergründe (Textur, Raster, Metall) mit geringer Deckkraft sind möglich. Es werden nur eigene oder lizenzfreie Bilder verwendet, bis die Frage zu GW-Artwork geklärt ist (siehe Abschnitt 7). | K |
| DS-4 | **Mock-ups zuerst:** Vor der Umsetzung entstehen Design-Entwürfe (Game-Phase, Phasenübersicht, Rules, More), siehe [mockups/game-wizard.html](mockups/game-wizard.html). Umsetzung erst nach Freigabe. | M |
| DS-5 | **Referenz:** Die Orange-Farbwerte und Schriften orientieren sich an der offiziellen Kill-Team-App. Die genauen Werte sind noch zu klären (Abschnitt 7). | S |
| DS-6 | **Eigenes Select-Symbol für Tac Ops:** Der Button zum Auswählen und Entfernen einer Tac Op (Karte im Spielablauf, MG-6, AT-7) nutzt keine Wahapedia-Grafik (bisher grüner Haken im Kreis), sondern ein eigenes, selbst gezeichnetes **Fadenkreuz** als Vektor-Drawable: "Select" = Fadenkreuz als orange Outline, "Remove" = gefüllte orange Scheibe mit dunklem Fadenkreuz. Größe 36 dp, Touch-Fläche mindestens 48 dp. Die Wahapedia-Grafiken `btn_select`/`btn_remove` entfallen. | M |
| DS-7 | **Zähler rechtsbündig** (ersetzt die linksbündige Fassung vom selben Tag): Alle Stepper stehen als Zeile über die volle Kartenbreite. Die Beschriftung steht links, der Zähler ("-", Wert, "+") sitzt am rechten Rand, sodass die Zähler untereinander bündig in einer Spalte rechts stehen (Spielerkarte, Score, Rechner, Turnier). Die Wertspalte hat eine feste Mindestbreite und den Wert zentriert. | M |

Status 2026-11-04: DS-1, DS-2, DS-4, DS-6 und DS-7 sind umgesetzt (`Palette` auf Orange/Anthrazit; Mock-up liegt vor). Der Orange-Wert ist ein Richtwert (DS-5 offen), DS-3 ist nicht umgesetzt.

### 4.17 Game Wizard

Weiterentwicklung des Game-Tabs (4.2). Der Spielablauf führt den Spieler durch jede Phase klar und verständlich, ohne dass er suchen muss.

| ID | Anforderung | Prio |
| --- | --- | --- |
| GW-1 | **Phasenseite:** Jede Phase zeigt Titel, Kurzbeschreibung, die Liste "Was jetzt zu tun ist" als reine Textliste mit "- " (keine Haken, keine Bestätigungsfelder), relevante Regeln (einklappbar) und Aktionsbuttons. Betrifft AS-1, AT-9. | M |
| GW-2 | ~~Weiter-Pfeil~~ **Entfällt (2026-11-04, nach Test):** Der runde Pfeil unten rechts und die Sperre durch Pflichtschritte werden nicht umgesetzt. Der Phasenwechsel läuft wie bisher über die Phasenwahl und den Button "Next". | - |
| GW-3 | ~~Zurück-Pfeil~~ **Entfällt** mit GW-2. | - |
| GW-4 | **Fortschritt:** Oben in der Phasenkarte zeigt eine Leiste Schritt 1-5 den aktuellen Stand; der Turning Point steht im Seitentitel. Eine eigene Übersichtsseite ist nicht Teil dieser Umsetzung. | S |
| GW-5 | **Punktestand:** Der Score (Crit, Kill, Tac, Primary, Summe je Spieler) ist in der Phasenseite sichtbar (AT-2). | S |
| GW-6 | **Persistenz:** Der Spielfortschritt bleibt nach App-Neustart erhalten (SH-3). Bei jedem Phasenwechsel scrollt die Ansicht nach oben (AT-5). | M |
| GW-7 | ~~Umsetzung über `PhaseScaffold`~~ **Entfällt** mit GW-2. | - |
| GW-8 | **Primary-Op-Auswahl:** Die Buttons "Player 1/2: select/change primary op" im Strategic gambit (Turning Point 1) sind größer: volle Breite, Mindesthöhe 56 dp. | M |
| GW-9 | **Next am Seitenende:** Der Button zur nächsten Phase ("Next: <Phase>", Phasen 1 bis 3) steht ganz unten am Ende der Phasenseite, über "Discard game", in voller Breite und in derselben Größe wie "Next turning point" in Phase 4 (Standardhöhe), nicht mehr in der Phasenkarte oben. In Phase 4 bleibt der Button "Next turning point" am Ende, in Phase 5 gibt es keinen Next-Button. | M |
| GW-10 | **Secret selection breiter:** Der Button "Secret selection (tac op, equipment)" in "Select Operatives" hat die volle Kartenbreite und mindestens 56 dp Höhe. | M |
| GW-11 | **Mission-Karte überarbeitet:** Die Karte hat den Titel "Mission". Darunter steht die Crit Op groß und hervorgehoben, darunter Killzone und Map als getrennte Tags (Pillen mit Orange-Rand) statt eines langen Textes. Der Button ("Show crit op and map" im Spiel, "Change mission" vor dem Spielstart) hat die volle Breite. Dieselbe Darstellung gilt in der laufenden Partie und auf der Seite zum Starten eines neuen Spiels. | S |
| GW-12 | **Result-Karte überarbeitet:** Das Ergebnis zeigt oben ein Banner mit dem Gewinner (orange gefüllt, große Schrift) oder "Draw" (neutral). Darunter steht eine Tabelle mit Spalten Crit, Kill, Tac, Primary, VP wie die Score-Karte, der Gewinner ist hervorgehoben. Der Button "Save result and close game" hat die volle Breite und mindestens 56 dp Höhe. | S |

Status 2026-11-04: GW-1, GW-4, GW-5, GW-6 und GW-8 bis GW-12 sind umgesetzt. GW-2, GW-3 und GW-7 (Pfeile, Haken) wurden nach dem Test wieder zurückgenommen.

### 4.18 Rules-Tab mit Favoriten-Schalter und Regeln zusammenführen

Der Tab bleibt "Rules" (4.5) und zeigt nur die Regeln. Die zuerst geplante Umbenennung in "Reference" mit aufklappbaren Gruppen und Missionspaketen wurde nach dem Test verworfen (2026-11-04): Die Missionspakete (Crit Ops, Tac Ops, Kill Op, Maps) stehen im Generator. Behalten wird der Schalter ALL / Favoriten.

| ID | Anforderung | Prio |
| --- | --- | --- |
| RF-1 | ~~Tab "Reference"~~ **Entfällt:** Der Tab heißt wieder "Rules" (Icon `ic_rules`, TI-1, TI-6, TI-7 gelten unverändert). | - |
| RF-2 | ~~Aufklappbare Gruppen~~ **Entfällt:** Die Rules-Übersicht zeigt wieder die Regelseiten als Karten mit Anzahl der Abschnitte und die Universal Map Key. | - |
| RF-3 | ~~Missionspakete in der Referenz~~ **Entfällt:** Missionspakete werden im Generator nachgeschlagen. | - |
| RF-4 | **Favoriten-Schalter:** Ganz oben wählt ein Schalter ALL / Favorites. Im Favoriten-Modus erscheinen die markierten Einträge (Regelseiten, Teams, Crit Ops, Tac Ops) als antippbare Liste. Regelseiten werden mit dem Stern auf ihrer Karte markiert (Betrifft KB-4, AT-16). | M |
| RF-5 | **Favoriten aus More:** Der Favoriten-Bereich unter More entfällt. Bestehende Favoriten-Daten bleiben unverändert. Notizen bleiben unter More. | M |
| RF-6 | **Suche:** Die Suche arbeitet über alle Regelseiten (mindestens 3 Buchstaben). Treffer haben ebenfalls einen Stern. | S |
| RF-7 | **Favoriten für Unterregeln:** Auch die Abschnitte (Unterregeln) einer Regelseite tragen einen Stern oben rechts auf ihrer Karte. Der Favoriten-Schlüssel ist `section:<Seite>/<Abschnitt>`. Im Favoriten-Modus erscheint der Eintrag als "Rule: <Abschnitt> (<Seite>)"; ein Tipp öffnet die Seite, scrollt zum Abschnitt und hebt ihn kurz hervor. | M |
| RF-8 | **Seite "About" entfällt:** Die Seite "About" der Quelle (Formular und Werbung, kein Regeltext) wird nicht angezeigt und nicht durchsucht. Die App filtert sie beim Laden, der Import `import-rules.ps1` überspringt sie. Die Seite "Data sources" unter More bleibt. | M |
| RG-1 | **Regeln zusammenführen (pausiert, 2026-11-04):** Mehrfach auftretende identische Regeln werden auf eine zentrale Regel reduziert (z. B. "Fight (1 AP)"). Verweise (Keyword-Popup, Team-Detail) zeigen auf die zentrale Regel. Abweichende Team-Varianten bleiben getrennt und verweisen auf die Grundregel. Das soll im Importskript (UP-1) passieren, damit es bei jedem Update erhalten bleibt. Befund und Vorschlag siehe unten. | S |

Status 2026-11-04: RF-4 bis RF-8 sind umgesetzt (`ui/RulesScreen.kt`; Favoriten-Schlüssel `rule:<Seite>` und `section:<Seite>/<Abschnitt>`, Notizen unter More). RF-1 bis RF-3 wurden zurückgenommen. RG-1 ist pausiert.

**RG-1: Befund der Prüfung (2026-11-04)**

- Team-Daten (`killteams.json`): 1379 Regelzeilen bei den Operatives, 98 Texte mehrfach (57 in mehr als einem Team). Am häufigsten steht "This operative cannot perform this action while within control range of an enemy operative." 117-mal in 38 Teams, außerdem Aktionsköpfe wie Signal 1AP (10-mal) sowie Fähigkeiten wie "Icon bearer" (9-mal). Ploys, Equipment und Fraktionsregeln haben bei gleichem Namen immer unterschiedlichen Text (26 Namen), hier gibt es nichts zusammenzuführen.
- Regel-Daten (`rules.json`, 1177 Abschnitte): 73 Gruppen identischer Abschnitte mit 505 überzähligen Kopien. Die Grundaktionen Shoot, Charge, Dash, Fight, Reposition und Fall Back stehen auf 9 bis 15 Seiten (2 bis 4 geringfügig abweichende Varianten je Aktion).
- Importfehler: Der Abschnitt "Disable Ads" (E-Mail- und PIN-Formular der Quellseite) steht auf 15 Seiten und ist kein Regeltext.
- Doppelte IDs: Tomb World hat 434 Abschnitte, aber nur 94 verschiedene IDs (neun Abschnitte 28 bis 29-mal, "Awakened Warrior" 58-mal), Typhon 154 Abschnitte bei 75 IDs. Ob das ein Importfehler ist oder zum Seitenaufbau gehört (Karten je Szenario), ist nicht geklärt. Ein Regel-Link springt bei doppelten IDs immer zum ersten Treffer.

Vorschlag bei Wiederaufnahme: (1) "Disable Ads" im Importskript herausfiltern, (2) die sechs Grundaktionen nur auf der Core-Rules-Seite behalten und von den anderen Seiten dorthin verweisen, (3) die Standard-Bedingung und gängige Aktionsköpfe als zentrale Regel führen, (4) Tomb World und Typhon erst an der Quelle prüfen.

### 4.19 Rechtliche Seiten

Betrifft Abschnitt 2 (Rechtliches) und ZC-11.

| ID | Anforderung | Prio |
| --- | --- | --- |
| RC-1 | **Seiten:** Unter More gibt es die Einträge "Nutzungsbedingungen" und "Datenschutz". Die Texte liegen lokal gebündelt und sind offline lesbar. | M |
| RC-2 | **Zustimmung:** Beim ersten Start nach dem Zugangscode wird die Zustimmung eingeholt (bisher nur Hinweis in ZC-11). Die Zustimmung wird mit der Version der Texte gespeichert. Bei geänderten Texten wird erneut gefragt. | S |
| RC-3 | **Datenschutz-Inhalt:** Offline-Betrieb, keine Konten, keine Weitergabe von Daten, lokale Speicherung (Notizen, Rosters, Verlauf). Die Kamera wird nur erwähnt, wenn die Messfunktion aktiv ist (KM-20). | M |
| RC-4 | **Nutzungsbedingungen-Inhalt:** Privat, nicht kommerziell, Zugangscode nicht weitergeben, Inhalte gehören Games Workshop bzw. Wahapedia, kein Anspruch auf Verfügbarkeit. | M |
| RC-5 | **Prüfung:** Die Texte sind Entwürfe und werden vor einer Verbreitung über den Testkreis hinaus rechtlich geprüft. | S |

Status 2026-11-04: RC-1, RC-3, RC-4 und RC-5 (Entwurf) sind umgesetzt (More: "Terms of use", "Privacy"). RC-2 ist offen (bisher gilt die Eingabe des Codes als Zustimmung, ZC-11).

### 4.20 Inkrementelle Updates und PDF-Import

Bisher überschreiben `import-wahapedia.ps1` und `import-rules.ps1` alles (Abschnitt 6). Ziel: nur neue oder geänderte Objekte schreiben.

| ID | Anforderung | Prio |
| --- | --- | --- |
| UP-1 | **Upsert:** Jedes Objekt (Team, Operative, Regel, Ploy, Equipment) hat eine stabile ID und einen Inhalts-Hash. Pro Lauf gilt: nicht vorhanden = anlegen, Hash anders = aktualisieren, sonst unverändert lassen. | M |
| UP-2 | **Bereiche:** Die Skripte laufen auf einzelne Bereiche beschränkt (z. B. `-Teams`, `-Rules`, `-Team "Name"`, `-Section "Core Rules"`), damit aktuelle Regeln und Teams nicht erneut überschrieben werden. | M |
| UP-3 | **Bericht und Backup:** Jeder Lauf erzeugt einen Änderungsbericht (neu, geändert, unverändert, nicht mehr in der Quelle) und sichert vorher die JSON-Dateien. Gelöschte Objekte werden nur markiert, nicht automatisch entfernt. | S |
| UP-4 | **Datenstand:** `dataDate` (VR-2) wird nur bei tatsächlichen Änderungen gesetzt. | S |
| UP-5 | **PDF-Reader:** Ein Update-Skript liest die Team- und Regel-PDFs, extrahiert den Text und mappt ihn auf das Datenmodell des Wahapedia-Imports. Nicht vorhandene Objekte werden neu angelegt, bei Änderungen aktualisiert (gleiche Logik wie UP-1). Die Textextraktion nutzt ein externes Werkzeug (Vorschlag: `pdftotext -layout`), da PowerShell PDFs nicht zuverlässig liest. Treffer mit geringer Sicherheit werden im Bericht markiert und nicht automatisch übernommen. | S |

Status 2026-11-04: UP-1 bis UP-4 sind für die Teams umgesetzt (`tools/import-wahapedia.ps1 [-Team slug] [-Full]`: Vergleich pro Team, Bericht, Backup `.bak`, Datenstand nur bei Änderungen). `import-rules.ps1` ist noch nicht umgestellt, UP-5 (PDF-Reader) ist offen.

### 4.21 Button-System

Vereinheitlicht die vorhandenen Buttons zu festen Arten, Größen und Regeln (Konzept J in [konzept.md](konzept.md), Mock-up [mockups/buttons.html](mockups/buttons.html)). Betrifft DS-2, DS-6, DS-7, GW-8 bis GW-12.

| ID | Anforderung | Prio |
| --- | --- | --- |
| BT-1 | **Arten:** Es gibt sieben feste Arten: Primary (orange gefüllt, weiße Schrift), Secondary (orange Outline), Text (nur orange Schrift), Danger (rote Outline, im Bestätigungsdialog rot gefüllt), Choice (Chip, gewählt orange gefüllt), Segmented (gleich breite Gruppe aus 2 bis 3 Optionen) und Icon (nur Symbol). Zusätzliche Bausteine sind der Stepper und die schwebende Nav-Pille (Index, Cards, Top). | M |
| BT-2 | **Rollen:** Primary ist die Hauptaktion einer Karte oder Seite (z. B. Generate Mission, Next: <Phase>, Save result and close game). Secondary sind weitere Aktionen und Ein-/Ausblenden. Text ist für Cancel, Close, Back und Link-Aktionen. Danger ist für Aktionen, die Daten löschen oder verwerfen (Discard game, Delete, Clear all, Out). Pro Karte und Seite gibt es höchstens eine Primary. | M |
| BT-3 | **Größen:** S = 40 dp (Zeilen mit mehreren Aktionen, Stepper), M = 48 dp (Standard, auch Next: <Phase>), L = 56 dp (nur Weitergabe- und Abschlussaktionen: Secret selection, Primary op wählen, Save result and close game, "I am <Spieler>"). Die Touch-Fläche beträgt immer mindestens 48 dp. | M |
| BT-4 | **Form und Farbe:** Primary, Secondary, Text und Danger sind Pillen, Chips haben 8 dp Rundung, Icons sind rund. Alle Farben kommen aus `Palette` (neu: `Danger`; der gedrückte Zustand kommt vom Ripple des Systems). Deaktiviert heißt 38 % Deckkraft ohne Interaktion. Auf dem Desktop zeigt ein Fokusrahmen die Tastaturauswahl. | M |
| BT-5 | **Beschriftung:** Englisch, Satzanfang groß, Verb zuerst, kein Punkt am Ende. Schalter zeigen die Aktion des nächsten Zustands ("Show" / "Hide"). Abbrechen heißt immer "Cancel", Schließen immer "Close". Bestätigen nutzt ein konkretes Verb statt "OK" (außer in reinen Hinweisdialogen). Lange Texte brechen zentriert um. | S |
| BT-6 | **Anordnung:** Eine einzelne Aktion in einer Karte hat volle Breite. Gestapelt steht Primary oben, Secondary darunter, Danger zuletzt und abgesetzt. Zwei Aktionen nebeneinander sind gleich breit mit Primary rechts. In Dialogen steht Bestätigen rechts (bei Löschen als Danger) und Abbrechen links als Text. S-Buttons in Zeilen laufen in einer `FlowRow` und brechen um. | M |
| BT-7 | **Zentrale Umsetzung:** Alle Buttons entstehen über `ui/Buttons.kt` (`AppButton` mit `ButtonKind` und `ButtonSize`, `SegmentedChoice`, `ChipRow`, `Stepper`, `FavoriteButton`, `NavButton`). Direkte Aufrufe von `Button`, `OutlinedButton` und `TextButton` außerhalb dieser Datei entfallen. | M |
| BT-8 | **Migration:** In Etappen: (1) `Buttons.kt` und `Palette`, (2) Game, (3) Teams, Generator, More, Rules, (4) Dialoge, (5) Messfunktion (pausiert, zuletzt). Jede Etappe baut und besteht `:shared:jvmTest` und `:androidApp:assembleDebug`. Gespeicherte Daten sind nicht betroffen. | M |
| BT-9 | **Abnahme:** Auf Android und Desktop haben gleichartige Aktionen in allen Tabs dieselbe Art, Höhe und Beschriftung. Eine Suche nach `OutlinedButton(` und `TextButton(` außerhalb von `Buttons.kt` findet nichts mehr. | M |

Status 2026-11-04: BT-1 bis BT-9 sind umgesetzt (`ui/Buttons.kt`; alle Tabs, Dialoge und die Messfunktion nutzen `AppButton`). Ausnahmen: die Kamera-Toolbar der Messfunktion (eigene Farben auf dem Kamerabild, KM-20) und die Einträge der Index-Dialoge, die als Listenzeilen statt als Buttons gezeichnet sind. "Favorite" ist jetzt ein Stern-Icon, "Open rule" hat keinen Pfeil mehr.

### 4.22 Roster mit Waffenwahl, Equipment und Datenkarten im Spiel

Erweitert den Roster-Builder (4.6) und den Datenblatt-Tracker (4.7). Konzept K in [konzept.md](konzept.md). Betrifft RB-1, RB-4, DT-1, AT-8, GA-1, GA-2.

**Roster**

| ID | Anforderung | Prio |
| --- | --- | --- |
| RW-1 | **Datenmodell:** Ein Roster speichert pro Eintrag der Operative-Liste die gewählte Waffenoption (`Roster.options`, parallel zu `operatives`, Label der Option aus dem Optionstext). Zusätzlich speichert es eine Equipment-Liste. Alte Rosters (nur Datenkartennamen) laden unverändert weiter, ohne Waffenwahl und ohne Equipment. Die neuen Felder haben Standardwerte, damit gespeicherte Daten nicht verloren gehen. | M |
| RW-2 | **Equipment im Roster:** Der Roster-Editor hat den Abschnitt "Equipment (n/4)" mit "Faction equipment" (aus der Teamliste) und "Universal equipment" (`ApprovedOps.universalEquipment`) als Choice-Chips sowie dem Feld "Other equipment". Höchstens 4 verschiedene Einträge (`ApprovedOps.MAX_EQUIPMENT`), jeder nur einmal. | M |
| RW-3 | **Optionen aus den Optionstexten:** Die Waffenoptionen kommen ausschließlich aus den Optionstexten der Zusammensetzung (`compositionParas`), z. B. "1 KASRKIN SERGEANT operative with one of the following options:" mit "Bolt pistol; power weapon". Die Aufzählungen werden an `;` und " and " zerlegt und auf die Waffennamen der Datenkarte abgebildet (ohne Groß-/Kleinschreibung; "(standard)" und "(supercharge)" gehören zur selben Waffe). | M |
| RW-4 | **Waffenwahl im Editor (geändert 2026-11-04, nach Test):** Die Waffenwahl steht nicht mehr unter jedem Operative, sondern in einem eigenen Abschnitt "Weapons" **nach der Operative-Liste**. Der Nutzer wählt zuerst den Typ in der Operative-Liste (Zähler `-` / `+`). Danach listet der Abschnitt "Weapons" jeden gewählten Operative, der Optionen hat, als eigene Zeile (Typ mit laufender Nummer, z. B. "Nemesis Claw Gunner 2") und daneben eine Auswahl der gelisteten Waffenkombinationen, genau in der Schreibweise des Optionstextes (z. B. "Bolt pistol; flamer; fists"). Die Reihenfolge entspricht der Operative-Liste. Die gewählte Kombination setzt die Waffen. Hat ein Typ keinen Optionstext oder lässt sich der Text nicht abbilden, erscheint er nicht im Abschnitt, und es gibt keine freie Auswahl einzelner Waffenprofile. Ist kein Operative mit Optionen gewählt, entfällt der Abschnitt. | M |
| RW-5 | **Prüfung:** Zusätzlich zu den bisherigen Meldungen (Anzahl je Zusammensetzungszeile, Unknown operative) zeigt der Editor die Warnung "Weapon option not selected" (nur dort, wo es Optionen gibt) und "More than 4 equipment". Warnungen sperren das Speichern nicht. | S |
| RW-6 | **Parser-Bericht:** Ein Test oder Hilfsskript listet alle Teams, bei denen der Optionstext nicht (vollständig) abgebildet werden kann. Für diese Teams gilt die normale Datenkarte (RW-12). | S |
| RW-7 | **Export:** Das Teilen als Text (RB-4) enthält Waffenwahl und Equipment. | K |

**Im Spiel**

| ID | Anforderung | Prio |
| --- | --- | --- |
| RW-8 | **Vorbelegung:** Beim Spielstart bekommt jeder Operative (`OperativeTrack`) den Namen seiner Datenkarte und die Waffen aus dem Roster. `PlayerState.equipment` wird aus `Roster.equipment` vorbelegt. Ohne Roster wird die Datenkarte über den Namen des Operatives zugeordnet, soweit er bekannt ist. | M |
| RW-9 | **Aufklappbare Zeile:** In der Spielerkarte (Liste "Operatives") hat jede Operative-Zeile den Button "Show datacard" / "Hide datacard" zum Auf- und Zuklappen (statt eines Pfeils, BT-5). Zugeklappt zeigt sie Name, Wounds, Status und die Aktionsbuttons -1 W, +1 W, Ready/Expended und Out/Revive. **Die Buttons Engage/Conceal entfallen** (siehe DT-1). Die Aufklapp-Zustände gelten nur für die laufende Ansicht und ändern keine Spieldaten. | M |
| RW-10 | **Datenkarte wie im Teams-Tab:** Aufgeklappt erscheint dieselbe Datenkarte wie unter Teams. Die vorhandene Komponente `OperativeDatacard` wird unverändert wiederverwendet (Wertekopf, Waffentabelle, Fähigkeiten und Aktionen, antippbare Schlüsselbegriffe), es gibt keine eigene Variante. Mit Waffenwahl bekommt sie nur die gewählten Waffen. Der Button "Show all weapons" unter der Karte zeigt die vollständige Datenkarte. | M |
| RW-11 | ~~Unit rules als zweite Aufklapp-Ebene~~ **Entfällt (2026-11-04):** Die Datenkarte aus dem Teams-Tab zeigt ihre Regeln direkt (RW-10), eine zusätzliche Ebene gibt es nicht. | - |
| RW-12 | **Ohne Waffenwahl:** Ohne Roster, ohne Optionstext oder bei nicht abbildbarem Text zeigt die aufgeklappte Karte die normale Datenkarte mit allen Waffen, und "Show all weapons" entfällt. | M |
| RW-13 | **Equipment im Spiel (geändert 2026-11-04, nach Test):** Das Equipment erscheint im Spiel nur, wenn es aus dem Roster kommt, und wird wie die Operatives ein- und ausgeklappt. In der Spielerkarte steht der Button "Equipment (n)" bzw. "Hide equipment" (standardmäßig zugeklappt, wie bei "Operatives"). Aufgeklappt erscheinen die Ausrüstungen des Rosters als dieselben Karten wie im Teams-Tab (Name, Kosten, Text; Komponente `NamedTextCard`). Fraktions-Equipment kommt aus den Teamdaten. Für allgemeine Ausrüstung (nur Namen in `ApprovedOps.universalEquipment`) zeigt die Karte den Namen mit Link zur Regelseite "Universal equipment", sofern vorhanden. Wurde das Spiel ohne Roster oder mit einem Roster ohne Equipment gestartet, gibt es den Button und den Abschnitt nicht. Equipment, das in der geheimen Auswahl ("Secret selection") von Hand gewählt oder geändert wird, erscheint nicht als Karten, es zählt nur in der Anzeige "equipment n/4" dort (AT-8). In Schritt 2 ("Select Operatives") steht dieselbe Ein-/Ausklappen-Anzeige je Spieler anstelle der früheren Karte "Reveal equipment". | M |
| RW-17 | **Optionstexte mit "equipped with":** Der Parser (RW-3) liest auch Zusammensetzungszeilen mit "equipped with" statt "with", sowohl als Pfeilzeile ("1 NEMESIS CLAW VISIONARY equipped with one of the following options:") als auch als Listenzeile ("GUNNER equipped with one of the following options:", "HEAVY GUNNER equipped with ...", "WARRIOR equipped with ..."). Aufzählungen wie "Bolt pistol; flamer; fists" werden wie bisher auf die Waffen der Datenkarte abgebildet. Der Bericht (RW-6) listet zusätzlich alle Teams, in deren Zusammensetzung Zeilen mit "one of the following" stehen, für die kein Optionstext erkannt wurde, damit solche Teams nicht unbemerkt ohne Optionen bleiben. | M |
| RW-18 | **Herkunft des Equipments:** Der Spielzustand merkt sich, welche Ausrüstungen aus dem Roster stammen (`PlayerState.rosterEquipment`, wird beim Spielstart aus `Roster.equipment` gefüllt, Standard leer, alte Spielstände laden unverändert). Nur diese Liste speist RW-13. `PlayerState.equipment` bleibt die Liste der geheimen Auswahl. | M |
| RW-14 | **Sichtbarkeit:** Roster, Waffen, Equipment und Datenkarten sind für beide Spieler jederzeit sichtbar. Nur die Tac Op (und die Primary Op bis zum Aufdecken) bleiben verdeckt. | M |

**Qualität**

| ID | Anforderung | Prio |
| --- | --- | --- |
| RW-15 | **Tests (`commonTest`/`jvmTest`):** Laden alter Rosters ohne neue Felder, Parser mit mindestens Kasrkin (Optionsliste), einem Team mit Gunner-Liste und einem Team ohne Optionen, Zuordnung "(standard)/(supercharge)", Prüfung von mehr als 4 Equipment und die Vorbelegung von `OperativeTrack` und `PlayerState.equipment`. | M |
| RW-16 | **Abnahme:** Auf Android und Desktop lässt sich ein Kasrkin-Roster mit gewählter Sergeant-Option und Equipment speichern. Im Spiel klappt die Karte des Sergeants auf und zeigt dieselbe Datenkarte wie im Teams-Tab, nur mit den gewählten Waffen. "Show all weapons" lässt sich aufklappen, das Equipment steht als Karten wie im Teams-Tab in der Spielerkarte und es gibt keine Engage/Conceal-Buttons mehr. Ein Operative ohne Optionstext zeigt alle Waffen. Nach der Änderung vom 2026-11-04 gilt zusätzlich: Im Roster eines Nemesis-Claw-Teams steht der Abschnitt "Weapons" nach der Operative-Liste und bietet für Gunner und Heavy Gunner die gelisteten Kombinationen an. Im Spiel erscheint das Equipment nur bei einem Roster mit Equipment und lässt sich mit "Equipment (n)" ein- und ausklappen. | M |

**Umsetzung in Etappen:** (1) Datenmodell und Migration (RW-1, RW-8, Tests), (2) Parser und Bericht (RW-3, RW-6), (3) Roster-Editor mit Waffenwahl und Equipment (RW-2, RW-4, RW-5), (4) aufklappbare Karte im Spiel (RW-9 bis RW-14). Jede Etappe baut und besteht `:shared:jvmTest` und `:androidApp:assembleDebug`.

Status 2026-11-04: RW-1 bis RW-6 und RW-8 bis RW-15 sind umgesetzt (`data/WeaponOptions.kt`, `logic/History.kt`, `ui/TeamsScreen.kt` Roster-Editor, `ui/GameScreen.kt`, Tests in `WeaponOptionsTest`). Der Parser bildet bei 29 von 48 Teams Optionen ab (RW-6, Bericht im Testlauf). Nicht vollständig abgebildet werden u. a. Teams mit Gunner-Einträgen in anderer Schreibweise (z. B. Hunter Clade Warriors, Death Korps, Legionary, Wyrmblade); dort gilt die normale Datenkarte. RW-7 (Export) ist offen, RW-16 (Abnahme auf dem Gerät) ist noch zu prüfen.

Änderungen nach dem Test am 2026-11-04: RW-4, RW-13, RW-17 und RW-18 sind umgesetzt (Abschnitt "Weapons" nach der Operative-Liste im Roster-Editor, Equipment im Spiel nur aus dem Roster mit "Equipment (n)" / "Hide equipment", Parser liest "equipped with" und weitere Kopfzeilen, `PlayerState.rosterEquipment`). Der Parser bildet jetzt bei 31 von 48 Teams Optionen ab, und der Testlauf meldet keine nicht erkannten Optionszeilen mehr. Die Teams mit nur teilweise abbildbaren Texten (z. B. Hunter Clade Warriors, Death Korps, Angels of Death) zeigen die normale Datenkarte.

### 4.23 Roster-Editor mit Einheitenkarten

Ersetzt den Editor aus 4.22 (RW-2 Equipment-Abschnitt mit Chips, RW-4 Abschnitt "Weapons") und die Operative-Liste mit Zählern aus 4.6. Mock-up: [mockups/roster.html](mockups/roster.html). Entscheidungen 2026-11-04: Man darf höchstens 4 Equipment mitnehmen, jedes gehört genau einem Operative (kein Team-Equipment, der Regeltext beschreibt selbst, was es kann). Die Auswahl geschieht direkt an der Einheit, es gibt keinen getrennten Equipment-Pool.

| ID | Anforderung | Prio |
| --- | --- | --- |
| RE-1 | **Drei Ansichten:** Der Roster-Editor hat die Ansichten "Roster" (Übersicht), "Add operatives" und "Einheit bearbeiten". Zurück führt aus den beiden Unteransichten zur Übersicht, aus der Übersicht zur Roster-Liste. | M |
| RE-2 | **Budget:** In allen drei Ansichten stehen oben zwei Felder: "Operatives n/erwartet" (erwartete Anzahl aus der Teamzusammensetzung, ohne Angabe nur n) und "Equipment n/4" mit vier Balken (`ApprovedOps.MAX_EQUIPMENT`). | M |
| RE-3 | **Übersicht:** Oben die Karte des Teams mit Name, Budget, Zusammensetzungstext und Meldungen (Größe, Waffen nicht gewählt, Equipment nicht zugewiesen, mehr als 4 Equipment). Darunter pro Eintrag der Operative-Liste eine antippbare Karte mit Name (nummeriert bei gleichen Datenkarten), gewählter Waffenkombination und den Equipment-Chips der Einheit. Hat die Datenkarte Waffenoptionen und keine ist gewählt, steht "Choose weapons" in Gelb. Ohne Optionen steht "Standard weapons". Ohne Equipment steht "No equipment". Darunter "+ Add operative" und "Delete roster" (mit Rückfrage). | M |
| RE-4 | **Add operatives:** Pro Datenkarte des Teams eine Karte mit Zähler (`-` / `+`) und der Zahl der Waffenoptionen. `+` fügt einen Eintrag hinzu, `-` entfernt den letzten Eintrag dieser Datenkarte mit Waffenwahl und Equipment. "Done" kehrt zur Übersicht zurück. | M |
| RE-5 | **Einheit bearbeiten:** Tippen auf eine Karte öffnet die Einheit. Abschnitt "Weapons": eine Auswahl (rund) der gelisteten Waffenkombinationen genau in der Schreibweise des Optionstextes, bei Einheiten ohne Optionen der Hinweis "All weapons of the datacard". Abschnitt "Equipment": Tabs "Faction" und "Universal", darin jedes Equipment als wählbare Zeile (eckiger Haken) mit Name und dem ersten Regeltext-Absatz (höchstens drei Zeilen). Unten "Done" und "Remove operative". | M |
| RE-6 | **Regeln für Equipment:** Höchstens 4 verschiedene Einträge pro Roster, jeder nur einmal, jeder bei genau einer Einheit. Antippen eines freien Equipments gibt es dieser Einheit. Antippen eines Equipments dieser Einheit nimmt es aus dem Roster. Antippen eines Equipments einer anderen Einheit verschiebt es hierher; die Zeile trägt den Hinweis "on <Einheit>". Sind 4 erreicht, sind noch nicht gewählte Zeilen gesperrt ("limit reached"). | M |
| RE-7 | **Eigenes Equipment:** Im Tab "Universal" gibt es das Feld "Other equipment" mit "Add"; der Eintrag wird dieser Einheit zugewiesen und erscheint danach als Zeile im Tab. Es gilt dasselbe Limit. | S |
| RE-8 | **Einheit entfernen:** "Remove operative" entfernt den Eintrag samt Waffenwahl und seinem Equipment (das Equipment verlässt das Roster, `equipment.size` sinkt). Die Zuweisungen der folgenden Einheiten bleiben richtig (Indizes rücken nach). | M |
| RE-9 | **Alte Rosters:** Rosters mit Equipment ohne Zuweisung (aus älteren Versionen) laden unverändert. Das Equipment zählt im Budget, die Übersicht warnt "Equipment not given to an operative: ...", und in der Einheit trägt die Zeile den Hinweis "not assigned"; Antippen weist es der Einheit zu. | M |
| RE-10 | **Im Spiel:** Zugewiesenes Equipment steht bei der Einheit im Spiel als aufklappbare Karte. Nicht zugewiesenes Roster-Equipment erscheint unter "Other equipment" in der Spielerkarte. In der Spielerkarte stehen die Umschalter "Operatives" (an erster Stelle), "Strategy ploys", "Firefight ploys", "Faction rules" und bei Bedarf "Other equipment". | M |
| RE-11 | **Tests (`jvmTest`):** Equipment geht an die Einheit und tritt dem Roster bei, Verschieben zwischen Einheiten, Limit von 4, Entfernen einer Einheit mit Nachrücken der Zuweisungen und Waffenoptionen, nicht zugewiesenes Equipment aus alten Rosters (`EquipmentOwnerTest`). | M |
| RE-12 | **Abnahme:** Auf Android und Desktop lässt sich für Kasrkin ein Roster anlegen, Operatives hinzufügen, bei Sergeant und Gunner Waffen wählen und 4 Equipment auf Einheiten verteilen. Die Übersicht zeigt Waffen und Equipment je Einheit, das fünfte Equipment ist gesperrt, und das Spiel zeigt dasselbe bei den Einheiten. | M |

Status 2026-11-04: RE-1 bis RE-11 sind umgesetzt (`ui/TeamsScreen.kt` mit `RosterOverview`, `RosterAddPage`, `RosterUnitPage`, `logic/History.kt` mit `Roster.withEquipmentOn`, `withRemovedAt`, `unassignedEquipment`, `ui/GameScreen.kt`). `:shared:jvmTest` besteht. RE-12 (Abnahme auf dem Gerät) ist noch zu prüfen. Die Spielzeilen aus RW-13 (Equipment als Karten) bleiben gültig; der Text "Equipment (n)" der Spielerkarte heißt jetzt "Other equipment" und enthält nur nicht zugewiesenes Equipment.

## 5. Nicht-funktionale Anforderungen

| ID | Anforderung |
| --- | --- |
| NF-1 | Offline-Betrieb: Alle Daten und Bilder liegen lokal, keine Netzwerkanfragen zur Laufzeit. |
| NF-2 | Die Mission-Generator-Logik (`commonMain`) ist plattformunabhängig und mit Unit-Tests abgedeckt. |
| NF-3 | Persistenz der Nutzerdaten (Auswahl, Rosters, Spielstände, Historie) lokal, plattformübergreifend. |
| NF-4 | Responsive UI für Smartphone, Tablet und Desktop; Hoch- und Querformat. |
| NF-5 | Bedienbarkeit am Spieltisch: große Touch-Flächen, Dunkelmodus, Bildschirm bleibt wach. |
| NF-6 | Daten-Import als nachvollziehbares, wiederholbares Skript. Datenstand (Datum, Version laut Wahapedia, z. B. "August 2026") wird in der App angezeigt, zusammen mit der App-Version unter More (VR-2). |
| NF-7 | Datenschutz: Keine Konten, keine Telemetrie, keine Werbung. |

## 6. Import-Pipeline

1. Einmaliges Skript (außerhalb der App) liest Wahapedia-Seiten (Approved Ops, Kill Teams, Rules, Killzones).
2. Ausgabe als strukturierte Daten (z. B. JSON) in `shared/src/commonMain/composeResources/files`.
3. Bilder werden lokal gespeichert und nach Typ benannt.
4. Manuelle Stichproben-Prüfung gegen die Webseite.
5. Regelupdates (Wahapedia aktualisiert regelmäßig) erfordern einen erneuten Import und ein neues App-Build.
6. Ab 1.4.0.a laufen Updates inkrementell und bereichsweise, zusätzlich per PDF-Reader (siehe 4.20).

## 7. Offene Punkte

- Reihenfolge der Umsetzung (Vorschlag: Generator, Tac-Op-Auswahl, Karten, Spielablauf, VP-Tracker, Browser, Roster, Tracker, Turnier).
- Genaue Kill-Grade-Tabelle liegt nur als Bild vor und muss für die Berechnung manuell in Daten überführt werden.
- Prüfung, ob die Wahapedia-Nutzungsbedingungen den einmaligen Import ausdrücklich erlauben.
- Umfang des Importes der Kill Teams (alle Seasons oder nur Auswahl).
- Offizielle Farbwerte und Schriften für das Orange-Design (DS-5), z. B. aus eigenen Screenshots der offiziellen App.
- Darf Artwork von Games Workshop als Hintergrund verwendet werden, solange die App nur im Testkreis bleibt (DS-3)?
- Welche PDFs (Quelle, Format) liest der PDF-Reader (UP-5) und liegen sie in einem festen Layout vor?

Entschieden für 1.4.0.a: Alpha und Beta sind nicht parallel installierbar (VR-4). Favoriten gelten für Regelseiten, Teams, Crit Ops und Tac Ops (RF-4). Die Referenz mit Missionspaketen wurde verworfen, RG-1 ist pausiert.

Reihenfolge der Umsetzung 1.4.0.a: Kameramessung ausblenden (KM-20), Versionsanzeige (VR-2), Rechtliches (RC), Mock-ups (DS-4), Orange-Theme (DS), Rules-Tab mit Favoriten-Schalter (RF), Game Wizard (GW), Button-System (BT), Roster mit Waffenwahl und Datenkarten im Spiel (RW), inkrementelle Updates und PDF-Reader (UP), Beta-Release mit Schlüsselrotation (ZC-12). Regeln zusammenführen (RG-1) ist pausiert.

### 7.1 Offene Punkte aus dem Alpha-Test (Bedienung der App)

Priorität: **M** = Muss, **S** = Soll, **K** = Kann.

Status 2026-11-02: AT-1 bis AT-16 sind umgesetzt (AT-13 wurde durch die Zugangscodes aus Abschnitt 4.12 ersetzt).

| ID | Bereich | Anforderung | Prio |
| --- | --- | --- | --- |
| AT-1 | Generator / Game | "Select Tac Op" aus dem Generator entfernen und stattdessen in den Spielablauf (Game, Schritt 2 "Select Operatives") integrieren. Betrifft MG-5, MG-6, GA-1. | M |
| AT-2 | Game / Score | Formatierung des Score-Bereichs überarbeiten: Zeilen je Spieler mit Crit, Kill, Tac, Primary und Summe (VP) sauber ausgerichtet, ohne Zeilenumbrüche mitten in den Werten (siehe Screenshot, Score-Karte). | M |
| AT-3 | Game / Initiative | Formatierung "Initiative and Drop Zones" überarbeiten: Buttons "Player 1/2 has initiative" einheitlich in Größe und Textumbruch, Ergebnistext klar abgesetzt (siehe Screenshot). | M |
| AT-4 | Teams | Team-Auswahl (Kill-Team-Liste, Dropdowns im Game) alphabetisch sortieren. | M |
| AT-5 | Game | Bei jedem Phasenwechsel (Set up, Select Operatives, Set up Operatives, Play, End) und Turning-Point-Wechsel scrollt die Ansicht an den Seitenanfang. | M |
| AT-6 | Game / Team | Bei der Team-Wahl im Game wird das Team gleich konfiguriert: Teamgröße (Operative-Anzahl laut Team-Regeln) und passende Tac Ops, nach Archetyp sortiert. Betrifft AS-5, RB-1, RB-3. | S |
| AT-7 | Game / Tac Ops | Grafische Darstellung von "Select Tac Ops" wie im Generator (Karten mit Archetyp-Filter, Select/Remove). Betrifft MG-5, MG-6. | M |
| AT-8 | Game / Equipment | Team-Ausrüstung und allgemeine (universelle) Ausrüstung in "Select Operatives" integrieren, maximal 4 Optionen pro Spieler (siehe Spielablauf Schritt 2). | M |
| AT-9 | Game / Phase | Unter der Phasen-Überschrift die Phasen des Spiels kurz als Liste aufführen (Kurzbeschreibung je Schritt 1-5). Betrifft AS-1. | S |
| AT-10 | Tracker | Anzahl der "Incapacitated"-Operatives auf die Operative-Anzahl des gewählten Teams begrenzen. Betrifft DT-1, VP-4. | M |
| AT-11 | Teams | Suchleiste im Teams-Bereich mit Wortsuche (Volltext über Teamname, Operatives, Regeln, Equipment). Betrifft KB-1, KB-3. | S |
| AT-12 | Teams / Darstellung | Bei "Following Equipment" (Team-Detail) Einträge um eine Ebene weiter einrücken. | S |
| AT-13 | Zugang | Passwortschutz für Alpha-Tests (Vorstufe, ersetzt durch die Zugangscodes aus Abschnitt 4.12; das Skript `set-access-password.ps1` entfällt): Die App fragt beim Start ein Passwort ab. Es gibt zwei Passwörter: (1) ein unbegrenztes Passwort ohne Ablauf; (2) ein Einmal-Passwort, das nach der ersten erfolgreichen Eingabe 7 Tage gültig ist und danach nicht mehr funktioniert (auch nicht erneut nach Neuinstallation oder Löschen der App-Daten, soweit offline möglich). Der Ablaufzeitpunkt wird bei der ersten Eingabe lokal gespeichert und gegen Zurückstellen der Systemuhr geprüft. Offline prüfbar (Passwörter nur gehasht im Build, Hilfsskript `tools/set-access-password.ps1`). | S |
| AT-14 | Design | Design bewusst von Wahapedia abgrenzen (eigene Farben, Typografie, Komponenten, Icons), da nur die Inhalte, nicht das Layout übernommen werden. Betrifft Rechtliches (Abschnitt 2). | S |
| AT-15 | Teams / Suche | Ein Treffer der Teams-Suche ist antippbar und führt direkt zur Fundstelle: Sucht man z. B. "Midnight Claw" und tippt auf den Treffer, öffnet die App das zugehörige Team (Operative, Regel, Ploy oder Equipment), scrollt zum gefundenen Eintrag und hebt ihn kurz hervor. Der Treffer zeigt dafür Team und Art des Eintrags (z. B. "Teamname, Equipment"). Zurück führt zur Trefferliste mit unverändertem Suchbegriff. Betrifft KB-1, KB-3, AT-11. | M |
| AT-16 | More / Favoriten | Unter More, Favorites and notes, sind die Einträge (Favoriten und Notizen) antippbar und führen zum jeweiligen Bereich: ein Favorit "Kill team" öffnet das Team im Teams-Tab, "Crit op" und "Tac op" öffnen den Generator an der zugehörigen Karte (scrollt dorthin und hebt sie kurz hervor). Notizen führen zu dem Eintrag, zu dem sie gehören. Die Einträge sind als antippbar erkennbar (Farbe und Pfeil). Betrifft KB-4, TI-7. | M |

## 8. Abgrenzung

- Kein Online-Multiplayer, keine Benutzerkonten, kein Cloud-Sync.
- Kein Ersatz für die offiziellen Regelbücher oder die Warhammer Community App.
- Keine automatische Regelprüfung des Spielgeschehens über die hier genannten Funktionen hinaus.
