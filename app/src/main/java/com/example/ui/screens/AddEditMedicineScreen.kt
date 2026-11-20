package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.text.format.DateFormat
import java.util.Calendar
import com.example.data.DayMaskHelper
import com.example.data.FrequencyType
import com.example.data.MedicineEntity
import com.example.data.MedicineForm
import com.example.data.ScheduleTime
import com.example.ui.components.medicineFormIcon
import com.example.ui.theme.MedicineTagColors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditMedicineScreen(
    medicineId: Long?,
    loadMedicine: suspend (Long) -> MedicineEntity?,
    isSpeakingPreview: Boolean,
    onPreviewSpeech: (name: String, dosage: String, quantity: String, instructions: String, firstTime: ScheduleTime?) -> Unit,
    onStopSpeech: () -> Unit,
    onSave: (
        existingId: Long?,
        name: String,
        dosage: String,
        quantity: String,
        form: MedicineForm,
        instructions: String,
        colorTag: Int,
        times: List<ScheduleTime>,
        frequencyType: FrequencyType,
        daysMask: Int,
        ttsEnabled: Boolean,
    ) -> Unit,
    onNavigateBack: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1 tablet") }
    var form by remember { mutableStateOf(MedicineForm.TABLET) }
    var instructions by remember { mutableStateOf("") }
    var colorTag by remember { mutableIntStateOf(0) }
    var frequencyType by remember { mutableStateOf(FrequencyType.DAILY) }
    var daysMask by remember { mutableIntStateOf(DayMaskHelper.ALL_DAYS) }
    var ttsEnabled by remember { mutableStateOf(true) }
    val times = remember { mutableStateListOf(ScheduleTime(8, 0)) }

    var showTimePicker by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(medicineId) {
        if (medicineId != null && medicineId > 0L) {
            loadMedicine(medicineId)?.let { existing ->
                name = existing.name
                dosage = existing.dosage
                quantity = existing.quantity
                form = existing.form
                instructions = existing.instructions
                colorTag = existing.colorTag
                frequencyType = existing.frequencyType
                daysMask = existing.daysMask
                ttsEnabled = existing.ttsEnabled
                times.clear()
                times.addAll(existing.scheduledTimes().ifEmpty { listOf(ScheduleTime(8, 0)) })
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (medicineId != null && medicineId > 0L) "Edit Medicine" else "New Medicine",
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("editor_back_button"),
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
            validationError?.let { errorMsg ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = errorMsg,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(14.dp),
                    )
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    validationError = null
                },
                label = { Text("Medicine Name *") },
                placeholder = { Text("e.g., Metformin, Vitamin D3") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("medicine_name_input"),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = dosage,
                    onValueChange = { dosage = it },
                    label = { Text("Strength / Dosage") },
                    placeholder = { Text("e.g., 500 mg") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("medicine_dosage_input"),
                )
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Dose Amount") },
                    placeholder = { Text("e.g., 1 tablet") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("medicine_quantity_input"),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Form", style = MaterialTheme.typography.titleSmall)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MedicineForm.entries.forEach { option ->
                        FilterChip(
                            selected = form == option,
                            onClick = { form = option },
                            leadingIcon = {
                                Icon(
                                    imageVector = medicineFormIcon(option),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                            label = { Text(option.label) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("form_chip_${option.name.lowercase()}"),
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Color Badge", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MedicineTagColors.forEachIndexed { idx, swatchColor ->
                        val isSelected = colorTag == idx
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(swatchColor)
                                .then(
                                    if (isSelected) {
                                        Modifier.border(3.dp, MaterialTheme.colorScheme.onBackground, CircleShape)
                                    } else {
                                        Modifier
                                    },
                                )
                                .clickable { colorTag = idx }
                                .testTag("color_swatch_$idx"),
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Selected color",
                                    tint = Color.White,
                                )
                            }
                        }
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f), RoundedCornerShape(20.dp)),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text("Scheduled Times *", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Add one or more daily reminder times",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        OutlinedButton(
                            onClick = { showTimePicker = true },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("add_time_button"),
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add Time")
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        times.sorted().forEach { time ->
                            InputChip(
                                selected = true,
                                onClick = {
                                    if (times.size > 1) times.remove(time)
                                },
                                label = {
                                    Text(
                                        text = time.toFormatted12h(),
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Schedule,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                },
                                trailingIcon = if (times.size > 1) {
                                    {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "Remove ${time.toFormatted12h()}",
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                } else {
                                    null
                                },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("time_chip_${time.toStorageString()}"),
                            )
                        }
                    }

                    Text(
                        text = "Quick presets",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val presets = listOf(
                        ScheduleTime(8, 0),
                        ScheduleTime(12, 0),
                        ScheduleTime(18, 0),
                        ScheduleTime(21, 0),
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        presets.forEach { preset ->
                            val alreadyAdded = times.contains(preset)
                            FilterChip(
                                selected = alreadyAdded,
                                onClick = {
                                    if (alreadyAdded) {
                                        if (times.size > 1) times.remove(preset)
                                    } else {
                                        times.add(preset)
                                    }
                                },
                                label = { Text(preset.toFormatted12h()) },
                                modifier = Modifier.minimumInteractiveComponentSize(),
                            )
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Frequency", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FrequencyType.entries.forEach { option ->
                        FilterChip(
                            selected = frequencyType == option,
                            onClick = { frequencyType = option },
                            label = { Text(option.label) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("frequency_chip_${option.name.lowercase()}"),
                        )
                    }
                }

                if (frequencyType == FrequencyType.SPECIFIC_DAYS) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        DayMaskHelper.dayInitials.forEachIndexed { idx, initial ->
                            val selected = DayMaskHelper.isDayEnabled(daysMask, idx)
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    )
                                    .clickable {
                                        daysMask = DayMaskHelper.toggleDay(daysMask, idx)
                                        validationError = null
                                    }
                                    .testTag("day_toggle_$idx"),
                            ) {
                                Text(
                                    text = initial,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = instructions,
                onValueChange = { instructions = it },
                label = { Text("Instructions or Notes") },
                placeholder = { Text("e.g., Take after meal with a full glass of water") },
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("medicine_instructions_input"),
            )

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Voice Announcement (TTS)", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "Read medicine name, dosage, and notes aloud when reminder triggers",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = ttsEnabled,
                            onCheckedChange = { ttsEnabled = it },
                            modifier = Modifier.testTag("editor_tts_switch"),
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            if (isSpeakingPreview) {
                                onStopSpeech()
                            } else {
                                onPreviewSpeech(name, dosage, quantity, instructions, times.firstOrNull())
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("editor_preview_voice_button"),
                    ) {
                        Icon(
                            imageVector = if (isSpeakingPreview) Icons.Rounded.Stop else Icons.AutoMirrored.Rounded.VolumeUp,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(if (isSpeakingPreview) "Stop Voice Preview" else "Preview Voice Announcement")
                    }
                }
            }

            Button(
                onClick = {
                    val trimmedName = name.trim()
                    when {
                        trimmedName.isEmpty() -> validationError = "Please enter the medicine name."
                        times.isEmpty() -> validationError = "Please add at least one reminder time."
                        frequencyType == FrequencyType.SPECIFIC_DAYS && daysMask == 0 ->
                            validationError = "Select at least one day of the week."
                        else -> onSave(
                            medicineId,
                            trimmedName,
                            dosage.trim(),
                            quantity.trim(),
                            form,
                            instructions.trim(),
                            colorTag,
                            times.toList(),
                            frequencyType,
                            daysMask,
                            ttsEnabled,
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("save_medicine_button"),
            ) {
                Text(
                    text = if (medicineId != null && medicineId > 0L) "Save Changes" else "Save & Schedule Reminder",
                    style = MaterialTheme.typography.titleSmall,
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showTimePicker) {
        val context = LocalContext.current
        val defaultCal = remember(showTimePicker) {
            Calendar.getInstance().apply { add(Calendar.MINUTE, 1) }
        }
        val pickerState = rememberTimePickerState(
            initialHour = defaultCal.get(Calendar.HOUR_OF_DAY),
            initialMinute = defaultCal.get(Calendar.MINUTE),
            is24Hour = DateFormat.is24HourFormat(context),
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Select Reminder Time") },
            text = {
                TimePicker(state = pickerState)
            },
            confirmButton = {
                Button(
                    onClick = {
                        val picked = ScheduleTime(pickerState.hour, pickerState.minute)
                        if (!times.contains(picked)) {
                            times.add(picked)
                        }
                        showTimePicker = false
                    },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("confirm_time_picker_button"),
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showTimePicker = false },
                    modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                    Text("Cancel")
                }
            },
        )
    }
}
