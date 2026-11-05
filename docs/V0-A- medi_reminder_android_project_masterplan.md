# MediReminder – Android Kotlin Direct-Boot Medication Reminder App

## Projektname

**MediReminder**

Alternativen:
- SecureDose
- MedAlert DirectBoot
- PillGuardian
- PharmaNotify

Empfehlung: **MediReminder**

Begründung:
- Klar verständlich
- Beschreibend
- Professionell
- Englisch
- Skalierbar

---

# Projektziel

## Ausgangslage

Entwicklung einer Android-App in Kotlin, die Nutzer zuverlässig an Medikamenteneinnahmen erinnert — auch nach einem Geräte-Neustart vor der ersten Entsperrung.

## Kernproblem

Normale Android-Apps verlieren vor dem ersten Unlock Zugriff auf verschlüsselte Nutzerdaten.

## Ziel

Implementierung einer App mit:

- Direct Boot Support
- Device Encrypted Storage (DE)
- Credential Encrypted Storage (CE)
- Datenschutzkonformen Notifications
- Offline-Funktionalität
- Exakten Alarmen
- Historie / Einnahmelogbuch
- Erweiterbarer Architektur

---

# Technische Hauptanforderungen

## Pflicht

### Direct Boot
```xml
android:directBootAware="true"
```

### Speichertrennung

### CE (Credential Encrypted)
Speichert:
- Medikamentenname
- Dosierung
- Einnahmepläne
- Historie

### DE (Device Encrypted)
Speichert:
- Alarmzeiten
- Alarm IDs
- Minimale Reminder-Metadaten

## AlarmManager
- `setExactAndAllowWhileIdle()`
- Wiederherstellung nach Reboot

## Notifications
### Vor Unlock:
"Time for your medication!"

### Nach Unlock:
"Please take 20mg Ibuprofen now."

---

# Mobile First Prinzipien

## Design
- Jetpack Compose
- Bottom navigation
- Große Touch-Zonen
- Minimale Eingabemasken
- Priorisierung Daumenbereich

## Performance
- Room lokal
- Keine Cloud-Pflicht
- LazyColumn
- StateFlow
- ViewModel
- WorkManager nur wenn notwendig

## Speicher
- Kleine APK
- ProGuard / R8 aktivieren
- Nur notwendige Libraries

## Akku
- AlarmManager statt permanenter Services
- BroadcastReceiver
- Keine Polling-Prozesse

---

# Empfohlene Technologien

## Core
- Kotlin
- Android Studio
- Jetpack Compose
- MVVM Architecture
- Room Database
- Hilt (Dependency Injection)
- AlarmManager
- DataStore Preferences
- NotificationCompat

## Libraries

### build.gradle (Module)
```kotlin
implementation("androidx.core:core-ktx:1.13.1")
implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
implementation("androidx.activity:activity-compose:1.9.0")
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.material3:material3")
implementation("androidx.navigation:navigation-compose:2.7.7")
implementation("androidx.room:room-runtime:2.6.1")
ksp("androidx.room:room-compiler:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
implementation("androidx.datastore:datastore-preferences:1.1.1")
implementation("com.google.dagger:hilt-android:2.52")
ksp("com.google.dagger:hilt-compiler:2.52")
implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
```

## Begründung

| Library | Zweck |
|--------|------|
| Room | Offline Datenbank |
| DataStore | DE-Speicherung |
| Hilt | Saubere Dependency Injection |
| Compose | Modernes UI |
| Navigation Compose | Navigation |
| AlarmManager | Exakte Erinnerungen |

---

# Projektstruktur

