package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.EyeGuardRepository
import com.example.service.EyeGuardService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val repository = EyeGuardRepository.getInstance()
                    val settings = repository.getSettingsSnapshot()
                    if (settings.enabled && settings.startOnBoot) {
                        EyeGuardService.startService(context)
                    }
                } catch (_: Exception) {
                }
            }
        }
    }
}
