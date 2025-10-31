# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**DailyDrug (매일 약먹기)** is a medication reminder Android app that helps users maintain consistent medication schedules with persistent notifications until doses are taken.

**Key Features**:
- Medication schedule management with flexible patterns (e.g., "5 days on, 1 day off")
- Persistent hourly reminder notifications until medication is taken
- Visual status indicators (red for missed, blue for completed)
- Medication history tracking with calendar views

**Tech Stack**: Kotlin, Jetpack Compose, Material3, Room, Hilt, WorkManager/AlarmManager, Coroutines/Flow

## Build & Development Commands

### Basic Commands
```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Install debug build to connected device
./gradlew installDebug

# Run app on emulator/device
./gradlew installDebug && adb shell am start -n com.dailydrug/.MainActivity

# Clean build
./gradlew clean
```

### Testing
```bash
# Run all unit tests
./gradlew test

# Run single test class
./gradlew test --tests "com.dailydrug.domain.usecase.CalculateSchedulePatternsUseCaseTest"

# Run all instrumentation tests
./gradlew connectedAndroidTest

# Run tests with coverage
./gradlew testDebugUnitTest jacocoTestReport

# Run specific test method
./gradlew test --tests "*.shouldCalculateDailyMedication"
```

### Code Quality
```bash
# Lint checks
./gradlew lint

# Generate lint report (build/reports/lint-results-debug.html)
./gradlew lintDebug

# Format code with ktlint (if configured)
./gradlew ktlintFormat
```

### Database Management
```bash
# View Room database (requires device/emulator)
adb shell "run-as com.dailydrug cat /data/data/com.dailydrug/databases/daily_drug_db" > local_db.db

# Clear app data (resets database)
adb shell pm clear com.dailydrug
```

## Architecture

### Clean Architecture Layers

The codebase follows **Clean Architecture** with strict layer separation:

```
┌─────────────────────────────────────────┐
│  Presentation Layer                     │
│  - Compose UI Screens                   │
│  - ViewModels (state management)        │
│  - Navigation (AppNavHost)              │
└──────────────┬──────────────────────────┘
               │ depends on
┌──────────────▼──────────────────────────┐
│  Domain Layer (Business Logic)          │
│  - UseCases (single responsibility)     │
│  - Repository Interfaces                │
│  - Domain Models (pure Kotlin)          │
└──────────────┬──────────────────────────┘
               │ depends on
┌──────────────▼──────────────────────────┐
│  Data Layer                              │
│  - Repository Implementations           │
│  - Room Database (Entity, DAO)          │
│  - Type Converters (LocalDate/Time)     │
│  - WorkManager Workers                  │
└─────────────────────────────────────────┘
```

**Dependency Rule**: Inner layers never depend on outer layers. Domain layer is pure Kotlin with no Android dependencies.

### Package Structure

```
com.dailydrug/
├── data/
│   ├── local/
│   │   ├── dao/              # Room DAO interfaces
│   │   ├── entity/           # Room entities (@Entity)
│   │   ├── converter/        # TypeConverters (LocalDate, LocalTime, List<LocalTime>)
│   │   └── database/         # AppDatabase singleton
│   ├── repository/           # Repository implementations
│   └── worker/               # WorkManager background tasks
│       ├── DailyScheduleWorker      # Daily midnight schedule generation
│       └── MedicationReminderWorker # Notification triggers
├── domain/
│   ├── model/                # Pure domain models (no Room annotations)
│   ├── repository/           # Repository interfaces
│   └── usecase/              # Business logic
│       ├── CalculateSchedulePatternsUseCase  # Core medication cycle logic
│       ├── GetTodayMedicationsUseCase
│       ├── RecordMedicationUseCase
│       ├── CreateScheduleUseCase
│       └── ScheduleNotificationUseCase
├── presentation/
│   ├── main/                 # Home screen (today's medications)
│   ├── schedule/             # Schedule input/edit screen
│   ├── detail/               # Medicine detail/history screen
│   ├── settings/             # Settings screen
│   ├── component/            # Reusable Composables
│   ├── navigation/           # AppNavHost & sealed route objects
│   └── theme/                # Material3 theme (blue color scheme)
├── di/                       # Hilt dependency injection modules
│   ├── DatabaseModule
│   ├── RepositoryModule
│   └── UseCaseModule
├── DailyDrugApplication.kt   # @HiltAndroidApp entry point
└── MainActivity.kt           # Compose setContent entry
```

