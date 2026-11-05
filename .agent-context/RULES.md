# Regeln

## Produkt
- Sichtbarer Name: **MediGuard**
- Max. **5** Bottom-Tabs: Start · Medizin · Verlauf · Profil · Mehr
- Deutsch: echte Umlaute (ä, ö, ü, ß); parallel immer Englisch in `AppStrings`
- PDFs / Gesundheitsdaten: nur lokal (CE-Storage)
- Kein Cloud-Backup im aktuellen Scope

## Technik
- Kotlin, Jetpack Compose, Hilt, Room
- Direct Boot: keine sensiblen Namen auf dem Lock-Screen
- Dosis als `Double` + Freitext
- Gruppierung **slotbasiert** (`schedule.intakeGroupId`)
- Tageszeit-Reminder: Notification ab Fensterstart bis Fensterende oder Confirm; danach In-App nachtragbar

## UX
- Konsistente Abstände, lesbare Hierarchie, klare Primäraktionen
- FAB nur wo nötig (Medikamente hinzufügen), kein 6. Tab
- Profil: Accordion-Kategorien
- Historie: Default = aktueller Monat, Filter + Edit

## Git
- Siehe `WORKFLOW.md`
- Nie pushen ohne Erlaubnis
