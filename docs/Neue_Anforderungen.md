# Neue Anforderungen – MediReminder / MediGuard

Stand: 16.08.2026  
Status: Spezifikation (Entscheidungen getroffen, noch nicht umgesetzt)

Rahmenbedingung: **unten maximal 5 Tabs**.  
Tab 4 heißt verbindlich **Profil** (nicht „Akte“ / „Gesundheit“).

---

## 0. Getroffene Entscheidungen

| Thema | Entscheidung |
|--------|----------------|
| Historie-Zeitraum | **Default = aktueller Monat**. Nutzer kann zu **vergangenen Monaten** zurücknavigieren und dort bearbeiten/filtern. |
| Bruchdosen | Als **`Double`** modellieren; Eingabe per **Freitext** (Kommazahlen, z. B. `0.5`, `1.25`). Keine festen Stufen-Limiter. |
| PDFs | **Nur lokal** (App-interner Speicher). Kein Cloud-Backup in diesem Scope. |
| Notfallkarte | Nur über Tab **Profil**, kein Extra-Icon auf Start. |
| Tab-Label | **Profil** |

---

## 1. Produktanforderungen

### 1.1 Bearbeitbare & filterbare Historie

**Problem:** Einnahme wird oft genommen, der Klick vergessen. Historie ist heute praktisch „schreib einmal“.

**Soll:**
- Einträge nachträglich bearbeitbar (Status, tatsächliche Uhrzeit, optionale Notiz).
- Kennzeichnung **„nachträglich geändert“** (kein stilles Überschreiben).
- Historie **filterbar**, mindestens nach:
  - Monat / Zeitraum (Default: laufender Monat)
  - Medikament
  - Status (eingenommen / ausstehend / übersprungen / nachträglich …)
  - optional: nur nachträglich geänderte Einträge
- Monatsnavigation: Vor/Zurück zu vergangenen Monaten; Default beim Öffnen immer **aktueller Monat**.

### 1.2 Dosierung als `Double` (Bruchdosen / Freitext)

- Dosis und Bestandsänderungen als `Double`.
- Freitext-Eingabe mit Kommazahlen (lokales Dezimalzeichen beachten).
- Validierung: `> 0`, sinnvolle Obergrenze, parsebar; Anzeige gerundet aber intern präzise.
- Bestand und Historie müssen Teilmengen korrekt abziehen.

> Hinweis Ist-Zustand: `doseQuantity` / Bestand sind im Modell bereits `Double` – Freitext und durchgängige UX/Historie müssen vereinheitlicht und abgesichert werden.

### 1.3 Einmalige Einnahmen (z. B. IBU)

- Medikamente ohne Dauerplan: **einmalig geplant** und/oder **bei Bedarf**.
- Klar getrennt von Dauertherapie in Liste, Start und Historie.

### 1.4 Gruppierung nur slotweise (nicht „zweimal“ dasselbe Medikament)

**Szenario:** A@X, B@Y, C@X+Y  

- Gruppierung pro **Einnahme-Slot**, nicht pauschal pro Medikament.
- C@X mit A, C@Y mit B = zwei Slots, keine doppelte Mitgliedschaft desselben Slots.

### 1.5 Persönliche Gesundheitsdaten (Profil)

Pflichtnahe Basis + erweiterte Vorschläge (umsetzbar als Felder / Abschnitte):

**Basis**
- Blutgruppe (+ Rhesus)
- Größe (cm), Gewicht (kg), BMI (berechnet)

**Erweitert – Vorschläge**
- Allergien / Unverträglichkeiten (Liste + Freitext)
- Chronische Erkrankungen / Diagnosen (Freitext)
- Regelmäßige Medikamente außerhalb der App (Freitext, optional)
- Implantate / Prothesen / Schrittmacher (Ja/Nein + Notiz)
- Schwangerschaft / Stillzeit (optional, sensibel, klar als Privatfeld)
- Rauchen / Alkohol (optional, grobe Angabe)
- Impfstatus-Notiz (Freitext; Details lieber als PDF)
- Letzte Vorsorge / letzter Arztbesuch (Datum + Notiz)
- Krankenkasse / Versichertennummer (optional, lokal, klar als sensibel markiert)
- Hausarzt (Kurzverweis; Details unter Kontakte)
- Notfallhinweise (Freitext, z. B. „bei Anaphylaxie Adrenalin“)
- Organspendeausweis vorhanden (Ja/Nein)
- Bevorzugte Apotheke (Name/Telefon)
- Sehhilfe / Hörgerät (kurz)
- Blutdruck / Blutzucker „letzter bekannter Wert“ (optional, manuell, kein Medizinprodukt-Claim)

