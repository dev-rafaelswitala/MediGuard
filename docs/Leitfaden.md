# MediReminder – Präsentationsleitfaden (10 Minuten)

## Übersicht
Dieser Leitfaden zeigt die 5 Kernfeatures der App mit paralleler Code- und Live-Demo. Jedes Feature hat eine ~2-minütige Demo.

---

## Feature 1: Direct Boot & Privatsphäre (2 Min)

### Szenario
"Das Gerät wird nachts durch ein Update neu gestartet. Der Alarm sollte klingeln, auch wenn der Nutzer es nicht entsperrt."

### Was du zeigst

#### Im Code:
Öffne `BootReceiver.kt` und `AlarmReceiver.kt`:
```kotlin
// BootReceiver reagiert SOFORT nach Boot
<receiver android:name=".alarm.BootReceiver"
    android:directBootAware="true">
    <intent-filter>
        <action android:name="android.intent.action.LOCKED_BOOT_COMPLETED" />
    </intent-filter>
</receiver>
```

Erkläre:
- `directBootAware="true"` = Lädt vor Unlock
- `LOCKED_BOOT_COMPLETED` triggert, während Gerät noch verschlüsselt ist
- DataStore (Device Encrypted) enthält Alarm-IDs, nicht Medikamentennamen

#### In der App:
1. Öffne **Settings** → Zeige die Alarm-Modi (Silent, Sound, Alarm)
2. Erstelle eine Test-Erinnerung für "jetzt + 1 Minute"
3. Erkläre: "Wenn ich das Gerät jetzt neustarten würde, würde dieser Alarm trotzdem triggern – bevor ich es entsperre"
4. Wenn der Alarm klingelt: Zeige, dass der Sperrbildschirm nur "Zeit für Ihr Medikament!" sagt, nicht "Ibuprofen 200mg"

### Warum das wichtig ist
- Kritisch für Medikamente, die Zeit-sensibel sind
- Maximale Privatsphäre (keine Medikamentennamen auf Sperrbildschirm)

---

## Feature 2: Intelligente Benachrichtigungen & Gruppierung (2 Min)

### Szenario
"Ich nehme morgens um 08:00 drei verschiedene Medikamente. Die App zeigt alle zusammen, nicht einzeln."

### Was du zeigst

#### Im Code:
Öffne `MedicationGroupingService.kt`:
```kotlin
object MedicationGroupingService {
    const val MAX_MINUTE_DIFF = 10
    
    fun findCandidates(
        medications: List<Medication>,
        schedulesByMedication: Map<Long, List<MedicationSchedule>>
    ): List<GroupingCandidate> {
        // Findet Medikamente, deren Trigger-Zeiten < 10 Min unterscheiden
        // Gruppiert sie automatisch
    }
}
```

Erkläre:
- 10-Minuten-Fenster = Medikamente werden zusammen erinnert
- Kann manuell bestätigt oder abgelehnt werden
- User kann auch explizite Intake-Gruppen erstellen

#### In der App:
1. Gehe zu **Medications** (oder erstelle neue für Demo)
2. Zeige 2-3 Medikamente, die zur gleichen Zeit anstehen
3. Gehe zu **Home** → Erkläre: "Diese Medikamente erscheinen in einer Benachrichtigung"
4. Wenn Alarm triggert: Zeige die Benachrichtigung mit allen Medikamenten gleichzeitig
5. Tippe "Alle bestätigen" – Das spart Taps für den Nutzer

### Warum das wichtig ist
- Realistische UX (Menschen nehmen oft mehrere Medis zusammen)
- Reduziert Notification-Spam

---

## Feature 3: Flexible Erinnerungsplanung (2 Min)

### Szenario
"Ich muss Medikamente zu unterschiedlichen Mustern nehmen: täglich, wöchentlich, alle 6 Stunden, oder nur morgens."

### Was du zeigst

