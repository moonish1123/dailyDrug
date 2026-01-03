# Task Completion Checklist (Definition of Done)

## Verification Steps
1. **Tests**: Run unit tests to ensure no regressions.
   - Command: `./gradlew test`
2. **Linting**: Run lint checks to verify code quality.
   - Command: `./gradlew lint`
3. **Build**: Ensure the app builds successfully.
   - Command: `./gradlew assembleDebug`
4. **Clean Code**: Remove unused imports, comments, and temporary logging.

## Commit Guidelines
- **Format**: `[Phase N] Description of work`
  - Example: `[Phase 4] Implement MainScreen UI and ViewModel connection`
- **Phases**: Refer to `PROJECT_SPEC.md` for phase numbers.
- **Content**: Include "Why" and "What" in the description if complex.

## Final Review
- Check `PROJECT_SPEC.md` to ensure requirements are met.
- Verify Deep Review items (Edge cases, Data flow, Consistency).