Alles lokal, mit Disclaimer: **kein Ersatz für Arztakte / kein Medizinprodukt**.

### 1.6 Notfall- & Arztkontakte

- Notfallkontakte und Ärzt:innen (Name, Fachrichtung, Telefon, Adresse/Notiz).
- Erreichbar nur über **Profil** (inkl. Notfallkarte als Unterseite).

### 1.7 Arztberichte & Ergebnisse (PDF, nur lokal)

- Upload, Liste, Öffnen, Löschen von PDFs.
- Metadaten: Titel, Datum, Kategorie (Labor, Arztbrief, Bildgebung, Sonstiges).
- Speicherung nur im App-Speicher; Export/Share optional später, **kein** Cloud-Backup jetzt.

### 1.8 Navigation: max. 5 Tabs

| # | Tab | Inhalt |
|---|-----|--------|
| 1 | **Start** | Heutige / fällige Einnahmen |
| 2 | **Medikamente** | Pläne, einmalig/Bedarf, Hinzufügen |
| 3 | **Verlauf** | Historie: Monat, Filter, Bearbeiten |
| 4 | **Profil** | Gesundheitsdaten, Kontakte, Notfallkarte, PDFs |
| 5 | **Mehr** | App-Einstellungen (Sprache, Tageszeiten, Benachrichtigungen, …) |

Unterseiten unter **Profil** (keine eigenen Tabs): Persönliche Daten · Kontakte · Notfallkarte · Dokumente.

### 1.9 Erinnerungsart „Tageszeit“ – Fenster-Verhalten

**Problem:** Bei „Mittags“ und Einstellung z. B. 12:00–14:00 erscheint die Logik heute oft nur punktuell (Start = preferred/start), nicht als Fenster.

**Soll (Beispiel Mittag 12:00–14:00):**
1. **Erste Meldung um 12:00** (Fensterbeginn).
2. Meldung bleibt **mindestens bis 14:00** sichtbar/aktiv, **solange nicht eingenommen**.
3. **Nach 14:00** darf die System-Benachrichtigung verschwinden / beendet werden.
4. **In der App** bleibt die Einnahme danach **nachträglich bestätigbar** (Start-Nachzug und/oder Verlauf bearbeiten).

Gilt analog für alle `DAY_PERIOD`-Fenster (Morgen, Mittag, Nachmittag, Abend, Nacht) gemäß Einstellungen.

---

## 2. Akzeptanzkriterien

1. Historie: Default aktueller Monat; andere Monate erreichbar; Filter wirken kombiniert.
2. Vergessenen Klick kann man nachträglich setzen (Status + Zeit); nachträgliche Änderung ist sichtbar.
3. Einmalige Einnahme (z. B. IBU) ohne Dauerplan anlegbar und in Start/Verlauf sinnvoll.
4. Dosis als `Double` per Freitext; Bestand/Historie korrekt.
5. Slot-Gruppierung ohne doppelte Slot-Mitgliedschaft.
6. PDFs lokal speichern, listen, öffnen, löschen.
7. Profildaten & Kontakte speicher-/editierbar; Notfallkarte nur über Profil.
8. Maximal 5 Tabs; Tab 4 = **Profil**.
9. Tageszeit: Notification ab Fensterstart bis Fensterende (oder bis Confirm); danach in App nachtragbar.

---

## 3. Übernommene Ideen (verbindlich für Konzept)

### 3.1 Historie & Compliance *(vormals 4.1)*
- Badge „Nachträglich bestätigt“ + optionale Begründung.
- Undo per SnackBar (5–10 s).
- Bestätigungsdialog bei Änderung älterer Einträge (z. B. außerhalb des angezeigten Monats oder > N Tage).
- Optional später: Einnahmequote mit/ohne Nachträge.
- Start: Schnellaktion „Vergessen zu tippen?“ für offene heutige Slots.

