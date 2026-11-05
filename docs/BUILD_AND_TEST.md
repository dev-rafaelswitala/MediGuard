# MediGuard - Build & Testing Guide

## Building the Application

### Prerequisites
- Android Studio Hedgehog or newer
- Android SDK 36 (API 36)
- Gradle 8.13.2 or newer
- JDK 11+

### Build Commands

#### Debug Build
```bash
./gradlew assembleDebug
# Creates: app/build/outputs/apk/debug/app-debug.apk
```

#### Release Build
```bash
./gradlew assembleRelease
# Creates: app/build/outputs/apk/release/app-release.apk
# ProGuard/R8 minification enabled
```

#### Run on Emulator/Device
```bash
./gradlew installDebug
./gradlew runDebug  # Build and install

# Or use Android Studio:
# Run → Select emulator/device → OK
```

#### Clean Build
```bash
./gradlew clean
./gradlew build
```

---

## Testing Scenarios

### 1. Basic Functionality
- [ ] **Add Medication**
  - Tap FAB on Home screen
  - Fill in: Name, Dosage, Unit, Description
  - Tap "Add Medication"
  - Verify: Card appears in home list

- [ ] **View Medications**
  - Home screen shows medication cards
  - Each card displays: Name, Dosage, Unit, Description
  - Delete button on each card

- [ ] **Delete Medication**
  - Tap delete icon on any medication card
  - Verify: Card is removed from list

### 2. Navigation
- [ ] **Home Screen**
  - FAB opens AddMedication
  - Bottom nav options accessible

- [ ] **Add Medication Screen**
  - Back button returns to Home
  - Form validation (name/dosage required)

- [ ] **History Screen**
  - Back button returns to Home
  - Shows intake history entries
  - Displays status, date, medication name

- [ ] **Settings Screen**
  - Back button returns to Home
  - Shows language/notification info

### 3. Direct Boot Support

#### Testing Direct Boot (Real Device)
1. Enable encryption if not already encrypted
2. Enable "Lock immediately" in settings
3. Restart device
4. Do NOT unlock
5. Verify app shows on lock screen (if scheduled alarms exist)

#### Testing Direct Boot (Emulator)
```bash
# Boot in encrypted state
adb shell getprop ro.crypto.state
adb shell getprop ro.crypto.type
```

### 4. Notifications

#### Before Unlock
- [ ] Privacy-safe notification appears
- [ ] Shows only: "Time for your medication!"
- [ ] Does NOT show medication details
- [ ] Tap notification opens app in locked state

#### After Unlock
- [ ] Full notification appears
- [ ] Shows: "Please take [medication name]"
- [ ] Shows: Dosage information
- [ ] Can swipe to confirm intake

### 5. Alarm Scheduling

#### Exact Alarm Test
```bash
# Set medication reminder for 1 minute from now
# Device must have SCHEDULE_EXACT_ALARM permission
adb shell dumpsys alarm
```

#### Check Scheduled Alarms
```bash
adb shell dumpsys alarm | grep mediguard
```

#### Boot Receiver Test
1. Schedule an alarm for future time
2. Restart device
3. Wait for alarm time
4. Verify alarm fires (notification appears)
5. Verify: Alarm time = Scheduled time (exact delivery)

### 6. Database Testing

#### View Room Database
```bash
# Copy database file
adb pull /data/data/com.rafaelswitala.mediguard/databases/mediguard_db

# Use DB Browser for SQLite to inspect
# Tables: medications, medication_schedules, intake_history
```

#### Check DataStore
```bash
# View Direct Boot DataStore
adb shell dumpsys datastore
adb shell ls -la /data/misc/apexdata/com.android.systemui/
```

### 7. Localization Testing

#### Switch Language
1. Android Settings → System → Languages & input → Languages
2. Move German to top (or change to German)
3. Reopen MediGuard
4. Verify: All strings are in German
5. Change back to English
6. Verify: All strings are in English

#### Strings to Check
- Home: "No medications yet"
- Add: "Add Medication", "Medication Name", "Dosage"
- History: "Intake History", "No intake history yet"
- Settings: "Settings", "Language"
- Notifications: "Time for your medication!"

### 8. UI/UX Testing

#### Dark Mode
```bash
# Enable dark mode on device
Settings → Display → Dark theme → On

# In emulator
View → Theme → Dark
```
- [ ] Colors are readable in dark mode
- [ ] Text contrast is sufficient

#### Large Text
- [ ] Form fields accept long medication names
- [ ] Card wraps text properly
- [ ] No text is truncated

#### Touch Targets
- [ ] Delete button is large enough (48dp min)
- [ ] FAB is easily tappable
- [ ] Form inputs have adequate spacing

### 9. Performance Testing

