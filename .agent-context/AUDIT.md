# Screen-/Qualitäts-Audit (2026-11-05)

Vollständige Prüfung aller Screens + Reminder-Pfad. Status: Fixes laufen auf `feature/ux-reminders-polish`.

## P0 (kritisch)
1. Snooze cancelt Window-End nicht → false MISSED
2. Einzel-Confirm schließt Gruppen-Notification für Geschwister
3. BootReceiver Unlock: History ohne windowEnd/scheduleId
4. Edit-State-Race Medications ↔ AddMedication (id=0 Update)
5. Späte DAY_PERIOD-Feuerung ohne markMissed
6. App-Start Catch-up für abgelaufene Fenster

## P1 (UX)
7. TopAppBar/Padding/Cards vereinheitlichen
8. Settings Back tot / More vs Settings
9. Home Stop-Alarm Semantik
10. MedicationCard Spacing/Typo
11. Time-Picker statt Freitext (schrittweise)
12. TIME_RANGE tot oder wieder aktivieren
13. DirectBoot DayPeriodSettings.Default statt User-Prefs
14. Profil Save/Hydration
15. Snackbars für ViewModel-Messages

## P2
Icons, Dead-Code, DynamicColor Default false, LazyColumn Keys, Farben Due vs Missed vs LowSupply
