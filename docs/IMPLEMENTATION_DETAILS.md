# MediGuard - Key Implementation Details

## Direct Boot Support Implementation

### Manifest Configuration
```xml
android:directBootAware="true"  <!-- App is Direct Boot aware -->

<receiver android:name=".alarm.BootReceiver"
    android:directBootAware="true">
    <intent-filter>
        <action android:name="android.intent.action.LOCKED_BOOT_COMPLETED" />
        <action android:name="android.intent.action.BOOT_COMPLETED" />
    </intent-filter>
</receiver>
```

### Storage Strategy
- **Room Database (CE)**: Medications, schedules, history - encrypted, accessible only after unlock
- **DataStore (DE)**: Alarm metadata - device encrypted, accessible immediately after boot
- Can show alarms before unlock using DE data, full details after unlock with CE data

---

## Polymorphism in Scheduling

### Sealed Class for Type Safety
```kotlin
sealed class ScheduleType {
    data object ExactTime : ScheduleType()
    data object TimeRange : ScheduleType()
    data object DayOfWeek : ScheduleType()
    data object Interval : ScheduleType()
    data object DayPeriod : ScheduleType()
}
```

### Supporting Models
```kotlin
data class ExactTimeSchedule(val hour: Int, val minute: Int)
data class TimeRangeSchedule(...)
data class DayPeriodSchedule(val period: DayPeriod)
enum class DayPeriod { MORNING, NOON, EVENING, NIGHT }
```

This allows extensible scheduling without if/when statements.

---

## Privacy-Safe Notifications

### Implementation
```kotlin
suspend fun showMedicationNotification(...) {
    val isDeviceLocked = isDeviceLocked()
    
    val notification = if (isDeviceLocked) {
        createLockedNotification()  // Generic: "Time for medication!"
    } else {
        createUnlockedNotification()  // Full: "Please take 20mg Ibuprofen"
    }
}

private fun isDeviceLocked(): Boolean {
    val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) 
        as android.app.KeyguardManager?
    return keyguardManager?.isDeviceLocked ?: false
}
```

### Channels
- `medication_locked` - For before unlock (minimal info)
- `medication_unlocked` - For after unlock (full details)

---

## Alarm Scheduling with ExactAndAllowWhileIdle

### Key Implementation
```kotlin
fun scheduleAlarm(schedule: MedicationSchedule, medicationName: String, 
                  nextAlarmTime: Long) {
    val intent = Intent(context, AlarmReceiver::class.java).apply {
        putExtra("medication_id", schedule.medicationId)
        putExtra("schedule_id", schedule.id)
        putExtra("medication_name", medicationName)
    }

    val pendingIntent = PendingIntent.getBroadcast(
        context, schedule.id.toInt(), intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE  // IMMUTABLE!
    )

    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(...)  // Exact
            } else {
                AlarmManagerCompat.setAndAllowWhileIdle(...)  // Fallback
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(...)  // Pre-S devices
        }
    } catch (e: SecurityException) {
        AlarmManagerCompat.setAndAllowWhileIdle(...)  // No permission fallback
    }
}
```

### Critical Points
✅ IMMUTABLE PendingIntent flags (Android 12+ requirement)
✅ Check canScheduleExactAlarms() on Android 12+
✅ Fallback for missing SCHEDULE_EXACT_ALARM permission
✅ Works across device reboots via BootReceiver

---

## Repository Pattern with Mapping

### Domain vs Data Layer
```kotlin
// Domain
interface MedicationRepository {
    suspend fun addMedication(medication: Medication): Long
    fun getAllActiveMedications(): Flow<List<Medication>>
}

// Data
class MedicationRepositoryImpl(
    private val medicationDao: MedicationDao
) : MedicationRepository {
    override suspend fun addMedication(medication: Medication): Long =
        medicationDao.insert(medication.toEntity())
    
    override fun getAllActiveMedications(): Flow<List<Medication>> =
        medicationDao.getAllActiveMedications().map { entities ->
            entities.map { it.toDomain() }
        }
    
    private fun Medication.toEntity(): MedicationEntity = 
        MedicationEntity(
            id = id, name = name, dosage = dosage,
            dosageUnit = dosageUnit, description = description,
            createdAt = createdAt, isActive = isActive
        )
    private fun MedicationEntity.toDomain(): Medication = 
        Medication(id, name, dosage, ...)
}
```

Benefits:
- Domain layer independence from database
- Easy to test with mock repositories
- Database can change without affecting domain logic

---

## MVVM State Management

### ViewModel Pattern
```kotlin
@HiltViewModel
class MedicationViewModel @Inject constructor(
    private val medicationRepository: MedicationRepository,
    private val addMedicationUseCase: AddMedicationUseCase
) : ViewModel() {

    val activeMedications: StateFlow<List<Medication>> = 
        medicationRepository.getAllActiveMedications()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    fun addMedication(medication: Medication) {
        viewModelScope.launch {
            try {
                addMedicationUseCase(medication)
                _uiState.value = _uiState.value.copy(
                    successMessage = "Medication added"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Error: ${e.message}"
                )
            }
        }
    }
}
```

