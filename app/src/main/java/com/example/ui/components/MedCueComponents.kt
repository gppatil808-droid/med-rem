package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Snooze
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Healing
import androidx.compose.material.icons.rounded.LocalDrink
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material.icons.rounded.Vaccines
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.DoseStatus
import com.example.data.MedicineEntity
import com.example.data.MedicineForm
import com.example.data.ScheduleTime
import com.example.ui.ActiveReminderAlert
import com.example.ui.DailyAdherenceBar
import com.example.ui.TodayDoseItem
import com.example.ui.theme.MedicineTagColors
import com.example.ui.theme.SuccessContainer
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningContainer

fun medicineFormIcon(form: MedicineForm): ImageVector = when (form) {
    MedicineForm.TABLET -> Icons.Rounded.Medication
    MedicineForm.CAPSULE -> Icons.Rounded.Medication
    MedicineForm.LIQUID -> Icons.Rounded.Science
    MedicineForm.INJECTION -> Icons.Rounded.Vaccines
    MedicineForm.DROPS -> Icons.Rounded.WaterDrop
    MedicineForm.INHALER -> Icons.Rounded.Air
    MedicineForm.CREAM -> Icons.Rounded.Healing
}

fun medicineTagColor(colorTag: Int): Color =
    MedicineTagColors[colorTag.coerceIn(0, MedicineTagColors.lastIndex)]

@Composable
fun VoiceSpeakingBanner(
    isVisible: Boolean,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(visible = isVisible, modifier = modifier) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                VoiceEqualizerBars(color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Speaking medicine announcement...",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = onStop,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("stop_tts_banner_button"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Stop,
                        contentDescription = "Stop voice announcement",
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Stop")
                }
            }
        }
    }
}

