# Anforderungen: Kill Team Companion App

Stand: 2026-10-27 | Status: Entwurf

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
| MG-6 | Tac Op mit "Select" zur Liste "Selected Tac Ops" hinzufügen, mit "Remove" entfernen. Der Generator begrenzt auf maximal eine Tac Op (wie Wahapedia), die Regel "Select Tac Ops" entfällt, sodass jede Tac Op frei wählbar ist. | M |
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

## 8. Abgrenzung

- Kein Online-Multiplayer, keine Benutzerkonten, kein Cloud-Sync.
- Kein Ersatz für die offiziellen Regelbücher oder die Warhammer Community App.
- Keine automatische Regelprüfung des Spielgeschehens über die hier genannten Funktionen hinaus.
