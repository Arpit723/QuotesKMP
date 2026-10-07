# Decisions

## 2026-10-06 — SQLDelight iOS framework needs `-lsqlite3` in two places

Adding the SQLDelight native driver (`app.cash.sqldelight:native-driver`, sqliter) introduced
unresolved `sqlite3_*` symbols when linking the `SharedLogic` framework into the iOS app
(`ld: symbol(s) not found for architecture arm64` in the sqliter cinterop object files).

The framework is **static** (`isStatic = true`), so the final link happens inside the Xcode
app target, not in Gradle. Fix has two parts:

1. `linkerOpts("-lsqlite3")` in the `iosTarget.binaries.framework { }` block of
   `sharedLogic/build.gradle.kts` — the fix documented in project rules §12.
2. `OTHER_LDFLAGS = $(inherited) -lsqlite3` in `iosApp/Configuration/Config.xcconfig` —
   required in addition: for a static framework, Gradle's linkerOpts never reach the app's
   link step (verified: the synced framework's Info.plist carries no linker flags and the
   project has no other linker-flag mechanism), so the Xcode target must link
   `libsqlite3` itself.

## 2026-10-07 — QuoteRepository persistence functions

- **`kotlin.time.Clock`, not `kotlinx.datetime.Clock`.** kotlinx-datetime 0.8.0 moved
  `Clock`/`Instant` into the Kotlin stdlib; the kotlinx variants are deprecated. The
  repository injects `clock: Clock = Clock.System` (constructor injection) so tests can
  control `savedAt` via a mutable fake clock.
- **Dispatcher injection.** `QuoteRepositoryImpl` takes
  `dispatcher: CoroutineDispatcher = Dispatchers.Default`. It is passed to
  `mapToList`/`mapToOneOrNull` and used in `withContext` for `randomSavedQuote`/`save`/
  `delete`, so no DB work runs on the main thread. `Dispatchers.Default` (not `.IO`)
  because `.IO` is not a commonMain-safe API.
- **Saving an already-saved quote is "replace + refresh" (intentional).** `INSERT OR
  REPLACE` overwrites the whole row, so `savedAt` is refreshed from the clock and the
  quote moves to the top of the newest-first list. Re-saving is treated as "saved now",
  which matches the Save-toggle UX of the home screen. Covered by
  `savingTheSameIdTwiceReplacesTheRowAndRefreshesSavedAt`.

## 2026-10-07 — HomeViewModel save toggle and offline fallback

- **`isSaved` is derived from the database, never set by `toggleSave`.** The first
  `Success` reads `observeIsSaved(quote.id).first()` before emitting (no false→true
  flicker), then a background job keeps observing; observed values are applied only
  while the state is still `Success` for the same quote id. `toggleSave` only calls
  `save`/`delete`; the state update arrives via the observation.
- **First `isSaved` read failure = load failure.** If `observeIsSaved(...).first()`
  throws (non-cancellation), the load takes the normal failure path: offline fallback
  via `randomSavedQuote()`, else `Error`.
- **Toggle failure → `Error` state** (per "ViewModels turn failures into Error state");
  retry re-enters the normal load path.
- **Later observation failures are logged and swallowed** (last known `isSaved` kept).
  Unlike the first read, they are not user-initiated; the failure surfaces on the next
  user action, which itself follows the Error-state rule.
- **No default arguments on `HomeUiState.Success`.** Kotlin defaults are invisible to
  Swift, so both `isSaved` and `isOffline` are explicit at every construction site
  (`HomeViewModel`, `HomeViewModelTest`, the `HomeView.swift` preview).

## 2026-10-07 — SavedViewModel: manual StateFlow, shared error messages

- **Manual `MutableStateFlow` over `stateIn`/`SharingStarted`.** Per kotlinx.coroutines
  docs, an upstream exception in a `stateIn`-shared flow "terminates the sharing
  coroutine … handled by the scope" — under `viewModelScope` that crashes unless a
  `catch` operator is prepended, and the read-only `StateFlow` it returns cannot be set
  from `delete()` without merging in a side-channel. One `MutableStateFlow` owned by the
  ViewModel (HomeViewModel precedent) avoids both problems.
- **`delete` never edits the list.** `SavedViewModel.delete(id)` only calls
  `repository.delete(id)`; every `Content`/`Empty` update arrives solely via a new
  `observeSaved()` emission collected once in `init` (single source of truth, mirrors
  the proven `isSaved` design in HomeViewModel). No optimistic state that can drift
  from the DB; concurrent saves/deletes stay consistent; `Error` comes only from real
  failures.
- **`toUserMessage()` moved, not duplicated.** From a private extension at the bottom
  of `HomeViewModel.kt` to `internal` in `presentation/UserMessages.kt` — shared
  message policy across both ViewModels, no drift. HomeViewModel behaviour unchanged.
- **Test fake extracted and extended additively.** `FakeQuoteRepository` moved from a
  nested private class in `HomeViewModelTest.kt` to its own commonTest file
  (`internal`, same package). Extensions: `savedQuotes: MutableStateFlow<List<Quote>>`
  backing a stateful `observeSaved()` (kept in sync by `save` — newest-first prepend
  replacing same id, matching `QuoteRepositoryImpl`'s INSERT OR REPLACE — and `delete`),
  plus `deleteError`/`observeSavedError`/`deleteGate` knobs (gate awaits before any
  mutation, fetchGate pattern). No existing knob's behaviour changed; no Home test
  observes `savedQuotes`.
- **ViewModels collect eagerly in `init`, not `SharingStarted.WhileSubscribed`.** The
  `observeSaved()` collector starts the moment the ViewModel is created, so the state
  moves off `Loading` even before a UI subscribes — the Saved screen never idles on a
  stale initial value while nobody is collecting.
- **`retry()` restarts collection from scratch.** `observe()` cancels the previous
  collector job, sets `Loading`, and launches a fresh `observeSaved()` collection
  (shared by `init` and `retry`). Two quick retries leave exactly one active collector:
  the second `cancel()` stops the first restart's job before it can duplicate
  emissions — covered by `retryTwiceQuicklyLeavesExactlyOneActiveCollector` via the
  fake's `activeObservers` counter (added for that test only).
- **An error from `delete` (or the collector) replaces the list until `retry()` or the
  next emission.** On failure the last `Content` is dropped rather than kept beside the
  error, and the next `observeSaved()` emission — or a `retry()` — legitimately
  overwrites the `Error` state.


