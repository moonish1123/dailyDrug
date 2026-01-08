package com.dailydrug.presentation.llm

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dailydrug.data.model.LlmSettingsEvent
import com.dailydrug.data.model.LlmSettingsUiState
import com.dailydrug.presentation.llm.LlmSettingsViewModel
import com.llmmodule.domain.model.LlmProvider
import kotlinx.coroutines.launch
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult

/**
 * LLM 설정 화면
 * 프로바이더 선택, API 키 설정, 테스트 기능 제공
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LlmSettingsScreen(
    viewModel: LlmSettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            val result = snackbarHostState.showSnackbar(
                message = error,
                actionLabel = "닫기",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.clearError()
            }
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
        // 상단 바
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "뒤로")
            }

            Text(
                text = "LLM 설정",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // 설정 내용
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp)
        ) {
            Text(
                text = "AI 서비스 제공업체를 선택하고 API 키를 설정하세요",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // 프로바이더 선택 및 API 키 관리 (통합)
            LlmProvider.getAllProviders().forEach { provider ->
                ProviderCard(
                    provider = provider,
                    isSelected = uiState.settings.selectedProvider == provider,
                    hasApiKey = uiState.isApiKeyConfigured(provider),
                    selectedModel = uiState.settings.getModel(provider),
                    onModelSelected = { modelId ->
                        viewModel.onEvent(LlmSettingsEvent.ModelSelected(provider, modelId))
                    },
                    onClick = {
                        viewModel.onEvent(LlmSettingsEvent.ProviderSelected(provider))
                    },
                    onSetApiKey = { viewModel.onEvent(LlmSettingsEvent.ShowApiKeyDialog(provider)) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // 저장 버튼
        Button(
            onClick = {
                coroutineScope.launch {
                    keyboardController?.hide()
                    viewModel.onEvent(LlmSettingsEvent.SaveSettings)
                    onNavigateBack()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            enabled = uiState.settings.isValid()
        ) {
            Text("설정 저장")
        }
    }

    // API 키 설정 다이얼로그
    if (uiState.showApiKeyDialog && uiState.editingProvider != null) {
        ApiKeyDialog(
            provider = uiState.editingProvider!!,
            currentApiKey = when (val provider = uiState.editingProvider) {
                is LlmProvider.Claude -> uiState.settings.claudeApiKey
                is LlmProvider.Gpt -> uiState.settings.gptApiKey
                is LlmProvider.ZAI -> uiState.settings.zaiApiKey
                is LlmProvider.Local -> ""
                else -> "" // shouldn't happen
            },
            onDismiss = { viewModel.onEvent(LlmSettingsEvent.DismissDialog) },
            onSave = { apiKey ->
                viewModel.onEvent(LlmSettingsEvent.ApiKeyUpdated(uiState.editingProvider!!, apiKey))
            }
        )
    }
    }
}

/**
 * 프로바이더 선택 섹션
 */
