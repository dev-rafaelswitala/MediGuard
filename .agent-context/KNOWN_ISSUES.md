# Bekannte Issues

## Kritisch / hoch
1. **Reminder-Zuverlässigkeit (teilweise behoben)** – Fenster-Ende, Snooze, Gruppenbestätigung, Locked-Boot-Wiederherstellung und App-Start-Catch-up sind korrigiert. Offen bleiben OEM-Battery-Optimierung und gerätespezifisches Doze-Verhalten.
2. **UX-Inkonsistenz (teilweise behoben)** – TopAppBar-Farben, Kartenabstände, Medikamenten-Icon und Einstellungsbezeichnung sind vereinheitlicht. Formular-Dichte auf AddMedication/Profil bleibt offen.

## Mittel
3. ~~Home-Header croppt Logo aggressiv (`ContentScale.Crop`).~~ **Behoben:** `ContentScale.Fit`.
4. ~~`colors.xml` noch mit Legacy-Blau-Werten.~~ **Behoben:** Primär-/Sekundärfarben entsprechen der Teal-Marke.
5. ~~Dynamic Color kann Markenfarben überschreiben.~~ **Behoben:** Standardmäßig deaktiviert.
6. ONE_SHOT-Eingabe nur `dd/MM/yyyy` + `HH:mm`.

## Niedrig
7. Desktop-Ordner heißt noch `MediReminder`.
8. Alte Docs nennen noch MediReminder.
9. `ic_splash_logo.xml` ist farblich aktualisiert, wird aber weiterhin nicht verwendet.
