package com.example.data

data class DayAdherence(
    val dateString: String,
    val dayOfWeek: String,
    val completedBreaks: Int,
    val restMinutes: Int,
    val targetGoal: Int = 12
)

data class EyeGuardSettings(
    val enabled: Boolean = false,
    val usageMinutes: Int = 20,
    val breakMinutes: Int = 5,
    val strictMode: Boolean = false,
    val soundOn: Boolean = true,
    val vibrateOn: Boolean = true,
    val oneMinuteWarningEnabled: Boolean = true,
    val onboardingDone: Boolean = false,
    val startOnBoot: Boolean = true,
    val breaksToday: Int = 0,
    val restMinutesToday: Int = 0,
    val lastStatsDate: String = "",
    val historyDataRaw: String = ""
)
