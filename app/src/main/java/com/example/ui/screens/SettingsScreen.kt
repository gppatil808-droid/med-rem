package com.example.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.MedCueUiState
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: MedCueUiState,
    hasNotificationPermission: Boolean,
    hasExactAlarmPermission: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    onSetAutoSpeak: (Boolean) -> Unit,
    onSetIncludeInstructions: (Boolean) -> Unit,
    onSetSpeechRate: (Float) -> Unit,
    onSetSpeechPitch: (Float) -> Unit,
    onSetSnoozeMinutes: (Int) -> Unit,
    onTestVoice: () -> Unit,
    onStopVoice: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    val settings = state.settings
    val isTestingVoice = state.ttsState.isSpeaking && state.ttsState.activeKey == "settings_test"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Voice & Reminder Settings", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("settings_back_button"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Navigate back",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .widthIn(max = 640.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SettingsCard(title = "Text-to-Speech Announcements") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto-announce reminders", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "Speak medicine details aloud automatically when a scheduled alarm triggers",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = settings.autoSpeakReminders,
                        onCheckedChange = onSetAutoSpeak,
                        modifier = Modifier.testTag("settings_auto_speak_switch"),
                    )
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Include notes in announcement", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "Read special instructions such as taking with food or water",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = settings.includeInstructionsInSpeech,
                        onCheckedChange = onSetIncludeInstructions,
                        modifier = Modifier.testTag("settings_include_notes_switch"),
                    )
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    text = String.format(Locale.US, "Speaking speed: %.2fx", settings.ttsSpeechRate),
                    style = MaterialTheme.typography.labelLarge,
                )
                Slider(
                    value = settings.ttsSpeechRate,
                    onValueChange = onSetSpeechRate,
                    valueRange = 0.6f..1.5f,
                    modifier = Modifier.testTag("settings_speech_rate_slider"),
                )

                Text(
                    text = String.format(Locale.US, "Voice pitch: %.2fx", settings.ttsPitch),
                    style = MaterialTheme.typography.labelLarge,
                )
                Slider(
                    value = settings.ttsPitch,
                    onValueChange = onSetSpeechPitch,
                    valueRange = 0.75f..1.3f,
                    modifier = Modifier.testTag("settings_speech_pitch_slider"),
                )

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = { if (isTestingVoice) onStopVoice() else onTestVoice() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("settings_test_voice_button"),
                ) {
                    Icon(
                        imageVector = if (isTestingVoice) Icons.Rounded.Stop else Icons.AutoMirrored.Rounded.VolumeUp,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(if (isTestingVoice) "Stop Voice Test" else "Test Voice Announcement")
                }
            }

            SettingsCard(title = "Snooze Duration") {
                Text(
                    text = "Choose how long to delay a snoozed medicine reminder",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(5, 10, 15, 30).forEach { mins ->
                        FilterChip(
                            selected = settings.snoozeMinutes == mins,
                            onClick = { onSetSnoozeMinutes(mins) },
                            label = { Text("${mins}m") },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("snooze_option_$mins"),
                        )
                    }
                }
            }

            SettingsCard(title = "Device Permissions") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Notifications", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = if (hasNotificationPermission) "Enabled" else "Required to show dose alerts",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (!hasNotificationPermission) {
                        OutlinedButton(
                            onClick = onRequestNotificationPermission,
                            modifier = Modifier.minimumInteractiveComponentSize(),
                        ) {
                            Text("Enable")
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Alarm,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Exact Alarms", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = if (hasExactAlarmPermission) "Granted for on-time reminders" else "Allow exact alarms for precise timing",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (!hasExactAlarmPermission) {
                        OutlinedButton(
                            onClick = onOpenExactAlarmSettings,
                            modifier = Modifier.minimumInteractiveComponentSize(),
                        ) {
                            Text("Open")
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                shape = RoundedCornerShape(22.dp),
            ),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}