@Composable
private fun ProviderSelectionSection(
    selectedProvider: LlmProvider,
    onProviderSelected: (LlmProvider) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "LLM 프로바이더 선택",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier.selectableGroup()
        ) {
            LlmProvider.getAllProviders().forEach { provider ->
                val isSelected = selectedProvider == provider

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .selectable(
                            selected = isSelected,
                            onClick = { onProviderSelected(provider) }
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 프로바이더 아이콘
                        Icon(
                            imageVector = getProviderIcon(provider),
                            contentDescription = provider.displayName,
                            tint = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        // 프로바이더 정보
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = provider.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )

                            Text(
                                text = getProviderDescription(provider),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // 선택 상태 라디오 버튼
                        RadioButton(
                            selected = isSelected,
                            onClick = { onProviderSelected(provider) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * API 키 관리 섹션
 */
@Composable
private fun ApiKeyManagementSection(
    uiState: LlmSettingsUiState,
    onShowApiKeyDialog: (LlmProvider) -> Unit,
    onTestConnection: (LlmProvider) -> Unit,
    onClearTestResult: () -> Unit,
    modifier: Modifier = Modifier
) {
    val onlineProviders = LlmProvider.getOnlineProviders()

    Column(modifier = modifier) {
        Text(
            text = "API 키 관리",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        onlineProviders.forEach { provider ->
            val hasApiKey = uiState.isApiKeyConfigured(provider)
            val testResult = uiState.testConnectionResult
            val isThisProviderTested = testResult?.provider == provider

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = provider.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (hasApiKey) Icons.Default.CheckCircle else Icons.Default.Error,
                                    contentDescription = null,
                                    tint = if (hasApiKey) Color.Green else Color.Red,
                                    modifier = Modifier.size(16.dp)
                                )

                                Spacer(modifier = Modifier.width(4.dp))

                                Text(
                                    text = if (hasApiKey) "API 키 설정됨" else "API 키 필요",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (hasApiKey) Color.Green else Color.Red
                                )
                            }
                        }

                        Row {
                            // API 키 설정 버튼
                            OutlinedButton(
                                onClick = { onShowApiKeyDialog(provider) },
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Key,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("설정")
                            }

                            // 연결 테스트 버튼
                            Button(
                                onClick = { onTestConnection(provider) },
                                enabled = hasApiKey && !uiState.testConnectionInProgress
                            ) {
                                if (uiState.testConnectionInProgress) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.CloudQueue,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("테스트")
                            }
                        }
                    }

                    // 연결 테스트 결과
                    if (isThisProviderTested && testResult != null) {
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                                .background(
                                    color = if (testResult.success) {
                                        Color.Green.copy(alpha = 0.1f)
                                    } else {
                                        Color.Red.copy(alpha = 0.1f)
                                    }
                                )
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (testResult.success) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = null,
                                tint = if (testResult.success) Color.Green else Color.Red,
                                modifier = Modifier.size(20.dp)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = testResult.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (testResult.success) Color.Green else Color.Red
                                )

                                testResult.responseTime?.let { time ->
                                    Text(
                                        text = "응답 시간: ${time}ms",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            IconButton(
                                onClick = onClearTestResult
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Local LLM 설정 섹션
 */
@Composable
private fun LocalLlmSettingsSection(
    localLlmEnabled: Boolean,
    autoSwitchToOffline: Boolean,
    onLocalLlmToggled: (Boolean) -> Unit,
    onAutoSwitchToggled: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Local LLM 설정",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Local LLM 활성화 토글
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Local LLM 활성화",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "오프라인에서 로컬 LLM 사용 (ExecuTorch)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = localLlmEnabled,
                        onCheckedChange = onLocalLlmToggled
                    )
                }

            Spacer(modifier = Modifier.height(16.dp))

            // 오프라인 자동 전환 토글
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "오프라인 자동 전환",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "인터넷 연결 없을 때 자동으로 Local LLM로 전환",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = autoSwitchToOffline,
                    onCheckedChange = onAutoSwitchToggled,
                    enabled = localLlmEnabled
                )
            }

            if (localLlmEnabled) {
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Column {
                                Text(
                                    text = "Local LLM 정보",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "• 최소 2GB RAM 필요\n• 약 500MB 저장 공간 필요\n• 모델 로딩 시 약간의 지연 발생",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * API 키 설정 다이얼로그
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ApiKeyDialog(
    provider: LlmProvider,
    currentApiKey: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var apiKey by remember { mutableStateOf(currentApiKey) }
    var showPassword by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("${provider.displayName} API 키")
        },
        text = {
            Column {
                Text(
                    text = "${provider.displayName}의 API 키를 입력하세요.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key") },
                    placeholder = { Text("sk-... 또는 Claude API 키") },
                    visualTransformation = if (showPassword) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (showPassword) "숨김" else "표시"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "• API 키는 앱 내에 안전하게 저장됩니다\n• 외부에 유출되지 않도록 주의하세요",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(apiKey.trim())
                    onDismiss()
                },
                enabled = apiKey.isNotBlank()
            ) {
                Text("저장")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}

/**
 * 통합 프로바이더 카드 (선택 + API 키 설정 + 모델 선택)
 */
@Composable
private fun ProviderCard(
    provider: LlmProvider,
    isSelected: Boolean,
    hasApiKey: Boolean,
    selectedModel: String,
    onModelSelected: (String) -> Unit,
    onClick: () -> Unit,
    onSetApiKey: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Provider selection row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = isSelected,
                        onClick = onClick
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Provider icon
                Icon(
                    imageVector = getProviderIcon(provider),
                    contentDescription = provider.displayName,
                    tint = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Provider info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = provider.displayName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )

                    Text(
                        text = getProviderDescription(provider),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Radio button for selection
                RadioButton(
                    selected = isSelected,
                    onClick = onClick
                )
            }

            // Model Selection (if supported)
            if (provider.supportedModels.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                
                var expanded by remember { mutableStateOf(false) }
                val currentModelName = provider.supportedModels.find { it.id == selectedModel }?.displayName ?: selectedModel

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "사용 모델:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Box {
                        OutlinedButton(
                            onClick = { expanded = true }
                        ) {
                            Text(currentModelName)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                        
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            provider.supportedModels.forEach { model ->
                                DropdownMenuItem(
                                    text = { Text(model.displayName) },
                                    onClick = {
                                        onModelSelected(model.id)
                                        expanded = false
                                    },
                                    trailingIcon = if (model.id == selectedModel) {
                                        { Icon(Icons.Default.Check, contentDescription = null) }
                                    } else null
                                )
                            }
                        }
                    }
                }
            }

            // API Key status and set button (for online providers)
            if (provider.isOnline) {
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // API Key status
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (hasApiKey) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (hasApiKey) Color.Green else Color.Red,
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = if (hasApiKey) "API 키 설정됨" else "API 키 필요",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (hasApiKey) Color.Green else Color.Red
                        )
                    }

                    // Set API Key button
                    OutlinedButton(
                        onClick = onSetApiKey
                    ) {
                        Icon(
                            Icons.Default.Key,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("API 키 설정", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

/**
 * 프로바이더별 아이콘 반환
 */
private fun getProviderIcon(provider: LlmProvider): ImageVector {
    return when (provider) {
        is LlmProvider.Claude -> Icons.Default.CloudQueue
        is LlmProvider.Gpt -> Icons.Default.Chat
        is LlmProvider.ZAI -> Icons.Filled.Psychology
        is LlmProvider.Local -> Icons.Default.Memory
        else -> Icons.Default.Help
    }
}

/**
 * 프로바이더별 설명 반환
 */
private fun getProviderDescription(provider: LlmProvider): String {
    return when (provider) {
        is LlmProvider.Claude -> "Anthropic Claude (인터넷 필요)"
        is LlmProvider.Gpt -> "OpenAI GPT (인터넷 필요)"
        is LlmProvider.ZAI -> "Z.AI GLM (인터넷 필요)"
        is LlmProvider.Local -> "오프라인 LLM (인터넷 불필요)"
        else -> "알 수 없는 프로바이더"
    }
}