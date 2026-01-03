# Project Overview: DailyDrug (매일 약먹기)

## Project Purpose
A medication reminder Android app designed to help users maintain consistent medication schedules with persistent notifications until doses are taken.
Key Value: "Notify until taken" (Repeat every hour if missed).
Design: Blue-themed, modern Material3.

## Tech Stack
- **OS**: Darwin (MacOS) environment
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose + Material3
- **Architecture**: Clean Architecture (Presentation -> Domain -> Data) + MVVM
- **Dependency Injection**: Hilt
- **Database**: Room
- **Async**: Coroutines + Flow
- **Background**: WorkManager (Scheduling), AlarmManager (Exact timing)
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 34 (Android 14)

## Module Structure
- `app`: Main application module (UI, DI, DB, Workers)
- `llmmodule`: LLM related features (Specifics TBD)
- `networkmodule`: Network communication
- `ocrmodule`: OCR features (Specifics TBD)

## Directory Structure (Clean Architecture)
```
com.dailydrug
├── data (Repository Impl, Room Entity/DAO, Worker)
├── domain (UseCase, Repository Interface, Model) - Pure Kotlin
├── presentation (Screen, ViewModel, State)
└── di (Hilt Modules)
```