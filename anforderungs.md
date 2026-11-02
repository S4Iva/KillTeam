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
| ZC-1 | **Format:** Der Code hat die Form `XXXX-XXXX-XXXX` (12 Zeichen, Base32 ohne verwechselbare Zeichen wie 0/O und 1/I/L). Bei der Eingabe sind Groß-/Kleinschreibung und Bindestriche egal. | M |
| ZC-2 | **Inhalt:** Ein Code enthält den Ablaufzeitpunkt (in vollen Stunden, UTC; Wert 0 = unbegrenzt), eine Zufallszahl, damit jeder Code einmalig ist, und eine Signatur (HMAC-SHA256 mit geheimem Schlüssel, gekürzt). Die App liest daraus ab, ob der Code unbegrenzt ist oder bis wann er gilt. | M |
| ZC-3 | **Frist ab Generierung:** Der Ablauf ist fest im Code eingetragen und gilt ab dem Zeitpunkt der Erzeugung, nicht ab der ersten Eingabe. Ein Code lässt sich nach Ablauf nicht mehr verwenden, auch nicht nach Neuinstallation oder Löschen der App-Daten. | M |
| ZC-4 | **Generator (extern):** Das Skript `tools/generate-access-code.ps1` erzeugt einen Code und gibt Code und Ablaufzeitpunkt aus. Die Dauer ab Generierung wird mit `-d` (Tage) und `-h` (Stunden) angegeben, beides ist kombinierbar (z. B. `-d 7 -h 12`). Alternativ legt `-Until yyyy-MM-dd[ HH:mm]` ein festes Ablaufdatum in UTC fest (nur Datum = bis Ende dieses Tages, auf volle Stunden aufgerundet). Die App rechnet den Zeitpunkt auf die Zeitzone des Geräts um (siehe ZC-9). `-Unlimited` erzeugt einen unbegrenzten Code. Genau eine dieser drei Angaben ist nötig. | M |
| ZC-5 | **Schlüssel:** Der geheime Schlüssel steht einmalig in `AccessConfig` (`logic/AccessGate.kt`); Generator und App nutzen denselben. Ein Schlüsselwechsel macht alle bisherigen Codes ungültig. Ohne Schlüssel ist die Abfrage abgeschaltet. | M |
| ZC-6 | **Prüfung beim Start:** Die App fragt beim Start nach einem Code, solange kein gültiger gespeichert ist. Ein gültiger Code wird lokal gespeichert, bis er abläuft. Ungültige oder manipulierte Codes werden mit "Wrong code" abgelehnt, abgelaufene mit "Code expired". | M |
| ZC-7 | **Ablauf im Betrieb:** Läuft der Code ab (auch bei geöffneter App, Prüfung mindestens minütlich), sperrt die App und fragt nach einem neuen Code. Ein neuer Code ersetzt den alten. Die Spielstände bleiben erhalten. | M |
| ZC-8 | **Uhr-Schutz:** Wird die Systemuhr hinter den zuletzt gesehenen Zeitpunkt zurückgestellt (Toleranz 5 Minuten), sperrt die App, bis die Uhr wieder stimmt. | S |
| ZC-9 | **Anzeige:** Nach der Eingabe zeigt ein Dialog einmalig "Unlimited" oder den Ablaufzeitpunkt (aus UTC in die Zeitzone des Geräts umgerechnet) und die Restzeit. Unter More steht dauerhaft der Status: unbegrenzt oder Restzeit und Ablaufzeitpunkt. | M |
| ZC-10 | **Tests:** Unit-Tests für Erzeugen und Prüfen (Round-Trip), Tippfehler und manipulierte Zeichen, unbegrenzt, abgelaufen, Uhr zurückgestellt und ein fester Testvektor, damit PowerShell-Generator und App-Prüfung übereinstimmen. | M |

Grenzen: Wer die APK zerlegt, findet den Schlüssel und kann eigene Codes erzeugen (kein Kopierschutz, wie bisher). Ein Code ist nicht an ein Gerät gebunden und lässt sich weitergeben. Die Frist startet bei der Erzeugung, ein spät eingegebener Code hat entsprechend weniger Restzeit.

Entscheidung: Die zwei festen Passwörter (`tools/set-access-password.ps1`, `HASH`/`HASH_TEMP`) sind nach der Umstellung entfallen.

Status 2026-11-02: ZC-1 bis ZC-10 sind umgesetzt (`tools/generate-access-code.ps1`, `logic/AccessGate.kt`).

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

Status 2026-11-02: AT-1 bis AT-14 sind umgesetzt (AT-13 wurde durch die Zugangscodes aus Abschnitt 4.12 ersetzt).

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

## 8. Abgrenzung

- Kein Online-Multiplayer, keine Benutzerkonten, kein Cloud-Sync.
- Kein Ersatz für die offiziellen Regelbücher oder die Warhammer Community App.
- Keine automatische Regelprüfung des Spielgeschehens über die hier genannten Funktionen hinaus.
