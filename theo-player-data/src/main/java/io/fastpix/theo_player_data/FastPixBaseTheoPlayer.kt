package io.fastpix.theo_player_data

import android.content.Context
import android.content.res.Configuration
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.theoplayer.android.api.THEOplayerView
import com.theoplayer.android.api.event.EventListener
import com.theoplayer.android.api.event.player.ErrorEvent
import com.theoplayer.android.api.event.player.PlayerEventTypes
import com.theoplayer.android.api.event.track.mediatrack.video.VideoTrackEventTypes
import com.theoplayer.android.api.event.track.mediatrack.video.list.AddTrackEvent
import com.theoplayer.android.api.event.track.mediatrack.video.list.VideoTrackListEventTypes
import com.theoplayer.android.api.player.PreloadType
import com.theoplayer.android.api.player.ReadyState
import io.fastpix.data.FastPixAnalytics
import io.fastpix.data.FastPixDataSDK
import io.fastpix.data.domain.SDKConfiguration
import io.fastpix.data.domain.enums.PlayerEventType
import io.fastpix.data.domain.listeners.PlayerListener
import io.fastpix.data.domain.model.BandwidthModel
import io.fastpix.data.domain.model.ErrorModel
import io.fastpix.data.domain.model.PlayerDataDetails
import io.fastpix.data.utils.Logger
import io.fastpix.theo_player_data.info.THEOPlayerLibraryInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.ceil

