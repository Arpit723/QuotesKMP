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
