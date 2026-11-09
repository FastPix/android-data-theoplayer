# FastPix Video Data SDK for THEOplayer - monitor playback performance and QoE on Android

[![License](https://img.shields.io/badge/License-Proprietary-blue.svg)](LICENSE)
[![Version](https://img.shields.io/badge/version-0.0.6-green.svg)](CHANGELOG.md)
[![Min SDK](https://img.shields.io/badge/minSdk-24-orange.svg)](theo-player-data/build.gradle.kts)

The **FastPix Video Data SDK for THEOplayer** is a third-party analytics SDK that monitors video playback performance, quality of service (QoS/QoE), and user interactions in **THEOplayer on Android**. It attaches to your `THEOplayerView`, automatically tracks playback events, and reports real-time performance and user engagement data to the [FastPix dashboard](https://dashboard.fastpix.com).

It is **not a video player** and does not replace THEOplayer. It is the monitoring layer you add to the THEOplayer you already ship - the same role Mux Data plays - so you can track startup time, rebuffering, bitrate, errors, and viewer engagement without building an analytics pipeline.

**Works with:** THEOplayer Android SDK 5.0+ (tested with 10.5.0) · Android 7.0+ (API 24) · Kotlin and Java · `io.fastpix.data:theo`

[Monitor THEOplayer docs](https://fastpix.com/docs/video-data/monitors/theoplayer) · [Create a free account](https://dashboard.fastpix.com/signup?utm_source=github&utm_medium=referral&utm_campaign=github_signup)

## Jump to

Skip straight to a section without scrolling:

| Get started | Understand | Reference |
|---|---|---|
| [Requirements](#requirements) | [Why FastPix](#why-fastpix) | [Detailed configuration](#detailed-configuration) |
| [Install the SDK](#installation) | [Who it's for](#who-its-for) | [Which FastPix SDK?](#which-fastpix-sdk-for-which-android-player) |
| [Configure your THEOplayer license](#configure-your-theoplayer-license) | [What you can track](#what-you-can-track) | [FAQ](#faq) |
| [Quick start](#quick-start) | [Lifecycle management](#lifecycle-management) | [Troubleshooting](#troubleshooting) |
| [Complete sample](#complete-sample-player-implementation) | | |

## Why FastPix

THEOplayer fires playback events, but turning them into viewer-facing quality metrics, storing them, and watching them across devices and app versions is work you would otherwise build yourself. FastPix does it for you:

- **Real-time QoE monitoring.** Startup time, rebuffering, bitrate and resolution changes, and errors as they happen on real devices, in one dashboard.
- **Automatic event tracking.** The SDK listens to THEOplayer's native events, so you keep full control of playback and add monitoring with a few lines.
- **One data model across platforms.** The same FastPix Video Data metrics are reported from web, iOS, Android, and other player integrations, so you compare experiences like for like.
- **Your metadata, your dashboard.** Tag views with video, player, and up to ten custom fields and analyze them in the FastPix dashboard.

## Who it's for

- **Android teams shipping THEOplayer** who want production QoE monitoring without building an analytics backend.
- **Streaming, OTT, and broadcast apps** that need to catch rebuffering, slow startup, and playback errors before users churn.
- **Teams standardizing analytics across platforms** who want THEOplayer data in the same FastPix dashboard as their web and iOS players.

## What you can track

- **Automatic event tracking** - automatically captures all playback events (play, pause, seek, buffering, etc.)
- **THEOplayer integration** - built specifically for the THEOplayer Android SDK
- **Real-time analytics** - instant access to video performance metrics on the FastPix dashboard
- **Minimal setup** - easy integration with just a few lines of code
- **Custom metadata** - support for custom video and player metadata

## Requirements

- **Minimum Android SDK**: 24 (Android 7.0)
- **Target/Compile SDK**: 35+
- **THEOplayer SDK**: 5.0+ (tested with 10.5.0)
- **A THEOplayer SDK license**: required to play your own content (see [Configure your THEOplayer license](#configure-your-theoplayer-license))
- **Kotlin**: 2.0.21+
- **Java**: 11

You will also need a FastPix account and a workspace key. [Create a free account](https://dashboard.fastpix.com/signup?utm_source=github&utm_medium=referral&utm_campaign=github_signup), then follow [set up a workspace](https://fastpix.com/docs/getting-started/set-up-a-workspace) to find your workspace key.

## Installation

### Add the repositories

In your project's `settings.gradle.kts`, add all three repositories the SDK needs. THEOplayer is not on Maven Central, and the FastPix core library lives in its own GitHub Packages repository:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        // THEOplayer SDK (not on Maven Central)
        maven { url = uri("https://maven.theoplayer.com/releases") }
        // FastPix THEOplayer SDK
        maven {
            url = uri("https://maven.pkg.github.com/FastPix/android-data-theoplayer")
            credentials {
                val props = java.util.Properties()
                val f = file("${rootDir}/local.properties")
                if (f.exists()) f.inputStream().use { props.load(it) }
                username = props.getProperty("gpr.user")
                password = props.getProperty("gpr.key")
            }
        }
        // FastPix core (transitive dependency of the THEOplayer SDK)
        maven {
            url = uri("https://maven.pkg.github.com/FastPix/android-core-data-sdk")
            credentials {
                val props = java.util.Properties()
                val f = file("${rootDir}/local.properties")
                if (f.exists()) f.inputStream().use { props.load(it) }
                username = props.getProperty("gpr.user")
                password = props.getProperty("gpr.key")
            }
        }
    }
}
```

### Configure authentication

Create or update `local.properties` in your project root with your GitHub credentials. This file is git-ignored, so your token is never committed:

```properties
gpr.user=YOUR_GITHUB_USERNAME
gpr.key=YOUR_GITHUB_PERSONAL_ACCESS_TOKEN
```

Use a GitHub personal access token with the `read:packages` scope.

### Add Dependencies

Add the FastPix THEOplayer SDK to your app's `build.gradle.kts`. The THEOplayer SDK (`com.theoplayer.theoplayer-sdk-android:core`) and the FastPix core library resolve automatically as transitive dependencies, provided the repositories above are configured:

```kotlin
dependencies {
    // FastPix THEOplayer SDK (pulls in THEOplayer + FastPix core)
    implementation("io.fastpix.data:theo:0.0.6")
}
```

## Configure your THEOplayer license

THEOplayer requires a license to play your own content. The SDK ships with a **baked-in demo license** that only covers THEOplayer's demo streams (`cdn.theoplayer.com`) - if you point it at your own video URL without a license, playback fails and Logcat shows:

```
THEOplayer License: The baked-in license is used. Please update the configuration with your license
```

1. Get your license string from the [THEOplayer portal](https://portal.theoplayer.com) (the same account you use for the THEOplayer Maven repository).
2. Apply it when you create the player, either in code:

   ```kotlin
   import com.theoplayer.android.api.THEOplayerConfig
   import com.theoplayer.android.api.THEOplayerView

   val config = THEOplayerConfig.Builder()
       .license("YOUR_THEOPLAYER_LICENSE_STRING")
       .build()
   val theoPlayerView = THEOplayerView(this, config)
   ```

   or as an attribute in your layout XML:

   ```xml
   <com.theoplayer.android.api.THEOplayerView
       android:id="@+id/theo_player_view"
       android:layout_width="match_parent"
       android:layout_height="match_parent"
       app:license="YOUR_THEOPLAYER_LICENSE_STRING" />
   ```

Attach the FastPix SDK to the licensed `THEOplayerView` exactly as shown in [Quick start](#quick-start).

## Quick Start

### Add THEOplayerView to Your Layout

Add the `THEOplayerView` to your activity/fragment layout (include your `app:license` here, or set it in code - see [Configure your THEOplayer license](#configure-your-theoplayer-license)):

```xml
<com.theoplayer.android.api.THEOplayerView
    android:id="@+id/theo_player_view"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

### Initialize FastPix SDK in Your Activity

Here's a complete example of how to integrate FastPix SDK with THEOplayer. Replace `workSpaceId` with your real workspace key from the [FastPix dashboard](https://dashboard.fastpix.com):

```kotlin
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.theoplayer.android.api.source.SourceDescription
import com.theoplayer.android.api.source.SourceType
import com.theoplayer.android.api.source.TypedSource
import io.fastpix.data.domain.model.CustomDataDetails
import io.fastpix.data.domain.model.PlayerDataDetails
import io.fastpix.data.domain.model.VideoDataDetails
import io.fastpix.theo_player_data.CustomerData
import io.fastpix.theo_player_data.FastPixBaseTheoPlayer
import io.fastpix.theo_data.databinding.ActivityMainBinding
import java.util.UUID

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var fastPixTheoPlayer: FastPixBaseTheoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configure THEOplayer first
        configureTHEOplayer()

        // Then configure FastPix SDK
        configureFastPix()
    }

    private fun configureTHEOplayer() {
        // Create video source
        val typedSource = TypedSource
            .Builder("https://your-video-url.m3u8") // Your video URL
            .type(SourceType.HLS) // or SourceType.MP4, SourceType.DASH, etc.
            .build()

        val sourceDescription = SourceDescription
            .Builder(typedSource)
            .build()

        // Set source to player
        binding.theoPlayerView.player.source = sourceDescription
        binding.theoPlayerView.player.isAutoplay = true
    }

    private fun configureFastPix() {
        // Create video metadata
        val videoDataDetails = VideoDataDetails(
            videoId = "video-id",
            videoTitle = "Sample Video",
            videoSeries = "Sample Series",
            videoProducer = "Producer Name",
            videoContentType = "Video Content Type",
            videoVariant = "HD",
            videoLanguage = "en",
            videoDrmType = null // or "Widevine", "FairPlay", etc.
        )

        // Create customer data configuration
        val customerData = CustomerData(
            workSpaceId = "YOUR_WORKSPACE_KEY", // Your FastPix workspace key
            beaconUrl = null, // Optional: Custom beacon URL
            videoDetails = videoDataDetails,
            playerData = PlayerDataDetails(
                playerName = "THEOplayer",
                playerVersion = "10.5.0" // Your THEOplayer version
            ),
            customDataDetails = CustomDataDetails(
                customField1 = "custom-value-1",
                customField2 = "custom-value-2"
                // Add up to customField10 if needed
            )
        )

        // Initialize FastPix SDK
        fastPixTheoPlayer = FastPixBaseTheoPlayer(
            context = this,
            playerView = binding.theoPlayerView,
            customerData = customerData,
            enableLogging = true // Set to false in production
        )
    }

    override fun onResume() {
        super.onResume()
        binding.theoPlayerView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.theoPlayerView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Important: Release THEOplayer and FastPix SDK
        binding.theoPlayerView.onDestroy()
        fastPixTheoPlayer?.release()
        fastPixTheoPlayer = null
    }
}
```

## Detailed Configuration

### CustomerData Parameters

The `CustomerData` class accepts the following parameters:

| Parameter           | Type              | Required | Description                                       |
|---------------------|-------------------|----------|---------------------------------------------------|
| `workSpaceId`       | String            | ✅        | Your FastPix workspace key                        |
| `beaconUrl`         | String            | ❌        | Custom beacon URL (default: metrix.ws)            |
| `videoDetails`      | VideoDataDetails  | ❌        | Video metadata (see below)                        |
| `playerData`        | PlayerDataDetails | ❌        | Player information (default: "theo-player", "5+") |
| `customDataDetails` | CustomDataDetails | ❌        | Custom metadata fields                            |

### VideoDataDetails

Configure video metadata for better analytics:

```kotlin
val videoDataDetails = VideoDataDetails(
    videoId = "unique-video-id",           // Required
    videoTitle = "Video Title",            // Required
    videoSeries = "Series Name",           // Optional
    videoProducer = "Producer Name",       // Optional
    videoContentType = "Movie/TV Show",    // Optional
    videoVariant = "HD/SD/4K",             // Optional
    videoLanguage = "en",                  // Optional
    videoDrmType = "Widevine"              // Optional
)
```

### CustomDataDetails

Add custom metadata fields (up to 10 fields):

```kotlin
val customDataDetails = CustomDataDetails(
    customField1 = "value1",
    customField2 = "value2",
    // ... up to customField10
)
```

## Complete Sample Player Implementation

Here's a more complete example with custom controls:

```kotlin
class VideoPlayerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var fastPixTheoPlayer: FastPixBaseTheoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configure THEOplayer
        setupTHEOplayer()

        // Configure FastPix SDK
        setupFastPix()

        // Setup UI controls
        setupControls()
    }

    private fun setupTHEOplayer() {
        videoUrl?.let { url ->
            val typedSource = TypedSource.Builder("https://example-url.m3u8")
                .type(SourceType.HLS)
                .build()

            val sourceDescription = SourceDescription.Builder(typedSource).build()
            binding.theoPlayerView.player.source = sourceDescription
            binding.theoPlayerView.player.isAutoplay = true

            // Listen to player events for UI updates
            binding.theoPlayerView.player.addEventListener(PlayerEventTypes.PLAY) {
                updatePlayPauseButton(playing = true)
            }

            binding.theoPlayerView.player.addEventListener(PlayerEventTypes.PAUSE) {
                updatePlayPauseButton(playing = false)
            }
        }
    }

    private fun setupFastPix() {
        val videoDataDetails = VideoDataDetails(
            videoId = "video-id",
            videoTitle = "video-title",
            videoSeries = "Sample Series",
            videoProducer = "Sample Producer",
            videoContentType = "Video Content",
            videoVariant = "HD",
            videoLanguage = "en"
        )

        val customerData = CustomerData(
            workSpaceId = "YOUR_WORKSPACE_KEY",
            videoDetails = videoDataDetails,
            playerData = PlayerDataDetails("THEOplayer", "10.5.0"),
            customDataDetails = CustomDataDetails(
                customField1 = "custom-data-1"
            )
        )

        fastPixTheoPlayer = FastPixBaseTheoPlayer(
            context = this,
            playerView = binding.theoPlayerView,
            customerData = customerData,
            enableLogging = BuildConfig.DEBUG
        )
    }

    private fun setupControls() {
        // Setup your custom controls here
        binding.playPauseButton.setOnClickListener {
            if (binding.theoPlayerView.player.isPaused) {
                binding.theoPlayerView.player.play()
            } else {
                binding.theoPlayerView.player.pause()
            }
        }
    }

    private fun updatePlayPauseButton(playing: Boolean) {
        // Update UI based on playback state
    }

    override fun onResume() {
        super.onResume()
        binding.theoPlayerView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.theoPlayerView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.theoPlayerView.onDestroy()
        fastPixTheoPlayer?.release()
        fastPixTheoPlayer = null
    }
}
```

## Lifecycle Management

It's crucial to properly manage the SDK lifecycle:

1. **Initialize** the SDK after THEOplayer is configured
2. **Call `onResume()`** and `onPause()` on THEOplayerView in your activity lifecycle
3. **Always call `release()`** in `onDestroy()` to clean up resources

```kotlin
override fun onDestroy() {
    super.onDestroy()
    binding.theoPlayerView.onDestroy()
    fastPixTheoPlayer?.release()
    fastPixTheoPlayer = null
}
```

## Switching Videos

When switching to a new video, release and reinitialize:

```kotlin
private fun switchToNewVideo(newVideoUrl: String, newVideoTitle: String) {
    // Release existing SDK instance
    fastPixTheoPlayer?.release()
    fastPixTheoPlayer = null

    // Update THEOplayer source
    val typedSource = TypedSource.Builder(newVideoUrl)
        .type(SourceType.HLS)
        .build()
    binding.theoPlayerView.player.source = SourceDescription.Builder(typedSource).build()

    // Reinitialize FastPix SDK with new video data
    setupFastPix()
}
```

## Debugging

Enable logging during development:

```kotlin
fastPixTheoPlayer = FastPixBaseTheoPlayer(
    context = this,
    playerView = binding.theoPlayerView,
    customerData = customerData,
    enableLogging = true // Set to false in production
)
```

Logs will appear in Logcat with the tag `FastPixBaseTheoPlayer`.

## Troubleshooting

### Playback does not start / "baked-in license" warning

- If Logcat shows `THEOplayer License: The baked-in license is used`, configure your own THEOplayer license. See [Configure your THEOplayer license](#configure-your-theoplayer-license). The baked-in license only plays THEOplayer's demo streams (`cdn.theoplayer.com`).

### SDK Not Tracking Events

- Ensure you've initialized the SDK after configuring THEOplayer
- Check that `workSpaceId` is correct
- Verify THEOplayer events are firing (check THEOplayer logs)
- Enable logging to see FastPix SDK activity

### Dependencies fail to resolve

- Confirm all three repositories are configured (THEOplayer, FastPix THEOplayer SDK, FastPix core) - see [Installation](#installation)
- Confirm `gpr.user` / `gpr.key` are set in `local.properties` and your token has the `read:packages` scope

### Memory Leaks

- Always call `release()` in `onDestroy()`
- Ensure `THEOplayerView.onDestroy()` is called before releasing FastPix SDK

### Missing Events

- The SDK automatically tracks all events from THEOplayer
- Events are tracked based on THEOplayer's native event system
- Check that THEOplayer is properly configured and receiving events

## Which FastPix SDK for which Android player

FastPix ships a Video Data SDK per player. Pick the one that matches the player your app ships:

| Your Android player | Use this SDK |
| --- | --- |
| **THEOplayer** | **FastPix/android-data-theoplayer** (this repository) |
| **AndroidX Media3** (the current ExoPlayer) | [FastPix/android-data-androidXmedia3](https://github.com/FastPix/android-data-androidXmedia3) |
| **Google ExoPlayer** (standalone / legacy) | [FastPix/android-data-exoplayer-sdk](https://github.com/FastPix/android-data-exoplayer-sdk) |
| **Brightcove Player** | [FastPix/android-data-brightcove](https://github.com/FastPix/android-data-brightcove) |
| Any player, shared core layer | [FastPix/android-core-data-sdk](https://github.com/FastPix/android-core-data-sdk) |

## FAQ

**How do I monitor THEOplayer performance on Android?**
Add the FastPix Video Data SDK, then create a `FastPixBaseTheoPlayer` with your `THEOplayerView`, workspace key, and optional metadata. It automatically tracks playback events and reports real-time performance and user engagement data to the FastPix dashboard. See [Install the SDK](#installation) and [Quick start](#quick-start).

**What metrics does it track?**
Playback and quality-of-experience signals: play/pause/seek/buffering events, startup time, rebuffering, bitrate and resolution changes, errors, and viewer engagement, plus any custom metadata you attach.

**Is this a video player?**
No. It does not play or render video. It is a monitoring SDK that attaches to the THEOplayer you already use.

**Do I need a THEOplayer license?**
Yes, to play your own content. THEOplayer's baked-in license only covers its demo streams. See [Configure your THEOplayer license](#configure-your-theoplayer-license).

**Does THEOplayer have built-in analytics, and how is this different?**
THEOplayer integrates with third-party analytics and QoE providers through connectors. FastPix is one such Video Data SDK: it collects real-time performance and user engagement data on Android and unifies it with your other FastPix-monitored players in one dashboard.

**How does it compare to Mux Data for THEOplayer?**
Both are third-party Video Data SDKs for THEOplayer. FastPix reports the same class of QoE metrics and unifies THEOplayer data with your web, iOS, and other FastPix-monitored players.

**Do I need a FastPix account?**
Yes. You need a workspace key from your FastPix account to report data. [Create a free account](https://dashboard.fastpix.com/signup?utm_source=github&utm_medium=referral&utm_campaign=github_signup).

## Documentation

- [Monitor THEOplayer - FastPix docs](https://fastpix.com/docs/video-data/monitors/theoplayer)
- [FastPix Video Data overview](https://fastpix.com/docs/video-data/overview)
- [Use custom dimensions](https://fastpix.com/docs/video-data/use-custom-dimensions)
- [THEOplayer Android SDK docs](https://optiview.dolby.com/docs/theoplayer/android/)

## Support

For questions, issues, or feature requests:

- **Email**: support@fastpix.com
- **Documentation**: [FastPix Documentation](https://fastpix.com/docs)
- **GitHub Issues**: [Report an issue](https://github.com/FastPix/android-data-theoplayer/issues)

## License

Copyright © 2025 FastPix. All rights reserved.

This SDK is proprietary software. Unauthorized copying, modification, distribution, or use of this
software is strictly prohibited.
