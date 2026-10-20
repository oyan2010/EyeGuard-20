package com.example.model

enum class Phase {
    IDLE,
    USING,
    BREAK,
    PAUSED
}

data class ServiceState(
    val phase: Phase = Phase.IDLE,
    val targetElapsedRealtime: Long = 0L,
    val totalDurationMs: Long = 0L,
    val remainingMs: Long = 0L,
    val isScreenOn: Boolean = true,
    val isPaused: Boolean = false,
    val isDevTestCycle: Boolean = false,
    val pauseRemainingMs: Long = 0L
)
