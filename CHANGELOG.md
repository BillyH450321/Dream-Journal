# Changelog

All notable changes to Dream Journal are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.0.0] - 2026-07-08

### Added

- Initial Android project with Gradle configuration, base activity, theme, and local development docs
- Dream titles with Gemini auto-generation and manual editing on the detail screen
- Titles shown in dashboard list, drawer, and search
- Voice recording with audio playback (play/pause and seek) on recorded dreams
- In-app API key settings for Gemini
- **Analyze Later** mode to save dreams without triggering immediate API calls
- **Analyze Later** toggle on the dashboard with persisted preference
- Phase 1 usage limits: 3 free AI analyses per month tracked in DataStore
- Paywall UI, usage status display, and dev Pro toggle for testing

### Fixed

- Gradle sync issues (signing config and wrapper)
- Gradle wrapper bumped to 9.3.1
- Gemini API retries on HTTP 503 with fallback to alternate models
- Improved handling of Gemini 503 errors
- `GEMINI_API_KEY` explicitly loaded from `.env` into `BuildConfig`
- Request throttling to reduce Gemini rate-limit (429) errors

[Unreleased]: https://github.com/BillyH450321/Dream-Journal/compare/main...HEAD
[1.0.0]: https://github.com/BillyH450321/Dream-Journal/compare/f88894b...HEAD