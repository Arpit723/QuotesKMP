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

