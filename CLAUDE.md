# CLAUDE.md

Personal fork of [Loop Habit Tracker](https://github.com/iSoron/uhabits) (`axs192/uhabits`, **public**) that adds **goals**: finite, staged targets alongside Loop's open-ended habits. `origin` is the fork, `upstream` is `iSoron/uhabits`.

## Layout

- `uhabits-core`: Kotlin Multiplatform (JVM + JS). Models, SQLite repositories and migrations (`assets/main/migrations/NN.sql`), importers, presenters (`ui/screens`) and chart drawing (`ui/views`). Most changes and nearly all tests live here.
- `uhabits-android`: the Android shell. XML layouts, view binding, custom views, widgets. Consumes the core JVM target.

## Goals (the fork's feature)

- **Habits are indefinite; goals are finite.** A goal is `HabitType.GOAL` (stored as type `2`): a measurable habit (`isNumerical` is true) that always has a `TargetSchedule` (start date, stage length in days, one target per stage). It ends at `TargetSchedule.endDate`.
- **Model** (`uhabits-core/.../models/`):
  - `TargetSchedule` holds the stages; `Habit.targetValueOn(date)` returns the target in force on a date.
  - `Habit.isActiveOn(date)` is false outside a goal's dates.
  - `GoalProgress` reports per-stage actuals, totals and status. A goal is achieved when, after it ends, the total reaches the sum of the stage targets.
  - For goals, scores and streaks are computed only within the goal's dates.
- **Storage:** migration `26.sql` adds `target_schedule_*` columns to `Habits`, and `DATABASE_VERSION` is 26. Backups from the fork don't import into official Loop. If upstream ships its own migration 26, renumber ours when syncing.
- **Semantics to keep:**
  - A goal's `frequency` is `Frequency(1, stageLength)`, so a stage target is "per stage".
  - Stages that last a whole number of weeks start on the first day of the week (`TargetSchedule.alignStart`).
  - Measurable habits have no staged targets; don't reintroduce them there.
- **UI:**
  - Goal option in `HabitTypeDialog`, and a goal mode in `EditHabitActivity`.
  - In the list, goal days are coloured per stage and an ended goal shows Achieved or Not achieved.
  - `GoalCardView` on the habit page (the calendar Target card is hidden for goals), plus a stepped target line on the bar chart.
  - `GoalWidget`: Total vs Days rows, picked through `GoalPickerDialog`.

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
- The phone is a Xiaomi (MIUI). `adb install` shows an on-screen confirmation that is cancelled (`INSTALL_FAILED_USER_RESTRICTED`) if the phone is locked; wake it, have the user unlock it, and ask them to tap Install.
- When driving the UI with `adb shell input`, re-dump before every tap (coordinates move when the keyboard opens), and don't send BACK to hide the keyboard: with no keyboard up it leaves the screen. Use keyevent 111 (Escape).
- In the manifest, class names in `meta-data` values must be fully qualified: Android resolves short names against the application ID, not the namespace.
- The fork installs next to official Loop under its own application ID; builds are signed with `~/.android/debug.keystore`.

## Security (public repo)

- Commit with the GitHub noreply address (repo-local `user.email`); a pre-commit hook enforces it.
- Keystores, env files and credentials live outside the working tree. `.gitignore` patterns are a backstop, not the control.
- `.gitleaksignore` holds one inherited upstream finding. Add to it only for confirmed false positives or upstream history, never for a real secret.
- CI uses GitHub-hosted runners with actions pinned by commit SHA. Do not add a self-hosted runner to this repo.
