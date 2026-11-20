package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.DailyBarEntry
import com.example.data.VocabItemEntity
import com.example.model.WorldLanguagesCatalog
import com.example.ui.theme.CoralTertiary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.WarmGold
import com.example.viewmodel.LingoPulseUiState

@Composable
fun ProgressScreen(
    uiState: LingoPulseUiState,
    onSpeakVocab: (String, String) -> Unit,
    onRemoveVocab: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = uiState.progress
    val goalMinutes = uiState.settings.dailyGoalMinutes.coerceAtLeast(1)
    val goalFraction = (progress.todayMinutes.toFloat() / goalMinutes.toFloat()).coerceIn(0f, 1f)
    val animatedSweep by animateFloatAsState(
        targetValue = goalFraction * 360f,
        animationSpec = tween(750),
        label = "goal_ring_sweep"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("progress_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "progress_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.progress_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "${uiState.settings.targetLanguage.flag} ${uiState.settings.level.cefr}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        // Daily Goal Hero Ring Card
        item(key = "daily_goal_card") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(104.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val trackColor = MaterialTheme.colorScheme.surfaceVariant
                        val activeColor = if (goalFraction >= 1f) EmeraldSuccess else MaterialTheme.colorScheme.primary
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeW = 11.dp.toPx()
                            val inset = strokeW / 2f
                            drawArc(
                                color = trackColor,
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                topLeft = Offset(inset, inset),
                                size = Size(size.width - strokeW, size.height - strokeW),
                                style = Stroke(width = strokeW, cap = StrokeCap.Round)
                            )
                            drawArc(
                                color = activeColor,
                                startAngle = -90f,
                                sweepAngle = animatedSweep.coerceAtLeast(if (progress.todayMinutes > 0) 14f else 0f),
                                useCenter = false,
                                topLeft = Offset(inset, inset),
                                size = Size(size.width - strokeW, size.height - strokeW),
                                style = Stroke(width = strokeW, cap = StrokeCap.Round)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${progress.todayMinutes}",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "/ $goalMinutes min",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.width(18.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.progress_daily_goal),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(4.dp))
                        val remaining = (goalMinutes - progress.todayMinutes).coerceAtLeast(0)
                        Text(
                            text = if (remaining == 0) {
                                "Goal reached today! Amazing consistency."
                            } else {
                                "$remaining more min to hit today's goal. Total logged: ${progress.totalMinutes} min."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 3 Stat Tiles Row: Streak, Lessons, Vocab
        item(key = "stats_row") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ProgressStatTile(
                    icon = Icons.Rounded.LocalFireDepartment,
                    tint = CoralTertiary,
                    value = "${progress.streakDays}",
                    label = stringResource(R.string.progress_streak),
                    modifier = Modifier.weight(1f)
                )
                ProgressStatTile(
                    icon = Icons.Rounded.School,
                    tint = MaterialTheme.colorScheme.primary,
                    value = "${progress.totalLessonsCompleted}",
                    label = stringResource(R.string.progress_lessons),
                    modifier = Modifier.weight(1f)
                )
                ProgressStatTile(
                    icon = Icons.AutoMirrored.Rounded.MenuBook,
                    tint = MaterialTheme.colorScheme.secondary,
                    value = "${uiState.savedVocab.size}",
                    label = stringResource(R.string.progress_vocab),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 7-Day Activity Bar Chart
        item(key = "weekly_chart") {
            WeeklyActivityChartCard(
                bars = progress.weeklyBars,
                dailyGoal = goalMinutes
            )
        }

        // Vocabulary Practiced Section Header
        item(key = "vocab_header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.progress_vocab_bank),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${uiState.savedVocab.size} saved",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (uiState.savedVocab.isEmpty()) {
            item(key = "vocab_empty") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.progress_vocab_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(uiState.savedVocab, key = { it.id }) { item ->
                VocabBankCard(
                    item = item,
                    onSpeak = {
                        val bcp = WorldLanguagesCatalog.byCode(item.targetLangCode).bcp47
                        onSpeakVocab(item.targetText, bcp)
                    },
                    onDelete = { onRemoveVocab(item.id) }
                )
            }
        }

        item { Spacer(Modifier.height(14.dp)) }
    }
}

@Composable
private fun ProgressStatTile(
    icon: ImageVector,
    tint: Color,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WeeklyActivityChartCard(
    bars: List<DailyBarEntry>,
    dailyGoal: Int
) {
    val maxMinutes = maxOf(dailyGoal, bars.maxOfOrNull { it.minutes } ?: dailyGoal).coerceAtLeast(5)
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.progress_weekly_activity),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Goal: $dailyGoal min",
                    style = MaterialTheme.typography.labelSmall,
                    color = WarmGold
                )
            }
            Spacer(Modifier.height(14.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(116.dp)
            ) {
                if (bars.isEmpty()) return@Canvas
                val slotW = size.width / bars.size
                val barW = (slotW * 0.44f).coerceAtMost(28.dp.toPx())
                val chartH = size.height

                // Subtle goal dashed reference line
                val goalY = chartH * (1f - (dailyGoal.toFloat() / maxMinutes.toFloat()).coerceIn(0f, 1f))
                drawLine(
                    color = WarmGold.copy(alpha = 0.45f),
                    start = Offset(0f, goalY),
                    end = Offset(size.width, goalY),
                    strokeWidth = 1.5.dp.toPx()
                )

                bars.forEachIndexed { idx, entry ->
                    val x = idx * slotW + (slotW - barW) / 2f
                    // Background track
                    drawRoundRect(
                        color = trackColor,
                        topLeft = Offset(x, 0f),
                        size = Size(barW, chartH),
                        cornerRadius = CornerRadius(barW / 2f, barW / 2f)
                    )
                    // Active bar
                    val ratio = (entry.minutes.toFloat() / maxMinutes.toFloat()).coerceIn(0f, 1f)
                    val filledH = (chartH * ratio).coerceAtLeast(if (entry.minutes > 0) 8.dp.toPx() else 0f)
                    if (filledH > 0f) {
                        drawRoundRect(
                            color = if (entry.isToday) secondaryColor else primaryColor,
                            topLeft = Offset(x, chartH - filledH),
                            size = Size(barW, filledH),
                            cornerRadius = CornerRadius(barW / 2f, barW / 2f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                bars.forEach { entry ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = entry.dayLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (entry.isToday) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (entry.isToday) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${entry.minutes}m",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VocabBankCard(
    item: VocabItemEntity,
    onSpeak: () -> Unit,
    onDelete: () -> Unit
) {
    val langFlag = WorldLanguagesCatalog.byCode(item.targetLangCode).flag
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$langFlag ${item.targetText}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (item.pronunciation.isNotBlank()) {
                    Text(
                        text = item.pronunciation,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (item.nativeMeaning.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = item.nativeMeaning,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(
                onClick = onSpeak,
                modifier = Modifier.testTag("speak_vocab_${item.id}")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                    contentDescription = stringResource(R.string.action_play_audio),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_vocab_${item.id}")
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Remove vocabulary item",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
