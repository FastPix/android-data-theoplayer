# FastPix THEO Player SDK

[![License](https://img.shields.io/badge/License-Proprietary-blue.svg)](LICENSE)
[![Version](https://img.shields.io/badge/version-0.0.3-green.svg)](CHANGELOG.md)
[![Min SDK](https://img.shields.io/badge/minSdk-24-orange.svg)](theo-player-data/build.gradle.kts)

The FastPix THEO Player SDK provides seamless integration between THEOplayer and the FastPix
analytics platform. This SDK automatically tracks video playback events, metrics, and analytics data
from your THEOplayer instances, enabling real-time monitoring and insights on the FastPix dashboard.

## Key Features

- **Automatic Event Tracking** – Automatically captures all playback events (play, pause, seek,
  buffering, etc.)
- **THEOplayer Integration** – Built specifically for THEOplayer Android SDK
- **Real-time Analytics** – Provides instant access to video performance metrics on the FastPix
  dashboard
- **Minimal Setup** – Easy integration with just a few lines of code
- **Custom Metadata** – Support for custom video and player metadata

## Requirements

- **Minimum Android SDK**: 24 (Android 7.0)
- **Target/Compile SDK**: 35+
- **THEOplayer SDK**: 5.0+
- **Kotlin**: 2.0.21+
- **Java**: 11

## Installation

### Step 1: Add GitHub Packages Repository

Add the GitHub Packages repository to your project's `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://maven.pkg.github.com/FastPix/android-data-theoplayer")
            credentials {
                username =
                    project.findProperty("lpr.user")
                password =
                    project.findProperty("lpr.key")
            }
        }
    }
}
```

### Step 2: Add Dependencies

Add the FastPix THEO Player SDK and THEOplayer dependencies to your app's `build.gradle.kts`:

```kotlin
dependencies {
    // FastPix THEO Player SDK
    implementation("io.fastpix.data:theo:0.0.4")
}
```

### Step 3: Configure Authentication

Create or update `local.properties` in your project root with your GitHub credentials:

```properties
lpr.user=YOUR_GITHUB_USERNAME
lpr.key=YOUR_GITHUB_PERSONAL_ACCESS_TOKEN
```

> **Note**: Make sure to add `local.properties` to your `.gitignore` to keep credentials secure.

## Quick Start

### 1. Add THEOplayerView to Your Layout

Add the `THEOplayerView` to your activity/fragment layout:

```xml

<com.theoplayer.android.api.THEOplayerView 
    android:id="@+id/theo_player_view"
    android:layout_width="match_parent" 
    android:layout_height="match_parent" />
```

### 2. Initialize FastPix SDK in Your Activity

Here's a complete example of how to integrate FastPix SDK with THEOplayer:

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
            workSpaceId = "workspace-id", // Your FastPix workspace ID
            beaconUrl = null, // Optional: Custom beacon URL
            videoDetails = videoDataDetails,
            playerData = PlayerDataDetails(
                playerName = "THEOplayer",
                playerVersion = "5.0" // Your THEOplayer version
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
| `workSpaceId`       | String            | ✅        | Your FastPix workspace identifier                 |
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
            workSpaceId = "workspace-id",
            videoDetails = videoDataDetails,
            playerData = PlayerDataDetails("THEOplayer", "5.0"),
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

### SDK Not Tracking Events

- Ensure you've initialized the SDK after configuring THEOplayer
- Check that `workSpaceId` is correct
- Verify THEOplayer events are firing (check THEOplayer logs)
- Enable logging to see FastPix SDK activity

### Memory Leaks

- Always call `release()` in `onDestroy()`
- Ensure `THEOplayerView.onDestroy()` is called before releasing FastPix SDK

### Missing Events

- The SDK automatically tracks all events from THEOplayer
- Events are tracked based on THEOplayer's native event system
- Check that THEOplayer is properly configured and receiving events

## Support

For questions, issues, or feature requests:

- **Email**: support@fastpix.io
- **Documentation**: [FastPix Documentation](https://docs.fastpix.io)
- **GitHub Issues**: [Report an issue](https://github.com/FastPix/android-data-theoplayer/issues)

## License

Copyright © 2025 FastPix. All rights reserved.

This SDK is proprietary software. Unauthorized copying, modification, distribution, or use of this
software is strictly prohibited.
