package com.example.ui.main

import android.app.Application
import android.app.admin.DevicePolicyManager
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DayAdherence
import com.example.data.EyeGuardRepository
import com.example.data.EyeGuardSettings
import com.example.model.ServiceState
import com.example.service.EyeGuardService
import com.example.util.PermissionHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SystemPermissionsState(
    val isDeviceAdminGranted: Boolean = false,
    val isNotificationGranted: Boolean = false,
    val isBatteryOptimizationIgnored: Boolean = false,
    val canScheduleExactAlarm: Boolean = true,
    val canUseFullScreenIntent: Boolean = true
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = EyeGuardRepository.getInstance()

    val settings: StateFlow<EyeGuardSettings> = repository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = EyeGuardSettings()
    )

    val sevenDayAdherence: StateFlow<List<DayAdherence>> = repository.sevenDayAdherenceFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val serviceState: StateFlow<ServiceState> = repository.serviceState

    private val _permissionsState = MutableStateFlow(checkPermissions())
    val permissionsState: StateFlow<SystemPermissionsState> = _permissionsState.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun refreshPermissions() {
        _permissionsState.value = checkPermissions()
    }

    private fun checkPermissions(): SystemPermissionsState {
        val context = getApplication<Application>()
        return SystemPermissionsState(
            isDeviceAdminGranted = PermissionHelper.isDeviceAdminActive(context),
            isNotificationGranted = PermissionHelper.isNotificationPermissionGranted(context),
            isBatteryOptimizationIgnored = PermissionHelper.isBatteryOptimizationIgnored(context),
            canScheduleExactAlarm = PermissionHelper.canScheduleExactAlarms(context),
            canUseFullScreenIntent = PermissionHelper.canUseFullScreenIntent(context)
        )
    }

    fun toggleProtection(enable: Boolean) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            if (enable) {
                if (!PermissionHelper.isDeviceAdminActive(context)) {
                    _userMessage.value = "Device Administrator permission is required to start protection."
                    return@launch
                }
                repository.setEnabled(true)
                EyeGuardService.startService(context)
            } else {
                repository.setEnabled(false)
                EyeGuardService.stopService(context)
            }
        }
    }

    fun updateUsageMinutes(minutes: Int) {
        viewModelScope.launch {
            repository.setUsageMinutes(minutes)
        }
    }

    fun updateBreakMinutes(minutes: Int) {
        viewModelScope.launch {
            repository.setBreakMinutes(minutes)
        }
    }

    fun setStrictMode(strict: Boolean) {
        viewModelScope.launch {
            repository.setStrictMode(strict)
        }
    }

    fun setSoundOn(sound: Boolean) {
        viewModelScope.launch {
            repository.setSoundOn(sound)
        }
    }

    fun setVibrateOn(vibrate: Boolean) {
        viewModelScope.launch {
            repository.setVibrateOn(vibrate)
        }
    }

    fun setOneMinuteWarningEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setOneMinuteWarningEnabled(enabled)
        }
    }

    fun setStartOnBoot(boot: Boolean) {
        viewModelScope.launch {
            repository.setStartOnBoot(boot)
        }
    }

    fun completeOnboarding(usageMin: Int, breakMin: Int) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            repository.setUsageMinutes(usageMin)
            repository.setBreakMinutes(breakMin)
            repository.setOnboardingDone(true)
            if (PermissionHelper.isDeviceAdminActive(context)) {
                repository.setEnabled(true)
                EyeGuardService.startService(context)
            }
        }
    }

    fun takeBreakNow() {
        val context = getApplication<Application>()
        EyeGuardService.triggerBreakNow(context)
    }

    fun startDevUsageTest() {
        val context = getApplication<Application>()
        EyeGuardService.startDevUsage(context)
    }

    fun startDevBreakTest() {
        val context = getApplication<Application>()
        EyeGuardService.startDevBreak(context)
    }

    fun deactivateAdminAndUninstall(onDeactivated: () -> Unit) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            try {
                repository.setEnabled(false)
                EyeGuardService.stopService(context)
                val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
                dpm?.removeActiveAdmin(PermissionHelper.getAdminComponent(context))
                refreshPermissions()
                onDeactivated()
            } catch (_: Exception) {
            }
        }
    }
}
