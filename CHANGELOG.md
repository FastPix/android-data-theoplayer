# Changelog

All notable changes to this project will be documented in this file.

## [0.0.6]
- Bumped `SDK_VERSION` to `0.0.6` in `THEOPlayerLibraryInfo`.
- Updated Maven publication version to `0.0.6` in `build.gradle.kts`.
- Updated SDK version references in `README.md` to `0.0.6`.
- Incremented `coreVersion` to `1.3.1` in `libs.versions.toml`.

## [`0.0.5`]
- Upgraded Core SDK to 1.3.0

## [0.0.4]

- Upgraded `coreVersion` dependency from `1.2.7` to `1.2.9`.
- Refined pulse event scheduling across playback events (`play`, `viewBegin`, `buffering`, and `seeked`) to keep heartbeat behavior consistent during active playback.
- Improved `release()` cleanup by cancelling pulse events and releasing analytics only when SDK is initialized.

## [0.0.3]
Update FastPix THEO Player SDK to version 0.0.3

- Transitioned to `FastPixAnalytics` for SDK initialization and management.
- Implemented a heartbeat pulse mechanism using coroutines to dispatch periodic pulse events during playback.
- Updated event dispatching logic to manage pulse scheduling and cancellations across player states (playing, pause, buffering, etc.).
- Refactored `FastPixBaseTheoPlayer` to use nullable `fastPixDataSDK` and improved resource cleanup in `release()`.
- Updated `preLoad`, `sourceFps`, and `sourceAdvertiseFrameRate` overrides to match updated SDK interfaces.
- Bumped `coreVersion` and library metadata to `0.0.3`.

## [0.0.2]
 - Version Upgrade

## [0.0.1]

### Added
- Initial release of FastPix THEO Player SDK
- **THEOplayer Integration**: Built specifically for THEOplayer Android SDK 5.0+
- **Automatic Event Tracking**: Automatically captures all playback events (play, pause, seek, buffering, etc.)
- **Real-time Analytics**: Provides instant access to video performance metrics on the FastPix dashboard
- **Custom Metadata Support**: Support for custom video and player metadata
- **Minimal Setup**: Easy integration with just a few lines of code