```plaintext
MediReminder/
│
├── app/
│   ├── src/main/
│   │   ├── java/com/example/medireminder/
│   │   │   ├── data/
│   │   │   │   ├── local/
│   │   │   │   │   ├── entity/
│   │   │   │   │   │   ├── MedicationEntity.kt
│   │   │   │   │   │   ├── MedicationScheduleEntity.kt
│   │   │   │   │   │   └── MedicationHistoryEntity.kt
│   │   │   │   │   ├── dao/
│   │   │   │   │   │   ├── MedicationDao.kt
│   │   │   │   │   │   └── MedicationHistoryDao.kt
│   │   │   │   │   └── AppDatabase.kt
│   │   │   │   ├── datastore/
│   │   │   │   │   └── DirectBootAlarmStore.kt
│   │   │   │   └── repository/
│   │   │   │       └── MedicationRepositoryImpl.kt
│   │   │   │
│   │   │   ├── domain/
│   │   │   │   ├── model/
│   │   │   │   │   ├── Medication.kt
│   │   │   │   │   ├── MedicationSchedule.kt
│   │   │   │   │   └── IntakeHistory.kt
│   │   │   │   ├── repository/
│   │   │   │   │   └── MedicationRepository.kt
│   │   │   │   ├── usecase/
│   │   │   │   │   ├── AddMedicationUseCase.kt
│   │   │   │   │   ├── ScheduleMedicationUseCase.kt
│   │   │   │   │   └── ConfirmMedicationIntakeUseCase.kt
│   │   │   │
│   │   │   ├── alarm/
│   │   │   │   ├── AlarmScheduler.kt
│   │   │   │   ├── AlarmReceiver.kt
│   │   │   │   └── BootReceiver.kt
│   │   │   │
│   │   │   ├── notification/
│   │   │   │   └── MedicationNotificationManager.kt
│   │   │   │
│   │   │   ├── ui/
│   │   │   │   ├── screens/
│   │   │   │   │   ├── HomeScreen.kt
│   │   │   │   │   ├── AddMedicationScreen.kt
│   │   │   │   │   ├── HistoryScreen.kt
│   │   │   │   │   └── SettingsScreen.kt
│   │   │   │   ├── components/
│   │   │   │   │   ├── MedicationCard.kt
│   │   │   │   │   └── TimeSelector.kt
│   │   │   │   └── navigation/
│   │   │   │       └── AppNavigation.kt
│   │   │   │
│   │   │   ├── viewmodel/
│   │   │   │   ├── MedicationViewModel.kt
│   │   │   │   └── HistoryViewModel.kt
│   │   │   │
│   │   │   ├── di/
│   │   │   │   └── AppModule.kt
│   │   │   │
│   │   │   ├── MainActivity.kt
│   │   │   └── MediReminderApplication.kt
│   │   │
│   │   └── AndroidManifest.xml
│   │
│   └── build.gradle.kts
│
└── README.md
```

---

# Minimale Startreihenfolge

## Phase 1 – Setup

1. Android Studio installieren
2. Kotlin + Compose Projekt erstellen
3. Hilt integrieren
4. Room einrichten
5. DataStore einrichten
6. Notification Channel erstellen

---

## Phase 2 – Datenmodell

