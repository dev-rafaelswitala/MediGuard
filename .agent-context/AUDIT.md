# Screen-/Qualitäts-Audit

## 2026-11-05 – Plan/Gruppierung UX
Branch: `feature/plan-and-grouping-ux`

### Erledigt
- Medikament anlegen ohne Erinnerungsart → Button **Planen** → `PlanScheduleScreen`
- Automatische Gruppierungs-Prompts entfernt
- Selection: Bearbeiten (nur 1), Löschen, Link/LinkOff für Gruppe
- Icon-Hintergrund weiß (kein Petrolgrün), Splash hell/dunkel getrennt
- Home-Logo transparent ohne Karten-Rahmen
- TopAppBars auf `surface` (Dark Mode)
- Bottom-Tab „Mehr“ kurz

### Edge Cases bedacht
- Gruppieren nur wenn alle geplant und ungruppiert
- Beitritt: 1 gruppierte + n ungruppierte
- Gruppe lösen → Schedules weg → wieder Planen
- Bereits gruppierte nicht in andere Gruppe ziehen (außer Join-Fall)
- Tageszeit: Mehrfachauswahl = mehrere Einnahmen/Tag (Hilfetext)

### Offen / weiter beobachten
- Launcher-Icon-Cache am Emulator manchmal alt bis Neuinstall
- Freitext-Zeiten statt TimePicker (nächster Polish)
- Bestehende Testdaten haben schon Zeiten → Planen nur nach Lösen/Neuanlage sichtbar
