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
