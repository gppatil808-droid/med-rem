package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AlarmOff
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.DoseStatus
import com.example.data.MedicineEntity
import com.example.data.ScheduleTime
import com.example.data.TimeBucket
import com.example.tts.MedicineTtsManager
import com.example.ui.MedCueUiState
import com.example.ui.components.DailyProgressHeroCard
import com.example.ui.components.DoseScheduleCard
import com.example.ui.components.VoiceSpeakingBanner
import java.util.Calendar
import java.util.Locale

@Composable
fun TodayScreen(
    state: MedCueUiState,
    hasNotificationPermission: Boolean,
    hasExactAlarmPermission: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    onAddMedicineClick: () -> Unit,
    onOpenSettingsClick: () -> Unit,
    onReadScheduleClick: () -> Unit,
    onStopVoiceClick: () -> Unit,
    onTakeDose: (MedicineEntity, ScheduleTime) -> Unit,
    onSkipDose: (MedicineEntity, ScheduleTime) -> Unit,
    onSnoozeDose: (MedicineEntity, ScheduleTime) -> Unit,
    onUndoDose: (Long, ScheduleTime) -> Unit,
    onSpeakDose: (MedicineEntity, ScheduleTime) -> Unit,
    modifier: Modifier = Modifier,
) {
    val nowCal = Calendar.getInstance()
    val greeting = when (nowCal.get(Calendar.HOUR_OF_DAY)) {
        in 5..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        in 17..20 -> "Good Evening"
        else -> "Good Night"
    }
    val dayOfWeek = nowCal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault()) ?: ""
    val month = nowCal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()) ?: ""
    val dateLabel = "$dayOfWeek, $month ${nowCal.get(Calendar.DAY_OF_MONTH)}"

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .widthIn(max = 720.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = dateLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.headlineMedium,
                    )
                }
                IconButton(
                    onClick = onOpenSettingsClick,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("today_settings_button"),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = stringResource(R.string.action_settings),
                    )
                }
            }
        }

        item {
            VoiceSpeakingBanner(
                isVisible = state.ttsState.isSpeaking,
                onStop = onStopVoiceClick,
            )
        }

        if (!hasNotificationPermission) {
            item {
                PermissionNoticeCard(
                    icon = Icons.Outlined.NotificationsOff,
                    title = "Notifications are off",
                    subtitle = "Allow notifications so MedCue can display medicine alerts.",
                    buttonTag = "enable_notifications_button",
                    onEnableClick = onRequestNotificationPermission,
                )
            }
        }

        if (!hasExactAlarmPermission) {
            item {
                PermissionNoticeCard(
                    icon = Icons.Outlined.AlarmOff,
                    title = "Exact alarms are off",
                    subtitle = "Allow Alarms & reminders so medicine alerts fire at the exact minute.",
                    buttonTag = "enable_exact_alarms_button",
                    onEnableClick = onOpenExactAlarmSettings,
                )
            }
        }

        item {
            DailyProgressHeroCard(
                takenCount = state.takenTodayCount,
                totalCount = state.totalTodayCount,
                nextDose = state.nextUpcomingDose,
                isSpeakingSummary = state.ttsState.isSpeaking &&
                    state.ttsState.activeKey == MedicineTtsManager.SUMMARY_KEY,
                onReadScheduleClick = onReadScheduleClick,
            )
        }

        if (state.allMedicines.isEmpty()) {
            item {
                EmptyMedicineCard(onAddMedicineClick = onAddMedicineClick)
            }
        } else if (state.todayDoses.isEmpty()) {
            item {
                NoDosesTodayCard(onAddMedicineClick = onAddMedicineClick)
            }
        } else {
            val grouped = state.todayDoses.groupBy { it.scheduleTime.timeBucket() }
            TimeBucket.entries.forEach { bucket ->
                val dosesInBucket = grouped[bucket].orEmpty()
                if (dosesInBucket.isNotEmpty()) {
                    item(key = "header_${bucket.name}") {
                        TimeBucketHeader(bucket = bucket, count = dosesInBucket.size)
                    }
                    items(
                        items = dosesInBucket,
                        key = { it.key },
                    ) { doseItem ->
                        val activeKey = "med_${doseItem.medicine.id}_${doseItem.scheduleTime.toStorageString()}"
                        DoseScheduleCard(
                            item = doseItem,
                            isSpeakingThis = state.ttsState.isSpeaking && state.ttsState.activeKey == activeKey,
                            onTake = { onTakeDose(doseItem.medicine, doseItem.scheduleTime) },
                            onSkip = { onSkipDose(doseItem.medicine, doseItem.scheduleTime) },
                            onSnooze = { onSnoozeDose(doseItem.medicine, doseItem.scheduleTime) },
                            onUndo = { onUndoDose(doseItem.medicine.id, doseItem.scheduleTime) },
                            onSpeak = { onSpeakDose(doseItem.medicine, doseItem.scheduleTime) },
                        )
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(72.dp))
        }
    }
}

@Composable
private fun TimeBucketHeader(bucket: TimeBucket, count: Int) {
    val icon = when (bucket) {
        TimeBucket.MORNING -> Icons.Rounded.WbSunny
        TimeBucket.AFTERNOON -> Icons.Rounded.WbTwilight
        TimeBucket.EVENING -> Icons.Rounded.NightsStay
        TimeBucket.NIGHT -> Icons.Rounded.Bedtime
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 4.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = bucket.label,
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.width(8.dp))
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(8.dp),
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
private fun PermissionNoticeCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    buttonTag: String,
    onEnableClick: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f),
                )
            }
            Spacer(Modifier.width(8.dp))
            FilledTonalButton(
                onClick = onEnableClick,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag(buttonTag),
            ) {
                Text("Allow")
            }
        }
    }
}

@Composable
private fun EmptyMedicineCard(onAddMedicineClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_empty_schedule),
                contentDescription = "Illustrated pill box and water glass",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(18.dp)),
            )
            Spacer(Modifier.height(18.dp))
            Text(
                text = stringResource(R.string.empty_schedule_title),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.empty_schedule_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = onAddMedicineClick,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("empty_add_medicine_button"),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text("Add First Medicine")
            }
        }
    }
}

@Composable
private fun NoDosesTodayCard(onAddMedicineClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Rest Day — No Doses Scheduled Today",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Your active medicines aren't scheduled for today's weekday, or reminders are paused.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(14.dp))
            FilledTonalButton(
                onClick = onAddMedicineClick,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("no_doses_add_button"),
            ) {
                Text("Add Medicine")
            }
        }
    }
}
