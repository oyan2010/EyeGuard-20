package com.example.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object SoundVibrationHelper {

    fun playBreakStartAlert(context: Context, soundEnabled: Boolean, vibrateEnabled: Boolean) {
        if (vibrateEnabled) {
            triggerVibration(context, longBreak = true)
        }
        if (soundEnabled) {
            playGentleChime(isBreakStart = true)
        }
    }

    fun playBreakEndAlert(context: Context, soundEnabled: Boolean, vibrateEnabled: Boolean) {
        if (vibrateEnabled) {
            triggerVibration(context, longBreak = false)
        }
        if (soundEnabled) {
            playGentleChime(isBreakStart = false)
        }
    }

    fun playOneMinuteWarningAlert(context: Context, soundEnabled: Boolean, vibrateEnabled: Boolean) {
        if (vibrateEnabled) {
            try {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    manager?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }
                vibrator?.let {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        it.vibrate(VibrationEffect.createOneShot(120, 120))
                    } else {
                        @Suppress("DEPRECATION")
                        it.vibrate(120L)
                    }
                }
            } catch (_: Exception) {
            }
        }
        if (soundEnabled) {
            CoroutineScope(Dispatchers.Default).launch {
                try {
                    val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 55)
                    toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 140)
                    delay(250)
                    toneGen.release()
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun playGentleChime(isBreakStart: Boolean) {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 65)
                if (isBreakStart) {
                    toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
                    delay(160)
                    toneGen.startTone(ToneGenerator.TONE_PROP_ACK, 250)
                } else {
                    toneGen.startTone(ToneGenerator.TONE_PROP_ACK, 180)
                    delay(200)
                    toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 220)
                }
                delay(400)
                toneGen.release()
            } catch (_: Exception) {
            }
        }
    }

    private fun triggerVibration(context: Context, longBreak: Boolean) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            } ?: return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val pattern = if (longBreak) {
                    longArrayOf(0, 180, 120, 300)
                } else {
                    longArrayOf(0, 150, 100, 150)
                }
                val amplitudes = if (longBreak) {
                    intArrayOf(0, 160, 0, 200)
                } else {
                    intArrayOf(0, 140, 0, 140)
                }
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(if (longBreak) 400L else 200L)
            }
        } catch (_: Exception) {
        }
    }
}
