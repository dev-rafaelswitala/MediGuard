# Bekannte Issues

## Kritisch / hoch
1. **Reminder-Zuverlässigkeit** – Exact Alarms, OEM-Battery-Optimierung, Reschedule nach Reboot/Doze; Tageszeit-Fenster-Ende muss zuverlässig canceln.
2. **UX-Inkonsistenz** – TopAppBar-Farben, Abstände, BottomBar-Duplikation früher, Formular-Dichte auf AddMedication/Profil.

## Mittel
3. Home-Header croppt Logo aggressiv (`ContentScale.Crop`).
4. `colors.xml` noch mit Legacy-Blau-Werten (Compose-Theme ist Teal) – XML für Notifications/Statusbar angleichen.
5. Dynamic Color (Android 12+) kann Markenfarben überschreiben.
6. ONE_SHOT-Eingabe nur `dd/MM/yyyy` + `HH:mm`.

## Niedrig
7. Desktop-Ordner heißt noch `MediReminder`.
8. Alte Docs nennen noch MediReminder.
9. `ic_splash_logo.xml` (Schild+Kreuz) ungenutzt / veraltet.
