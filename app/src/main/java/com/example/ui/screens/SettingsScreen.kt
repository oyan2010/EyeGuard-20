package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.EyeGuardSettings
import com.example.ui.main.SystemPermissionsState
import com.example.ui.theme.AmberWarning
import com.example.util.PermissionHelper
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    settings: EyeGuardSettings,
    permissionsState: SystemPermissionsState,
    snackbarHostState: SnackbarHostState,
    onUsageChanged: (Int) -> Unit,
    onBreakChanged: (Int) -> Unit,
    onStrictChanged: (Boolean) -> Unit,
    onSoundChanged: (Boolean) -> Unit,
    onVibrateChanged: (Boolean) -> Unit,
    onOneMinuteWarningChanged: (Boolean) -> Unit,
    onBootChanged: (Boolean) -> Unit,
    onStartDevUsage: () -> Unit,
    onStartDevBreak: () -> Unit,
    onDeactivateAdmin: (onDone: () -> Unit) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var showDeactivateDialog by remember { mutableStateOf(false) }
    var devTapCount by remember { mutableIntStateOf(0) }
    var devModeUnlocked by remember { mutableStateOf(false) }

    val noticeText = stringResource(R.string.settings_cycle_notice)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .testTag("settings_screen_container")
    ) {
        Text(
            text = stringResource(R.string.nav_settings),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        // SECTION 1: TIMING
        Text(
            text = stringResource(R.string.settings_timing_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {

                // Usage Time Slider + Stepper
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.settings_usage_label),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(R.string.minutes_format, settings.usageMinutes),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                val next = (settings.usageMinutes - 5).coerceAtLeast(5)
                                onUsageChanged(next)
                                coroutineScope.launch { snackbarHostState.showSnackbar(noticeText) }
                            },
                            enabled = settings.usageMinutes > 5
                        ) {
                            Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease usage")
                        }
                        IconButton(
                            onClick = {
                                val next = (settings.usageMinutes + 5).coerceAtMost(60)
                                onUsageChanged(next)
                                coroutineScope.launch { snackbarHostState.showSnackbar(noticeText) }
                            },
                            enabled = settings.usageMinutes < 60
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Increase usage")
                        }
                    }
                }

                Slider(
                    value = settings.usageMinutes.toFloat(),
                    onValueChange = {
                        onUsageChanged(it.toInt())
                    },
                    onValueChangeFinished = {
                        coroutineScope.launch { snackbarHostState.showSnackbar(noticeText) }
                    },
                    valueRange = 5f..60f,
                    steps = 10,
                    colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.testTag("usage_slider")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Break Time Slider + Stepper
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.settings_break_label),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(R.string.minutes_format, settings.breakMinutes),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                val next = (settings.breakMinutes - 1).coerceAtLeast(1)
                                onBreakChanged(next)
                                coroutineScope.launch { snackbarHostState.showSnackbar(noticeText) }
                            },
                            enabled = settings.breakMinutes > 1
                        ) {
                            Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease break")
                        }
                        IconButton(
                            onClick = {
                                val next = (settings.breakMinutes + 1).coerceAtMost(15)
                                onBreakChanged(next)
                                coroutineScope.launch { snackbarHostState.showSnackbar(noticeText) }
                            },
                            enabled = settings.breakMinutes < 15
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Increase break")
                        }
                    }
                }

                Slider(
                    value = settings.breakMinutes.toFloat(),
                    onValueChange = {
                        onBreakChanged(it.toInt())
                    },
                    onValueChangeFinished = {
                        coroutineScope.launch { snackbarHostState.showSnackbar(noticeText) }
                    },
                    valueRange = 1f..15f,
                    steps = 13,
                    colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.testTag("break_slider")
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // SECTION 2: BEHAVIOR
        Text(
            text = stringResource(R.string.settings_behavior_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {

                // Strict Mode
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_strict_mode),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.settings_strict_mode_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(
                        checked = settings.strictMode,
                        onCheckedChange = onStrictChanged,
                        modifier = Modifier.testTag("strict_mode_switch")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Sound Chime
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.settings_sound),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.settings_sound_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = settings.soundOn,
                        onCheckedChange = onSoundChanged,
                        modifier = Modifier.testTag("sound_switch")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Vibration Alert
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Vibration,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.settings_vibrate),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.settings_vibrate_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = settings.vibrateOn,
                        onCheckedChange = onVibrateChanged,
                        modifier = Modifier.testTag("vibrate_switch")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 1-Minute Break Warning
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.settings_warning_toggle),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.settings_warning_toggle_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = settings.oneMinuteWarningEnabled,
                        onCheckedChange = onOneMinuteWarningChanged,
                        modifier = Modifier.testTag("one_minute_warning_switch")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Start on Boot
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.settings_boot),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.settings_boot_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = settings.startOnBoot,
                        onCheckedChange = onBootChanged,
                        modifier = Modifier.testTag("boot_switch")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // SECTION 3: SYSTEM PERMISSIONS
        Text(
            text = stringResource(R.string.settings_permissions_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Device Admin
                PermissionRow(
                    icon = Icons.Default.Security,
                    title = stringResource(R.string.perm_device_admin),
                    isGranted = permissionsState.isDeviceAdminGranted,
                    onFix = {
                        context.startActivity(PermissionHelper.getRequestAdminIntent(context))
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Notifications
                PermissionRow(
                    icon = Icons.Default.Notifications,
                    title = stringResource(R.string.perm_notifications),
                    isGranted = permissionsState.isNotificationGranted,
                    onFix = {
                        val intent = Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        try {
                            context.startActivity(intent)
                        } catch (_: Exception) {
                        }
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Battery Optimization
                PermissionRow(
                    icon = Icons.Default.BatteryAlert,
                    title = stringResource(R.string.perm_battery),
                    isGranted = permissionsState.isBatteryOptimizationIgnored,
                    onFix = {
                        try {
                            context.startActivity(PermissionHelper.getRequestBatteryOptimizationIntent(context))
                        } catch (_: Exception) {
                        }
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Exact Alarms
                PermissionRow(
                    icon = Icons.Default.Alarm,
                    title = stringResource(R.string.perm_exact_alarm),
                    isGranted = permissionsState.canScheduleExactAlarm,
                    onFix = {
                        try {
                            context.startActivity(PermissionHelper.getExactAlarmSettingIntent(context))
                        } catch (_: Exception) {
                        }
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Full Screen Intent
                PermissionRow(
                    icon = Icons.Default.Fullscreen,
                    title = stringResource(R.string.perm_fullscreen),
                    isGranted = permissionsState.canUseFullScreenIntent,
                    onFix = {
                        try {
                            context.startActivity(PermissionHelper.getFullScreenIntentSettingIntent(context))
                        } catch (_: Exception) {
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // SECTION 4: DANGER ZONE
        Text(
            text = stringResource(R.string.settings_danger_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "To uninstall EyeGuard 20, Android security requires you to first deactivate Device Administrator privileges.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { showDeactivateDialog = true },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("deactivate_admin_button")
                ) {
                    Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.btn_deactivate_uninstall))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // HIDDEN DEVELOPER TEST CARD (Revealed by tapping Version 5 times)
        AnimatedVisibility(
            visible = devModeUnlocked,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
                    .testTag("developer_test_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.dev_test_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.dev_test_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = onStartDevUsage,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dev_btn_10s_usage")
                    ) {
                        Text(text = stringResource(R.string.dev_btn_10s_usage))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onStartDevBreak,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dev_btn_15s_break")
                    ) {
                        Text(text = stringResource(R.string.dev_btn_15s_break))
                    }
                }
            }
        }

        // About / Version Footer (Tappable for Dev Mode)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .clickable {
                    devTapCount++
                    if (devTapCount >= 5) {
                        devModeUnlocked = true
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Developer Emulator Test shortcuts unlocked!")
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.app_version_format, 1),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    // Confirmation Dialog for Deactivate Admin
    if (showDeactivateDialog) {
        AlertDialog(
            onDismissRequest = { showDeactivateDialog = false },
            title = {
                Text(text = stringResource(R.string.dialog_deactivate_title))
            },
            text = {
                Text(text = stringResource(R.string.dialog_deactivate_msg))
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeactivateDialog = false
                        onDeactivateAdmin {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Admin privileges removed. You can now uninstall the app.")
                            }
                            // Trigger system uninstall screen
                            val uninstallIntent = Intent(Intent.ACTION_DELETE).apply {
                                data = Uri.parse("package:${context.packageName}")
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            try {
                                context.startActivity(uninstallIntent)
                            } catch (_: Exception) {
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(text = stringResource(R.string.dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeactivateDialog = false }) {
                    Text(text = stringResource(R.string.dialog_cancel))
                }
            }
        )
    }
}

@Composable
fun PermissionRow(
    icon: ImageVector,
    title: String,
    isGranted: Boolean,
    onFix: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isGranted) MaterialTheme.colorScheme.primary else AmberWarning,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (isGranted) stringResource(R.string.perm_status_granted) else stringResource(R.string.perm_status_missing),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isGranted) MaterialTheme.colorScheme.primary else AmberWarning
                )
            }
        }

        if (isGranted) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        } else {
            OutlinedButton(
                onClick = onFix,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text(
                    text = stringResource(R.string.btn_fix),
                    fontSize = 12.sp
                )
            }
        }
    }
}