### Core Domain Logic: Medication Pattern Calculation

The most critical business logic is **medication cycle calculation** in `CalculateSchedulePatternsUseCase`:

**Problem**: Determine if medication should be taken on a target date given:
- `startDate`: First dose date
- `takeDays`: Consecutive days to take medication (e.g., 5)
- `restDays`: Days to rest after takeDays (e.g., 1)

**Algorithm**:
```kotlin
fun shouldTakeMedicationOn(
    startDate: LocalDate,
    targetDate: LocalDate,
    takeDays: Int,
    restDays: Int
): Boolean {
    if (restDays == 0) return true // Daily medication

    val daysSinceStart = ChronoUnit.DAYS.between(startDate, targetDate)
    val cycleLength = takeDays + restDays
    val dayInCycle = (daysSinceStart % cycleLength).toInt()

    return dayInCycle < takeDays
}
```

**Example**: "5 days on, 1 day off" pattern starting Jan 1
- Jan 1-5: Take (days 0-4 of cycle)
- Jan 6: Rest (day 5 of cycle)
- Jan 7-11: Take (days 0-4 of next cycle)

This logic is **heavily tested** in `CalculateSchedulePatternsUseCaseTest` with multiple edge cases.

### Notification System Architecture

**Two-tier notification system** for precision:

1. **WorkManager** (`DailyScheduleWorker`):
   - Runs daily at midnight
   - Generates next day's medication records in Room DB
   - Schedules exact-time alarms via AlarmManager
   - Survives device reboots

2. **AlarmManager** (`MedicationAlarmReceiver`):
   - Triggers at exact medication times
   - Shows high-priority notifications with sound/vibration
   - Schedules hourly re-notifications if not taken
   - Cancels reminders when medication is recorded

**Permissions Required**:
- `POST_NOTIFICATIONS` (Android 13+): Runtime request
- `SCHEDULE_EXACT_ALARM`: Deep link to system settings

### Data Flow Pattern

**Typical flow for recording medication**:
```
User taps "Take" button
    ↓
MainScreen (UI Event)
    ↓
MainViewModel.recordMedication(recordId)
    ↓
RecordMedicationUseCase.invoke(recordId)
    ↓
MedicationRepository.updateRecord(...)
    ↓
Room DAO.update(...)
    ↓
Flow<List<MedicationRecord>> emits update
    ↓
MainViewModel collects → updates UiState
    ↓
MainScreen recomposes with new data
    ↓
AlarmManager cancels pending notifications for recordId
```

**Key Pattern**: Use `Flow` from Room for reactive UI updates. ViewModels expose `StateFlow<UiState>` consumed by Composables.

### Type Converters (Critical for Room)

Room cannot store `LocalDate`, `LocalTime`, `LocalDateTime`, or `List<LocalTime>` directly. **Type Converters** in `data/local/converter/DateTimeConverters.kt` handle serialization:

```kotlin
@TypeConverter
fun fromLocalDate(date: LocalDate?): Long? =
    date?.toEpochDay()

@TypeConverter
fun toLocalDate(epochDay: Long?): LocalDate? =
    epochDay?.let { LocalDate.ofEpochDay(it) }

@TypeConverter
fun fromTimeList(times: List<LocalTime>): String =
    times.joinToString(",") { it.toString() }

@TypeConverter
fun toTimeList(data: String): List<LocalTime> =
    data.split(",").map { LocalTime.parse(it) }
```

**Register in AppDatabase**:
```kotlin
@Database(entities = [...], version = 1)
@TypeConverters(DateTimeConverters::class)
abstract class AppDatabase : RoomDatabase()
```

## Development Workflow

### Adding a New Feature

