# Repository instructions

## Project and scope

This is a Kotlin Android home-screen widget app for the owner's Counter-Strike stats. Start by reading:

- [Requirements](docs/REQUIREMENTS.md)
- [API research and official sources](docs/API_RESEARCH.md)
- [Implementation plan](docs/IMPLEMENTATION_PLAN.md)

The initial repository is a single-module Compose starter, not an implemented stats app. Do not claim a planned feature or check is complete without evidence. Keep the existing package/application ID and single-module structure unless the task requires otherwise.

## Required working style

Apply the installed `ponytail` skill (full by default) and `karpathy-guidelines` skill for coding tasks. These were verified installed on 17 November 2026 at:

- `C:/Users/Chroma/.codex/skills/ponytail/SKILL.md`
- `C:/Users/Chroma/.codex/skills/karpathy-guidelines/SKILL.md`

On another machine, resolve them through that environment's skills catalog or `$CODEX_HOME/skills`. Read the actual skills before using them; these paths are machine-specific, not dependencies to copy into the app. If unavailable, state that limitation and preserve the principles below.

- Read affected code and callers before changing them. State material assumptions.
- Make the smallest complete change; use existing patterns/platform facilities before adding dependencies.
- Avoid speculative layers, generic repositories, DI frameworks, modules, settings, and unrelated cleanup.
- Define a concrete success check, verify it, and report real results and remaining limits.
- Add a focused test for nontrivial behavior; documentation and trivial reversible changes do not need mirror tests.
- Do not delegate to subagents unless the user or an applicable instruction explicitly requests it.

## Git workflow — user instructions

- Create a **new branch for each new feature**. Names must start with `feat/` or `feature/`; prefer `feat/<short-feature-name>`. **Do not use a `codex/` prefix.**
- Use `feat/project-planning` for the initial research/planning task. Planned feature branch names are in the implementation plan.
- Inspect branch/status before editing. Branch from the agreed integration base with required prerequisites. Do not silently combine independent features on one branch or switch away from unrelated dirty work.
- **Commit after each completed task**, once its relevant checks pass. Several completed tasks require several focused commits; never postpone everything for one final project commit. A task is a meaningful reviewable change, not each file or tool call. Do not create empty commits just to record a check.
- The user's standing instruction authorizes these task commits. Preserve the configured Git `user.name`/`user.email`; no AI attribution or co-author trailers.
- Follow existing commit conventions. Initial history used `Initial Commit`; planning uses concise `docs:` messages. Prefer concise `feat:`, `fix:`, `test:`, or `docs:` subjects where appropriate.
- Stage explicit task-owned paths. Leave unrelated files, including the pre-existing untracked `.idea/` directory, untouched.
- Never commit secrets, personal API responses, local SDK configuration, build outputs, or credentials.
- Do not amend, reset, force-push, merge, or push without an explicit request. Local task commits do not authorize publishing.
- Git metadata may be read-only in this sandbox. Use the normal escalation mechanism for necessary Git writes; never circumvent filesystem restrictions.

## API and data rules

- Use the official public API contract, not scraped/internal endpoints. Recheck live documentation when implementing changed behavior.
- One `/v3/profile` request supplies the initial stats widget. Do not fetch every match or full history without a feature need.
- Aim is `rating.aim`, Utility is `rating.utility`, overall Leetify Rating is `ranks.leetify`, and Premier is `ranks.premier`. Preserve provider scales/names; never silently multiply a rating to imitate the UI.
- Treat missing/null values as unavailable, not zero. Keep SteamID64 as a string. Unknown fields/outcomes and partial responses must not crash rendering.
- Premier chart semantics are unverified. Do not infer ladder type from numeric ranges, mix competitive/FACEIT points, or invent history. Complete the evidence gate in the implementation plan first.
- Follow the provider's data-storage guidance: no durable API response/stat/history/bitmap cache by default. Keep processing state transient. Launcher rendering retention is an explicit provider clarification before release, not established cache permission.
- Store only necessary configuration/refresh metadata; protect owner keys with Keystore-backed encryption and exclude credentials from backups. Never put a shared API key in source or APK resources.
- Do not log keys, headers, identifiers, or API bodies. Use synthetic data in tests.
- Clear transient API state and replace all rendered widgets on reset/account change or confirmed access revocation. Prevent obsolete in-flight responses from restoring old content.
- Use official attribution assets and source links. Public branding must be independent; the repository name does not establish the release app name.

## Android implementation and verification

- Preserve minSdk 24 unless explicitly changed. Inspect Gradle/version catalog and SDK/JDK compatibility before adding dependencies; do not upgrade tools speculatively.
- Use Compose for activity UI, Glance for widgets, and WorkManager for deferrable refresh. Ordinary Compose UI composables are not directly usable as Glance widgets.
- Share requests across widget instances. Background work is inexact; default to hourly with network constraints and bounded retries. Honor Retry-After when supplied. Never promise real-time monitoring.
- Prefer an in-memory Canvas bitmap for a small static chart once its data is verified.
- Typical local checks from the repository root in PowerShell:

  ```powershell
  .\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
  ```

- Run focused tests during development, then the relevant build/lint checks. Use `:app:connectedDebugAndroidTest` only with a suitable connected device/emulator. Report unavailable SDK/network/device checks as blocked or unrun, not passed.
- Exercise widgets in a launcher: add/remove, multiple instances, resize, font scaling, dark/light, process death, reboot, offline, reset/account change, and rate limiting. An activity preview is not proof of widget behavior.
- For docs-only changes, check links, consistency, `git diff --check`, branch name, and intended commit contents; no Android build is needed solely for prose edits.

## Handoff

Report what changed, relevant verification, current branch and completed task commits, and any actual blocker. Never imply an authenticated owner API check, device test, or provider approval happened when it did not.
