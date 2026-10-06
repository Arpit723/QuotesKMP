# QuotesKMPDemo: Project Rules

Read this file at the start of every task. These rules override your defaults.

## 1. What this project is
A small Kotlin Multiplatform demo app that shows random quotes from the internet.
- Android UI: Jetpack Compose (Material 3)
- iOS UI: SwiftUI (minimum iOS 16)
- Shared Kotlin module: networking, database, domain, and presentation logic only
- Purpose: demonstrate "share the logic, keep the UI native"

Package: `com.demo.quotes`. Modules: `sharedLogic`, `androidApp`, `iosApp`. The shared module builds an iOS framework named `SharedLogic` (Swift: `import SharedLogic`).

## 2. Current scope (do not exceed it)
Home screen:
- shows one random quote (text and author) from the internet
- "New Quote" button fetches another from the internet
- loading state while fetching
- error state with a Retry button
- a Save toggle that saves or unsaves the current quote

Offline fallback:
- if the network call fails, show a random SAVED quote with an "Offline" label
- if nothing is saved, show the error state with Retry

Saved screen:
- all saved quotes, newest first, updating live
- delete a quote

Saved quotes persist across app restarts.

Navigation:
- bottom tabs (Home, Saved), built natively on each platform

Out of scope: login, search, categories, sharing, analytics, settings, localization, backend.
Do NOT add any out-of-scope feature, library, or layer unless I explicitly ask.

## 3. Architecture
sharedLogic module layers (package by layer):
- `domain`: `Quote` model, `QuoteRepository` interface with:
  - `suspend fun fetchRandomQuote(): Quote`
  - `suspend fun randomSavedQuote(): Quote?`
  - `fun observeSaved(): Flow<List<Quote>>`
  - `fun observeIsSaved(id: Long): Flow<Boolean>`
  - `suspend fun save(quote: Quote)`
  - `suspend fun delete(id: Long)`
- `data`: `remote` (Ktor `QuoteApi`, `QuoteDto`, mapper), `local` (SQLDelight database, table `SavedQuote` (id, text, author, savedAt)), `QuoteRepositoryImpl`
- `platform`: expect/actual `DatabaseDriverFactory` (Android needs `Context`, iOS needs nothing)
- `presentation`: `HomeUiState` (sealed: Loading, `Success(quote, isSaved, isOffline)`, Error), `HomeViewModel`, `SavedUiState` (sealed: Loading, `Empty`, `Content(quotes)`), `SavedViewModel`
- `di`: Koin module and `initKoin()`

Rules:
- All business logic lives in `sharedLogic`. Platform UI code only renders state and forwards user actions.
- `commonMain` must not import Android or iOS APIs. Use `expect/actual` or constructor injection for platform differences.
- No Compose Multiplatform. Never share UI code.
- ViewModels expose `StateFlow<UiState>` and plain functions. No callbacks, no `LiveData`.
- Repositories do not catch exceptions. ViewModels turn failures into `Error` state.
- Always rethrow `CancellationException` when catching exceptions in coroutine code.
- Dependencies are injected through Koin. No singletons created by hand in UI code.

## 4. Kotlin conventions
- Prefer immutable data classes and sealed interfaces for state.
- Keep files small and focused: one main type per file.
- Name things for what they are (`QuoteRepositoryImpl`, not `Manager`).
- Public shared APIs get a short KDoc comment.
- No `!!`. No catching `Throwable`.

## 5. Android conventions
- Collect state with `collectAsStateWithLifecycle`.
- Composables are stateless where possible. State comes from the ViewModel.
- No business logic in composables.

## 6. iOS / SwiftUI conventions
- Shared Kotlin is consumed through SKIE: `StateFlow` becomes an `AsyncSequence` (`for await`), `suspend` functions become `async`/`await`, sealed types are switched with `onEnum(of:)`.
- A `@MainActor` `ObservableObject` wrapper holds the shared ViewModel and publishes its state.
- The wrapper collects the flow in a `Task` and cancels it when the view disappears.
- State switches must be exhaustive. No `default` case.
- No business logic in views. No force unwraps.
- Provide SwiftUI previews for each state (Loading, Success, Error).
- Call `KoinHelper.shared.start()` once at app launch.
- Navigation uses a native SwiftUI `TabView` (Home, Saved).
- `SavedViewModel` is wrapped the same way as `HomeViewModel`: a `@MainActor` `ObservableObject` collecting its flow in a `Task`.

## 7. Versions and dependencies
- All versions live in `gradle/libs.versions.toml`.
- Use the latest stable versions that are compatible with our Kotlin version. Check official release notes or docs. Never guess a version number.
- Before using a library API you are not sure about (especially SKIE, Ktor, Koin, androidx lifecycle), read its official documentation first.
- Do not add a library that is not already part of the stack without asking.

## 8. Testing
- Shared logic is tested in `commonTest` with `kotlin.test`, Turbine, and Ktor `MockEngine`.
- Use fake repositories, not mocking frameworks.
- Every ViewModel state transition has a test: Loading to Success, Loading to Error, retry after error.
- Database code is tested with an in-memory SQLDelight driver.
- The offline fallback has its own ViewModel test.
- Never leave failing tests or a broken build at the end of a task.

## 9. Commands (run these yourself to verify)
- Shared tests: `./gradlew :sharedLogic:allTests`
- Shared build: `./gradlew :sharedLogic:build`
- Android build: `./gradlew :androidApp:assembleDebug`
- iOS build: build `iosApp` for any available iOS simulator with `xcodebuild` (or Xcode)

## 10. Workflow rules
- Do only the task in the prompt, then stop. Do not continue to the next task on your own.
- For non-trivial tasks, plan first: list files to create or change, order, and risks. Wait for approval before editing.
- After a change, run the relevant build and tests. If something fails, fix it and retry up to 3 times, then report the problem.
- If a decision is ambiguous, choose the simplest option and note it in `DECISIONS.md`.
- Do not refactor or reformat code that is unrelated to the task.
- Do not delete or rewrite working code to fix a symptom elsewhere. Find the root cause first.

## 11. Report format (end every task with this)
1. Files created or changed
2. Commands run and their results
3. Deviations from the prompt or from these rules, with the reason
4. Anything I should check manually
5. Two or three lines explaining the key concept used (so I can learn from it)

## 12. Known gotchas (add a line here whenever a mistake repeats)
- Kotlin sealed types appear as plain Obj-C classes in Swift unless SKIE is active. If `onEnum(of:)` is missing, rebuild the shared framework.
- The iOS app must run the Gradle task that embeds the shared framework before compiling Swift, or Xcode will not find the `SharedLogic` module.
- Rethrowing `CancellationException` is required, or coroutine cancellation silently breaks.
- Generated `build/` and `.gradle/` folders must never be committed (see `.gitignore`).
- SQLDelight `.sq` files must sit in a folder path matching the database package name.
- If the iOS build shows a sqlite3 linker error, add `linkerOpts("-lsqlite3")` to the iOS framework binaries.
