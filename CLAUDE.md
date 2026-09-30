# CLAUDE.md

Personal fork of [Loop Habit Tracker](https://github.com/iSoron/uhabits) (`axs192/uhabits`, **public**), adding progressive targets for numerical habits. `origin` is the fork, `upstream` is `iSoron/uhabits`.

## Layout

- `uhabits-core`: Kotlin Multiplatform (JVM + JS). Models, SQLite repositories and migrations (`assets/main/migrations/NN.sql`), importers, presenters (`ui/screens`) and chart drawing (`ui/views`). Most changes and nearly all tests live here.
- `uhabits-android`: the Android shell. XML layouts, view binding, custom views, widgets. Consumes the core JVM target.

## Workflow

- Trunk is `dev` (not `main`). Branch (`feat/`, `fix/`, `chore/`) → PR → squash merge. No direct pushes to `dev`.
- Open PRs with `gh pr create --repo axs192/uhabits --base dev`. On a fork `gh` defaults to the upstream repo; never open a PR against `iSoron/uhabits` by accident.
- Test first. Never modify or weaken an existing passing test without asking; prefer optional parameters over signature changes so upstream tests stay untouched.
- Keep diffs to upstream files small to ease pulling from `upstream/dev`. New database migrations can collide with upstream's numbering.

## Commands

- `./gradlew :uhabits-core:jvmTest` — the fast test loop.
- `./gradlew ktlintCheck` — lint. Do not run `./build.sh build`: it reformats the whole tree and builds the JS target.
- `./gradlew :uhabits-android:assembleDebug` / `:uhabits-android:installDebug` — build / deploy.
- `gitleaks detect --source . --redact -v` — secret scan (also runs in hooks and CI).
- `pre-commit install --hook-type pre-commit --hook-type pre-push` — once per clone.

## Constraints

- Dev machine: 4 threads, 8 GB RAM. Do not raise the Gradle heap or enable parallel builds. Never build the JS target locally.
- Gradle CLI only; no Android Studio, no emulator. Instrumented (`androidTest`) tests are not part of the local loop.
- Deploy target is a physical phone over wireless ADB. `adb` is not on PATH: use `~/Android/Sdk/platform-tools/adb`, pinned with `-s <id>` when more than one entry is listed.
- On-device checks: prefer `adb shell uiautomator dump /sdcard/ui.xml 2>/dev/null` + grep over screenshots.
- The fork installs next to official Loop under its own application ID; builds are signed with `~/.android/debug.keystore`.

## Security (public repo)

- Commit with the GitHub noreply address (repo-local `user.email`); a pre-commit hook enforces it.
- Keystores, env files and credentials live outside the working tree. `.gitignore` patterns are a backstop, not the control.
- `.gitleaksignore` holds one inherited upstream finding. Add to it only for confirmed false positives or upstream history, never for a real secret.
- CI uses GitHub-hosted runners with actions pinned by commit SHA. Do not add a self-hosted runner to this repo.
