# Android foundation verification

Checked on Windows, 17 November 2026. Branch: `feat/android-foundation`.

## Untouched starter baseline

Base commit: `fadc4ec` (merged planning PR). No build or application source changes were needed for the baseline.

| Component | Observed version |
| --- | --- |
| Android Studio bundled JDK | JetBrains 25.0.4 |
| Gradle wrapper | 9.6.0; configured distribution checksum retained |
| Gradle daemon requirement | Java 25, satisfied by bundled JDK |
| Android Gradle plugin | 9.5.1 |
| Kotlin / Compose compiler plugin | 2.2.10 |
| SDK platform | API 37.0, revision 2 |
| SDK build tools | 36.0.0 |
| App SDK configuration | minSdk 24; compile/target SDK 37 |

Java is not on this shell's PATH and JAVA_HOME is unset. Use a process-local override; no system configuration or local.properties change is required:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat --version
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --console=plain
```

In this restricted workspace, Gradle needs approved access to its user cache and Android SDK outside the repository. The initial sandbox attempt failed on the Gradle wrapper lock; rerunning with approved access succeeded.

Results: BUILD SUCCESSFUL, 52 tasks executed. Debug APK assembled; one existing placeholder unit test passed; lint completed with **0 errors and 10 warnings**. Existing warnings cover a redundant activity label, Kotlin/Compose BOM updates, and seven unused starter colors. A native library strip warning packaged the existing library unchanged. None required a toolchain upgrade or unrelated starter cleanup.

Generated, ignored reports:

- `app/build/reports/tests/testDebugUnitTest/index.html`
- `app/build/reports/lint-results-debug.html`
- `app/build/outputs/apk/debug/app-debug.apk`

No emulator/device test, release build, or authenticated API request was run in the baseline task. The placeholder test confirms the harness runs, not that any widget behavior exists.

Reference: [AGP 9.5 compatibility](https://developer.android.com/build/releases/agp-9-5-0-release-notes) lists Gradle 9.6.0 and support through API 37.

## Foundation dependencies

Versions are pinned in `gradle/libs.versions.toml` and used by `app/build.gradle.kts`:

| Dependency | Version | Purpose |
| --- | --- | --- |
| Glance AppWidget | 1.2.0 | Home-screen widget composables and host integration |
| WorkManager runtime | 2.12.0 | Coroutine workers and deferrable refresh; minimum API 24 |
| DataStore preferences | 1.3.0 | Configuration/settings only, not API stats |
| OkHttp | 5.3.0 | Public API HTTP requests; published Kotlin stdlib dependency is 2.2.21 |
| kotlinx.serialization JSON | 1.9.0 | JSON parsing; release targets Kotlin 2.2 |
| Kotlin serialization compiler plugin | 2.2.10 | Matches the existing Kotlin/Compose plugin version |

These are stable releases selected for the existing Kotlin 2.2 toolchain. They are not a claim that every selected library is the latest available. No existing dependency/toolchain versions were upgraded. WorkManager's Kotlin APIs live in `work-runtime`, so a separate `work-runtime-ktx` dependency is unnecessary. Glance core is available transitively through AppWidget. No logging interceptor, database, chart library, or extra module was added.

The manifest now grants INTERNET and explicitly disables cleartext HTTP, including on API 24 where the platform default differs. No client, API credentials, disk cache, network request, widget provider, or worker is created by this foundation task.

After adding dependencies, the same build/test/lint command succeeded with 52 tasks executed. The existing single unit test passed. Lint reported **0 errors and 13 warnings**: the 10 baseline warnings plus update notices for the serialization plugin, OkHttp, and serialization runtime. Versions remain pinned for the existing toolchain rather than upgrading Kotlin as part of dependency setup. Native strip warnings included DataStore's shared counter library; both native libraries were packaged unchanged.

The generated merged debug manifest was checked for exactly one INTERNET permission, `usesCleartextTraffic=false`, and minSdk 24. `git diff --check` passed. No new behavior was introduced that warrants additional unit tests at this stage; actual network/parsing, scheduling, and settings tests belong to their implementation tasks. Device and release-build checks remain unrun.

Release references:

- [Glance releases](https://developer.android.com/jetpack/androidx/releases/glance)
- [WorkManager releases](https://developer.android.com/jetpack/androidx/releases/work)
- [DataStore releases](https://developer.android.com/jetpack/androidx/releases/datastore)
- [OkHttp 5.3.0 published Android metadata](https://repo.maven.apache.org/maven2/com/squareup/okhttp3/okhttp-android/5.3.0/okhttp-android-5.3.0.pom)
- [Serialization 1.9.0 release](https://github.com/Kotlin/kotlinx.serialization/releases/tag/v1.9.0)