#### Memory Usage
```bash
adb shell dumpsys meminfo com.rafaelswitala.mediguard
```
- [ ] Initial memory < 50MB
- [ ] No memory leaks after repeated add/delete

#### Battery Usage
- [ ] AlarmManager uses exact alarms (battery efficient)
- [ ] No continuous services running
- [ ] No wake locks held unnecessarily

#### Database Performance
- [ ] List displays smoothly with 100+ medications
- [ ] No ANR (Application Not Responding)
- [ ] Database queries complete quickly

### 10. Permissions Testing

#### Check Permissions
```bash
adb shell pm dump com.rafaelswitala.mediguard | grep permission
```

#### Grant Permissions Manually
```bash
adb shell pm grant com.rafaelswitala.mediguard \
  android.permission.SCHEDULE_EXACT_ALARM
adb shell pm grant com.rafaelswitala.mediguard \
  android.permission.POST_NOTIFICATIONS
```

- [ ] Request SCHEDULE_EXACT_ALARM on first alarm
- [ ] Request POST_NOTIFICATIONS on first notification
- [ ] App works without exact alarm (fallback to inexact)
- [ ] App shows notification permission request

### 11. Error Handling

#### Database Error
- [ ] If room DB fails, app shows error message
- [ ] Can retry and recover

#### Notification Permission Missing
- [ ] App gracefully handles missing POST_NOTIFICATIONS
- [ ] Still shows alarm even if notification fails

#### Invalid Input
- [ ] Empty medication name → "Name is required"
- [ ] Empty dosage → "Dosage is required"
- [ ] Cancel form → Returns to Home without save

### 12. State Persistence

#### Configuration Changes
1. Add medication
2. Rotate device
3. Verify: Medication still there
4. Verify: UI state preserved (no data loss)

#### App Termination
1. Add medication
2. Force stop app (Settings → Apps → MediGuard → Force Stop)
3. Reopen app
4. Verify: Medication is still in list

---

## Debug Commands

### Logcat Filtering
```bash
# View MediGuard logs
adb logcat *:S mediguard:V

# View Room database logs
adb logcat *:S room_db:V

# View component logs
adb logcat ActivityManager:I
```

### Simulate Alarms
```bash
# Set test alarm (requires mock permissions in debug)
adb shell am broadcast -a com.rafaelswitala.mediguard.alarm.TRIGGER \
  --es medication_name "Test Medication" --el schedule_id 1
```

### Device Lock Status
```bash
adb shell locksettings get-pin
adb shell dumpsys lock_settings
```

### Direct Boot State
```bash
# Check if device is encrypted
adb shell getprop ro.crypto.state
# encrypted or unencrypted

# Check crypto type
adb shell getprop ro.crypto.type
aes256 or block
```

---

## CI/CD Integration

### GitHub Actions Example
```yaml
name: Build MediGuard
on: [push, pull_request]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - uses: actions/setup-java@v2
        with:
          java-version: '11'
      - run: ./gradlew build --stacktrace
      - run: ./gradlew assembleDebug
      - uses: actions/upload-artifact@v2
        with:
          name: app-debug.apk
          path: app/build/outputs/apk/debug/
```

---

## Troubleshooting

### Build Issues

#### Gradle Sync Failed
```bash
./gradlew clean
./gradlew --refresh-dependencies
# Rebuild in Android Studio
```

#### Hilt Compilation Error
- Ensure all @HiltViewModel classes have @Inject constructor
- Check @AndroidEntryPoint is present on Activities/Receivers
- Clean build cache: Build → Clean Project

#### Room Schema Error
- Make sure all @Entity classes are in @Database entities list
- Version number matches expected schema
- exportSchema = true for debugging

### Runtime Issues

#### Crash on Alarm
```
java.lang.SecurityException: "android.permission.SCHEDULE_EXACT_ALARM"
```
- Grant permission: adb shell pm grant ... SCHEDULE_EXACT_ALARM
- Or test fallback inexact alarm logic

#### Notification Not Showing
```
NotificationCompat.Builder#build() NOT called
```
- Check: notification.build() is called before notify()
- Verify channel is created before posting

#### Direct Boot Not Working
- Device must be encrypted
- Ensure manifest has `android:directBootAware="true"`
- Check DataStore is using device-protected context

---

## Release Checklist

Before releasing to Play Store:

- [ ] Version code incremented
- [ ] Version name updated (semantic versioning)
- [ ] ProGuard/R8 minification enabled
- [ ] Test on real device (not just emulator)
- [ ] Direct Boot tested on real encrypted device
- [ ] All strings localized (EN, DE)
- [ ] No debug logs in production
- [ ] Certificate properly configured
- [ ] Privacy policy prepared
- [ ] Screenshots for Play Store
- [ ] Release notes written

---

**Happy testing! 🚀**
