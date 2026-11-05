# MediReminder - Projektkontext und Umsetzungsplan

## Projektname

Empfohlener Name: **MediReminder**

Begruendung:
- beschreibt den Zweck direkt
- klingt professionell und neutral
- ist auf Englisch skalierbar
- passt zu Medication Reminder, Direct Boot und Offline First

Technischer Hinweis: Das bestehende Android-Package kann `com.rafaelswitala.mediguard` bleiben. Der sichtbare App-Name ist `MediReminder`.

## Zielbild

MediReminder ist eine Android-App in Kotlin, die Nutzer an Medikamenteneinnahmen erinnert. Die App soll auch dann funktionieren, wenn das Smartphone nachts durch ein Update neu startet und noch nicht zum ersten Mal entsperrt wurde.

Das Kernproblem ist Androids Dateiverschluesselung:
- Credential Encrypted Storage (CE) ist erst nach dem ersten Entsperren verfuegbar.
- Device Encrypted Storage (DE) ist bereits vor dem ersten Entsperren verfuegbar.

Die App trennt deshalb sensible und nicht-sensible Daten:
- CE: Medikamentenname, Dosierung, Historie, vollstaendige Nutzerdaten.
- DE: Alarm-ID, Medikament-ID, naechster Trigger-Zeitpunkt, Zeitplan-Metadaten ohne Medikamentennamen.

## Kernanforderungen

### Direct Boot

Pflicht:
```xml
android:directBootAware="true"
```

Eingesetzt bei:
- `BootReceiver`
- `AlarmReceiver`

Der `BootReceiver` reagiert auf:
- `Intent.ACTION_LOCKED_BOOT_COMPLETED`
- `Intent.ACTION_BOOT_COMPLETED`
- `Intent.ACTION_USER_UNLOCKED`

### Datenschutz

Vor dem ersten Unlock:
```text
Time for your medication!
```

Nach dem Unlock:
```text
Please take 20mg Ibuprofen now.
```

Vor dem ersten Unlock wird kein Medikamentenname aus CE gelesen.

### Offline First

Die App funktioniert vollstaendig lokal:
- Room speichert Medikamente und Historie.
- DataStore speichert Direct-Boot-Alarmmetadaten in DE.
- Es gibt keine Cloud-Pflicht.
- Es gibt keinen permanenten Hintergrundservice.

### AlarmManager

Die App nutzt:
- `AlarmManager.setExactAndAllowWhileIdle()`
- Fallback auf `AlarmManagerCompat.setAndAllowWhileIdle()`, falls exakte Alarme nicht erlaubt sind.
- `BroadcastReceiver` statt dauerhaften Services.

## Datenmodell

### Medication

Speicherort: CE / Room

```kotlin
data class Medication(
    val id: Long = 0,
    val name: String,
    val dosage: String,
    val dosageUnit: String = "mg",
    val description: String = "",
    val durationDays: Int? = null,
    val remainingDoses: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)
```

### MedicationSchedule

Speicherort:
- CE: vollstaendiger Zeitplan
- DE: nur nicht-sensitive Alarmmetadaten

```kotlin
data class MedicationSchedule(
    val id: Long = 0,
    val medicationId: Long,
    val scheduleType: FrequencyType,
    val scheduleData: String,
    val reminderMinutes: Int = 15,
    val snoozeOptions: List<Int> = listOf(5, 10, 15, 30, 60),
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
```

### FrequencyType

```kotlin
enum class FrequencyType {
    EXACT_TIME,
    TIME_RANGE,
    DAY_PERIOD,
    INTERVAL,
    WEEKLY
}
```

## Projektstruktur