**Follow phased implementation** (see PROJECT_SPEC.md):

1. **Domain First**: Define domain models and UseCase interface
2. **Data Layer**: Implement repository, add DAOs if needed
3. **Presentation**: Create ViewModel → Screen Composable
4. **DI Wiring**: Add providers to Hilt modules
5. **Testing**: Write unit tests for UseCase, UI tests for Screen

**Example: Adding medication notes**
```
Phase 1: Add `note: String` to MedicationRecord entity
Phase 2: Create UpdateMedicationNoteUseCase
Phase 3: Update RecordMedicationUseCase to accept notes
Phase 4: Add note input field to MainScreen
Phase 5: Write tests for note persistence
```

### Testing Strategy

**Unit Tests** (80%+ coverage target):
- **Domain Layer**: All UseCase logic (pure Kotlin, fast)
- **ViewModels**: State transformations with test coroutines

**Integration Tests**:
- **DAO operations**: Use in-memory Room database
- **Repository**: Verify DAO ↔ Repository contract

**UI Tests** (Compose Testing):
- **Screen rendering**: Verify UI elements present
- **User interactions**: Button clicks, text input
- **Navigation flows**: Screen transitions

**Example test pattern**:
```kotlin
// Unit test
@Test
fun `should calculate 5-day-1-rest pattern correctly`() {
    val useCase = CalculateSchedulePatternsUseCase()
    val startDate = LocalDate.of(2024, 1, 1)

    // Days 0-4: should take
    assertTrue(useCase.shouldTakeMedicationOn(startDate, startDate.plusDays(0), 5, 1))

    // Day 5: rest day
    assertFalse(useCase.shouldTakeMedicationOn(startDate, startDate.plusDays(5), 5, 1))

    // Day 6: cycle restarts
    assertTrue(useCase.shouldTakeMedicationOn(startDate, startDate.plusDays(6), 5, 1))
}

// UI test
@Test
fun mainScreen_showsTodayMedications() {
    composeTestRule.setContent {
        MainScreen(...)
    }

    composeTestRule
        .onNodeWithText("약 이름")
        .assertIsDisplayed()
        .performClick()
}
```

### Dependency Injection with Hilt

**Module organization**:
- `DatabaseModule`: Provides AppDatabase, DAOs
- `RepositoryModule`: Binds repository interfaces to implementations
- `UseCaseModule`: Provides UseCase instances (constructor injection preferred)

**Common patterns**:
```kotlin
// Providing singleton database
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "daily_drug_db")
            .build()
}

// Binding repository interface
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindMedicationRepository(
        impl: MedicationRepositoryImpl
    ): MedicationRepository
}
```

**ViewModel injection**:
```kotlin
@HiltViewModel
class MainViewModel @Inject constructor(
    private val getTodayMedicationsUseCase: GetTodayMedicationsUseCase
) : ViewModel()
```

**Composable injection**:
```kotlin
@Composable
fun MainScreen(viewModel: MainViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // ...
}
```

## Common Development Tasks

### Adding a New Medication Schedule

1. User inputs data in `ScheduleInputScreen`
2. `ScheduleInputViewModel` validates inputs
3. Calls `CreateScheduleUseCase` with `CreateScheduleParams`
4. UseCase creates `Medicine` + `MedicationSchedule` entities
5. Repository saves to Room database
6. `DailyScheduleWorker` picks up schedule at next midnight
7. Generates `MedicationRecord` entries for following day
8. Schedules AlarmManager notifications for each time slot

### Handling Midnight Transitions

**Challenge**: User might open app at 11:59 PM, and date changes to next day

**Solution**:
- `MainScreen` observes `Flow<LocalDate>` from `LocalDateProvider`
- When date changes, `MainViewModel` reloads today's medications
- Use `LaunchedEffect(key = selectedDate)` to trigger reload

### Notification Cancellation

**When to cancel**:
- User marks medication as taken
- User skips medication
- Next day begins (midnight)

