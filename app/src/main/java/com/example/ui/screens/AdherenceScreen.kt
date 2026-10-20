package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.DayAdherence
import com.example.ui.theme.AmberWarning
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun AdherenceScreen(
    adherenceList: List<DayAdherence>
) {
    val scrollState = rememberScrollState()

    // Default select today (the last item)
    var selectedIndex by remember { mutableIntStateOf(if (adherenceList.isNotEmpty()) adherenceList.size - 1 else 0) }

    // Summary Calculations
    val totalBreaks = adherenceList.sumOf { it.completedBreaks }
    val totalRestMinutes = adherenceList.sumOf { it.restMinutes }
    val daysCount = if (adherenceList.isNotEmpty()) adherenceList.size else 1
    val dailyAverage = totalBreaks.toFloat() / daysCount

    // Target is 12 breaks/day (e.g. 4 hours of screen time = 12 breaks)
    val totalGoal = daysCount * 12
    val adherencePercent = if (totalGoal > 0) ((totalBreaks.toFloat() / totalGoal) * 100f).coerceIn(0f, 150f).roundToInt() else 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .testTag("adherence_screen_container")
    ) {
        // Title & Header
        Text(
            text = stringResource(R.string.adherence_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = stringResource(R.string.adherence_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Motivation Achievement Banner
        val badgeTitle = when {
            adherencePercent >= 90 -> stringResource(R.string.adherence_badge_champion)
            adherencePercent >= 65 -> stringResource(R.string.adherence_badge_consistent)
            else -> stringResource(R.string.adherence_badge_building)
        }

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("achievement_banner")
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = badgeTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "$adherencePercent% of recommended 20-20-20 breaks completed this week",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 4 Summary Metrics (2x2 Grid)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                icon = Icons.Default.RemoveRedEye,
                title = stringResource(R.string.adherence_total_breaks),
                value = "$totalBreaks",
                accentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                icon = Icons.Default.TrendingUp,
                title = stringResource(R.string.adherence_daily_average),
                value = String.format(Locale.US, "%.1f", dailyAverage),
                accentColor = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                icon = Icons.Default.Star,
                title = stringResource(R.string.adherence_score),
                value = "$adherencePercent%",
                accentColor = AmberWarning,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                icon = Icons.Default.SelfImprovement,
                title = "Rest Time",
                value = "${totalRestMinutes}m",
                accentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // MAIN FEATURE: 7-DAY LINE CHART
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("adherence_line_chart_card")
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Daily Break Adherence",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.adherence_chart_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Goal legend chip
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = AmberWarning.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = stringResource(R.string.adherence_goal_target),
                            style = MaterialTheme.typography.labelSmall,
                            color = AmberWarning,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Interactive Selected Tooltip
                val selectedDay = adherenceList.getOrNull(selectedIndex)
                AnimatedVisibility(
                    visible = selectedDay != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    selectedDay?.let { day ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .testTag("chart_selected_tooltip")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "${day.dayOfWeek} (${day.dateString})",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${day.completedBreaks} breaks taken (${day.restMinutes} min rest)",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                val isGoalMet = day.completedBreaks >= day.targetGoal
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isGoalMet) MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                                    else AmberWarning.copy(alpha = 0.18f)
                                ) {
                                    Text(
                                        text = if (isGoalMet) stringResource(R.string.adherence_status_met) else stringResource(R.string.adherence_status_progress),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isGoalMet) MaterialTheme.colorScheme.secondary else AmberWarning,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Canvas Line Chart
                AdherenceLineChartCanvas(
                    adherenceList = adherenceList,
                    selectedIndex = selectedIndex,
                    onSelectIndex = { selectedIndex = it }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // DAILY BREAKDOWN LIST
        Text(
            text = stringResource(R.string.adherence_breakdown_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(10.dp))

        adherenceList.reversed().forEachIndexed { index, day ->
            val actualIndex = adherenceList.size - 1 - index
            DayBreakdownRow(
                day = day,
                isSelected = actualIndex == selectedIndex,
                onClick = { selectedIndex = actualIndex }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Why 7-Day Consistency Matters Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.adherence_card_info_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.adherence_card_info_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun AdherenceLineChartCanvas(
    adherenceList: List<DayAdherence>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
    val textVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

    // Determine max breaks for Y-axis scale (at least 20)
    val maxVal = ((adherenceList.maxOfOrNull { it.completedBreaks } ?: 16) + 4).coerceAtLeast(20)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp)
            .pointerInput(adherenceList) {
                detectTapGestures { offset ->
                    val width = size.width
                    val paddingHorizontal = 40.dp.toPx()
                    val chartWidth = width - (paddingHorizontal * 2)
                    val pointsCount = adherenceList.size
                    if (pointsCount > 1) {
                        val stepX = chartWidth / (pointsCount - 1)
                        val touchX = (offset.x - paddingHorizontal).coerceIn(0f, chartWidth)
                        val closestIndex = (touchX / stepX).roundToInt().coerceIn(0, pointsCount - 1)
                        onSelectIndex(closestIndex)
                    }
                }
            }
            .testTag("canvas_line_chart")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val paddingLeft = 36.dp.toPx()
            val paddingRight = 24.dp.toPx()
            val paddingTop = 20.dp.toPx()
            val paddingBottom = 32.dp.toPx()

            val chartWidth = width - paddingLeft - paddingRight
            val chartHeight = height - paddingTop - paddingBottom

            // 1. Draw Horizontal Gridlines & Y-labels (0, 5, 10, 15, 20)
            val gridLevels = listOf(0, 5, 10, 15, 20)
            gridLevels.forEach { level ->
                if (level <= maxVal) {
                    val y = paddingTop + chartHeight - (level.toFloat() / maxVal * chartHeight)
                    drawLine(
                        color = gridColor,
                        start = Offset(paddingLeft, y),
                        end = Offset(width - paddingRight, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = if (level > 0) PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f) else null
                    )
                }
            }

            // 2. Draw Target Goal line (12 breaks)
            val targetLevel = 12f
            val targetY = paddingTop + chartHeight - (targetLevel / maxVal * chartHeight)
            drawLine(
                color = AmberWarning.copy(alpha = 0.85f),
                start = Offset(paddingLeft, targetY),
                end = Offset(width - paddingRight, targetY),
                strokeWidth = 1.8.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
            )

            if (adherenceList.isEmpty()) return@Canvas

            // 3. Compute (x, y) coordinates for each data point
            val stepX = if (adherenceList.size > 1) chartWidth / (adherenceList.size - 1) else chartWidth
            val points = adherenceList.mapIndexed { index, day ->
                val x = paddingLeft + (index * stepX)
                val y = paddingTop + chartHeight - (day.completedBreaks.toFloat() / maxVal * chartHeight)
                Offset(x, y)
            }

            // 4. Construct Smooth Cubic Bezier Path
            val linePath = Path()
            val fillPath = Path()

            linePath.moveTo(points.first().x, points.first().y)
            fillPath.moveTo(points.first().x, paddingTop + chartHeight)
            fillPath.lineTo(points.first().x, points.first().y)

            for (i in 0 until points.size - 1) {
                val p0 = points[i]
                val p1 = points[i + 1]
                val controlPoint1 = Offset(p0.x + (p1.x - p0.x) / 2f, p0.y)
                val controlPoint2 = Offset(p0.x + (p1.x - p0.x) / 2f, p1.y)

                linePath.cubicTo(
                    controlPoint1.x, controlPoint1.y,
                    controlPoint2.x, controlPoint2.y,
                    p1.x, p1.y
                )
                fillPath.cubicTo(
                    controlPoint1.x, controlPoint1.y,
                    controlPoint2.x, controlPoint2.y,
                    p1.x, p1.y
                )
            }

            fillPath.lineTo(points.last().x, paddingTop + chartHeight)
            fillPath.close()

            // 5. Draw Gradient Fill below the line
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.35f),
                        primaryColor.copy(alpha = 0.04f)
                    ),
                    startY = paddingTop,
                    endY = paddingTop + chartHeight
                )
            )

            // 6. Draw Stroke Line
            drawPath(
                path = linePath,
                color = primaryColor,
                style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
            )

            // 7. Draw Vertical Highlight Line for Selected Index
            if (selectedIndex in points.indices) {
                val selectedPt = points[selectedIndex]
                drawLine(
                    color = primaryColor.copy(alpha = 0.5f),
                    start = Offset(selectedPt.x, paddingTop),
                    end = Offset(selectedPt.x, paddingTop + chartHeight),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                )
            }

            // 8. Draw Circular Data Points
            points.forEachIndexed { index, pt ->
                val isSelected = index == selectedIndex

                if (isSelected) {
                    // Outer glow halo
                    drawCircle(
                        color = AmberWarning.copy(alpha = 0.35f),
                        radius = 12.dp.toPx(),
                        center = pt
                    )
                    // Inner ring
                    drawCircle(
                        color = AmberWarning,
                        radius = 6.5.dp.toPx(),
                        center = pt
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 3.5.dp.toPx(),
                        center = pt
                    )
                } else {
                    // Normal point
                    drawCircle(
                        color = primaryColor.copy(alpha = 0.2f),
                        radius = 8.dp.toPx(),
                        center = pt
                    )
                    drawCircle(
                        color = primaryColor,
                        radius = 5.dp.toPx(),
                        center = pt
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 2.5.dp.toPx(),
                        center = pt
                    )
                }
            }
        }

        // X-Axis Day Labels below canvas
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(start = 32.dp, end = 20.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            adherenceList.forEachIndexed { index, day ->
                val isSelected = index == selectedIndex
                Text(
                    text = day.dayOfWeek,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) primaryColor else textVariantColor,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(36.dp)
                )
            }
        }
    }
}

@Composable
fun DayBreakdownRow(
    day: DayAdherence,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isGoalMet = day.completedBreaks >= day.targetGoal
    val progressFraction = (day.completedBreaks.toFloat() / day.targetGoal).coerceIn(0f, 1f)

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 2.dp else 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("day_breakdown_card_${day.dateString}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (isGoalMet) MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.primaryContainer
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isGoalMet) Icons.Default.CheckCircle else Icons.Default.ShowChart,
                            contentDescription = null,
                            tint = if (isGoalMet) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = day.dayOfWeek,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = day.dateString,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${day.completedBreaks} breaks",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${day.restMinutes} min rest",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress bar toward daily goal
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LinearProgressIndicator(
                    progress = { progressFraction },
                    color = if (isGoalMet) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "${(progressFraction * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun MetricCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