### 3.2 Dosierung & Bestand *(vormals 4.2)*
- Freitext-`Double` für alle relevanten Mengen (Tablette, ml, Hub).
- Restbestand-Warnung auch mit Bruchwerten.
- Sanfter Hinweis, wenn Bruchdose zur Form unüblich ist (z. B. Kapsel halbieren) – nicht blockierend.

### 3.3 Einmalig / bei Bedarf *(vormals 4.3 + Anforderung 1.3)*
- Typen: **Dauerplan** · **Einmalig geplant** · **Bedarf (PRN)** (optional Abstand/Max pro Tag).
- Bedarfskarte auf Start ohne Pflicht-Alarm.
- Optional Ablauf / „nur X Tage“ für Akutmittel.

### 3.4 Dokumente unter Profil *(vormals 4.5, ohne Cloud)*
- Import via SAF → App-interner Speicher → System-PDF-Viewer.
- Metadaten, Suche/Filter, Speicherhinweis bei großen Dateien.
- **Kein** verschlüsseltes Cloud-Backup in diesem Scope.

### 3.5 Persönliche Daten & Notfall *(vormals 4.6)*
- Notfallkarte als Unterseite von **Profil**.
- Ärzt:innen/Apotheke als Kontaktkategorien.
- `tel:`-Link nur nach Bestätigung, keine Auto-Wahl.

### 3.6 Datenschutz *(vormals 4.7)*
- Gesundheitsdaten & PDFs nur **CE-Storage**.
- Keine Dokument-/Medikamentennamen auf dem Sperrbildschirm.
- „Profil leeren“ / Dokumente löschen.
- Kurzer Disclaimer im Profil.

### 3.7 UX unter 5 Tabs *(vormals 4.8)*
- FAB nur auf Medikamente (Hinzufügen), kein 6. Tab.
- Profil-Unterseiten per Back-Stack.
- Medizinisches → **Profil**; App-Technik → **Mehr**.

### 3.8 Nice-to-have später *(vormals 4.9)*
- Nachfüll-Erinnerung, Notfall-QR auf Nutzeraktion, Familienprofile, manuelle Laborwerte + Diagramm.

### 3.9 Gruppierung Slot-Modell *(weiterhin nötig, vormals 4.4)*
- `IntakeSlot(medicationId, triggerMinutes, dayMask)` als Gruppierungsobjekt.
- Farben/Vorschläge pro Slot-Gruppe; Entkoppeln einzelner Slots.

---

## 4. Konzept: konkrete Änderungen (was folgt)

### Phase A – Historie (Filter, Monat, Bearbeiten)

| Bereich | Änderung |
|---------|----------|
| Domain / Entity | `IntakeHistory`: Felder `note`, `editedAt`, `editedLate` (oder äquivalent); Status-Updates erlauben Re-Open. |
| Repository / DAO | Queries nach Monat (`from`/`to`), kombiniert mit Medikament + Status; Update-API für Status/Zeit/Notiz. |
| Use Cases | `EditIntakeHistoryUseCase`, `QueryIntakeHistoryUseCase` (Monat + Filter). |
| UI `HistoryScreen` | Monats-Pager (← Monat →), Chip-/Dropdown-Filter, Edit-Dialog/Sheet. |
| UI Start | Aktion „nachtragen“ für heutige offenen Slots. |
| Strings | DE/EN für Filter, Badge, Dialoge. |

### Phase B – Dosierung `Double` / Freitext

| Bereich | Änderung |
|---------|----------|
| UI Add/Edit | Einheitliche Dezimal-Eingabe (`filterDecimalInput`), Fehlertexte. |
| Domain | Sicherstellen: Abbuchung Historie/Bestand mit derselben `Double`-Dosis. |
| Anzeige | Formatierung (max. sinnvolle Nachkommastellen), keine implizite Int-Casts. |

### Phase C – Einmalig & Bedarf

| Bereich | Änderung |
|---------|----------|
| Model | `MedicationPlanType`: `RECURRING` / `ONE_SHOT` / `PRN` (Namen final im Code). |
| Schedule | ONE_SHOT: ein Termin oder „jetzt/heute“; PRN: ohne Cron, optional Constraints. |
| AlarmScheduler | ONE_SHOT einmal planen; PRN standardmäßig ohne periodischen Alarm. |
| UI Medikamente/Start | Filter/Sektionen; Bedarfskarte. |

### Phase D – Slot-Gruppierung