### Medication
```kotlin
data class Medication(
    val id: Long,
    val name: String,
    val dosageMg: Int,
    val frequencyType: FrequencyType,
    val scheduleTimes: List<String>,
    val durationDays: Int?
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

---

## Phase 3 – Alarm Logik

- Alarm setzen
- Alarm speichern im DE
- Receiver registrieren
- BootReceiver implementieren

---

## Phase 4 – UI

- HomeScreen
- AddMedicationScreen
- HistoryScreen

---

## Phase 5 – Erweiterungen

- Statistiken
- Dark Mode
- Export PDF
- CSV Export
- Adhärenzrate

---

# Offline-Funktionalität

## Vollständig lokal möglich

### Speicherung:
- Room Database
- DataStore
- SharedPreferences nur falls nötig

## Vorteile:
- Keine Internetpflicht
- Datenschutz
- Geringer Datenverbrauch
- Hohe Zuverlässigkeit

## Synchronisierung optional:
- Später Google Drive Backup
- Firebase optional

---

# Datenstrategie

## Soll gespeichert werden?
Ja.

### Warum?
- Individuelle Medikation
- Unterschiedliche Dosierungen
- Unterschiedliche Einnahmeintervalle
- Historie erforderlich
- Maximal ~20 Medikamente realistisch

## Aufwand Nutzer
Akzeptabel bei guter UX:
- Dropdowns
- Templates
- Wiederholungsoptionen
- Auto-Vervollständigung

---

# OOP / Kotlin Kursanforderungen

## Sinnvoll integrierbar

### Klassen
- Medication
- Scheduler
- NotificationManager

### Interfaces
```kotlin
interface MedicationRepository
interface AlarmScheduler
```

### Vererbung
- BaseReminder
- TimedReminder
- IntervalReminder

### Polymorphie
- Unterschiedliche Reminder-Typen

### Collections
- List<Medication>
- Map<DayOfWeek, List<Medication>>

### Higher Order Functions
```kotlin
medications.filter { it.isActive }
```

### Scope Functions
```kotlin
apply {}
let {}
run {}
```

### Singleton
```kotlin
object NotificationConstants
```

---

# Zusätzliche Differenzierungsmerkmale

## Besonders sinnvoll:

### 1. Einnahme-Adhärenz Score
- Prozentuale Einnahmetreue

### 2. Smart Snooze
- Erinnerung erneut nach X Minuten

### 3. Medikamentenbestand
- "Nur noch 3 Tabletten übrig"

### 4. Exportfunktion
- PDF für Arztbesuche

### 5. Familienmodus
- Pflege anderer Personen

### 6. Sprachsteuerung
- Einnahme per Voice bestätigen

### 7. Widget
- Homescreen Medikamentenübersicht

### 8. WearOS Support
- Smartwatch-Erinnerung

---

# Sicherheitsstrategie

## Datenschutz
- Sensible Daten CE
- DE nur Minimaldaten
- Keine Medikamentennamen im Lockstate

## Null-Safety
- Kotlin nullable/non-nullable
- Safe Calls
- Default Values

## Type-Safety
- Enums
- Sealed Classes
- Data Classes

---

# AndroidManifest Kernpunkte

```xml
<receiver
    android:name=".alarm.BootReceiver"
    android:directBootAware="true"
    android:exported="false">
    <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED" />
        <action android:name="android.intent.action.LOCKED_BOOT_COMPLETED" />
    </intent-filter>
</receiver>
```

---

# Schritt-für-Schritt Android Studio Setup

## Installation

### Installieren:
- Android Studio
- Android SDK 34+
- Kotlin Plugin
- Git
- VS Code Kotlin Extensions

---

## Neues Projekt:

### Template:
- Empty Activity
- Kotlin
- Minimum SDK 26+

### Package:
```plaintext
com.example.medireminder
```

---

## Danach:

1. Hilt Setup
2. Room Setup
3. DataStore Setup
4. Notification Channels
5. AlarmManager
6. BootReceiver
7. Compose Navigation
8. Screens

---

# GitHub Copilot Startprompt

```md
Create an Android application in Kotlin named MediReminder.

Requirements:
- MVVM architecture
- Jetpack Compose UI
- Hilt Dependency Injection
- Room database for Credential Encrypted medication data
- DataStore Device Protected Storage for Direct Boot alarm schedules
- AlarmManager exact alarms
- BootReceiver with LOCKED_BOOT_COMPLETED and BOOT_COMPLETED
- Privacy-safe notifications before unlock
- Full medication notifications after unlock
- Features:
  - Add medication form
  - Dosage
  - Scheduling options:
    - Exact time
    - Time range
    - Morning/noon/evening/night
    - Interval reminders
    - Weekly reminders
  - Intake history log
  - Offline-first design
  - Battery efficient architecture
- Use:
  - Interfaces
  - Repository Pattern
  - OOP principles
  - Polymorphism for reminder types
  - Type-safe enums
  - Kotlin best practices

Generate:
- Full project structure
- Initial entities
- DAO classes
- Repository interfaces
- Alarm scheduler
- Notification manager
- MainActivity
- Navigation
- Minimal functional implementation
```

---

# Zusammenfassung

## Ziel
Eine technisch anspruchsvolle, datenschutzorientierte Android-App mit:

- Direct Boot
- Professioneller Architektur
- Offline First
- Kotlin Best Practices
- Kursrelevanten Programmierkonzepten
- Erweiterungspotenzial

## Positionierung
Dieses Projekt hebt sich deutlich ab durch:

- Sicherheitsarchitektur
- Systemnähe
- Moderne Android Standards
- Erweiterbarkeit
- Wissenschaftlich begründbare Technologieentscheidungen

