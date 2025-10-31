# Repository Guidelines

## Project Structure & Module Organization
- `app/src/main/java/com/dailydrug`: Kotlin sources organized by layer (`domain`, `data`, `presentation`, `di`).
- `app/src/main/res`: Android resources including themes, strings, and layouts.
- `app/src/test/java`: JVM unit tests (Kotest/JUnit). Use matching package hierarchy when adding new suites.
- Specs and planning notes live at the repository root (`project_spec.md`, `PROJECT_SPEC.md`).

## Build, Test, and Development Commands
- `./gradlew :app:assembleDebug` – builds the debug APK and validates dependency wiring (Hilt, Room, WorkManager).
- `./gradlew :app:testDebugUnitTest` – executes JVM unit tests under the debug variant.
- Add `--tests "fully.qualified.TestName"` to focus on a single test class when iterating.

## Coding Style & Naming Conventions
- Kotlin code follows the official style guide: 4-space indentation, trailing commas where helpful, expressive property names.
- Compose composables use `PascalCase` and live under `presentation`. ViewModels end with `ViewModel`.
- Use the provided Material3 blue palette from `presentation/theme`. New colors should integrate through that package.
- Apply `ktlint` (configured via IDE) before committing; keep files ASCII unless localization requires otherwise.

## Testing Guidelines
- Prefer JUnit5-style tests; Compose UI tests belong under `androidTest`.
- Name test methods with backticks and natural language (e.g., ``fun `schedule skips rest days`()``) for readability.
- Run `./gradlew :app:testDebugUnitTest` locally before opening a PR; add targeted tests whenever adding business logic.

## Commit & Pull Request Guidelines
- Commit messages follow `[Phase N] 작업 내용` to mirror the project plan; keep commits scoped to a coherent task.
- PRs should describe scope, testing performed, and link to relevant phases/tasks in the spec. Include screenshots or screen recordings for UI work.
- Ensure CI (or local equivalent) is green before requesting review; highlight any follow-up TODOs in the PR body.

## Current Implementation Status
- Phase 0 complete: dependencies configured, Hilt entry points in place, and Compose navigation scaffolded with themed placeholder screens.
- Phase 1 complete: Room entities/DAOs/database implemented with Hilt wiring and `MedicationRepositoryImpl` providing domain mappings.
- Phase 2 complete: domain models defined, repository contract specified, use cases implemented (get daily doses, record intake, create schedules, compute take/rest patterns, schedule notifications).
- Phase 3 functional: notification helper, permission prompts, alarm receiver with quick actions, ReminderScheduler, and Hilt-enabled workers handling re-alerts and daily scheduling.
- Phase 4 complete: ViewModels + Compose screens for 홈/스케줄/상세 구현, 권한 UI 연동, Hilt 기반 네비게이션 파라미터 처리.
- Unit coverage: `CalculateSchedulePatternsUseCaseTest` validates cycle logic; rerun with `./gradlew :app:testDebugUnitTest --tests "com.dailydrug.domain.usecase.CalculateSchedulePatternsUseCaseTest"`.
- Upcoming work: integrate presentation layer with ViewModels, persist UI state, and build end-to-end flows for schedule creation and detail screens.
