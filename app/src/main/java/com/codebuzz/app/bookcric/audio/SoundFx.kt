package com.codebuzz.app.bookcric.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/** The game's sound effects. */
enum class Sfx { PAGE, BAT, CHEER, WICKET, FANFARE }

private const val SAMPLE_RATE = 22_050
private const val TWO_PI = 2f * PI.toFloat()

/**
 * Plays the game's sound effects. The clips are synthesised here rather than shipped as audio
 * files; to use recordings instead, load them in place of [synthesize].
 */
class SoundFx {
    var enabled = true

    private val tracks: Map<Sfx, AudioTrack?> = Sfx.entries.associateWith { track(synthesize(it)) }

    /** Play [sfx] from the start, cutting off an earlier play of the same sound. */
    fun play(sfx: Sfx) {
        if (!enabled) return
        val track = tracks[sfx] ?: return
        runCatching {
            track.stop()
            track.reloadStaticData()
            track.play()
        }
    }

    fun release() {
        tracks.values.forEach { it?.release() }
    }

    /** A device that can't give us an audio track just plays silently. */
    private fun track(pcm: ShortArray): AudioTrack? = runCatching {
        AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(pcm.size * 2)
            .build()
            .also { it.write(pcm, 0, pcm.size) }
    }.getOrNull()
}

private fun synthesize(sfx: Sfx): ShortArray = when (sfx) {
    Sfx.PAGE -> pageTick()
    Sfx.BAT -> batCrack()
    Sfx.CHEER -> crowdCheer()
    Sfx.WICKET -> wicketFall()
    Sfx.FANFARE -> fanfare()
}

/** Render [seconds] of audio from [sample], which maps time in seconds to a level in -1..1. */
private inline fun render(seconds: Float, sample: (t: Float) -> Float): ShortArray {
    val count = (seconds * SAMPLE_RATE).toInt()
    return ShortArray(count) { i ->
        (sample(i.toFloat() / SAMPLE_RATE).coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
    }
}

private fun Random.white(): Float = nextFloat() * 2f - 1f

/** One sheet flicking past the thumb: a short hiss of bright noise. */
private fun pageTick(): ShortArray {
    val rng = Random(1)
    var previous = 0f
    return render(0.045f) { t ->
        val white = rng.white()
        // Differencing successive samples strips the low end, leaving a papery rustle.
        val bright = (white - previous) * 0.5f
        previous = white
        bright * exp(-t * 80f) * 0.6f
    }
}

/** Willow on leather: a noise click on top of a quick, falling knock. */
private fun batCrack(): ShortArray {
    val rng = Random(2)
    var phase = 0f
    return render(0.18f) { t ->
        phase += TWO_PI * (160f + 500f * exp(-t * 60f)) / SAMPLE_RATE
        sin(phase) * exp(-t * 28f) * 0.8f + rng.white() * exp(-t * 220f) * 0.6f
    }
}

/** A crowd roar: band-limited noise that swells, wavers and dies away. */
private fun crowdCheer(): ShortArray {
    val rng = Random(3)
    var low = 0f
    var lower = 0f
    return render(1.5f) { t ->
        val white = rng.white()
        // The gap between two low-pass filters is a band-pass around the "roar" frequencies.
        low += (white - low) * 0.25f
        lower += (white - lower) * 0.03f
        val swell = min(t / 0.25f, 1f) * exp(-max(0f, t - 0.5f) * 2.2f)
        val waver = 0.8f + 0.2f * sin(TWO_PI * 7f * t)
        (low - lower) * swell * waver * 1.8f
    }
}

/** Stumps clattering, then a tone that sags away. */
private fun wicketFall(): ShortArray {
    val rng = Random(4)
    var phase = 0f
    return render(0.55f) { t ->
        phase += TWO_PI * (90f + 420f * exp(-t * 2.6f)) / SAMPLE_RATE
        val groan = (sin(phase) * 0.5f + sin(2f * phase) * 0.2f) * exp(-t * 4.5f)
        groan + rng.white() * exp(-t * 150f) * 0.7f
    }
}

/** Four rising notes, the last one held: a win. */
private fun fanfare(): ShortArray {
    val notes = listOf(523.25f to 0.13f, 659.25f to 0.13f, 783.99f to 0.13f, 1046.5f to 0.5f)
    return notes.map { (frequency, seconds) ->
        render(seconds) { t ->
            val x = TWO_PI * frequency * t
            // A soft attack avoids a click where one note hands over to the next.
            val envelope = min(t / 0.01f, 1f) * exp(-t * 3f)
            (sin(x) + 0.3f * sin(2f * x)) * envelope * 0.55f
        }
    }.reduce { tune, note -> tune + note }
}
