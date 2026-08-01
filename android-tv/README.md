# MyQ — Android TV

**MyQ** (a play on Sky Q — it even echoes the "My Q" section in Sky's own UI) is
an Android TV app that gathers **new programmes across UK Freeview and streaming
channels**, lets you set a **reminder X hours before a programme starts**, learns
what you like to build a **"For You"** rail, and wears a **Sky Q-inspired look**:
deep navy gradient, hero info panel at the top, horizontal card rails with a
white focus ring and gentle scale-up.

---

## 1. What to install

| Tool | Version | Notes |
| --- | --- | --- |
| **Android Studio** | Koala (2024.1) or newer | The one-stop install: bundles the JDK, Android SDK manager, emulator, and adb. Download from https://developer.android.com/studio |
| **Android SDK Platform 35** | API 35 | Android Studio → **Tools → SDK Manager → SDK Platforms** → tick *Android 15 (API 35)* → Apply. Usually installed automatically on first project sync. |
| **JDK 17** | (only for CLI builds without Studio) | Android Studio ships its own JDK, so nothing extra is needed if you build from the IDE. For pure command-line builds install [Temurin 17](https://adoptium.net/) and set `JAVA_HOME`. |

Nothing else — Gradle itself is downloaded automatically by the wrapper
(`gradlew` / `gradlew.bat`) pinned in this repo, and all app dependencies come
from Maven Central / Google Maven on first build.

### Windows specifics

- Install Android Studio with default options; when the setup wizard offers an
  emulator + SDK, accept.
- `adb.exe` lands in `%LOCALAPPDATA%\Android\Sdk\platform-tools\` — add that
  folder to your `PATH` if you want `adb` from any terminal.
- Use `gradlew.bat` instead of `./gradlew` in commands below.

## 2. Getting the code

```bash
git clone https://github.com/martypaz/MyQ.git
cd MyQ
```

The app is the `android-tv/` folder. Everything else in the repo is the
upstream Open Design project this repo was forked from — you can ignore it.

> **Important:** open the `android-tv/` **folder** in Android Studio
> (File → Open → select `MyQ/android-tv`), *not* the repo root. The repo root
> is not a Gradle project and Studio won't know what to do with it.

## 3. Building

**From Android Studio:** open `android-tv/`, let the Gradle sync finish
(first sync downloads Gradle 8.9 + dependencies, a few minutes), then
**Build → Make Project**. Green hammer = good.

**From the command line:**

```bash
cd android-tv
./gradlew :app:assembleDebug        # Windows: gradlew.bat :app:assembleDebug
```

The APK appears at `app/build/outputs/apk/debug/app-debug.apk`.

**From CI (no local tools at all):** every push touching `android-tv/` on
`main` runs the [Android TV build workflow](../.github/workflows/android-tv-build.yml),
which runs the tests and uploads the APK as a `myq-debug-apk` artifact —
download it from the run's page under **Actions**.

## 4. Running it

### On the Android TV emulator

1. Android Studio → **Tools → Device Manager → Create device**.
2. Pick the **TV** category → *Television (1080p)* → an API 34/35 system image
   → Finish.
3. Select it in the device dropdown and press **Run ▶**.

Drive it with your keyboard: arrow keys = D-pad, Enter = OK, Esc = Back.

### On a real Google TV / Android TV box

**Enable developer mode first** (exact path shown is Android TV 12; older
versions use *Settings → Device Preferences → About* instead of *System →
About*):

1. **Settings → System → About** → scroll to **Android TV OS build**.
2. Click **OK on that entry 7 times** — a countdown toast appears, then
   *"You are now a developer!"*.
3. Back one level: **Settings → System → Developer options** (newly visible,
   just below About) → enable **USB debugging**. Some models (Sony
   in particular) have a separate **Network debugging** toggle — enable that
   too if present. If `adb connect` is refused later, reboot the TV once.

Find the TV's IP under **Settings → Network & Internet → (your network)**.

Then from your PC (TV and PC on the same network):

```bash
adb connect <tv-ip>:5555      # TV shows a pairing prompt — accept it
adb install app/build/outputs/apk/debug/app-debug.apk
```

MyQ appears in the TV's app row. Two runtime prompts matter:

- **Notification permission** (Android 13+ only) — accept it, or reminders
  can't be shown. On Android 12 and below there is no prompt; notifications
  just work.
- **Alarms & reminders** (Android 12+) — if the OS asks, allow it so reminders
  fire at the exact time; otherwise delivery falls into a ~10-minute window.

The app runs on Android 8 (API 26) and newer, so any Android TV 12 set is
comfortably in range.

### Using the app

- **D-pad around the rails** — the hero panel at the top follows the focus,
  Sky Q style.
- **Press OK on a programme** → pick a reminder lead time (1/2/4/24 hours
  before start). Press OK again later to change or remove it.
- **For You** appears after you've browsed and set a few reminders — it learns
  from dwell (0.3), selection (1.0), and reminders (3.0) into per-genre and
  per-channel weights that decay ~2%/day. All of it stays on the device.
- No internet? The app falls back to bundled sample listings and shows an
  "Offline" badge.

## 5. Testing

**Unit tests** (pure JVM, no emulator needed — they cover the newness rules,
Freeview channel matching, TVmaze payload parsing, recommender ranking/decay,
reminder timing, and formatting):

```bash
cd android-tv
./gradlew test                      # Windows: gradlew.bat test
```

Results print to the console; an HTML report lands at
`app/build/reports/tests/testDebugUnitTest/index.html`.

In Android Studio: right-click `app/src/test` → **Run 'Tests in…'**, or click
the gutter arrow next to any test class.

**Manual smoke checklist** (things unit tests can't see):

1. Rails load with live listings (needs internet; "Offline" badge means the
   fetch failed).
2. Focus moves cleanly with the D-pad and the hero panel updates.
3. Set a 1-hour reminder on a programme starting >1h from now → a ⏰ badge
   appears on the card; the notification fires at start-minus-1h even with the
   app closed.
4. Reboot the TV → the reminder still fires (BootReceiver re-registers alarms).
5. Browse dramas / set reminders on them → a "For You" rail appears at the top
   ranking drama first.

**CI:** the same unit tests + a full APK assembly run on every push to `main`
touching `android-tv/` — check the **Actions** tab. Keep it green.

## 6. Project map

| Area | Where | What it does |
| --- | --- | --- |
| EPG data | `app/src/main/java/com/martypaz/myq/data/epg/` | Keyless [TVmaze](https://www.tvmaze.com/api) schedule API; broadcast filtered to a Freeview allowlist, `/schedule/web` for streaming; newness derived from season/episode + premiere date; `SampleData.kt` offline fallback. |
| Reminders | `.../myq/reminders/` + `data/prefs/ReminderStore.kt` | DataStore persistence, exact `AlarmManager` alarms, notification receiver, boot re-registration. |
| "For You" | `.../myq/recs/Recommender.kt` + `data/prefs/TasteStore.kt` | On-device scoring: interaction signals → genre/channel weights → ranked rail. Pure functions, fully unit-tested. |
| UI | `.../myq/ui/` | Compose: Sky Q theme (`theme/Theme.kt`), hero panel, 16:9 card rails with focus ring, D-pad reminder dialog. |
| Tests | `app/src/test/java/com/martypaz/myq/` | JVM unit tests (JUnit 4). |
| CI | `../.github/workflows/android-tv-build.yml` | Tests + debug APK on push to `main`. |

## 7. Troubleshooting

- **Gradle sync fails on first open** — almost always network/proxy: Gradle
  needs to reach `services.gradle.org`, `repo.maven.apache.org`, and
  `dl.google.com` once. Retry after checking connectivity.
- **"SDK location not found"** — Studio normally writes
  `android-tv/local.properties` (gitignored) automatically; if building CLI-only,
  create it with `sdk.dir=C:\\Users\\<you>\\AppData\\Local\\Android\\Sdk`.
- **`adb connect` refused** — re-check network debugging is on and both devices
  share a network; some TVs need `adb pair <ip>:<pairing-port>` first
  (Android 11+ wireless debugging).
- **Reminders don't appear** — check the notification permission was granted
  (TV Settings → Apps → MyQ → Notifications) and the programme start time is
  further away than the chosen lead time.
- **Empty "For You"** — by design until the taste profile has any signal;
  interact with a few programmes first.

## 8. Honest limitations / next steps

- TVmaze is community-sourced: coverage of smaller Freeview channels varies and
  regional variations (STV/S4C) are approximate. A production build would swap
  `TvMazeApi` for a licensed EPG feed (Freeview's own data or a commercial
  provider) behind the same `EpgRepository` interface.
- "Streaming" listings are release-date based, not per-service availability.
- Recommendations are deliberately simple (weighted genres/channels + decay).
  A next step is factoring in time-of-day and watch confirmations.
- No deep links into player apps yet (`iPlayer`/`ITVX` intents would slot into
  the card-select action alongside the reminder dialog).
