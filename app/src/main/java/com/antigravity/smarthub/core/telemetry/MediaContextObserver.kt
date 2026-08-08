package com.antigravity.smarthub.core.telemetry

import android.content.Context
import android.media.AudioManager
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState

class MediaContextObserver(
    private val context: Context? = null
) {

    fun isMediaPlaying(): TelemetryValue<Boolean> {
        if (context == null) return TelemetryValue.unavailable()

        try {
            // Priority 1: MediaSessionManager active session playback state
            val msm = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
            if (msm != null) {
                try {
                    val controllers: List<MediaController> = msm.getActiveSessions(null)
                    val isSessionPlaying = controllers.any { controller ->
                        val playbackState = controller.playbackState
                        playbackState != null && playbackState.state == PlaybackState.STATE_PLAYING
                    }
                    if (isSessionPlaying) {
                        return TelemetryValue(true, TelemetryState.AVAILABLE)
                    }
                } catch (e: SecurityException) {
                    // MEDIA_CONTENT_CONTROL permission required for getActiveSessions
                }
            }

            // Priority 2: AudioManager isMusicActive fallback
            val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (am != null) {
                val isAudioActive = am.isMusicActive
                return TelemetryValue(isAudioActive, TelemetryState.AVAILABLE)
            }
        } catch (e: Exception) {
            return TelemetryValue.unavailable()
        }

        return TelemetryValue(false, TelemetryState.AVAILABLE)
    }
}