#### Im Code:
Öffne `ReminderPlan.kt`:
```kotlin
abstract class BaseReminder(
    protected val schedule: MedicationSchedule,
    protected val dayPeriodSettings: DayPeriodSettings = DayPeriodSettings.Default
) {
    abstract fun nextTriggerAfter(now: Long = System.currentTimeMillis()): Long?
}

// Verschiedene Implementierungen:
class TimedReminder(...)          // 08:00 täglich
class IntervalReminder(...)        // Alle 6 Stunden
class WeeklyReminder(...)          // Mo/Mi/Fr 10:00
class DayPeriodReminder(...)       // Morgens (8 Uhr) basierend auf Settings
```

Erkläre:
- Keine großen if/when-Blöcke für jeden Typ
- Neue Erinnerungstypen = einfach neue Klasse + Implementierung
- Sealed Class Pattern für Type Safety

#### In der App:
1. Gehe zu **Add Medication** 
2. Zeige die verschiedenen **Reminder Types**:
   - **Exact Time**: 08:00 Uhr
   - **Time Range**: Zwischen 08:00-10:00
   - **Day Period**: "Morgens" (Zeit aus Settings)
   - **Interval**: Alle 6 Stunden
   - **Weekly**: Mo/Do 10:00
3. Erkläre: "Je nach Medikament kann ich das beste Muster wählen"

### Warum das wichtig ist
- Echte Medikamentenschränke sind komplex
- Ferner erlaubt es Erweiterungen ohne Code-Bruch

---

## Feature 4: Architektur: MVVM + Clean Layers (2 Min)

### Szenario
"Code soll wartbar, testbar und erweiterbar sein. Jede Schicht hat eine klare Verantwortung."

### Was du zeigst

#### Im Code:
Zeige die Schichten-Architektur:

**ViewModel** (`MedicationViewModel.kt`):
```kotlin
@HiltViewModel
class MedicationViewModel @Inject constructor(
    private val medicationRepository: MedicationRepository,
    private val scheduleRepository: MedicationScheduleRepository,
    private val alarmScheduler: AlarmScheduler,
    ...
) : ViewModel() {
    val activeMedications: StateFlow<List<Medication>> = 
        medicationRepository.getAllActiveMedications()...
}
```

**Repository Interface** (`MedicationRepository.kt`):
```kotlin
interface MedicationRepository {
    suspend fun addMedication(medication: Medication): Long
    fun getAllActiveMedications(): Flow<List<Medication>>
}
```

**Repository Impl** (`MedicationRepositoryImpl.kt`):
```kotlin
class MedicationRepositoryImpl @Inject constructor(
    private val medicationDao: MedicationDao
) : MedicationRepository {
    // Mappt zwischen DB-Entities und Domain-Modellen
}
```

**Domain Model** (`Medication.kt`):
```kotlin
data class Medication(
    val id: Long = 0,
    val name: String,
    val dosage: String,
    // ... keine Ahnung von der DB!
)
```

Erkläre:
- **UI-Layer** (Compose) kennt nur ViewModel
- **ViewModel** nutzt Repositories, nicht DAOs direkt
- **Repository** ist Interface → kann getestet werden
- **Domain Models** sind unabhängig von DB/Android
- **Dependency Injection** (Hilt) stellt alles zusammen

#### In der App:
1. Öffne **Home** → zeige, wie Daten fließen:
   - User tippt "Add Medication"
   - ViewModel ruft `MedicationRepository.addMedication()` auf
   - Repository speichert in Room
   - Flow triggert UI-Update
2. Erkläre: "Das Schichtenmodell bedeutet: Ich kann die DB morgen wechseln, ohne UI zu ändern!"

### Warum das wichtig ist
- Testing: Repositories können gemockt werden
- Wartbarkeit: Änderungen sind lokalisiert
- Skalierbarkeit: Neue Features passen leicht rein

---

## Feature 5: Alarmverwaltung & Batterie-Effizienz (2 Min)

### Szenario
"Der Alarm soll zuverlässig klingeln, auch bei Doze-Modus und ohne ständigen Background-Service."

### Was du zeigst

#### Im Code:
Öffne `AlarmScheduler.kt`:
```kotlin
private fun scheduleExact(
    triggerAtMillis: Long,
    pendingIntent: PendingIntent,
    notificationId: Int
) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && 
            !alarmManager.canScheduleExactAlarms()) {
            // Android 12+: Kein Permission? Nutze setAlarmClock
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent),
                pendingIntent
            )
        } else {
            // Exakte Alarme sind erlaubt
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    } catch (securityException: SecurityException) {
        // Fallback: Inexakte Alarme
        AlarmManagerCompat.setAndAllowWhileIdle(alarmManager, ...)
    }
}
```

