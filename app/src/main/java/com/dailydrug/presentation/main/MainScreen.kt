package com.dailydrug.presentation.main

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dailydrug.domain.model.MedicationStatus
import kotlinx.coroutines.flow.collectLatest
import java.time.format.DateTimeFormatter

@Composable
fun MainRoute(
    onAddSchedule: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenMedicineDetail: (Long) -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is MainUiEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    MainScreen(
        state = uiState,
        snackbarHostState = snackbarHostState,
        onAddSchedule = onAddSchedule,
        onOpenSettings = onOpenSettings,
        onPreviousDay = viewModel::onPreviousDay,
        onToday = viewModel::onToday,
        onNextDay = viewModel::onNextDay,
        onToggleTaken = viewModel::onToggleTaken,
        onSkipDose = viewModel::onSkip,
        onOpenMedicineDetail = onOpenMedicineDetail
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    state: MainUiState,
    snackbarHostState: SnackbarHostState,
    onAddSchedule: () -> Unit,
    onOpenSettings: () -> Unit,
    onPreviousDay: () -> Unit,
    onToday: () -> Unit,
    onNextDay: () -> Unit,
    onToggleTaken: (Long, MedicationStatus) -> Unit,
    onSkipDose: (Long) -> Unit,
    onOpenMedicineDetail: (Long) -> Unit
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy.MM.dd (EEE)") }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = "매일 약먹기",
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "설정 열기"
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddSchedule,
                icon = { Icon(Icons.Rounded.Add, contentDescription = "스케줄 추가") },
                text = { Text("새 스케줄") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                tonalElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onPreviousDay) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "이전 날짜"
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = dateFormatter.format(state.selectedDate),
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )
                        AssistChip(
                            onClick = onToday,
                            label = { Text("오늘") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.CalendarToday,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        )
                    }
                    IconButton(onClick = onNextDay) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "다음 날짜"
                        )
                    }
                }
            }

            if (state.isLoading) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                }
            }

            if (state.todayMedications.isEmpty() && !state.isLoading) {
                EmptyMedicationPlaceholder()
            } else {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    tonalElevation = 1.dp,
                    shape = MaterialTheme.shapes.large
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 16.dp,
                            bottom = 112.dp
                        )
                    ) {
                        items(
                            items = state.todayMedications,
                            key = { it.recordId }
                        ) { medication ->
                            MedicationItem(
                                medication = medication,
                                onToggleTaken = onToggleTaken,
                                onSkipDose = onSkipDose,
                                onOpenMedicineDetail = onOpenMedicineDetail
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyMedicationPlaceholder() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 48.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Medication,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            )
            Text(
                text = "오늘은 복용할 약이 없어요",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "오른쪽 아래 버튼을 눌러 새로운 스케줄을 등록해보세요.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun MedicationItem(
    medication: TodayMedication,
    onToggleTaken: (Long, MedicationStatus) -> Unit,
    onSkipDose: (Long) -> Unit,
    onOpenMedicineDetail: (Long) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val cardColor: Color
    val cardContentColor: Color
    val statusLabel: String
    val statusIcon: ImageVector
    val chipContainer: Color
    val chipContent: Color

    when (medication.status) {
        MedicationStatus.TAKEN -> {
            cardColor = colorScheme.primaryContainer
            cardContentColor = colorScheme.onPrimaryContainer
            statusLabel = "복용 완료"
            statusIcon = Icons.Rounded.Check
            chipContainer = colorScheme.primary
            chipContent = colorScheme.onPrimary
        }
        MedicationStatus.SKIPPED -> {
            cardColor = colorScheme.surfaceVariant
            cardContentColor = colorScheme.onSurfaceVariant
            statusLabel = "건너뜀"
            statusIcon = Icons.Rounded.Close
            chipContainer = colorScheme.surface
            chipContent = colorScheme.onSurface
        }
        MedicationStatus.PENDING -> {
            cardColor = colorScheme.errorContainer
            cardContentColor = colorScheme.onErrorContainer
            statusLabel = "복용 예정"
            statusIcon = Icons.Rounded.Schedule
            chipContainer = colorScheme.error
            chipContent = colorScheme.onError
        }
    }
    val actionLabel = if (medication.status == MedicationStatus.TAKEN) "복용 취소" else "복용 완료"

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenMedicineDetail(medication.medicineId) },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(
            containerColor = cardColor,
            contentColor = cardContentColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = medication.medicineName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = medication.dosage,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "예정 시간 ${medication.scheduledTime}",
                        style = MaterialTheme.typography.labelLarge
                    )
                    when (medication.status) {
                        MedicationStatus.TAKEN -> medication.takenTime?.let { taken ->
                            Text(
                                text = "복용 완료: ${taken.toLocalTime()}",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }

                        MedicationStatus.SKIPPED -> Text(
                            text = "이 복용은 건너뛰기로 기록됐어요.",
                            style = MaterialTheme.typography.labelMedium
                        )

                        MedicationStatus.PENDING -> Unit
                    }
                }
                AssistChip(
                    onClick = { onOpenMedicineDetail(medication.medicineId) },
                    label = { Text(statusLabel) },
                    leadingIcon = {
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = chipContainer,
                        labelColor = chipContent,
                        leadingIconContentColor = chipContent
                    )
                )
            }

            HorizontalDivider(color = cardContentColor.copy(alpha = 0.2f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(onClick = { onToggleTaken(medication.recordId, medication.status) }) {
                    Icon(
                        imageVector = if (medication.status == MedicationStatus.TAKEN) Icons.Rounded.Close else Icons.Rounded.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(actionLabel)
                }

                if (medication.status == MedicationStatus.PENDING) {
                    TextButton(onClick = { onSkipDose(medication.recordId) }) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "건너뛰기")
                    }
                }
            }
        }
    }
}