@Composable
fun VoiceEqualizerBars(color: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "voice_wave")
    val bar1 by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(380), RepeatMode.Reverse),
        label = "bar1",
    )
    val bar2 by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(460), RepeatMode.Reverse),
        label = "bar2",
    )
    val bar3 by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(320), RepeatMode.Reverse),
        label = "bar3",
    )

    Row(
        modifier = modifier.height(20.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf(bar1, bar2, bar3).forEach { fraction ->
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height((20f * fraction).dp)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}

@Composable
fun DailyProgressHeroCard(
    takenCount: Int,
    totalCount: Int,
    nextDose: TodayDoseItem?,
    isSpeakingSummary: Boolean,
    onReadScheduleClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress = if (totalCount == 0) 0f else (takenCount.toFloat() / totalCount.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(stiffness = 220f),
        label = "daily_progress",
    )
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.tertiary

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        primaryColor,
                        primaryColor.copy(alpha = 0.86f),
                        Color(0xFF0A4F52),
                    ),
                ),
            )
            .padding(20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (totalCount == 0) {
                        "No doses today"
                    } else if (takenCount == totalCount) {
                        "All doses complete"
                    } else {
                        "$takenCount of $totalCount doses taken"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                )
                Spacer(Modifier.height(6.dp))
                val subtitleText = when {
                    totalCount == 0 -> "Add a medicine schedule to start tracking."
                    nextDose != null -> "Next: ${nextDose.medicine.name} at ${nextDose.scheduleTime.toFormatted12h()}"
                    else -> "You stayed on track for every scheduled dose."
                }
                Text(
                    text = subtitleText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.88f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(14.dp))
                FilledTonalButton(
                    onClick = onReadScheduleClick,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color.White.copy(alpha = 0.18f),
                        contentColor = Color.White,
                    ),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("read_schedule_tts_button"),
                ) {
                    Icon(
                        imageVector = if (isSpeakingSummary) Icons.Rounded.Stop else Icons.AutoMirrored.Rounded.VolumeUp,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(if (isSpeakingSummary) "Stop Voice" else "Read Schedule")
                }
            }

            Spacer(Modifier.width(16.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(86.dp),
            ) {
                Canvas(modifier = Modifier.size(86.dp)) {
                    val strokeWidth = 9.dp.toPx()
                    drawArc(
                        color = Color.White.copy(alpha = 0.2f),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    )
                    drawArc(
                        color = if (progress >= 1f) Color(0xFF9AF3EE) else secondaryColor.copy(alpha = 0.95f),
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    )
                }
                Text(
                    text = "${(animatedProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
fun DoseScheduleCard(
    item: TodayDoseItem,
    isSpeakingThis: Boolean,
    onTake: () -> Unit,
    onSkip: () -> Unit,
    onSnooze: () -> Unit,
    onUndo: () -> Unit,
    onSpeak: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accentColor = medicineTagColor(item.medicine.colorTag)
    val isTaken = item.status == DoseStatus.TAKEN
    val isSkipped = item.status == DoseStatus.SKIPPED
    val isSnoozed = item.status == DoseStatus.SNOOZED
    val containerColor = when {
        isTaken -> MaterialTheme.colorScheme.surfaceContainerLow
        item.isDueNow -> MaterialTheme.colorScheme.surfaceContainerLowest
        else -> MaterialTheme.colorScheme.surfaceContainerLowest
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (item.isDueNow && !isTaken) 3.dp else 1.dp,
        ),
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (item.isDueNow && !isTaken) {
                    Modifier.border(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(20.dp),
                    )
                } else {
                    Modifier.border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(20.dp),
                    )
                },
            )
            .testTag("dose_card_${item.key}"),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(accentColor.copy(alpha = if (isTaken) 0.10f else 0.16f)),
                ) {
                    Icon(
                        imageVector = if (isTaken) Icons.Rounded.CheckCircle else medicineFormIcon(item.medicine.form),
                        contentDescription = item.medicine.form.label,
                        tint = if (isTaken) SuccessGreen else accentColor,
                        modifier = Modifier.size(24.dp),
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.medicine.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                textDecoration = if (isSkipped) TextDecoration.LineThrough else TextDecoration.None,
                            ),
                            color = if (isTaken || isSkipped) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Spacer(Modifier.width(8.dp))
                        StatusPill(item = item)
                    }
                    Spacer(Modifier.height(2.dp))
                    val doseLine = listOf(item.medicine.dosage, item.medicine.quantity)
                        .filter { it.isNotBlank() }
                        .joinToString(" • ")
                    Text(
                        text = "$doseLine • ${item.scheduleTime.toFormatted12h()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                FilledTonalIconButton(
                    onClick = onSpeak,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = if (isSpeakingThis) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                        },
                        contentColor = if (isSpeakingThis) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        },
                    ),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("speak_dose_button_${item.key}"),
                ) {
                    Icon(
                        imageVector = if (isSpeakingThis) Icons.Rounded.Stop else Icons.AutoMirrored.Rounded.VolumeUp,
                        contentDescription = if (isSpeakingThis) "Stop speaking ${item.medicine.name}" else "Speak reminder for ${item.medicine.name}",
                    )
                }
            }

            if (item.medicine.instructions.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LocalDrink,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = item.medicine.instructions,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            if (item.status == null || isSnoozed) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = onTake,
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("take_dose_button_${item.key}"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Take")
                    }
                    OutlinedButton(
                        onClick = onSnooze,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("snooze_dose_button_${item.key}"),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Snooze,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Snooze")
                    }
                    TextButton(
                        onClick = onSkip,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("skip_dose_button_${item.key}"),
                    ) {
                        Text("Skip")
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    val statusDesc = if (isTaken) {
                        "Taken"
                    } else {
                        "Skipped"
                    }
                    Text(
                        text = "$statusDesc at ${item.actedAtMillis?.let { com.example.data.LocalDateHelper.formatTimeMillis(it) } ?: ""}",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isTaken) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(
                        onClick = onUndo,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("undo_dose_button_${item.key}"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Undo,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Undo")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusPill(item: TodayDoseItem) {
    val (label, bg, fg) = when {
        item.status == DoseStatus.TAKEN -> Triple("Taken", SuccessContainer, SuccessGreen)
        item.status == DoseStatus.SKIPPED -> Triple(
            "Skipped",
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
        item.status == DoseStatus.SNOOZED -> Triple("Snoozed", WarningContainer, WarningAmber)
        item.isDueNow -> Triple(
            "Due Now",
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
        )
        item.isOverdue -> Triple("Missed", WarningContainer, WarningAmber)
        else -> Triple(
            "Upcoming",
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = fg,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

@Composable
fun AdherenceBarChart(
    bars: List<DailyAdherenceBar>,
    modifier: Modifier = Modifier,
) {
    val barColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val todayHighlight = MaterialTheme.colorScheme.tertiary

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                shape = RoundedCornerShape(22.dp),
            ),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "7-Day Adherence",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Daily percentage of scheduled doses marked taken",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(18.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
            ) {
                if (bars.isEmpty()) return@Canvas
                val slotWidth = size.width / bars.size
                val barWidth = (slotWidth * 0.42f).coerceAtMost(32.dp.toPx())
                val chartHeight = size.height

                bars.forEachIndexed { idx, bar ->
                    val left = idx * slotWidth + (slotWidth - barWidth) / 2f
                    // Track
                    drawRoundRect(
                        color = trackColor,
                        topLeft = Offset(left, 0f),
                        size = Size(barWidth, chartHeight),
                        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
                    )
                    // Filled bar
                    if (bar.ratio > 0f) {
                        val filledHeight = (chartHeight * bar.ratio).coerceAtLeast(barWidth)
                        drawRoundRect(
                            color = if (idx == bars.lastIndex) todayHighlight else barColor,
                            topLeft = Offset(left, chartHeight - filledHeight),
                            size = Size(barWidth, filledHeight),
                            cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                bars.forEach { bar ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = bar.shortLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = if (bar.scheduledCount > 0) "${(bar.ratio * 100).toInt()}%" else "—",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReminderAlertDialog(
    alert: ActiveReminderAlert,
    isSpeaking: Boolean,
    onTake: () -> Unit,
    onSnooze: () -> Unit,
    onSpeak: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiaryContainer),
            ) {
                Icon(
                    imageVector = Icons.Rounded.NotificationsActive,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(28.dp),
                )
            }
        },
        title = {
            Text(
                text = "Time for ${alert.medicine.name}",
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "${alert.medicine.dosage} • ${alert.medicine.quantity} at ${alert.scheduleTime.toFormatted12h()}",
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (alert.medicine.instructions.isNotBlank()) {
                    Text(
                        text = alert.medicine.instructions,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(4.dp))
                OutlinedButton(
                    onClick = onSpeak,
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("alert_speak_button"),
                ) {
                    Icon(
                        imageVector = if (isSpeaking) Icons.Rounded.Stop else Icons.Rounded.GraphicEq,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(if (isSpeaking) "Stop Voice" else "Announce Aloud")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onTake,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("alert_take_button"),
            ) {
                Text("Mark Taken")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    onClick = onSnooze,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("alert_snooze_button"),
                ) {
                    Text("Snooze")
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close",
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        },
    )
}
