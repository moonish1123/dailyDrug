package com.dailydrug.presentation.schedule

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun ScheduleInputScreen(
    onBack: () -> Unit,
    viewModel: ScheduleInputViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                ScheduleInputEvent.Success -> onBack()
                is ScheduleInputEvent.ShowMessage -> scope.launch {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    ScheduleInputContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onNameChange = viewModel::updateMedicineName,
        onDosageChange = viewModel::updateDosage,
        onMemoChange = viewModel::updateMemo,
        onSelectColor = viewModel::selectColor,
        onSelectStartDate = viewModel::updateStartDate,
        onSelectEndDate = viewModel::updateEndDate,
        onAddTime = viewModel::addTimeSlot,
        onRemoveTime = viewModel::removeTimeSlot,
        onChangeTakeDays = viewModel::updateTakeDays,
        onChangeRestDays = viewModel::updateRestDays,
        onSave = viewModel::saveSchedule
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@VisibleForTesting
@Composable
internal fun ScheduleInputContent(
    uiState: ScheduleInputUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onDosageChange: (String) -> Unit,
    onMemoChange: (String) -> Unit,
    onSelectColor: (Int) -> Unit,
    onSelectStartDate: (LocalDate) -> Unit,
    onSelectEndDate: (LocalDate?) -> Unit,
    onAddTime: (LocalTime) -> Unit,
    onRemoveTime: (LocalTime) -> Unit,
    onChangeTakeDays: (Int) -> Unit,
    onChangeRestDays: (Int) -> Unit,
    onSave: () -> Unit
) {
    val context = LocalContext.current
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy.MM.dd") }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("스케줄 등록") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.isSaving) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item {
                    SectionCard(
                        title = "약 기본 정보",
                        icon = Icons.Rounded.Medication
                    ) {
                        OutlinedTextField(
                            value = uiState.medicineName,
                            onValueChange = onNameChange,
                            label = { Text("약 이름") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = uiState.dosage,
                            onValueChange = onDosageChange,
                            label = { Text("복용량") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        ColorSelector(
                            selectedColor = uiState.selectedColor,
                            onSelectColor = onSelectColor
                        )
                    }
                }

                item {
                    SectionCard(
                        title = "복용 기간",
                        icon = Icons.Rounded.CalendarMonth
                    ) {
                        DateSelectorRow(
                            label = "시작일",
                            value = uiState.startDate.format(dateFormatter),
                            onClick = {
                                showDatePicker(context, uiState.startDate, onSelectStartDate)
                            }
                        )
                        DateSelectorRow(
                            label = "종료일 (선택)",
                            value = uiState.endDate?.format(dateFormatter) ?: "무기한",
                            onClick = {
                                val base = uiState.endDate ?: uiState.startDate
                                showDatePicker(context, base) { onSelectEndDate(it) }
                            },
                            supportingAction = {
                                if (uiState.endDate != null) {
                                    TextButton(onClick = { onSelectEndDate(null) }) {
                                        Text("초기화")
                                    }
                                }
                            }
                        )
                    }
                }

                item {
                    SectionCard(
                        title = "복용 시간",
                        icon = Icons.Rounded.Schedule
                    ) {
                        TimeSlotSection(
                            timeSlots = uiState.timeSlots,
                            timeFormatter = timeFormatter,
                            onAddTime = { showTimePicker(context, onAddTime) },
                            onRemoveTime = onRemoveTime
                        )
                    }
                }

                item {
                    SectionCard(
                        title = "복용 패턴"
                    ) {
                        PatternSection(
                            takeDays = uiState.takeDays,
                            restDays = uiState.restDays,
                            onTakeDaysChange = onChangeTakeDays,
                            onRestDaysChange = onChangeRestDays
                        )
                    }
                }

                item {
                    SectionCard(
                        title = "메모",
                        supportingText = "복용 시 참고할 내용을 자유롭게 적어주세요."
                    ) {
                        OutlinedTextField(
                            value = uiState.memo,
                            onValueChange = onMemoChange,
                            label = { Text("메모 (선택)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                        )
                    }
                }

                item {
                    SectionCard(
                        title = "복용 미리보기"
                    ) {
                        PreviewSection(uiState = uiState, dateFormatter = dateFormatter, timeFormatter = timeFormatter)
                    }
                }
            }

            Surface(
                tonalElevation = 6.dp
            ) {
                FilledTonalButton(
                    onClick = onSave,
                    enabled = !uiState.isSaving,
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .fillMaxWidth()
                ) {
                    Text("스케줄 저장")
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    supportingText: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                icon?.let {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp
                    ) {
                        Icon(
                            imageVector = it,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(20.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    supportingText?.let { helper ->
                        Text(
                            text = helper,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Column(content = content, verticalArrangement = Arrangement.spacedBy(12.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorSelector(
    selectedColor: Int,
    onSelectColor: (Int) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ScheduleInputUiState.DEFAULT_COLORS.forEachIndexed { index, colorInt ->
            val color = Color(colorInt)
            FilterChip(
                selected = selectedColor == colorInt,
                onClick = { onSelectColor(colorInt) },
                label = { Text("컬러 ${index + 1}") },
                leadingIcon = {
                    Surface(
                        modifier = Modifier.size(18.dp),
                        shape = CircleShape,
                        color = color,
                        tonalElevation = 0.dp,
                        content = {}
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    }
}

@Composable
private fun DateSelectorRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    supportingAction: @Composable (() -> Unit)? = null
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = label, style = MaterialTheme.typography.bodySmall)
                Text(text = value, style = MaterialTheme.typography.bodyLarge)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                supportingAction?.invoke()
                Icon(
                    imageVector = Icons.Rounded.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimeSlotSection(
    timeSlots: List<LocalTime>,
    timeFormatter: DateTimeFormatter,
    onAddTime: () -> Unit,
    onRemoveTime: (LocalTime) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (timeSlots.isEmpty()) {
            Text(
                text = "복용 시간을 추가해주세요.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                timeSlots.forEach { time ->
                    AssistChip(
                        onClick = { onRemoveTime(time) },
                        label = { Text(time.format(timeFormatter)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = "시간 삭제",
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    )
                }
            }
        }
        FilledTonalButton(onClick = onAddTime) {
            Icon(imageVector = Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("시간 추가")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PatternSection(
    takeDays: Int,
    restDays: Int,
    onTakeDaysChange: (Int) -> Unit,
    onRestDaysChange: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val presets = listOf(
            PatternPreset("매일 복용", 1, 0),
            PatternPreset("1일 복용 1일 휴식", 1, 1),
            PatternPreset("5일 복용 1일 휴식", 5, 1)
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presets.forEach { preset ->
                val selected = takeDays == preset.takeDays && restDays == preset.restDays
                FilterChip(
                    selected = selected,
                    onClick = {
                        onTakeDaysChange(preset.takeDays)
                        onRestDaysChange(preset.restDays)
                    },
                    label = { Text(preset.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = takeDays.toString(),
                onValueChange = { value -> value.toIntOrNull()?.let(onTakeDaysChange) },
                label = { Text("복용 일수") },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = restDays.toString(),
                onValueChange = { value -> value.toIntOrNull()?.let(onRestDaysChange) },
                label = { Text("휴식 일수") },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
        val patternText = if (restDays == 0) "매일 복용" else "${takeDays}일 복용 후 ${restDays}일 휴식"
        Text(patternText, style = MaterialTheme.typography.bodyMedium)
    }
}

private data class PatternPreset(val label: String, val takeDays: Int, val restDays: Int)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PreviewSection(
    uiState: ScheduleInputUiState,
    dateFormatter: DateTimeFormatter,
    timeFormatter: DateTimeFormatter
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (uiState.preview.isEmpty()) {
            Text(
                text = "복용 시간과 패턴을 입력하면 예시가 표시됩니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                uiState.preview.forEach { occurrence ->
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                "${occurrence.toLocalDate().format(dateFormatter)} • ${occurrence.toLocalTime().format(timeFormatter)}"
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            labelColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }
        }
    }
}

private fun showDatePicker(
    context: android.content.Context,
    initial: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            onDateSelected(LocalDate.of(year, month + 1, dayOfMonth))
        },
        initial.year,
        initial.monthValue - 1,
        initial.dayOfMonth
    ).show()
}

private fun showTimePicker(
    context: android.content.Context,
    onTimeSelected: (LocalTime) -> Unit
) {
    val now = LocalTime.now()
    TimePickerDialog(
        context,
        { _, hourOfDay, minute -> onTimeSelected(LocalTime.of(hourOfDay, minute)) },
        now.hour,
        now.minute,
        true
    ).show()
}
