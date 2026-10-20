package com.example.receiver

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import com.example.data.EyeGuardRepository
import com.example.service.EyeGuardService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EyeGuardAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        // If admin permission is revoked: stop the service, switch toggle OFF
        EyeGuardService.stopService(context)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                EyeGuardRepository.getInstance().setEnabled(false)
            } catch (_: Exception) {
            }
        }
    }
}