| Bereich | Änderung |
|---------|----------|
| `MedicationGroupingService` | Kandidaten auf **Slot-Ebene**; Medikament mit mehreren Zeiten → mehrere unabhängige Slots. |
| Persistenz Gruppe | Mitgliedschaft = Slot-ID, nicht nur `medicationId`. |
| UI | Gruppenvorschläge und Farben pro Slot; C@X und C@Y getrennt. |

### Phase E – Tageszeit-Fenster (Notification-Lifecycle)

| Bereich | Änderung |
|---------|----------|
| `AlarmScheduler` | Für `DAY_PERIOD`: Alarm/Notification **zum `startMinutes`** des konfigurierten Fensters. |
| Notification-Lifecycle | Notification **ongoing/persistent bis `endMinutes`** oder bis Confirm/Skip; danach cancel. |
| Optional | Leichter Refresh/Re-Post innerhalb des Fensters (falls System Notification verwirft) – nur falls nötig. |
| Domain Start/Upcoming | Nach Fensterende: Eintrag bleibt in App als **nachtragbar** (PENDING/MISSED → confirmbar), ohne aktive Notification. |
| Einstellungen | Verhalten kurz erklären („Meldung von Start bis Ende der Tageszeit“). |

**Ablauf Mittag 12–14 Uhr:**
```
12:00  Notification zeigen (falls noch nicht genommen)
12:00–14:00  Notification hält / bleibt relevant
Confirm     → Notification weg, Historie CONFIRMED
14:00       → Notification cancel (falls noch offen)
nach 14:00  → in App weiterhin „nachträglich einnehmen“ möglich
```

### Phase F – Tab Profil + Daten/Kontakte/PDFs

| Bereich | Änderung |
|---------|----------|
| Navigation | 5. Tab **Profil**; Routen: `profile`, `profile/health`, `profile/contacts`, `profile/emergency`, `profile/documents`. |
| Room / Files | Entities: `HealthProfile`, `Contact`, `MedicalDocument` (+ Datei unter `filesDir/documents/`). |
| UI | Hub-Screen Profil mit Kacheln; Formulare; PDF-Picker (SAF); Liste Öffnen/Löschen. |
| Privacy | Nur CE; Disclaimer; „Profil-/Dokumente löschen“. |
| Settings-Tab | Umbenennen/zuschneiden auf **Mehr** (Tageszeiten bleiben hier). |

### Phase G – Feinschliff

- Disclaimer, leere Zustände, Speicherhinweise, Accessibility.
- Regression: Direct Boot / generische Lock-Screen-Texte unverändert datensparsam.
- Tests: Monat-Filter, Double-Parse, DAY_PERIOD Start/Ende, Slot-Gruppierung C@X/C@Y.

---

## 5. Umsetzungsreihenfolge (empfohlen)

1. **Phase A** – Historie Monat + Filter + Edit  
2. **Phase E** – Tageszeit-Fenster Notifications (spürbarer Bugfix)  
3. **Phase B** – Double/Freitext durchziehen  
4. **Phase C** – Einmalig / Bedarf  
5. **Phase D** – Slot-Gruppierung  
6. **Phase F** – Profil-Tab, Gesundheitsdaten, Kontakte, lokale PDFs  
7. **Phase G** – Feinschliff & Tests  

---

## 6. Explizit nicht in diesem Scope

- Cloud-Sync / verschlüsseltes Online-Backup von PDFs  
- Notfallkarte als Icon auf dem Start-Screen  
- Mehr als 5 Bottom-Tabs  
- Medizinprodukt-Anspruch, automatische Diagnose, Gerätemesswerte-Import  

---

## 7. Kurzfazit – was neu gebaut wird

1. **Verlauf** wird zum Monats- + Filter-Werkzeug mit nachträglicher Korrektur.  
2. **Dosen** sind durchgängig kommazahlfähig (`Double` + Freitext).  
3. **Einmalig/Bedarf** ergänzt die Dauertherapie.  
4. **Gruppierung** wird slotbasiert (Fix für Mehrfachzeiten).  
5. **Tageszeit-Erinnerungen** leben im konfigurierten Fenster (Start → Ende), danach nur noch In-App-Nachtrag.  
6. Neuer Tab **Profil** bündelt Gesundheitsdaten (erweitert), Kontakte, Notfallkarte und **lokale** PDFs – ohne die Tab-Leiste zu sprengen.
