# Freeview Guide — Android TV

An Android TV app that gathers **new programmes across UK Freeview and streaming
channels**, lets you set a **reminder X hours before a programme starts**, learns
what you like to build a **"For You"** rail, and wears a **Sky Q-inspired look**:
deep navy gradient, hero info panel at the top, horizontal card rails with a
white focus ring and gentle scale-up.

## What's inside

| Area | Where | Notes |
| --- | --- | --- |
| EPG data | `data/epg/TvMazeApi.kt`, `EpgRepository.kt` | Free, keyless [TVmaze](https://www.tvmaze.com/api) schedule API. Broadcast listings are filtered to a Freeview channel allowlist; `/schedule/web` supplies streaming channels (iPlayer, ITVX, Netflix…). New-ness is derived from season/episode numbers and premiere dates (`NEW_SERIES` / `NEW_SEASON` / `NEW_EPISODE`). Offline? `SampleData.kt` keeps the UI populated. |
| Reminders | `reminders/` + `data/prefs/ReminderStore.kt` | Pick 1 / 2 / 4 / 24 hours before start. Stored in DataStore, scheduled with `AlarmManager.setExactAndAllowWhileIdle`, delivered as a high-priority notification by `ReminderReceiver`, re-registered after reboot by `BootReceiver`. |
| "For You" | `recs/Recommender.kt`, `data/prefs/TasteStore.kt` | On-device only. Browsing (2s dwell) = 0.3, selecting = 1.0, setting a reminder = 3.0 into per-genre/per-channel weights; ~2%/day decay; new-series boost. The rail appears once the profile has any signal. |
| Sky Q look | `ui/theme/Theme.kt`, `ui/components/` | Navy gradient background, top hero panel that follows D-pad focus (channel · time · NEW SERIES amber badge · synopsis), 16:9 tiles with channel chips and a white focus ring at 1.08× scale. |

## Build & run

Requirements: **JDK 17**, **Android SDK 35** (Android Studio Koala+ recommended).

```bash
cd android-tv
./gradlew :app:assembleDebug          # or open the folder in Android Studio
adb connect <tv-ip>:5555              # pair with your Android TV / Google TV device
adb install app/build/outputs/apk/debug/app-debug.apk
```

Or run on the **Android TV emulator**: Android Studio → Device Manager → create a
"Television (1080p)" device → Run.

First launch asks for notification permission (Android 13+) — accept it or
reminders can't be shown. On Android 12+ the system may also require the
"Alarms & reminders" special access for exact timing; without it the app falls
back to a ~10-minute delivery window.

## Using it

- **D-pad around the rails** — the hero panel at the top updates to whatever is
  focused, just like the Sky Q guide.
- **Press OK on a programme** — pick how many hours before start you want the
  reminder (1/2/4/24). Press OK again later to change or remove it.
- **For You** — appears at the top after you've browsed/selected/set reminders
  for a while. Everything it learns stays on the device.

## Honest limitations / next steps

- TVmaze is community-sourced: coverage of smaller Freeview channels varies and
  regional variations (STV/S4C) are approximate. A production build would swap
  `TvMazeApi` for a licensed EPG feed (Freeview's own data or a commercial
  provider) behind the same `EpgRepository` interface.
- "Streaming" listings are release-date based, not per-service availability.
- Recommendations are deliberately simple (weighted genres/channels + decay).
  A next step is factoring in time-of-day and watch confirmations.
- No deep links into player apps yet (`iPlayer`/`ITVX` intents would slot into
  the card-select action alongside the reminder dialog).
