# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Live Wallpaper It** is a [Muzei](https://muzei.co/) plugin for Android that fetches Reddit images (from configured subreddits) and displays them as a live wallpaper. It requires a Reddit API client ID (userless OAuth or anonymous). It is distributed on Play Store, F-Droid, and GitHub with three product flavors differing only in privacy-policy/rate-app UI visibility.

## Build Commands

```bash
# Build
./gradlew assembleDebug
./gradlew assembleRelease

# Code formatting (Spotless — must pass CI)
./gradlew spotlessCheck --no-configuration-cache
./gradlew spotlessApply

# Tests (no test files currently exist)
./gradlew test
./gradlew :app:testDebugUnitTest --tests "rocks.tbog.livewallpaperit.ClassName.methodName"
```

CI runs `spotlessCheck` and `assembleGithubDebug` on every push/PR to `main`.

## Module Structure

**`:app`** — Android application (Java), namespace `rocks.tbog.livewallpaperit`
- min SDK 21, target SDK 34
- Product flavors: `playstore`, `fdroid`, `github`
- Build types: `release` (minified), `debug`, `debugMini`

**`:ARAW`** — Embedded fork of [ARAW](https://github.com/KirkBushman/ARAW) (Kotlin), namespace `com.kirkbushman.araw`
- Reddit API client: Retrofit + OkHttp + Moshi (with KSP codegen)
- Auth strategies in `helpers/`: `AuthUserlessHelper`, `AuthAppHelper`, `NoAuthHelper`

All shared version constants live in the root `build.gradle` `ext {}` block.

## Architecture & Data Flow

The core flow is Muzei-driven via WorkManager:

```
Muzei → ArtProvider.onLoadRequested()
  → WorkManager chain:
      SetupWorker         (validates client ID, NSFW flag)
      → ArtLoadWorker × N (one per enabled Source/subreddit)
          → ARAW RedditClient → Reddit API (SubmissionsFetcher)
          → DBHelper (SQLite cache)
          → Filter (score, votes, comments, image size/orientation)
          → ProviderContract (Muzei artwork add/remove)
      → CleanupWorker     (prunes stale DB entries)
```

**Key classes in `:app`:**
- `ArtProvider` — Muzei `MuzeiArtProvider`, entry point for wallpaper loading
- `Source` — Parcelable subreddit config (filter thresholds, orientation, sort, etc.)
- `LWIActivity` / `LWIViewModel` — Launcher UI and its ViewModel
- `LWIApplication` — Initializes Picasso (OkHttp-backed) and WorkManager configuration
- `data/RedditDatabase` — `SQLiteOpenHelper` (v7), tables: `subreddits`, `sub_topics`, `topic_images`, `favorite_artwork`, `ignore_artwork`
- `data/DBHelper` — All static data-access methods (query/insert/delete)
- `work/` — WorkManager workers (`SetupWorker`, `ArtLoadWorker`, `CleanupWorker`, `VerifyClientIdWorker`)
- `asynchronous/` — Custom thread pool (`TaskRunner`, `RunnableTask`) replacing deprecated `AsyncTask`
- `preview/` — `SourcesActivity`, `FavoriteActivity`, `SubredditActivity` with RecyclerView adapters
- `preference/` — `SettingsActivity`, `SettingsFragment`, custom preference widgets

## Code Style

Spotless enforces formatting automatically. For Java: **Palantir Java Format** (google-java-format fork) + CleanThat mutators. For Kotlin: **ktlint** v0.47.0. Always run `spotlessApply` before committing to avoid CI failures. The `// @formatter:off` / `// @formatter:on` toggle is supported for special blocks.
