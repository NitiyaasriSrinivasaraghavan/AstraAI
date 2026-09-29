package com.example.aidrivencompetencyplatform.voice

import android.media.AudioAttributes
import android.media.MediaDataSource
import android.media.MediaPlayer
import android.util.Log

/**
 * Audio player helper that plays raw audio data bytes out loud using Android's MediaPlayer
 * without creating or downloading permanent media files to the device.
 */
object AudioPlayerHelper {
    private const val TAG = "AudioPlayerHelper"
    private var activePlayer: MediaPlayer? = null
    private var activeDataSource: MediaDataSource? = null

    /**
     * Plays raw audio data bytes (e.g., MP3 audio returned by the Gemini TTS API) out loud
     * using an in-memory [MediaDataSource] with [MediaPlayer] without storing permanent files.
     *
     * @param audioBytes Raw byte array containing the audio stream (e.g. MP3 bytes).
     * @param onCompletion Optional callback invoked when audio playback finishes successfully.
     * @param onError Optional callback invoked if an error occurs during preparation or playback.
     * @return The initialized and playing [MediaPlayer], or null if setup failed.
     */
    @Synchronized
    fun playAudioBytes(
        audioBytes: ByteArray,
        onCompletion: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ): MediaPlayer? {
        if (audioBytes.isEmpty()) {
            val msg = "Cannot play audio: Byte array is empty."
            Log.w(TAG, msg)
            onError?.invoke(msg)
            return null
        }

        // Stop and release any previously active playback
        stop()

        return try {
            val mediaDataSource = object : MediaDataSource() {
                override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
                    if (position >= audioBytes.size) return -1
                    val remaining = audioBytes.size - position
                    val bytesToRead = minOf(size.toLong(), remaining).toInt()
                    System.arraycopy(audioBytes, position.toInt(), buffer, offset, bytesToRead)
                    return bytesToRead
                }

                override fun getSize(): Long = audioBytes.size.toLong()

                override fun close() {
                    // In-memory stream closed
                }
            }
            activeDataSource = mediaDataSource

            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                        .build()
                )
                setDataSource(mediaDataSource)
                setOnPreparedListener { mp ->
                    try {
                        mp.start()
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to start MediaPlayer playback", e)
                        onError?.invoke("Failed to start playback: ${e.localizedMessage}")
                    }
                }
                setOnCompletionListener { mp ->
                    try {
                        mp.release()
                    } catch (_: Exception) {}
                    if (activePlayer == mp) {
                        activePlayer = null
                        activeDataSource = null
                    }
                    onCompletion?.invoke()
                }
                setOnErrorListener { mp, what, extra ->
                    Log.e(TAG, "MediaPlayer error occurred: what=$what extra=$extra")
                    try {
                        mp.release()
                    } catch (_: Exception) {}
                    if (activePlayer == mp) {
                        activePlayer = null
                        activeDataSource = null
                    }
                    onError?.invoke("MediaPlayer playback error (code $what, extra $extra)")
                    true
                }
                prepareAsync()
            }

            activePlayer = player
            player
        } catch (e: Exception) {
            Log.e(TAG, "Error configuring MediaPlayer for raw bytes playback", e)
            onError?.invoke("MediaPlayer setup failed: ${e.localizedMessage}")
            null
        }
    }

    /**
     * Stops any ongoing playback and safely releases the MediaPlayer instance.
     */
    @Synchronized
    fun stop() {
        try {
            activePlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
                player.release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Exception while stopping MediaPlayer", e)
        } finally {
            activePlayer = null
            activeDataSource = null
        }
    }

    /**
     * Checks if audio is currently being played out loud.
     */
    fun isPlaying(): Boolean {
        return try {
            activePlayer?.isPlaying == true
        } catch (_: Exception) {
            false
        }
    }
}
