package com.example.ui.breakmode

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.R
import com.example.data.EyeGuardRepository
import com.example.model.Phase
import com.example.service.EyeGuardService
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay

class BreakActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Show when locked & turn screen on
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }

        // Hide system bars for immersive focus
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        setContent {
            MyApplicationTheme {
                BreakScreenContent(
                    onEmergencyCall = {
                        val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        startActivity(dialIntent)
                    },
                    onSkipBreak = {
                        EyeGuardService.skipBreak(this)
                        finish()
                    },
                    onBreakCompletedAutoClose = {
                        finish()
                    }
                )
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Strictly block back press during active break
    }
}

@Composable
fun BreakScreenContent(
    onEmergencyCall: () -> Unit,
    onSkipBreak: () -> Unit,
    onBreakCompletedAutoClose: () -> Unit
) {
    val repository = remember { EyeGuardRepository.getInstance() }
    val serviceState by repository.serviceState.collectAsState()
    val settings by repository.settingsFlow.collectAsState(initial = com.example.data.EyeGuardSettings())

    // Block back gestures
    BackHandler(enabled = true) {}

    val remainingMs = serviceState.remainingMs
    val totalMs = if (serviceState.totalDurationMs > 0L) serviceState.totalDurationMs else 1L
    val isBreakFinished = serviceState.phase != Phase.BREAK || remainingMs <= 0L

    var showSuccessCelebration by remember { mutableStateOf(false) }

    LaunchedEffect(serviceState.phase) {
        if (serviceState.phase != Phase.BREAK && serviceState.phase != Phase.IDLE) {
            showSuccessCelebration = true
            delay(3000L)
            onBreakCompletedAutoClose()
        }
    }

    // Rotating tips every 20 seconds
    val tipResources = listOf(
        R.string.tip_rotate_1,
        R.string.tip_rotate_2,
        R.string.tip_rotate_3,
        R.string.tip_rotate_4,
        R.string.tip_rotate_5,
        R.string.tip_rotate_6
    )
    var currentTipIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(20000L)
            currentTipIndex = (currentTipIndex + 1) % tipResources.size
        }
    }

    // Gradient background deep teal to navy
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF072B28),
            Color(0xFF0A3B37),
            Color(0xFF092534),
            Color(0xFF061422)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradientBrush)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
            .testTag("break_screen_container")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            // Top Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.break_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color(0xFFE2F7F2),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("break_title_text")
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.break_main_instruction),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFA5D8D0),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Center Visual: Animated Window + Tree/Mountain Illustration
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                ScenicWindowIllustration()

                Spacer(modifier = Modifier.height(28.dp))

                if (showSuccessCelebration) {
                    SuccessCard()
                } else {
                    // Countdown Circular Progress
                    CountdownRing(
                        remainingMs = remainingMs,
                        totalMs = totalMs
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Rotating tips with cross-fade
                AnimatedContent(
                    targetState = currentTipIndex,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(600)) togetherWith fadeOut(animationSpec = tween(600))
                    },
                    label = "tip_crossfade"
                ) { targetIndex ->
                    Text(
                        text = stringResource(tipResources[targetIndex]),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFFD4F1EB),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .testTag("rotating_tip_text")
                    )
                }
            }

            // Bottom Actions: Emergency Call & 5-Second Hold Skip Button (if strictMode OFF)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Emergency Call Button (Always accessible)
                    OutlinedButton(
                        onClick = onEmergencyCall,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("emergency_call_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = stringResource(R.string.break_emergency_call),
                            tint = Color(0xFFF2B84B)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.break_emergency_call),
                            color = Color(0xFFF2B84B),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Strict Mode Check: Only show Skip Break button if strictMode is false
                if (!settings.strictMode && !showSuccessCelebration) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HoldToSkipButton(
                        onSkipped = onSkipBreak
                    )
                }
            }
        }
    }
}

