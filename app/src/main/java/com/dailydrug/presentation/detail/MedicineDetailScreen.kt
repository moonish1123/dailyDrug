package com.dailydrug.presentation.detail

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest
import java.time.format.DateTimeFormatter

@Composable
fun MedicineDetailScreen(
    medicineId: Long?,
    onBack: () -> Unit,
    onEditSchedule: (Long) -> Unit,
    viewModel: MedicineDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var scheduleToDelete by remember { mutableStateOf<ScheduleUiModel?>(null) }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is MedicineDetailEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    MedicineDetailContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onEditSchedule = { schedule -> onEditSchedule(schedule.id) },
        onDeleteSchedule = { schedule -> scheduleToDelete = schedule }
    )

    scheduleToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { scheduleToDelete = null },
            title = { Text("스케줄 삭제") },
            text = {
                Text(
                    "${target.period}\n${target.times}\n스케줄을 삭제할까요?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteSchedule(target.id)
                    scheduleToDelete = null
                }) {
                    Text("삭제")
                }
            },
            dismissButton = {
                TextButton(onClick = { scheduleToDelete = null }) {
                    Text("취소")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@VisibleForTesting
@Composable
internal fun MedicineDetailContent(
    uiState: MedicineDetailUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onEditSchedule: (ScheduleUiModel) -> Unit,
    onDeleteSchedule: (ScheduleUiModel) -> Unit
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy.MM.dd") }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("약 상세") },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (uiState.isLoading) {
                item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
            }
            uiState.header?.let { header ->
                item { HeaderSection(header = header) }
            }
            if (uiState.schedules.isNotEmpty()) {
                item { SectionTitle("스케줄") }
                items(uiState.schedules) { schedule ->
                    ScheduleCard(
                        schedule = schedule,
                        onEdit = onEditSchedule,
                        onDelete = onDeleteSchedule
                    )
                }
            }
            item { SectionTitle("복용 이력") }
            if (uiState.history.isEmpty()) {
                item { EmptyHistoryPlaceholder() }
            } else {
                items(uiState.history) { history ->
                    HistoryRow(history, dateFormatter, timeFormatter)
                }
            }
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun EmptyHistoryPlaceholder() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.large
    ) {
        Text(
            text = "복용 이력이 없습니다.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp)
        )
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun HistoryStatusChip(status: HistoryStatus) {
    val (text, container, content) = when (status) {
        HistoryStatus.TAKEN -> Triple("완료", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
        HistoryStatus.SKIPPED -> Triple("건너뜀", MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
        HistoryStatus.MISSED -> Triple("미복용", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
    }
    AssistChip(
        onClick = {},
        label = { Text(text) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = container,
            labelColor = content
        )
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HeaderSection(header: MedicineDetailHeader) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = Color(header.color),
            contentColor = Color.White
        ),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(header.name, style = MaterialTheme.typography.headlineSmall, color = Color.White)
                Text(header.dosage, style = MaterialTheme.typography.bodyLarge, color = Color.White)
            }
            if (header.memo.isNotBlank()) {
                Text(header.memo, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f))
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LinearProgressIndicator(
                    progress = { header.adherencePercent / 100f },
                    trackColor = Color.White.copy(alpha = 0.3f),
                    color = Color.White
                )
                Text(
                    text = "복용률 ${header.adherencePercent}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatChip(Icons.Rounded.CalendarMonth, "총 복용", header.totalDoses.toString())
                StatChip(Icons.Rounded.CheckCircle, "완료", header.takenCount.toString())
                StatChip(Icons.Rounded.Close, "건너뜀", header.skippedCount.toString())
                StatChip(Icons.Rounded.Bolt, "연속일", header.streak.toString())
            }
        }
    }
}

@Composable
private fun StatChip(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, value: String) {
    AssistChip(
        onClick = {},
        label = { Text("$title $value") },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = Color.White.copy(alpha = 0.18f),
            labelColor = Color.White,
            leadingIconContentColor = Color.White
        )
    )
}

@Composable
private fun ScheduleCard(
    schedule: ScheduleUiModel,
    onEdit: (ScheduleUiModel) -> Unit,
    onDelete: (ScheduleUiModel) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(schedule.period, style = MaterialTheme.typography.titleMedium)
            Text(schedule.times, style = MaterialTheme.typography.bodyMedium)
            val statusText = if (schedule.isActive) "진행 중" else "비활성"
            AssistChip(
                onClick = {},
                label = { Text(statusText) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (schedule.isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    labelColor = if (schedule.isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { onEdit(schedule) }) { Text("수정") }
                TextButton(onClick = { onDelete(schedule) }) { Text("삭제") }
            }
        }
    }
}

@Composable
private fun HistoryRow(
    history: HistoryItem,
    dateFormatter: DateTimeFormatter,
    timeFormatter: DateTimeFormatter
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
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
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = history.scheduledDate.format(dateFormatter),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = history.scheduledTime.format(timeFormatter),
                    style = MaterialTheme.typography.bodySmall
                )
                history.takenTime?.let { taken ->
                    Text(
                        text = "복용 완료: ${taken.toLocalTime().format(timeFormatter)}",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            HistoryStatusChip(status = history.status)
        }
    }
}