```text
app/src/main/java/com/rafaelswitala/mediguard/
├── alarm/
│   ├── AlarmReceiver.kt
│   ├── AlarmScheduler.kt
│   └── BootReceiver.kt
├── data/
│   ├── datastore/
│   │   └── DirectBootAlarmStore.kt
│   ├── local/
│   │   ├── AppDatabase.kt
│   │   ├── dao/
│   │   └── entity/
│   └── repository/
├── di/
│   └── AppModule.kt
├── domain/
│   ├── model/
│   ├── reminder/
│   │   └── ReminderPlan.kt
│   ├── repository/
│   └── usecases/
├── notification/
│   └── MedicationNotificationManager.kt
├── ui/
│   ├── components/
│   ├── navigation/
│   ├── screens/
│   └── theme/
├── viewmodel/
├── MainActivity.kt
└── MediReminderApplication.kt
```

## Android-Studio-Setup

1. Android Studio oeffnen.
2. Projektordner `MediGuard` oeffnen.
3. Gradle Sync ausfuehren.
4. Sicherstellen, dass JDK 17 verwendet wird.
5. In `local.properties` pruefen:

```properties
sdk.dir=C\:\\Users\\rafaelswitala\\AppData\\Local\\Android\\Sdk
```

6. Debug-APK bauen:

```powershell
powershell -ExecutionPolicy Bypass -File .\build-apk.ps1
```

7. APK auf das Handy kopieren:

```text
MediReminder-debug.apk
```

## Empfohlene Startreihenfolge

1. Projekt lauffaehig bauen.
2. Datenbank und Entities definieren.
3. Repositories und Use-Cases einrichten.
4. Direct-Boot-DataStore in Device Protected Storage einrichten.
5. AlarmManager und Receiver implementieren.
6. Notification-Kanaele erstellen.
7. Eingabemaske bauen.
8. Historie und Quittierung implementieren.
9. Zusatzfunktionen wie Adhaerenzscore oder Export ergaenzen.

## Library-Auswahl

| Library | Zweck | Begruendung |
| --- | --- | --- |
| Kotlin | Programmiersprache | Null-Safety, Data Classes, Coroutines |
| Jetpack Compose | UI | Moderne deklarative Android-Oberflaechen |
| Navigation Compose | Navigation | Schlanke Navigation zwischen Screens |
| Room | lokale CE-Datenbank | Offline, typsicher, geeignet fuer strukturierte Daten |
| DataStore Preferences | DE-Metadaten | Coroutine-basiert, besser als SharedPreferences |
| Hilt | Dependency Injection | klare Abhaengigkeiten, testbare Architektur |
| AlarmManager | Erinnerungen | batterieeffizient, exact alarms moeglich |
| NotificationCompat | Benachrichtigungen | kompatibel ueber Android-Versionen |

Hinweis: Das Projekt nutzt JVM Target 17. Gemeint ist nicht eine Kotlin-Version 17, sondern Java/JVM 17 fuer den Build.

## Offline-Funktionalitaet

Offline wird durch lokale Persistenz erreicht:
- Room fuer Medikamente und Historie.
- DataStore fuer Direct-Boot-Alarme.
- Keine Server- oder Internetpflicht.
- Keine Synchronisierung im MVP.

Optionale spaetere Synchronisierung:
- Google Drive Backup
- verschluesselter Export
- Firebase nur optional, nicht fuer die Kernfunktion notwendig

## Soll die App Daten speichern?

Ja. Ohne Speicherung waere die App nach dem Schliessen oder Neustart nutzlos.

Der Eingabeaufwand ist realistisch, weil ein Nutzer typischerweise eine begrenzte Anzahl Medikamente pflegt. Die Maske sollte sich auf die wichtigsten Daten begrenzen:
- Medikamentenname
- Dosierung
- Einheit
- Einnahmezeit oder Einnahmeregel
- Dauer oder verbleibende Dosen

Die App muss nicht alle Medikamente der Welt kennen. Der Nutzer traegt seine eigenen Medikamente ein.

## OOP- und Kotlin-Kurskonzepte

