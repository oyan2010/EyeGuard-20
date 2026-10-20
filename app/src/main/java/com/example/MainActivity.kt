package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.ui.main.MainViewModel
import com.example.ui.screens.AdherenceScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TipsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.util.PermissionHelper

enum class ScreenTab {
    HOME,
    ADHERENCE,
    TIPS,
    SETTINGS
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                MainAppRoot(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshPermissions()
    }
}

@Composable
fun MainAppRoot(viewModel: MainViewModel) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val serviceState by viewModel.serviceState.collectAsState()
    val permissionsState by viewModel.permissionsState.collectAsState()
    val sevenDayAdherence by viewModel.sevenDayAdherence.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var currentTab by remember { mutableStateOf(ScreenTab.HOME) }

    LaunchedEffect(userMessage) {
        userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUserMessage()
        }
    }

    if (!settings.onboardingDone) {
        OnboardingScreen(
            permissionsState = permissionsState,
            onRefreshPermissions = { viewModel.refreshPermissions() },
            onComplete = { usageMin, breakMin ->
                viewModel.completeOnboarding(usageMin, breakMin)
            }
        )
    } else {
        // Back handler to navigate back to Home tab if on another screen
        BackHandler(enabled = currentTab != ScreenTab.HOME) {
            currentTab = ScreenTab.HOME
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.testTag("bottom_navigation_bar")
                ) {
                    NavigationBarItem(
                        selected = currentTab == ScreenTab.HOME,
                        onClick = { currentTab = ScreenTab.HOME },
                        icon = {
                            Icon(imageVector = Icons.Default.Home, contentDescription = stringResource(R.string.nav_home))
                        },
                        label = { Text(text = stringResource(R.string.nav_home)) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("nav_item_home")
                    )

                    NavigationBarItem(
                        selected = currentTab == ScreenTab.ADHERENCE,
                        onClick = { currentTab = ScreenTab.ADHERENCE },
                        icon = {
                            Icon(imageVector = Icons.Default.ShowChart, contentDescription = stringResource(R.string.nav_adherence))
                        },
                        label = { Text(text = stringResource(R.string.nav_adherence)) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("nav_item_adherence")
                    )

                    NavigationBarItem(
                        selected = currentTab == ScreenTab.TIPS,
                        onClick = { currentTab = ScreenTab.TIPS },
                        icon = {
                            Icon(imageVector = Icons.Default.Info, contentDescription = stringResource(R.string.nav_tips))
                        },
                        label = { Text(text = stringResource(R.string.nav_tips)) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("nav_item_tips")
                    )

                    NavigationBarItem(
                        selected = currentTab == ScreenTab.SETTINGS,
                        onClick = { currentTab = ScreenTab.SETTINGS },
                        icon = {
                            Icon(imageVector = Icons.Default.Settings, contentDescription = stringResource(R.string.nav_settings))
                        },
                        label = { Text(text = stringResource(R.string.nav_settings)) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("nav_item_settings")
                    )
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    label = "tab_content_transition"
                ) { target ->
                    when (target) {
                        ScreenTab.HOME -> {
                            HomeScreen(
                                settings = settings,
                                serviceState = serviceState,
                                permissionsState = permissionsState,
                                onToggleProtection = { viewModel.toggleProtection(it) },
                                onNavigateToSettings = { currentTab = ScreenTab.SETTINGS },
                                onNavigateToAdherence = { currentTab = ScreenTab.ADHERENCE },
                                onTakeBreakNow = { viewModel.takeBreakNow() },
                                onRequestAdmin = {
                                    context.startActivity(PermissionHelper.getRequestAdminIntent(context))
                                }
                            )
                        }
                        ScreenTab.ADHERENCE -> {
                            AdherenceScreen(
                                adherenceList = sevenDayAdherence
                            )
                        }
                        ScreenTab.TIPS -> {
                            TipsScreen()
                        }
                        ScreenTab.SETTINGS -> {
                            SettingsScreen(
                                settings = settings,
                                permissionsState = permissionsState,
                                snackbarHostState = snackbarHostState,
                                onUsageChanged = { viewModel.updateUsageMinutes(it) },
                                onBreakChanged = { viewModel.updateBreakMinutes(it) },
                                onStrictChanged = { viewModel.setStrictMode(it) },
                                onSoundChanged = { viewModel.setSoundOn(it) },
                                onVibrateChanged = { viewModel.setVibrateOn(it) },
                                onOneMinuteWarningChanged = { viewModel.setOneMinuteWarningEnabled(it) },
                                onBootChanged = { viewModel.setStartOnBoot(it) },
                                onStartDevUsage = { viewModel.startDevUsageTest() },
                                onStartDevBreak = { viewModel.startDevBreakTest() },
                                onDeactivateAdmin = { onDone ->
                                    viewModel.deactivateAdminAndUninstall(onDone)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