**Implementation**:
```kotlin
// In RecordMedicationUseCase
suspend fun invoke(recordId: Long, isTaken: Boolean) {
    repository.updateMedicationRecord(recordId, isTaken, LocalDateTime.now())
    alarmManager.cancel(recordId) // Cancel pending notifications
}
```

## Troubleshooting

### Room Database Issues

**Problem**: `IllegalStateException: Cannot access database on main thread`
**Solution**: Use `suspend` functions or `.allowMainThreadQueries()` (only for debugging)

**Problem**: Type converter not found for `LocalDate`
**Solution**: Verify `@TypeConverters(DateTimeConverters::class)` on `@Database` class

**Problem**: Migration error on schema change
**Solution**:
```kotlin
Room.databaseBuilder(...)
    .fallbackToDestructiveMigration() // DEV ONLY - destroys data
    .build()
```

### Notification Not Showing

**Check**:
1. Permission granted: `POST_NOTIFICATIONS` (Settings → Apps → DailyDrug → Notifications)
2. Exact alarm permission: `SCHEDULE_EXACT_ALARM` (Settings → Apps → Special access)
3. Notification channel created: Verify in NotificationHelper
4. WorkManager scheduled: Check `adb shell dumpsys jobscheduler` for pending jobs
5. AlarmManager scheduled: Use `adb shell dumpsys alarm | grep dailydrug`

### Hilt Dependency Injection Errors

**Problem**: `Dagger` missing binding error
**Solution**:
1. Verify `@HiltAndroidApp` on Application class
2. Check module is `@InstallIn(SingletonComponent::class)`
3. Rebuild project (`./gradlew clean build`)
4. Invalidate caches (Android Studio → File → Invalidate Caches)

## Phase Implementation Status

Reference PROJECT_SPEC.md Section 7 for detailed phase breakdowns.

**Current Status**:
- ✅ Phase 0: Project setup (Hilt, Compose, Navigation configured)
- 🚧 Phase 1: Data layer (partial - entities defined, DAOs needed)
- 🚧 Phase 2: Domain layer (partial - UseCases stubbed)
- ⏳ Phase 3: Notification system (pending)
- 🚧 Phase 4: UI layer (screens created, ViewModels needed)
- ⏳ Phase 5: Integration & testing (pending)

**Next Priorities**:
1. Complete Room DAOs and database setup
2. Implement repository layer with actual data operations
3. Wire UseCases with real repository dependencies
4. Implement ViewModels with state management
5. Set up notification infrastructure

## Code Conventions

### Naming
- **Screens**: `MainScreen`, `ScheduleInputScreen` (PascalCase + "Screen" suffix)
- **ViewModels**: `MainViewModel` (PascalCase + "ViewModel" suffix)
- **UseCases**: `GetTodayMedicationsUseCase` (Verb + "UseCase" suffix)
- **Repositories**: `MedicationRepository` (Noun + "Repository" suffix)
- **DAOs**: `MedicineDao` (Entity name + "Dao" suffix)

### File Organization
- **One screen = one file**: `MainScreen.kt` contains only MainScreen composable
- **UiState in separate file**: `MainUiState.kt` unless trivial
- **Package by feature**: `presentation/main/` contains all main screen files

### Compose Best Practices
- **Stateless composables**: Pass state and callbacks as parameters
- **State hoisting**: Manage state in ViewModel, pass down to UI
- **remember for performance**: Cache expensive computations
- **LaunchedEffect for side effects**: Network calls, navigation
- **key parameters in lists**: Use unique keys in LazyColumn items

### Kotlin Conventions
- **Immutability**: Prefer `val` over `var`, use `data class`
- **Null safety**: Avoid `!!`, use `?.let`, `?:` Elvis operator
- **Coroutines**: Prefer `Flow` over `LiveData`, structured concurrency
- **Extension functions**: Use for utility functions (e.g., `LocalDate.isToday()`)

## Related Documentation

- **PROJECT_SPEC.md**: Comprehensive project specification with detailed requirements
- **project.md**: Original Korean project brief
- **app/build.gradle.kts**: Dependency versions and build configuration
- **gradle/libs.versions.toml**: Centralized version catalog