@Composable
fun ScenicWindowIllustration() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_scene")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = Modifier
            .size(150.dp)
            .scale(pulseScale)
            .clip(RoundedCornerShape(32.dp))
            .background(Color(0xFF0F3B37))
            .testTag("scenic_window_illustration"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Sky background
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF2E6561), Color(0xFF13423E))
                )
            )

            // Sun / Moon gentle orb
            drawCircle(
                color = Color(0xFFFCEEAC).copy(alpha = 0.8f),
                radius = 18.dp.toPx(),
                center = Offset(width * 0.72f, height * 0.32f)
            )

            // Distant Mountains
            val mountainPath = Path().apply {
                moveTo(0f, height * 0.75f)
                lineTo(width * 0.35f, height * 0.48f)
                lineTo(width * 0.7f, height * 0.68f)
                lineTo(width, height * 0.52f)
                lineTo(width, height)
                lineTo(0f, height)
                close()
            }
            drawPath(mountainPath, color = Color(0xFF0B2B28))

            // Gentle Tree on the hill
            val hillPath = Path().apply {
                moveTo(0f, height * 0.82f)
                quadraticBezierTo(width * 0.5f, height * 0.72f, width, height * 0.82f)
                lineTo(width, height)
                lineTo(0f, height)
                close()
            }
            drawPath(hillPath, color = Color(0xFF07211E))

            // Tree trunk
            drawRect(
                color = Color(0xFF051715),
                topLeft = Offset(width * 0.28f, height * 0.68f),
                size = Size(8.dp.toPx(), 18.dp.toPx())
            )
            // Tree foliage
            drawCircle(
                color = Color(0xFF1B594E),
                radius = 14.dp.toPx(),
                center = Offset(width * 0.295f, height * 0.62f)
            )

            // Window frame crosslines
            val frameColor = Color(0xFF88CEC1).copy(alpha = 0.45f)
            val strokeWidth = 3.dp.toPx()
            drawLine(
                color = frameColor,
                start = Offset(width / 2f, 0f),
                end = Offset(width / 2f, height),
                strokeWidth = strokeWidth
            )
            drawLine(
                color = frameColor,
                start = Offset(0f, height / 2f),
                end = Offset(width, height / 2f),
                strokeWidth = strokeWidth
            )
        }
    }
}

@Composable
fun CountdownRing(
    remainingMs: Long,
    totalMs: Long
) {
    val progress = if (totalMs > 0L) {
        (remainingMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(500),
        label = "ring_progress"
    )

    val seconds = (remainingMs / 1000L).coerceAtLeast(0L)
    val mins = seconds / 60
    val secs = seconds % 60
    val timeFormatted = String.format("%02d:%02d", mins, secs)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(190.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 8.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            // Background circle track
            drawCircle(
                color = Color(0xFF194641).copy(alpha = 0.5f),
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth)
            )

            // Animated Countdown Arc
            drawArc(
                color = Color(0xFF4CC3B0),
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = timeFormatted,
                style = MaterialTheme.typography.displayMedium,
                color = Color(0xFFE2F7F2),
                fontWeight = FontWeight.Light,
                letterSpacing = 1.sp,
                modifier = Modifier.testTag("break_timer_countdown")
            )
            Text(
                text = stringResource(R.string.timer_break_remaining),
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF88CEC1)
            )
        }
    }
}

@Composable
fun SuccessCard() {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF144741),
        modifier = Modifier.padding(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF4CC3B0),
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = stringResource(R.string.break_great_job),
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFE2F7F2),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun HoldToSkipButton(
    onSkipped: () -> Unit
) {
    var holdProgress by remember { mutableFloatStateOf(0f) }
    var isHolding by remember { mutableStateOf(false) }

    LaunchedEffect(isHolding) {
        if (isHolding) {
            val startTime = System.currentTimeMillis()
            val totalHoldTime = 5000L // 5 seconds
            while (isHolding) {
                val elapsed = System.currentTimeMillis() - startTime
                holdProgress = (elapsed.toFloat() / totalHoldTime).coerceIn(0f, 1f)
                if (holdProgress >= 1f) {
                    onSkipped()
                    break
                }
                delay(30L)
            }
        } else {
            // Animate progress down to 0
            while (holdProgress > 0f && !isHolding) {
                holdProgress = (holdProgress - 0.08f).coerceAtLeast(0f)
                delay(20L)
            }
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(Color(0xFF11322E))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isHolding = true
                        tryAwaitRelease()
                        isHolding = false
                    }
                )
            }
            .testTag("hold_to_skip_button")
    ) {
        // Progress fill inside button
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(26.dp)),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(holdProgress)
                    .height(52.dp)
                    .background(Color(0xFF286F64))
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isHolding) {
                CircularProgressIndicator(
                    progress = { holdProgress },
                    color = Color(0xFFF2B84B),
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.break_skipping),
                    color = Color(0xFFF2B84B),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = Color(0xFF9FB7B2),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.break_skip_button),
                    color = Color(0xFFD4EAE5),
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
            }
        }
    }
}
