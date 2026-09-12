# GigLister Android

Native Kotlin + Jetpack Compose app talking to the same Spring Boot backend as the web
frontend (`../src`) - nothing Android-specific on the server side beyond two small
additions (`POST/DELETE /api/me/device-token`, and a push notification sent from
`EventService` when a new event lands inside a user's saved home radius).

V1 scope: login/register, "Konzerte in deiner Nähe" using the phone's real GPS position,
a profile screen to save that position + a radius for push notifications, and push
notifications themselves.

## Important: this project was never built or run

It was written directly, without Android Studio and without network access to Google's
Maven repository (`dl.google.com`), which is where the Android Gradle Plugin, AndroidX,
Play Services and Firebase all get resolved from - that host was unreachable in the
sandbox this was written in, so not even a plain `./gradlew tasks` could succeed there.
Every file is correct to the best of available knowledge, but **your first build in
Android Studio is also the first time any of this has actually compiled.** Expect to fix
a handful of small things - a renamed API, a version bump, an import Android Studio
suggests differently - the architecture and logic shouldn't need to change, just details
a real compiler would normally catch immediately.

## One-time setup

### 1. Firebase project (required to build at all)

The Firebase Cloud Messaging Gradle plugin (`com.google.gms.google-services`, applied in
`app/build.gradle.kts`) fails the build immediately if `app/google-services.json` is
missing - so this has to happen before your very first build, not just before push
notifications work.

1. Create a project at <https://console.firebase.google.com>.
2. Add an Android app to it with package name exactly `com.giglister.app` (must match
   `applicationId` in `app/build.gradle.kts`).
3. Download the generated `google-services.json` and place it at `app/google-services.json`
   (already gitignored - it's project-specific, everyone building this needs their own).
4. For the backend to actually *send* pushes: Firebase console → Project settings →
   Service accounts → Generate new private key. Point the backend at that file via
   `GIGLISTER_FIREBASE_CREDENTIALS_FILE` (see the repo root's `.env.example` and
   `docker-compose.yml`). Without this, the app still runs fine and registers device
   tokens - it just never receives anything, same as if this whole feature were off.

### 2. Point the app at your backend

`app/build.gradle.kts` bakes the API base URL in per build type via `BuildConfig.API_BASE_URL`:

- **debug**: `http://10.0.2.2:8080` - this is how the Android *emulator* reaches your
  machine's own `localhost:8080`. On a real phone over Wi-Fi, change this to your
  machine's LAN IP instead (`http://192.168.x.x:8080`) - the phone can't resolve
  `10.0.2.2` or `localhost` to your dev machine.
- **release**: `https://sandbox.fotosvorju.de` - change this to wherever the backend
  actually ends up running before shipping a real build.

### 3. Build

Open the `android/` folder as a project in Android Studio (Iguana or newer) and let it
sync - this is what actually downloads the Android SDK components, AndroidX, Firebase,
etc., none of which happened in this session. From the command line instead:

```bash
cd android
./gradlew assembleDebug
```

The resulting APK (`app/build/outputs/apk/debug/app-debug.apk`) can be sideloaded onto a
phone with USB debugging enabled via `adb install app-debug.apk`, or dragged onto an
emulator window.

## Project layout

```
app/src/main/java/com/giglister/app/
  GigListerApp.kt          - Application class: creates the notification channel,
                              restores a saved login session at startup
  MainActivity.kt          - sets Compose content, wires the nav graph
  data/
    model/                 - plain data classes mirroring the backend's DTOs exactly
                              (see src/main/java/com/giglister/dto/** on the backend)
    api/
      GigListerApi.kt       - Retrofit interface - one method per backend endpoint used
      ApiClient.kt          - builds the shared Retrofit/OkHttp instance
      TokenStore.kt         - in-memory holder for the auth interceptor to read
    AuthRepository.kt       - login/register/logout, persists the token via DataStore
  location/
    LocationRepository.kt   - one-shot real GPS fix via FusedLocationProviderClient
  push/
    GigListerMessagingService.kt - FCM token refresh + foreground notification display
    NotificationHelper.kt        - notification channel + builder
  ui/
    theme/                  - Material3 theme using the same palette as the web app
    login/, events/, profile/ - the three screens, each a ViewModel + a Composable
    Navigation.kt            - NavHost + bottom nav bar
```

No dependency-injection framework (Hilt, etc.) - every ViewModel just takes its one or
two dependencies as plain constructor arguments via `SimpleViewModelFactory`. Not because
DI is a bad idea here, but because Hilt's annotation processor is exactly the kind of
thing that's easy to get subtly wrong without a compiler to check it against, per the
warning above - plain constructors are the boring, safe choice for a first, unverified
build.

## Testing on a device

1. Enable Developer options + USB debugging on the phone (Settings → About phone → tap
   "Build number" 7 times, then Settings → Developer options).
2. Connect via USB, accept the RSA key prompt, then `adb install app-debug.apk` (or use
   Android Studio's Run button with the device selected).
3. First launch will ask for location, then (Android 13+) notification permission - both
   are requested from the "Konzerte in deiner Nähe" screen, not up front at first launch.
