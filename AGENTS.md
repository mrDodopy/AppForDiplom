# AGENTS.md

Android app (single `:app` module): Kotlin + Jetpack Compose/Material3 + Hilt/KSP. Namespace/app id `com.roamly.app`; minSdk 24, compile/target SDK 36, JVM 11. UI is in Russian.

## Commands (Windows)

- `.\gradlew.bat :app:compileDebugKotlin` — fastest compile check
- `.\gradlew.bat :app:assembleDebug`
- `.\gradlew.bat :app:testDebugUnitTest` — only placeholder tests exist
- `.\gradlew.bat :app:connectedDebugAndroidTest` — requires a device/emulator
- `.\gradlew.bat :app:lintDebug`
- Single test: `.\gradlew.bat :app:testDebugUnitTest --tests "com.roamly.app.ExampleUnitTest"`

No CI, formatter, or real test suite is configured; `kotlin.code.style=official`.

## Architecture / wiring

- No general repository layer: Compose screens -> `@HiltViewModel` classes in `entity/ViewModel/` -> `ApiService` (small helpers live in `manager/`). Session state lives in `UserDataSource` (SharedPreferences).
- Routes are sealed classes in `ui/screens/sealed/`; `navigation/NavGraph.kt` wires every screen manually. Adding a screen means adding a route plus a `composable` in `NavGraph`; for a bottom tab also update `bottomBarVisibleRoutes` / `hideBackIcon` in `ui/screens/MainScreen.kt`.
- Hilt is used throughout (`RoamlyApp` is `@HiltAndroidApp`, `MainActivity` is `@AndroidEntryPoint`). Keep injection imports consistent with surrounding files; `WebSocketService` currently uses `jakarta.inject`.
- Package/dir mismatch: `entity/UserDataSource.kt` declares `package com.roamly.app.data.source`. Don't "fix" the path; several files import `com.roamly.app.data.source.UserDataSource`.

## Networking

- Base URL is hardcoded in `factory/RetrofitFactory.kt` (`BASE_URL`). `NetworkModule`, `SockJSManager`, and `WebSocketTester` all read it, so change the server in one place. `fetcher/GistFetcher.kt` is unused.
- Most ViewModels inject `ApiService` from `NetworkModule` (custom Gson with `LocalDateTimeAdapter`). `UserViewModel` alone builds its own client via `RetrofitFactory.create()` (plain Gson). Update both if changing Retrofit/Gson/client behavior.
- WebSocket notifications use a raw OkHttp WebSocket in `websocket/SockJSManager` (singleton, manual reconnect), not STOMP, despite the StompProtocolAndroid dependency. Its URL is derived from `BASE_URL` by stripping the scheme and prepending `ws://`.
- New HTTP endpoints go in `ApiService.kt`; DTOs live under `entity/DTO`, entities under `entity/classes`.

## UI / theme / maps

- Use `AppTheme.colors` (custom `MyAppColors` provided via CompositionLocal in `MainActivity`), not `MaterialTheme.colorScheme`. Theme choice is persisted by `ColorSettings` in DataStore.
- Maps are osmdroid/OpenStreetMap; there is no Google Maps key. osmdroid config/user agent is initialized in `RoamlyApp.onCreate`; individual `MapView`s load `Configuration` and set a tile source themselves.
- `minSdk` is 24, but `java.time` is used behind `@RequiresApi(Build.VERSION_CODES.O)` guards. Core library desugaring is declared as a dependency but not enabled in `app/build.gradle.kts`; don't assume `java.time` works below API 26.
- User-facing text and most comments are Russian and inline (only `app_name` is in `strings.xml`). Keep new UI text Russian to match.

## Versions

- Shared versions live in `gradle/libs.versions.toml`, but many dependencies (navigation, Retrofit, Coil, osmdroid, WebSocket, RxJava) are hardcoded in `app/build.gradle.kts`. Check both places before assuming a version.
