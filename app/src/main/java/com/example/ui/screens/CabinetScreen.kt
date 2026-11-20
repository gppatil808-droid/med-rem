package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.DayMaskHelper
import com.example.data.MedicineEntity
import com.example.ui.MedCueUiState
import com.example.ui.components.VoiceSpeakingBanner
import com.example.ui.components.medicineFormIcon
import com.example.ui.components.medicineTagColor

private enum class CabinetFilter(val label: String) {
    ALL("All"),
    ACTIVE("Active"),
    PAUSED("Paused"),
}

@Composable
fun CabinetScreen(
    state: MedCueUiState,
    onAddMedicineClick: () -> Unit,
    onEditMedicineClick: (Long) -> Unit,
    onToggleActive: (MedicineEntity) -> Unit,
    onDeleteMedicine: (MedicineEntity) -> Unit,
    onSpeakMedicine: (MedicineEntity) -> Unit,
    onStopVoice: () -> Unit,
    onTestNotification: (MedicineEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    var searchQuery by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(CabinetFilter.ALL) }
    var pendingDelete by remember { mutableStateOf<MedicineEntity?>(null) }

    val filtered = remember(state.allMedicines, searchQuery, filter) {
        state.allMedicines.filter { med ->
            val matchesFilter = when (filter) {
                CabinetFilter.ALL -> true
                CabinetFilter.ACTIVE -> med.isActive
                CabinetFilter.PAUSED -> !med.isActive
            }
            val q = searchQuery.trim()
            val matchesQuery = q.isEmpty() ||
                med.name.contains(q, ignoreCase = true) ||
                med.dosage.contains(q, ignoreCase = true) ||
                med.instructions.contains(q, ignoreCase = true)
            matchesFilter && matchesQuery
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .widthIn(max = 720.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column {
                Text(
                    text = "Medicine Cabinet",
                    style = MaterialTheme.typography.headlineMedium,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Manage prescriptions, reminder schedules, and voice announcements",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            VoiceSpeakingBanner(
                isVisible = state.ttsState.isSpeaking,
                onStop = onStopVoice,
            )
        }

        if (state.allMedicines.isNotEmpty()) {
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search medicines or instructions") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cabinet_search_input"),
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CabinetFilter.entries.forEach { option ->
                        FilterChip(
                            selected = filter == option,
                            onClick = { filter = option },
                            label = { Text(option.label) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("cabinet_filter_${option.name.lowercase()}"),
                        )
                    }
                }
            }
        }

        if (state.allMedicines.isEmpty()) {
            item {
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
                            contentDescription = "Empty medicine cabinet",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(170.dp)
                                .clip(RoundedCornerShape(18.dp)),
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "No medicines saved yet",
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Tap below to enter medicine details, dosage, and reminder times.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = onAddMedicineClick,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("cabinet_empty_add_button"),
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Add Medicine")
                        }
                    }
                }
            }
        } else if (filtered.isEmpty()) {
            item {
                Text(
                    text = "No medicines match your current search or filter.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            }
        } else {
            items(items = filtered, key = { it.id }) { medicine ->
                val speakingKey = "med_${medicine.id}_all"
                CabinetMedicineCard(
                    medicine = medicine,
                    isSpeaking = state.ttsState.isSpeaking && state.ttsState.activeKey == speakingKey,
                    onEdit = { onEditMedicineClick(medicine.id) },
                    onToggleActive = { onToggleActive(medicine) },
                    onSpeak = { onSpeakMedicine(medicine) },
                    onTestReminder = { onTestNotification(medicine) },
                    onDeleteRequest = { pendingDelete = medicine },
                )
            }
        }

        item {
            Spacer(Modifier.height(72.dp))
        }
    }

    pendingDelete?.let { med ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Remove ${med.name}?") },
            text = {
                Text("This will delete its schedule, cancel all pending reminders, and remove its log history.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteMedicine(med)
                        pendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("confirm_delete_medicine_button"),
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { pendingDelete = null },
                    modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                    Text("Cancel")
                }
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CabinetMedicineCard(
    medicine: MedicineEntity,
    isSpeaking: Boolean,
    onEdit: () -> Unit,
    onToggleActive: () -> Unit,
    onSpeak: () -> Unit,
    onTestReminder: () -> Unit,
    onDeleteRequest: () -> Unit,
) {
    val accent = medicineTagColor(medicine.colorTag)
    val freqLabel = DayMaskHelper.formatDaysSummary(medicine.frequencyType, medicine.daysMask)

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                shape = RoundedCornerShape(22.dp),
            )
            .testTag("cabinet_card_${medicine.id}"),
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
                        .background(accent.copy(alpha = 0.15f)),
                ) {
                    Icon(
                        imageVector = medicineFormIcon(medicine.form),
                        contentDescription = medicine.form.label,
                        tint = accent,
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = medicine.name,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    val meta = listOf(medicine.dosage, medicine.quantity, medicine.form.label)
                        .filter { it.isNotBlank() }
                        .joinToString(" • ")
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Switch(
                    checked = medicine.isActive,
                    onCheckedChange = { onToggleActive() },
                    modifier = Modifier.testTag("toggle_active_${medicine.id}"),
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = freqLabel + if (medicine.ttsEnabled) " • Voice announcements on" else " • Silent notification",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                medicine.scheduledTimes().forEach { time ->
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text(
                            text = time.toFormatted12h(),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }
                }
            }

            if (medicine.instructions.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = medicine.instructions,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = onSpeak,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("cabinet_speak_${medicine.id}"),
                ) {
                    Icon(
                        imageVector = if (isSpeaking) Icons.Rounded.Stop else Icons.AutoMirrored.Rounded.VolumeUp,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(if (isSpeaking) "Stop" else "Speak")
                }

                OutlinedButton(
                    onClick = onTestReminder,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("cabinet_test_notify_${medicine.id}"),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.NotificationsActive,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Test")
                }

                Spacer(Modifier.weight(1f))

                IconButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("cabinet_edit_${medicine.id}"),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Edit ${medicine.name}",
                    )
                }

                IconButton(
                    onClick = onDeleteRequest,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("cabinet_delete_${medicine.id}"),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete ${medicine.name}",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}
