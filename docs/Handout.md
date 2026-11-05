# MediReminder – Technisches Handout

## Einführung
MediReminder ist eine Android-Anwendung zur Verwaltung von Medikamentenerinnerungen mit einem Fokus auf **Datenschutz**, **Zuverlässigkeit** und **Offline-Funktionalität**. Die App funktioniert auch nach spontanen Systemneustarts, bevor das Gerät zum ersten Mal entsperrt wird – ein kritisches Feature für zuverlässige Erinnerungen.

---

## 1. Direct Boot & Datenschutz

### Herausforderung
Android verschlüsselt Benutzerdaten auf zwei Ebenen:
- **Credential Encrypted (CE)**: Verfügbar erst nach dem ersten Entsperren des Geräts
- **Device Encrypted (DE)**: Verfügbar sofort nach dem Boot

Nach einem Update kann das Gerät starten und Alarme triggern, bevor der Nutzer es zum ersten Mal entsperrt. In diesem Zeitfenster darf die App keine sensiblen Daten wie Medikamentennamen anzeigen.

### Umsetzung
- **BootReceiver** mit `android:directBootAware="true"` reagiert auf `LOCKED_BOOT_COMPLETED`
- **DataStore (DE)**: Speichert nur Alarm-IDs, Trigger-Zeiten und Metadaten
- **Room (CE)**: Speichert sensible Daten (Medikamentennamen, Dosierungen, Historie)
- **Benachrichtigungen vor Unlock**: Generisch "Zeit für Ihr Medikament!"
- **Benachrichtigungen nach Unlock**: Mit vollem Namen und Dosierung

**Begründung**: Maximale Privatsphäre + maximale Zuverlässigkeit. Nutzer werden erinnert, auch wenn sie das Gerät nachts nicht entsperren, aber sehen keine sensiblen Infos auf dem Sperrbildschirm.

---

## 2. Architektur & Clean Code

### Schichtenmodell (MVVM + Repository Pattern)

```
┌─────────────────────────────────┐
│   UI Layer (Jetpack Compose)    │  HomeScreen, MedicationsScreen, ...
└────────────────┬────────────────┘
                 │
┌────────────────▼────────────────┐
│   ViewModel (State Management)  │  MedicationViewModel, HistoryViewModel
└────────────────┬────────────────┘
                 │
┌────────────────▼────────────────┐
│   Repository Interfaces         │  MedicationRepository, ScheduleRepository
└────────────────┬────────────────┘
                 │
┌────────────────▼────────────────┐
│   Domain Models & Logic         │  Medication, MedicationSchedule, Use Cases
└────────────────┬────────────────┘
                 │
┌──┬─────────────▼──────────────┬──┐
│Room│   Data Layer             │DS│  DAOs, Entities, DataStore
└──┴──────────────────────────┬─┴──┘
   │                          │
   └──────────────┬───────────┘
             Datenbank
```

**Begründung**: Klare Separation of Concerns ermöglicht Tests, Wartbarkeit und einfache Erweiterung neuer Features ohne Abhängigkeitsprobleme.

### Dependency Injection (Hilt)
- `AppModule` stellt zentral Datenbank, DAOs und Repositories bereit
- `@AndroidEntryPoint` auf MainActivity, BootReceiver, AlarmReceiver
- **Begründung**: Kein Service-Locator-Anti-Pattern, automatisch korrekte Singletons

---

## 3. Flexible Erinnerungsplanung

### Polymorphes Scheduling-System
Die App unterstützt 5 Erinnerungstypen über eine Sealed Class:

```kotlin
enum class FrequencyType {
    EXACT_TIME,        // 08:00 Uhr täglich
    TIME_RANGE,        // Irgendwann zwischen 08:00 und 10:00
    DAY_PERIOD,        // Morgens, Mittags, Abends, Nachts
    INTERVAL,          // Alle 6 Stunden ab 08:00
    WEEKLY             // Spezifische Wochentage + Uhrzeit
}
```

Jeder Typ wird als `BaseReminder` implementiert mit einer `nextTriggerAfter()` Methode:
- **ExactTimeReminder**: Einfache tägliche Berechnung
- **IntervalReminder**: Modulo-Arithmetik für Intervalle
- **WeeklyReminder**: Kalender-basierte Wochentag-Berechnung

**Begründung**: Ohne if/when-Pyramiden bleibt der Code erweiterbar. Neue Erinnerungstypen können hinzugefügt werden, ohne bestehenden Code zu ändern.

---

## 4. Intelligente Benachrichtigungsverwaltung

### Gruppierung & Alarm-Handling
Die App erkennt automatisch, wenn mehrere Medikamente zur gleichen Zeit fällig sind:

- **Gruppierungserkennung** (`MedicationGroupingService`): Findet Medikamente mit zeitlichen Überschneidungen
- **Benachrichtigungsgruppen**: Zeigt bis zu 6 Medikamente in einer Benachrichtigung
- **Alarm-Service** (`AlarmRingingService`): Klingelt mit auswählbarem Klingelton oder Vibration
- **AlarmManager mit Fallback**: 
  - Android 12+: Prüft `canScheduleExactAlarms()`
  - Falls verboten: `setAlarmClock()` für Sperrbildschirm-Anzeige
  - Falls alles fehlschlägt: Fallback auf `setAndAllowWhileIdle()`

**Begründung**: Klingelt zuverlässig auch bei Doze-Modus, Batteriesparprogrammen und Geräte-Updates.

---

## 5. Bestandsverwaltung & Benutzerführung

### Unterstützung mehrerer Medikamentenformen
- **Tablets/Kapseln**: Stückzahlen-Tracking
- **Flüssigkeiten**: Milliliter-Tracking (z.B. Sirup mit ml pro Einnahme)
- **Dynamische Validierung**: Rechnen mit Dezimalzahlen für genaue Dosierungen

### Automatische Vorschläge
- Erkennt niedrigen Bestand und erinnert an Nachbestellung
- Warn-Schwellenwert konfigurierbar
- Kann auf Startbildschirm oder per Benachrichtigung gezeigt werden

**Begründung**: Viele ältere Menschen nehmen Medikamente regelmäßig; das System hilft, Versorgungsunterbrechungen zu vermeiden.

---

## Technische Stack
| Komponente | Technologie |
|---|---|
| **UI Framework** | Jetpack Compose + Material Design 3 |
| **Architektur** | MVVM + Repository Pattern |
| **Database** | Room (Kotlin Coroutines) |
| **Verschlüsselte Prefs** | DataStore (Device/Credential Encrypted) |
| **DI Container** | Hilt |
| **Navigation** | Jetpack Navigation |
| **Alarms** | AlarmManager + BroadcastReceiver |
| **Benachrichtigungen** | NotificationCompat (Channels) |
| **Min API** | 26 (Android 8) |
| **Target API** | 36 (Android 15) |

---

## Fazit
MediReminder verbindet **Enterprise-Grade-Architektur** (saubere Schichten, DI, Tests-freundlich) mit **Benutzer-orientiertem Design** (Gruppierung, Bestandsverwaltung, Privatsphäre). Das Direct-Boot-Feature unterscheidet die App von Standard-Reminder-Apps und macht sie zu einer zuverlässigen Lösung für kritische Erinnerungen.
