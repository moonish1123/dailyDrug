# Suggested Commands (Darwin/MacOS)

## Build & Run
- **Build Debug APK**: `./gradlew assembleDebug`
- **Build Release APK**: `./gradlew assembleRelease`
- **Clean Build**: `./gradlew clean`
- **Install Debug**: `./gradlew installDebug`

## Testing
- **Run All Unit Tests**: `./gradlew test`
- **Run Specific Test**: `./gradlew test --tests "fully.qualified.TestName"`
- **Run UI/Instrumentation Tests**: `./gradlew connectedAndroidTest`

## Quality & Linting
- **Run Lint**: `./gradlew lint`
- **Lint Debug**: `./gradlew lintDebug`
- **Format Code**: `./gradlew ktlintFormat` (if configured)

## Database Debugging
- **Export DB**: `adb shell "run-as com.dailydrug cat /data/data/com.dailydrug/databases/daily_drug_db" > local_db.db`
- **Clear Data**: `adb shell pm clear com.dailydrug`

## Git
- **Status**: `git status`
- **Log**: `git log --oneline -n 10`
