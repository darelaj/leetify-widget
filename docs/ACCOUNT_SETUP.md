# Account setup

Implemented on `feat/account-setup`, 18 November 2026.

The activity now hosts a stateless Material 3 Compose form backed by a ViewModel and immutable StateFlow UI state. The activity collects it with `collectAsStateWithLifecycle`. Each data class has a dedicated file; profile mapping and input validation are separate from models. Configuration changes retain the ViewModel; raw typed keys are not placed in SavedStateHandle or saved instance state.

## Behavior

- Enter a 17-digit SteamID64 and optional personal API key. The key field is masked; no Steam password is requested.
- Verify and save validates a supplied/effective key, checks profile access, and saves configuration only after success. It runs on an injected IO dispatcher, never on the main thread.
- A blank key retains a saved key only for the same SteamID64. A replacement overwrites it after verification. Remove saved key stages removal; verify and save without a replacement completes it. Reset deletes settings immediately without needing API access.
- Loading existing settings disables edits until loading completes. Verification failures preserve the last saved account and show an explicit status; editing clears the status and invalidates in-flight verification.
- Account generations, cancellation checks before saving, and serialized writes prevent old verification results from updating the new UI/account. Reset waits for a save already in progress, then clears it. Its deletion runs to completion even if the ViewModel is cleared while resetting.
- No fetched profile or stats are held in the ViewModel, DataStore, or saved UI state. The verification result is discarded after access is checked.

## Credential storage

Preferences DataStore writes `files/datastore/account.preferences_pb`, containing the SteamID64 and optional encrypted API key. AES-256-GCM encryption uses a fresh 12-byte random IV and an Android Keystore key; only IV and authenticated ciphertext are written. The key's hardware backing depends on the device and is not promised by the app. Models containing raw credentials have redacted `toString` implementations.

Legacy backup rules exclude the whole account file. Android 12+ rules exclude it from cloud backup and device transfer. This avoids transferring ciphertext without its Keystore key and keeps the account ID out of those backups too. Removing the saved API key removes its ciphertext; reset also removes the app's Keystore alias. No plaintext fallback is allowed if decryption fails: show a storage error and let reset/re-entry recover. A persistent filesystem or Keystore failure can still make reset fail and is reported explicitly.

References: [Android Keystore key generation](https://developer.android.com/reference/android/security/keystore/KeyGenParameterSpec), [backup exclusions](https://developer.android.com/identity/data/autobackup), and [lifecycle-aware Compose state](https://developer.android.com/develop/ui/compose/state).

## Verification

- 25 JVM tests passed: existing API/mapping tests, input validation, encrypted settings round trips, random IVs, ciphertext tampering/wrong-key rejection, key replacement/removal, fail-closed storage errors, and account change/reset races.
- Three instrumentation tests passed on the installed API 37 emulator: Compose input masking/validation/reset, Android Keystore encryption with an isolated test alias, and the existing package-name test.
- Debug assembly and lint passed. Lint has 0 errors and 14 warnings: the previous 13 plus an update notice for the coroutine test library, pinned to the existing 1.9.0 coroutine runtime. No unrelated dependencies were upgraded.
- The running account screen was visually inspected on API 37; fields, text, and buttons fit without clipping. The screenshot is an ignored local artifact at `app/build/outputs/account-setup.png`.
- Backup XML paths and secret logging were reviewed. Actual backup/restore transport behavior and API 24 device behavior were not exercised. No personal credentials or live authenticated requests were used.

Widgets and scheduled refresh are the next milestones. There are no widget instances or scheduled jobs to clear yet; their account-change invalidation and rendering cleanup must be connected when implemented. Attribution assets, release branding, and the provider's render-retention clarification remain release gates from the requirements.
