# Anforderungen: Kill Team Companion App

Stand: 2026-11-02 | Status: Entwurf

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
| MG-6 | Tac Ops werden nicht im Generator, sondern im Spiel (Schritt 2 "Select Operatives") geheim ausgewählt, mit "Select" / "Remove" an den Tac-Op-Karten; wählbar sind die Archetypen des Kill Teams. Der Generator zeigt die Tac Ops nur zum Nachlesen. | M |
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
| GA-1 | Verdeckte Eingabe von Tac Op, Primary Op und Equipment je Spieler auf einem Gerät mit Sichtschutz-Bildschirm zwischen den Spielern. | M |
| GA-2 | Gleichzeitiges Aufdecken der Auswahl. | M |
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
| DT-1 | Pro Operative: aktuelle Wounds, Order (Engage/Conceal), Aktivierungsstatus (ready/expended), Incapacitated. | M |
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

Nicht Teil dieser Anforderung (Basisversion): automatische Erkennung von Operatives oder Terrain per Bilderkennung, Sichtlinien- und Deckungsprüfung, Aufzeichnung von Videos, Cloud-Auswertung.

Status 2026-11-03: KM-1 bis KM-11, KM-13, KM-15 bis KM-18 sind umgesetzt (`logic/Measure.kt`, `ui/MeasureScreen.kt`, `ui/CameraSource*.kt`, Tests in `MeasureTest`). Offen: KM-12 und KM-14 (Prio K), iOS-Kamera (der Eintrag ist dort ausgeblendet, braucht macOS zum Bauen und Prüfen), Desktop-Webcam (dort nur Foto-Import), KM-19 (Abnahme auf dem Gerät).

## 5. Nicht-funktionale Anforderungen

| ID | Anforderung |
| --- | --- |
| NF-1 | Offline-Betrieb: Alle Daten und Bilder liegen lokal, keine Netzwerkanfragen zur Laufzeit. |
| NF-2 | Die Mission-Generator-Logik (`commonMain`) ist plattformunabhängig und mit Unit-Tests abgedeckt. |
| NF-3 | Persistenz der Nutzerdaten (Auswahl, Rosters, Spielstände, Historie) lokal, plattformübergreifend. |
| NF-4 | Responsive UI für Smartphone, Tablet und Desktop; Hoch- und Querformat. |
| NF-5 | Bedienbarkeit am Spieltisch: große Touch-Flächen, Dunkelmodus, Bildschirm bleibt wach. |
| NF-6 | Daten-Import als nachvollziehbares, wiederholbares Skript. Datenstand (Datum, Version laut Wahapedia, z. B. "August 2026") wird in der App angezeigt. |
| NF-7 | Datenschutz: Keine Konten, keine Telemetrie, keine Werbung. |

## 6. Import-Pipeline

1. Einmaliges Skript (außerhalb der App) liest Wahapedia-Seiten (Approved Ops, Kill Teams, Rules, Killzones).
2. Ausgabe als strukturierte Daten (z. B. JSON) in `shared/src/commonMain/composeResources/files`.
3. Bilder werden lokal gespeichert und nach Typ benannt.
4. Manuelle Stichproben-Prüfung gegen die Webseite.
5. Regelupdates (Wahapedia aktualisiert regelmäßig) erfordern einen erneuten Import und ein neues App-Build.

## 7. Offene Punkte

- Reihenfolge der Umsetzung (Vorschlag: Generator, Tac-Op-Auswahl, Karten, Spielablauf, VP-Tracker, Browser, Roster, Tracker, Turnier).
- Genaue Kill-Grade-Tabelle liegt nur als Bild vor und muss für die Berechnung manuell in Daten überführt werden.
- Prüfung, ob die Wahapedia-Nutzungsbedingungen den einmaligen Import ausdrücklich erlauben.
- Umfang des Importes der Kill Teams (alle Seasons oder nur Auswahl).

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