### In Composable
```kotlin
@Composable
fun HomeScreen(viewModel: MedicationViewModel) {
    val medications by viewModel.activeMedications.collectAsState()
    
    LazyColumn {
        items(medications) { medication ->
            MedicationCard(medication)
        }
    }
}
```

---

## Room Database Configuration

### Relationships
```kotlin
@Database(
    entities = [
        MedicationEntity::class,
        MedicationScheduleEntity::class,
        IntakeHistoryEntity::class
    ],
    version = 1, exportSchema = true
)
abstract class AppDatabase : RoomDatabase()
```

### Foreign Keys
```kotlin
@Entity(
    tableName = "medication_schedules",
    foreignKeys = [
        ForeignKey(
            entity = MedicationEntity::class,
            parentColumns = ["id"],
            childColumns = ["medicationId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class MedicationScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicationId: Long,  // Foreign key
    ...
)
```

### Flow Queries
```kotlin
@Dao
interface MedicationDao {
    @Query("SELECT * FROM medications WHERE isActive = 1")
    fun getAllActiveMedications(): Flow<List<MedicationEntity>>
}
```

---

## Hilt Dependency Injection

### AppModule
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase = Room.databaseBuilder(
        context, AppDatabase::class.java,
        AppDatabase.DATABASE_NAME
    ).build()

    @Provides
    @Singleton
    fun provideMedicationRepository(
        medicationDao: MedicationDao
    ): MedicationRepository = MedicationRepositoryImpl(medicationDao)
}
```

### Usage
```kotlin
@AndroidEntryPoint  // Enable Hilt in Activity
class MainActivity : ComponentActivity()

@AndroidEntryPoint  // Enable Hilt in Receiver
class AlarmReceiver : BroadcastReceiver() {
    @Inject
    lateinit var notificationManager: MedicationNotificationManager
}

@HiltViewModel  // Enable Hilt in ViewModel
class MedicationViewModel @Inject constructor(
    private val repository: MedicationRepository
) : ViewModel()
```

---

## Jetpack Compose Navigation

### Route Definition
```kotlin
sealed class Route(val route: String) {
    data object Home : Route("home")
    data object AddMedication : Route("add_medication")
    data object History : Route("history")
    data object Settings : Route("settings")
}
```

### Navigation Setup
```kotlin
@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Route.Home.route
    ) {
        composable(Route.Home.route) {
            HomeScreen(onNavigateToAdd = {
                navController.navigate(Route.AddMedication.route)
            })
        }
        // ... other composables
    }
}
```

### In MainActivity
```kotlin
val navController = rememberNavController()
AppNavigation(navController = navController)
```

---

## System Language Support

### Localization Files
- `values/strings.xml` - English (44 strings)
- `values-de/strings.xml` - German (44 strings)

### String Usage
```xml
<!-- English -->
<string name="app_name">MediGuard</string>
<string name="medication_name">Medication Name</string>

<!-- German (values-de) -->
<string name="app_name">MediGuard</string>
<string name="medication_name">Medikamentenname</string>
```

### In Compose
```kotlin
stringResource(R.string.medication_name)
```

Automatically uses device language (English or German).

---

## Color Scheme for Medical App

### Palette
- **Primary**: #2196F3 (Medical Blue)
- **Secondary**: #00BCD4 (Cyan - Complementary)
- **Success**: #4CAF50 (Green - Confirmation)
- **Warning**: #FFC107 (Amber - Alert)
- **Error**: #F44336 (Red - Errors)

### Dynamic Colors (API 31+)
```kotlin
val colorScheme = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        if (darkTheme) dynamicDarkColorScheme(context)
        else dynamicLightColorScheme(context)
    }
    darkTheme -> DarkColorScheme
    else -> LightColorScheme
}
```

---

## Build Configuration for Production

### ProGuard/R8 Enabled
```gradle
buildTypes {
    release {
        isMinifyEnabled = true  // Enable code shrinking
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
    }
}
```

### Java Compatibility
```gradle
compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}
kotlinOptions {
    jvmTarget = "11"
}
```

---

## Testing Checklist

```
✓ Add medication form
✓ List medications on home screen
✓ Delete medication
✓ Navigate between screens
✓ Dark mode theme
✓ German localization
✓ Exact alarm scheduling
✓ Notification before unlock (generic)
✓ Notification after unlock (detailed)
✓ BootReceiver on device reboot
✓ Room database persistence
✓ DataStore for Direct Boot
✓ Hilt dependency injection
✓ ViewModel state management
✓ Error handling with UI messages
```

---

**All technical specifications implemented and ready for testing!**
