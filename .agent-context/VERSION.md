# Versionen

| Version | Stand | Notiz |
|---------|-------|-------|
| 1.0 | vor V2 | Basis-Erinnerungen, 4 Tabs |
| **2.0** | `main` @ de40c46 | Profil, Historie-Edit/Filter, ONE_SHOT/PRN, Slot-Gruppierung, Tageszeit-Fenster, MD-Logo |
| **2.1-dev** | `dev` @ 9d412e1 | Reminder-Fixes (snooze/window-end, boot unlock, catch-up), UX-Chrome, Edit-Race |
| **2.2-wip** | `feature/profile-plan-polish` | Profil-Accordion View/Edit, Weekly-Tage+Uhrzeit, Alpha-Sort, Splash ohne Kreis-Zuschnitt |

## Branches (Ist)
- `main` – V2.0 Snapshot (ahead of origin, **nicht gepusht**)
- `dev` – Integration inkl. UX/Reminder-Polish
- `feature/profile-plan-polish` – Profil/Plan/Splash-Polish (lokal)
- `feature/ux-reminders-polish` – gemerged nach `dev`

## APK (lokal, nicht im Git)
- `MediGuardV2.0.apk` im Projektroot (Debug)
- Build-Quelle: `app/build/outputs/apk/debug/app-debug.apk`

## Branding-Quellen
- Master-Logos: `branding/Logo-Icons/`
- Runtime: `app/src/main/res/drawable-nodpi/`
- Splash API 31+: Schild-Icon (Kreis); MD-Wortmark als Compose-Overlay ohne Kreismaske