Sinnvoll integriert:
- Klassen: `Medication`, `MedicationSchedule`, `AndroidAlarmScheduler`
- Interfaces: `MedicationRepository`, `MedicationScheduleRepository`, `IntakeHistoryRepository`, `AlarmScheduler`
- Data Classes: Medikamente, Zeitplaene, Historie
- Enums: `FrequencyType`, `DayPeriod`, `DayOfWeekEnum`, `IntakeStatus`
- Singleton/Object: `ScheduleDataCodec`, `ReminderPlanFactory`
- Vererbung: `BaseReminder`
- Polymorphie: `TimedReminder`, `IntervalReminder`, `WeeklyReminder`, `TimeRangeReminder`
- Collections: `List`, `Set`, `Map`-aehnliche DataStore-Auswertung
- Higher-Order Functions: `map`, `filter`, `forEach`, `mapNotNull`
- Scope Functions: `apply`, `let`
- Null-Safety: nullable Felder fuer optionale Dauer, Einnahmezeit, Bestand

Nicht erzwungen:
- Infix-Funktionen: kein fachlicher Mehrwert im aktuellen Code.
- Abstracte Hierarchien fuer alle Modelle: wuerde die App kuenstlich komplizierter machen.
- Arrays: Lists sind idiomatischer fuer variable Medikamenten- und Zeitplanmengen.

## Differenzierungsmerkmale

Bereits sinnvoll vorbereitet oder implementiert:
- Direct-Boot-faehige Erinnerungen
- CE/DE-Datentrennung
- Datenschutzkonforme Notifications
- Einnahmehistorie
- Adhaerenzscore in Prozent
- Dauer in Tagen
- optionaler Medikamentenbestand
- mehrere Schedule-Typen

Gute Erweiterungen:
1. CSV-Export fuer Arztbesuche
2. PDF-Bericht
3. Smart Snooze mit frei waehlbaren Minuten
4. Bestand-Warnung bei wenigen Tabletten
5. Familienmodus fuer mehrere Profile
6. Widget mit den naechsten Einnahmen
7. WearOS-Erinnerung

Priorisierung fuer ein Kursprojekt:
1. CSV-Export
2. Bestand-Warnung
3. Adhaerenzscore
4. PDF erst danach

## GitHub-Copilot-Prompt

```md
You are working on an Android Kotlin app named MediReminder.

Goal:
Build an offline-first medication reminder app with Direct Boot support. The app must remind the user even after a reboot before the first unlock.

Technical requirements:
- Kotlin with JVM target 17
- Jetpack Compose UI
- MVVM architecture
- Hilt dependency injection
- Room for Credential Encrypted medication data
- DataStore Preferences using deviceProtectedStorageContext for Device Encrypted alarm metadata
- AlarmManager exact alarms with setExactAndAllowWhileIdle
- BootReceiver for LOCKED_BOOT_COMPLETED, BOOT_COMPLETED and USER_UNLOCKED
- AlarmReceiver marked directBootAware
- Privacy-safe notification before first unlock: "Time for your medication!"
- Full notification after unlock: "Please take 20mg Ibuprofen now."
- Offline-first, no cloud dependency
- Battery efficient, no polling, no permanent background service

Data requirements:
- Store medication name, dosage, unit, duration and history in Room CE storage.
- Store only schedule id, medication id, next trigger time, schedule type and schedule data in DE storage.
- Never store medication names in DE storage.

UI requirements:
- Mobile-first Compose UI
- Bottom navigation
- Home screen with medication reminders
- Add medication screen with name, dosage, unit, duration, stock and schedule options
- Schedule options: exact time, time range, day period, interval, weekly
- History screen with confirmation button and adherence percentage

Architecture requirements:
- Repository interfaces
- Use cases
- Type-safe enums
- Null-safe models
- OOP with an AlarmScheduler interface and polymorphic reminder classes:
  - BaseReminder
  - TimedReminder
  - IntervalReminder
  - WeeklyReminder
  - TimeRangeReminder

Keep the implementation minimal, compileable, and focused on the Direct Boot medication reminder use case.
```
