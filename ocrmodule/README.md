# OCR Module

## Overview
The `ocrmodule` provides functionality to extract text from drug bag images using Google ML Kit's On-Device Text Recognition (Korean). It follows Clean Architecture principles.

## Architecture
- **Domain Layer**: `DrugInfo` (Model), `OcrRepository` (Interface), `AnalyzeDrugBagUseCase` (UseCase).
- **Data Layer**: `OcrRepositoryImpl`, `OcrDataSource` (Interface), `OcrDataSourceImpl` (ML Kit Implementation).
- **DI**: Hilt module `OcrModule` binds implementations.

## Usage

### Dependency
Add the module to your app's `build.gradle.kts`:
```kotlin
implementation(project(":ocrmodule"))
```

### Analyzing an Image
Inject `AnalyzeDrugBagUseCase` and call it with a `Bitmap` (e.g., captured from Camera).

```kotlin
@Inject
lateinit var analyzeDrugBagUseCase: AnalyzeDrugBagUseCase

// ... inside a coroutine
// Ensure CAMERA permission is granted before calling if obtaining from Camera
val result = analyzeDrugBagUseCase(bitmap)
result.onSuccess { info ->
    println("Drug Name: ${info.drugName}")
    println("Intake Time: ${info.intakeTime}")
}.onFailure { e ->
    e.printStackTrace()
}
```

## Permissions
The module declares `android.permission.CAMERA` in its manifest.
The `AnalyzeDrugBagUseCase` is annotated with `@RequiresPermission(android.Manifest.permission.CAMERA)` to remind the caller to ensure the permission is granted if the image source is the camera.

## API Keys
This implementation uses **ML Kit On-Device Text Recognition**, which does **not** require a Google Cloud API Key. It runs entirely on the device.