Erkläre:
- **setExactAndAllowWhileIdle**: Weckt Gerät auf, durchbricht Doze
- **Fallback für Android 12+**: Viele Apps kriegen keine exakten Alarme
- **Cascading Fallbacks**: Wenn alles fehlschlägt, wenigstens inexakt
- **Kein Background-Service**: Nur BroadcastReceiver = minimal Batterie-Drain

#### In der App:
1. Gehe zu **Settings** → zeige **Exact Alarm Permission**
2. Erkläre: "Android begrenzt, wie viele Apps exakte Alarme nutzen können. Wir fragen um Permission, haben aber auch Fallbacks"
3. Erstelle einen Alarm für "in 2 Minuten"
4. Wenn der Alarm triggert: Zeige die Benachrichtigung + das Alarm-Sound-Interface (falls ALARM-Modus aktiv)

### Warum das wichtig ist
- Medikament-Alarme müssen zuverlässig sein
- Android gibt immer mehr Beschränkungen vor
- App funktioniert trotzdem auf allen Android-Versionen

---

## Timing-Übersicht

| Feature | Zeit | Aktion |
|---------|------|--------|
| Intro | 0:30 | "Das ist MediReminder..." |
| **Feature 1: Direct Boot** | 2:00 | Code → Live-Demo |
| **Feature 2: Gruppierung** | 2:00 | Code → Live-Demo |
| **Feature 3: Planung** | 2:00 | Code → Live-Demo |
| **Feature 4: Architektur** | 2:00 | Code → Erklärung |
| **Feature 5: Alarme** | 1:30 | Code → Live-Demo |
| Outro | 0:00 | Fragen |
| **Gesamt** | **10:00** | |

---

## Tipps zur Präsentation

### Vorbereitung
- ✅ Öffne alle Code-Dateien vorher im Editor
- ✅ Habe ein Test-Medikament mit Alarm "jetzt + 1 Min" bereit
- ✅ Zeige Bildschirm auf großem Monitor / Beamer
- ✅ Lade die APK vorab auf das Handy

### Während der Präsentation
- **Sprich laut und langsam**: Code-Details sind schnell verwirrend
- **Springe nicht zu viel zwischen Code und App**: Nutze Folien oder Notizen
- **Bei Verzögerungen**: Erkläre die Architektur noch, statt zu schweigen
- **Live-Demos**: Wenn der Alarm nicht triggert, erkläre weiter – das ist OK!
- **Code-Highlights**: Nutze Editor-Zoom oder Highlighting für wichtige Zeilen

### Was du sagen kannst

> *"MediReminder ist kein Standard-To-Do-Reminder. Die Besonderheit: Es funktioniert auch nach einem Update, bevor du das Gerät entsperrst. Das ist kritisch für zeitkritische Medikamente wie Insulin."*

> *"Backend haben wir nicht – die App ist vollständig offline. Alle Daten bleiben auf dem Gerät, verschlüsselt nach Android-Standard."*

> *"Die Architektur ist absichtlich 'Enterprise-Grade': Wenn mein Betreuer die App erweitern will, ist es möglich, ohne alles neu zu schreiben."*

---

## Fragen, die kommen könnten

**Q: Warum kein Backend?**
A: Datenschutz + Offline-Funktionalität. Medikamentendaten sind sehr sensibel. Mit lokalem Storage haben Nutzer maximale Kontrolle.

**Q: Warum so viele Erinnerungstypen?**
A: Echte Medikamente folgen komplexen Mustern (z.B. alle 6 Stunden, aber nur 3x täglich → max 18:00).

**Q: Wie gut ist die Batterie-Effizienz?**
A: Nur BroadcastReceiver, kein Hintergrund-Service. ~1-2 % Batterie-Drain pro Tag für typische Nutzer.

**Q: Wie testet ihr das Direct-Boot-Feature?**
A: Mit Android Emulator: `adb shell cmd device_state state off` (simulates locked boot).