class FastPixBaseTheoPlayer(
    private val context: Context,
    private val playerView: THEOplayerView,
    private val customerData: CustomerData,
    private val enableLogging: Boolean = false
) : PlayerListener {
    private var fastPixDataSDK: FastPixDataSDK? = null
    private val TAG = "FastPixBaseTheoPlayer"
    private val handler = Handler(Looper.getMainLooper())
    private var currentEventState: PlayerEvent? = null
    private var sourceFrameRate: Int? = null
    private var sourceBitrate: Int? = null
    private var videoSourceWidth: Int? = 0
    private var videoSourceHeight: Int? = 0
    private var playbackPosition: Double = 0.0
    private var isBuffering = false
    private var sourceDuration: Int? = null
    private var isPlaying = false
    private var errorCode: String? = null
    private var previousReadyState: ReadyState? = null
    private var errorMessage: String? = null
    private var isReleased = false

    private val PULSE_INTERVAL = 10_000L // 1 second
    private val isPulseScheduled = AtomicBoolean(false)
    private val dispatcherScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var pulseJob: Job? = null


    private fun initializeFastPixSDK() {
        val sdkConfiguration = SDKConfiguration(
            workspaceId = customerData.workSpaceId,
            beaconUrl = customerData.beaconUrl,
            playerData = customerData.playerData ?: PlayerDataDetails(
                "theo-player", playerView.version
            ),
            videoData = customerData.videoDetails,
            playerListener = this,
            enableLogging = enableLogging,
            customData = customerData.customDataDetails
        )
        FastPixAnalytics.initialize(sdkConfiguration, context)
        fastPixDataSDK = FastPixAnalytics.getSDK()
        dispatchViewBegin()
        dispatchPlayerReady()
    }

    init {
        initializeFastPixSDK()
        val handleAddTrackEvent = EventListener<AddTrackEvent> { addTrackEvent ->
            val track = addTrackEvent.track

            track.addEventListener(
                VideoTrackEventTypes.ACTIVEQUALITYCHANGEDEVENT
            ) { activeQualityChangedEvent ->

                val vQuality = activeQualityChangedEvent.quality ?: return@addEventListener

                // NOTE: TheoPlayer HLS/MPD often doesn't expose advertised bitrate here
                // val sourceAdvertisedBitrate = vQuality.bandwidth // <- if available in your version

                if (vQuality.frameRate > 0) {
                    sourceFrameRate = vQuality.frameRate.toInt()
                }

                videoSourceWidth = vQuality.width
                videoSourceHeight = vQuality.height
                dispatchVariantChange()
            }
        }

        playerView.player.videoTracks.addEventListener(
            VideoTrackListEventTypes.ADDTRACK, handleAddTrackEvent
        )

        playerView.player.addEventListener(PlayerEventTypes.TIMEUPDATE) { timeUpdate ->
            playbackPosition = timeUpdate.currentTime
        }

        playerView.player.addEventListener(PlayerEventTypes.READYSTATECHANGE) { readyStateChange ->
            val state = readyStateChange.readyState
            state.let {
                if (previousReadyState != null && (state.ordinal < ReadyState.HAVE_FUTURE_DATA.ordinal) && (state.ordinal < (previousReadyState?.ordinal
                        ?: 0))
                ) {
                    dispatchBuffering()
                }
                previousReadyState = state
            }
        }

        playerView.player.addEventListener(PlayerEventTypes.DURATIONCHANGE) { durationChangeEvent ->
            sourceDuration = durationChangeEvent.duration.toInt()
        }

        playerView.player.addEventListener(PlayerEventTypes.WAITING) { _ ->
            isBuffering = true
            dispatchBuffering()
        }

        playerView.player.addEventListener(PlayerEventTypes.LOADEDMETADATA) {
            sourceDuration = Utils.secondToMs(playerView.player.duration)
        }


        playerView.player.addEventListener(PlayerEventTypes.PLAY) { _ ->
            dispatchPlay()
        }

        playerView.player.addEventListener(PlayerEventTypes.PLAYING) { _ ->
            if (isBuffering) {
                isBuffering = false
                dispatchBufferedEvent()
            }

            videoSourceWidth = playerView.player.videoWidth
            videoSourceHeight = playerView.player.videoHeight
            dispatchPlaying()
        }

        playerView.player.addEventListener(PlayerEventTypes.PAUSE) { _ ->
            dispatchPause()
        }
        playerView.player.addEventListener(PlayerEventTypes.SEEKING) { _ ->
            dispatchPause()
            dispatchSeeking()
        }

        playerView.player.addEventListener(PlayerEventTypes.SEEKED) { seeked ->
            handler.post {
                val currentTime = playerView.player.currentTime
                val positionMs = Utils.secondToMs(currentTime)
                dispatchSeeked(positionMs)
                if (isPlaying) {
                    dispatchPlaying()
                }
            }
        }

        playerView.player.addEventListener(PlayerEventTypes.ENDED) { _ ->
            dispatchPause()
            dispatchEnded()
        }

        playerView.player.addEventListener(PlayerEventTypes.ERROR) { error ->
            dispatchError(error)
        }
    }

    private fun dispatchBufferedEvent() {
        if (isReleased || fastPixDataSDK == null) return
        if (transitionToEvent(PlayerEvent.BUFFERED)) {
            if (enableLogging) {
                Log.d(TAG, "Dispatching Buffered event")
            }
            cancelPulseEvent()
            fastPixDataSDK?.dispatchEvent(PlayerEventType.buffered)
        }
    }

    private fun dispatchBuffering() {
        if (isReleased || fastPixDataSDK == null) return
        if (transitionToEvent(PlayerEvent.BUFFERING)) {
            if (enableLogging) {
                Log.d(TAG, "Dispatching Buffering event")
            }
            cancelPulseEvent()
            fastPixDataSDK?.dispatchEvent(PlayerEventType.buffering)
        }
    }

    private fun dispatchError(error: ErrorEvent) {
        if (isReleased || fastPixDataSDK == null) return
        if (transitionToEvent(PlayerEvent.ERROR)) {
            if (enableLogging) {
                Log.d(TAG, "Dispatching Error event: ${error.errorObject.cause}")
            }
            errorCode = error.errorObject.code.id.toString()
            errorMessage = "${error.errorObject.code}"
            cancelPulseEvent()
            fastPixDataSDK?.dispatchEvent(PlayerEventType.error)
        }
    }

    private fun dispatchEnded() {
        if (isReleased || fastPixDataSDK == null) return
        isPlaying = false
        if (transitionToEvent(PlayerEvent.ENDED)) {
            if (enableLogging) {
                Log.d(TAG, "Dispatching Ended event")
            }
            cancelPulseEvent()
            fastPixDataSDK?.dispatchEvent(PlayerEventType.ended)
        }
    }

    private fun dispatchSeeked(currentTime: Int) {
        if (isReleased || fastPixDataSDK == null) return
        if (transitionToEvent(PlayerEvent.SEEKED)) {
            if (enableLogging) {
                Log.d(TAG, "Dispatching Seeked event")
            }
            cancelPulseEvent()
            fastPixDataSDK?.dispatchEvent(PlayerEventType.seeked, currentTime)
        }
    }

    private fun dispatchSeeking() {
        if (isReleased || fastPixDataSDK == null) return
        if (transitionToEvent(PlayerEvent.SEEKING)) {
            if (enableLogging) {
                Log.d(TAG, "Dispatching Seeking event")
            }
            cancelPulseEvent()
            fastPixDataSDK?.dispatchEvent(PlayerEventType.seeking)
        }
    }

    private fun dispatchPause() {
        if (isReleased || fastPixDataSDK == null) return
        isPlaying = false
        if (transitionToEvent(PlayerEvent.PAUSE)) {
            if (enableLogging) {
                Log.d(TAG, "Dispatching Pause event")
            }
            cancelPulseEvent()
            fastPixDataSDK?.dispatchEvent(PlayerEventType.pause)
        }
    }

    private fun dispatchVariantChange() {
        if (isReleased || fastPixDataSDK == null) return
        if (transitionToEvent(PlayerEvent.VARIANT_CHANGED)) {
            if (enableLogging) {
                Log.d(TAG, "Dispatching VariantChange event")
            }
            schedulePulseEvents()
            fastPixDataSDK?.dispatchEvent(PlayerEventType.variantChanged)
        }
    }

    /**
     * Safely transitions to a new event state if valid
     */
    private fun transitionToEvent(newEvent: PlayerEvent): Boolean {
        if (isValidTransition(newEvent)) {
            if (newEvent != PlayerEvent.VARIANT_CHANGED) {
                currentEventState = newEvent
            }
            return true
        } else {
            return false
        }
    }

    /**
     * Validates if the transition from current state to new state is valid
     */
    private fun isValidTransition(newEvent: PlayerEvent): Boolean {
        val allowedTransitions = validTransitions[currentEventState] ?: emptySet()
        return newEvent in allowedTransitions
    }

    private fun dispatchPlaying() {
        if (isReleased || fastPixDataSDK == null) return
        isPlaying = true
        if (transitionToEvent(PlayerEvent.PLAYING)) {
            if (enableLogging) {
                Log.d(TAG, "Dispatching Playing event")
            }
            schedulePulseEvents()
            fastPixDataSDK?.dispatchEvent(PlayerEventType.playing)
        }
    }

    private fun dispatchPlayerReady() {
        if (isReleased || fastPixDataSDK == null) return
        if (enableLogging) {
            Log.d(TAG, "Dispatching Play Ready event")
        }
        cancelPulseEvent()
        fastPixDataSDK?.dispatchEvent(PlayerEventType.playerReady)
    }

    private fun dispatchViewBegin() {
        if (isReleased || fastPixDataSDK == null) return
        if (enableLogging) {
            Log.d(TAG, "Dispatching ViewBegin event")
        }
        cancelPulseEvent()
        fastPixDataSDK?.dispatchEvent(PlayerEventType.viewBegin)
    }

    private fun dispatchPlay() {
        if (isReleased || fastPixDataSDK == null) return
        isPlaying = true
        if (transitionToEvent(PlayerEvent.PLAY)) {
            if (enableLogging) {
                Log.d(TAG, "Dispatching Play event")
            }
            cancelPulseEvent()
            fastPixDataSDK?.dispatchEvent(PlayerEventType.play)
        }
    }

    override fun playerHeight(): Int? {
        val density = context.resources.displayMetrics.density
        val rawHeight = playerView.height
        val height = ceil(rawHeight / density)
        return height.toInt()
    }

    override fun playerWidth(): Int? {
        val density = context.resources.displayMetrics.density
        val rawWidth = playerView.width
        val width = ceil(rawWidth / density)
        return width.toInt()
    }

    override fun videoSourceWidth(): Int? = videoSourceWidth

    override fun videoSourceHeight(): Int? = videoSourceHeight

    override fun playHeadTime(): Int? = Utils.secondToMs(playbackPosition)

    override fun mimeType(): String? =
        playerView.player.source?.sources?.firstOrNull()?.type?.mimeType

    override fun sourceFps(): Int? = sourceFrameRate

    override fun sourceAdvertisedBitrate(): String? = sourceBitrate?.toString()

    override fun sourceAdvertiseFrameRate(): Int? = sourceFrameRate

    override fun sourceDuration(): Int? = sourceDuration

    override fun isPause(): Boolean? = !isPlaying

    override fun isAutoPlay(): Boolean? = playerView.player.isAutoplay
    override fun preLoad(): Boolean? {
        return playerView.player.preload != PreloadType.NONE
    }

    override fun isBuffering(): Boolean? = currentEventState == PlayerEvent.BUFFERING

    override fun playerCodec(): String? = null

    override fun sourceHostName(): String? = playerView.player.source?.sources?.firstOrNull()?.src

    override fun isLive(): Boolean? {
        val duration = playerView.player.duration
        return duration.isInfinite()
    }

    override fun sourceUrl(): String? = playerView.player.source?.sources?.firstOrNull()?.src

    override fun isFullScreen(): Boolean? {
        val orientation = context.resources.configuration.orientation
        return orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    override fun getBandWidthData(): BandwidthModel = BandwidthModel()

    override fun getPlayerError(): ErrorModel = ErrorModel(errorCode, errorMessage)

    override fun getVideoCodec(): String? = null

    override fun getSoftwareName(): String? = THEOPlayerLibraryInfo.SDK_NAME

    override fun getSoftwareVersion(): String? = THEOPlayerLibraryInfo.SDK_VERSION

    fun release() {
        isReleased = true
        FastPixAnalytics.release()
        sourceFrameRate = null
        currentEventState = null
        videoSourceHeight = null
        videoSourceWidth = null
        isPlaying = false
        sourceBitrate = null
        errorCode = null
        sourceDuration = null
        errorMessage = null
    }

    private fun schedulePulseEvents() {
        if (isPulseScheduled.get()) return
        isPulseScheduled.set(true)
        pulseJob = dispatcherScope.launch {
            while (isPulseScheduled.get()) {
                delay(PULSE_INTERVAL)
                if (isPulseScheduled.get()) {
                    withContext(Dispatchers.Main) {
                        fastPixDataSDK?.dispatchEvent(PlayerEventType.pulse)
                    }
                }
            }
        }
    }

    private fun cancelPulseEvent() {
        if (isPulseScheduled.get()) {
            isPulseScheduled.set(false)
            pulseJob?.cancel()
        }
    }

}