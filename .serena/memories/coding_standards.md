# Coding Standards & Conventions

## Architecture Guidelines
- **Clean Architecture**: 
  - **Domain Layer**: Pure Kotlin, no Android dependencies. Contains UseCases, Models, Repository Interfaces.
  - **Data Layer**: Implements Repositories, handles Data Sources (Room, API).
  - **Presentation Layer**: UI (Compose) and State Holders (ViewModel).
- **Dependency Rule**: Presentation -> Domain <- Data. (Outer depends on Inner).
- **Dependency Injection**: Use Hilt for all DI.

## Naming Conventions
- **Screens**: PascalCase + `Screen` (e.g., `MainScreen.kt`)
- **ViewModels**: PascalCase + `ViewModel` (e.g., `MainViewModel.kt`)
- **UseCases**: Verb + `UseCase` (e.g., `GetTodayMedicationsUseCase.kt`)
- **Repositories**: Noun + `Repository` (e.g., `MedicationRepository.kt`)
- **DAOs**: Entity + `Dao` (e.g., `MedicineDao.kt`)
- **Entities**: PascalCase (e.g., `Medicine.kt`)

## File Organization
- One file per logical component (e.g., one Screen per file).
- Package by Feature within Presentation (e.g., `presentation/main/`, `presentation/detail/`).

## Language & Style
- **Kotlin**: Follow official Kotlin Style Guide.
- **Compose**: Use `remember`, `LaunchedEffect` appropriately. Hoist state to ViewModels.
- **Null Safety**: Avoid `!!`. Use `?.`, `?:`, `let`.

## Testing
- **Unit Tests**: JUnit5, Kotest (Focus on UseCases and ViewModels).
- **UI Tests**: Compose Testing.
- **Coverage Goal**: 80%+ for Domain Logic.