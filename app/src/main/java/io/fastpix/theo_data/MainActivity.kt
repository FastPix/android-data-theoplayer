package io.fastpix.theo_data

import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.WindowInsetsController
import android.widget.SeekBar
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.theoplayer.android.api.event.player.PlayerEventTypes
import com.theoplayer.android.api.source.SourceDescription
import com.theoplayer.android.api.source.SourceType
import com.theoplayer.android.api.source.TypedSource
import io.fastpix.data.domain.model.CustomDataDetails
import io.fastpix.data.domain.model.PlayerDataDetails
import io.fastpix.data.domain.model.VideoDataDetails
import io.fastpix.theo_data.databinding.ActivityMainBinding
import io.fastpix.theo_player_data.CustomerData
import io.fastpix.theo_player_data.FastPixBaseTheoPlayer
import java.util.UUID

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var theoPlayerData: FastPixBaseTheoPlayer? = null
    private var isFullscreen = false
    private var controlsVisible = true
    private val controlsHandler = Handler(Looper.getMainLooper())
    private val hideControlsRunnable = Runnable { hideControls() }
    private var userTriggeredFullscreen = false
    private var videoModel: DummyData? = null
    private var currentVideoIndex: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            videoModel = intent.getParcelableExtra("video_model", DummyData::class.java)
        } else {
            videoModel = intent.getParcelableExtra("video_model")
        }
        // Find current video index in the list
        currentVideoIndex =
            dummyData.indexOfFirst { it.id == videoModel?.id && it.url == videoModel?.url }
        if (currentVideoIndex == -1 && videoModel != null) {
            // Fallback: try to find by id only
            currentVideoIndex = dummyData.indexOfFirst { it.id == videoModel?.id }
        }
        configureTheoplayer()
        configureFastPix()
        updateEpisodeButtons()
    }

    private fun configureFastPix() {
        val videoDataDetails = VideoDataDetails(
            videoId = UUID.randomUUID().toString(),
            videoTitle = videoModel?.id,
            videoSeries = "Video Series",
            videoProducer = "Video Producer",
            videoContentType = "Video Content Type",
            videoVariant = "Video Variant",
            videoLanguage = "Video Language",
            videoDrmType = "Drm Type"
        )
        val customerData = CustomerData(
            workSpaceId = "work-space-key",
            videoDetails = videoDataDetails,
            playerData = PlayerDataDetails("Theo-player", "5+"),
            customDataDetails = CustomDataDetails(
                customField1 = "field1",
                customField2 = "field2",
                // Add more custom fields as needed
            )
        )
        theoPlayerData = FastPixBaseTheoPlayer(
            this, binding.theoPlayerView,
            customerData = customerData,
            enableLogging = true
        )
    }

    private fun showControls() {
        controlsVisible = true
        binding.controlsContainer.visibility = View.VISIBLE
        startHideControlsTimer()
    }

    private fun toggleControls() {
        if (controlsVisible) {
            hideControls()
        } else {
            showControls()
        }
    }

    private fun startHideControlsTimer() {
        controlsHandler.removeCallbacks(hideControlsRunnable)
        controlsHandler.postDelayed(hideControlsRunnable, 3000) // Hide after 3 seconds
    }

    private fun hideControls() {
        controlsVisible = false
        binding.controlsContainer.visibility = View.GONE
    }

    private fun configureTheoplayer() {
        val typedSource = TypedSource
            .Builder(videoModel!!.url)
            .type(SourceType.HLS)
            .build()
        val sourceDescription = SourceDescription
            .Builder(typedSource)
            .build()
        binding.theoPlayerView.player.source = sourceDescription
        binding.theoPlayerView.player.isAutoplay = true
        binding.theoPlayerView.keepScreenOn = true
        // Show loader initially
        showLoader()

        // Setup loader event listeners
        setupLoaderEvents()

        binding.theoPlayerView.player.addEventListener(PlayerEventTypes.LOADEDMETADATA) {
            val duration = binding.theoPlayerView.player.duration
            binding.seekBar.max = duration.toInt()
            binding.durationText.text = formatTime(duration)
            // Hide loader when metadata is loaded (video is ready to play)
            Handler(Looper.getMainLooper()).postDelayed({
                if (!binding.theoPlayerView.player.isPaused) {
                    hideLoader()
                }
            }, 100)
        }

        // Setup play/pause buttons
        setupPlayPauseButtons()

        // Setup fullscreen button
        setupFullscreenButton()

        binding.theoPlayerView.player.addEventListener(PlayerEventTypes.TIMEUPDATE) {
            val currentTime = binding.theoPlayerView.player.currentTime
            val duration = binding.theoPlayerView.player.duration
            binding.seekBar.progress = currentTime.toInt()
            binding.currentTimeText.text = formatTime(currentTime)

            // Update buffered progress
            if (binding.theoPlayerView.player.buffered.firstOrNull()?.end != null) {
                val bufferedEnd = binding.theoPlayerView.player.buffered.getEnd(0)
                val bufferedFraction = if (duration > 0) bufferedEnd / duration else 0.0
                binding.seekBar.secondaryProgress = (bufferedFraction * 1000).toInt()
            }

            // updateBufferedProgress(duration)
        }

        // Update button icons on play/pause events
        binding.theoPlayerView.player.addEventListener(PlayerEventTypes.PLAY) {
            updatePlayPauseButtonIcons()
        }

        binding.theoPlayerView.setOnClickListener {
            toggleControls()
        }

        binding.theoPlayerView.player.addEventListener(PlayerEventTypes.PAUSE) {
            updatePlayPauseButtonIcons()
        }

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    binding.currentTimeText.text = formatTime(progress.toDouble())
                }
            }

            override fun onStartTrackingTouch(sb: SeekBar?) {
                // Show loader when user starts seeking
                showLoader()
            }

            override fun onStopTrackingTouch(sb: SeekBar?) {
                sb?.let {
                    binding.theoPlayerView.player.currentTime = it.progress.toDouble()
                }
            }
        })
        binding.forwardSeek.setOnClickListener {
            val currentTime = binding.theoPlayerView.player.currentTime
            binding.theoPlayerView.player.currentTime = currentTime + 10
        }

        binding.backwardSeek.setOnClickListener {
            val currentTime = binding.theoPlayerView.player.currentTime
            binding.theoPlayerView.player.currentTime = currentTime - 10
        }

        binding.nextEpisode.setOnClickListener {
            navigateToNextEpisode()
        }

        binding.previousEpisode.setOnClickListener {
            navigateToPreviousEpisode()
        }
        // Start timer to hide controls
        startHideControlsTimer()
    }

    private fun setupLoaderEvents() {
        binding.theoPlayerView.player.addEventListener(PlayerEventTypes.WAITING) {
            showLoader()
        }
        binding.theoPlayerView.player.addEventListener(PlayerEventTypes.SEEKING) {
            showLoader()
        }
        binding.theoPlayerView.player.addEventListener(PlayerEventTypes.LOADSTART) {
            showLoader()
        }
        binding.theoPlayerView.player.addEventListener(PlayerEventTypes.SEEKED) {
            if (!binding.theoPlayerView.player.isPaused) {
                hideLoader()
            }
        }
        binding.theoPlayerView.player.addEventListener(PlayerEventTypes.CANPLAY) {
            hideLoader()
        }
        binding.theoPlayerView.player.addEventListener(PlayerEventTypes.PLAYING) {
            hideLoader()
        }
        binding.theoPlayerView.player.addEventListener(PlayerEventTypes.LOADEDDATA) {
            hideLoader()
        }
    }

    private fun showLoader() {
        binding.loadingIndicator.visibility = View.VISIBLE
    }

    private fun hideLoader() {
        binding.loadingIndicator.visibility = View.GONE
    }

    private fun setupPlayPauseButtons() {
        val togglePlayPause = {
            if (binding.theoPlayerView.player.isPaused) {
                binding.theoPlayerView.player.play()
            } else {
                binding.theoPlayerView.player.pause()
            }
        }

        // Play/pause button in control bar
        binding.playPauseButton.setOnClickListener {
            togglePlayPause()
        }

        // Initial icon state
        updatePlayPauseButtonIcons()
    }

    private fun updatePlayPauseButtonIcons() {
        val isPlaying = !binding.theoPlayerView.player.isPaused
        val playIcon = R.drawable.ic_play_arrow
        val pauseIcon = R.drawable.ic_pause

        binding.playPauseButton.setImageResource(if (isPlaying) pauseIcon else playIcon)
    }

    private fun setupFullscreenButton() {
        binding.fullscreenButton.setOnClickListener {
            toggleFullscreen()
        }
    }

    private fun toggleFullscreen() {
        if (isFullscreen) {
            exitFullscreen()
        } else {
            enterFullscreen()
        }
    }

    private fun enterFullscreen() {
        isFullscreen = true
        userTriggeredFullscreen = true
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        hideSystemUI()
        binding.fullscreenButton.setImageResource(R.drawable.ic_fullscreen)
    }

    private fun exitFullscreen() {
        isFullscreen = false
        userTriggeredFullscreen = true
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        showSystemUI()
        binding.fullscreenButton.setImageResource(R.drawable.ic_fullscreen_exit)
    }


    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE)
        } else {
            window.decorView.systemUiVisibility =
                (View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        }
    }

    private fun showSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.show(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE)
        } else {
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_VISIBLE
        }
    }

    private fun formatTime(seconds: Double): String {
        val totalSeconds = seconds.toInt()
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val secs = totalSeconds % 60

        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, secs)
        } else {
            String.format("%02d:%02d", minutes, secs)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.theoPlayerView.onDestroy()
        theoPlayerData?.release()
        theoPlayerData = null

    }

    override fun onResume() {
        super.onResume()
        binding.theoPlayerView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.theoPlayerView.onPause()
    }

    private fun navigateToNextEpisode() {
        if (currentVideoIndex >= 0 && currentVideoIndex < dummyData.size - 1) {
            switchToEpisode(currentVideoIndex + 1)
        }
    }

    private fun navigateToPreviousEpisode() {
        if (currentVideoIndex > 0) {
            switchToEpisode(currentVideoIndex - 1)
        }
    }

    private fun switchToEpisode(newIndex: Int) {
        if (newIndex < 0 || newIndex >= dummyData.size) {
            Log.e(TAG, "Invalid episode index: $newIndex")
            return
        }

        // Release existing players
        try {

            // Release FastPix
            theoPlayerData?.release()
            videoModel = dummyData[newIndex]
            currentVideoIndex = newIndex
            theoPlayerData = null
            configureTheoplayer()
            // Reset UI
            resetPlayerUI()

            // Reinitialize players with new video
            configureFastPix()
            updateEpisodeButtons()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing players: ${e.message}")
        }


    }

    private fun resetPlayerUI() {
        binding.seekBar.progress = 0
        binding.seekBar.max = 0
        binding.seekBar.secondaryProgress = 0
        binding.currentTimeText.text = formatTime(0.0)
        binding.durationText.text = formatTime(0.0)
        showLoader()
    }

    private fun updateEpisodeButtons() {
        // Enable/disable buttons based on current position
        binding.nextEpisode.isEnabled =
            currentVideoIndex >= 0 && currentVideoIndex < dummyData.size - 1
        binding.previousEpisode.isEnabled = currentVideoIndex > 0

        // Optionally change alpha to show disabled state
        binding.nextEpisode.alpha = if (binding.nextEpisode.isEnabled) 1.0f else 0.5f
        binding.previousEpisode.alpha = if (binding.previousEpisode.isEnabled) 1.0f else 0.5f
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}