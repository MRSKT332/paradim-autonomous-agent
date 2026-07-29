package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AgentHeader
import com.example.ui.components.AgentWorkingBottomBar
import com.example.ui.components.TaskHistorySidebarDrawer
import com.example.ui.components.VoiceCommandOverlay
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.viewmodel.AgentViewModel
import com.example.viewmodel.TaskLogViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: AgentViewModel by viewModels()
    private val taskLogViewModel: TaskLogViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val isSproutGreenTheme by viewModel.isSproutGreenTheme.collectAsStateWithLifecycle()

            ParadimAgentTheme(isSproutTheme = isSproutGreenTheme) {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val syncHealth by viewModel.syncHealth.collectAsStateWithLifecycle()
                val isAccessibilityActive by viewModel.isAccessibilityActive.collectAsStateWithLifecycle()
                val adSkippedCount by viewModel.adSkippedCount.collectAsStateWithLifecycle()
                val panicKillSwitchActive by viewModel.panicKillSwitchActive.collectAsStateWithLifecycle()
                val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
                val taskHistoryFromDb by taskLogViewModel.taskExecutions.collectAsStateWithLifecycle()
                val agentTaskHistory by viewModel.allTaskExecutions.collectAsStateWithLifecycle()
                val auditLogs by viewModel.allAuditLogs.collectAsStateWithLifecycle()

                val taskHistory = remember(taskHistoryFromDb, agentTaskHistory) {
                    if (taskHistoryFromDb.isNotEmpty()) taskHistoryFromDb else agentTaskHistory
                }

                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(uiState.snackbarMessage) {
                    uiState.snackbarMessage?.let { msg ->
                        snackbarHostState.showSnackbar(msg)
                        viewModel.dismissSnackbar()
                    }
                }

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        TaskHistorySidebarDrawer(
                            taskHistory = taskHistory,
                            adSkippedCount = adSkippedCount,
                            onReRunTask = { rawCmd ->
                                viewModel.executeUserCommand(rawCmd)
                            },
                            onClearHistory = {
                                taskLogViewModel.clearTaskHistory()
                            },
                            onCloseDrawer = {
                                scope.launch { drawerState.close() }
                            }
                        )
                    }
                ) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = MaterialTheme.colorScheme.background,
                        topBar = {
                            AgentHeader(
                                syncHealth = syncHealth,
                                isPanicActive = panicKillSwitchActive,
                                onTogglePanic = { viewModel.togglePanicKillSwitch() },
                                onSyncClick = { viewModel.forceExponentialSync() },
                                onOpenSidebar = {
                                    scope.launch { drawerState.open() }
                                },
                                onOpenVoiceInput = { viewModel.toggleVoiceListening(true) }
                            )
                        },
                        bottomBar = {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                AgentWorkingBottomBar(
                                    isExecuting = uiState.isExecutingTask,
                                    progressText = uiState.taskProgressText,
                                    currentStep = uiState.activeTaskStep,
                                    totalSteps = uiState.totalTaskSteps,
                                    onStopTask = { viewModel.stopCurrentTask() }
                                )

                                ScrollableTabRow(
                                    selectedTabIndex = uiState.selectedTab,
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    contentColor = SproutPrimary,
                                    edgePadding = 12.dp,
                                    divider = {},
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .windowInsetsPadding(WindowInsets.navigationBars)
                                ) {
                                    Tab(
                                        selected = uiState.selectedTab == 0,
                                        onClick = { focusManager.clearFocus(); viewModel.setSelectedTab(0) },
                                        icon = { Icon(Icons.Default.Chat, contentDescription = "Private Chat") },
                                        text = { Text("Chat") },
                                        selectedContentColor = SproutPrimary,
                                        unselectedContentColor = SproutTextMuted,
                                        modifier = Modifier.testTag("nav_tab_chat")
                                    )

                                    Tab(
                                        selected = uiState.selectedTab == 1,
                                        onClick = { focusManager.clearFocus(); viewModel.setSelectedTab(1) },
                                        icon = { Icon(Icons.Default.SmartToy, contentDescription = "Autonomous Agent") },
                                        text = { Text("Agent") },
                                        selectedContentColor = SproutPrimary,
                                        unselectedContentColor = SproutTextMuted,
                                        modifier = Modifier.testTag("nav_tab_agent")
                                    )

                                    Tab(
                                        selected = uiState.selectedTab == 2,
                                        onClick = { focusManager.clearFocus(); viewModel.setSelectedTab(2) },
                                        icon = { Icon(Icons.Default.Tune, contentDescription = "Directives") },
                                        text = { Text("Directives") },
                                        selectedContentColor = SproutPrimary,
                                        unselectedContentColor = SproutTextMuted,
                                        modifier = Modifier.testTag("nav_tab_directives")
                                    )

                                    Tab(
                                        selected = uiState.selectedTab == 3,
                                        onClick = { focusManager.clearFocus(); viewModel.setSelectedTab(3) },
                                        icon = { Icon(Icons.Default.Psychology, contentDescription = "AI Models") },
                                        text = { Text("AI Models") },
                                        selectedContentColor = SproutPrimary,
                                        unselectedContentColor = SproutTextMuted,
                                        modifier = Modifier.testTag("nav_tab_ai_models")
                                    )

                                    Tab(
                                        selected = uiState.selectedTab == 4,
                                        onClick = { focusManager.clearFocus(); viewModel.setSelectedTab(4) },
                                        icon = {
                                            BadgedBox(
                                                badge = {
                                                    if (uiState.isTelegramConnected) {
                                                        Badge(containerColor = SproutEmerald) {
                                                            Text("ON")
                                                        }
                                                    }
                                                }
                                            ) {
                                                Icon(Icons.Default.SendToMobile, contentDescription = "Telegram API")
                                            }
                                        },
                                        text = { Text("Telegram") },
                                        selectedContentColor = SproutAmber,
                                        unselectedContentColor = SproutTextMuted,
                                        modifier = Modifier.testTag("nav_tab_telegram")
                                    )

                                    Tab(
                                        selected = uiState.selectedTab == 5,
                                        onClick = { focusManager.clearFocus(); viewModel.setSelectedTab(5) },
                                        icon = {
                                            BadgedBox(
                                                badge = {
                                                    if (!isAccessibilityActive) {
                                                        Badge(containerColor = SproutAmber) {
                                                            Text("!")
                                                        }
                                                    }
                                                }
                                            ) {
                                                Icon(Icons.Default.Shield, contentDescription = "Permissions")
                                            }
                                        },
                                        text = { Text("Perms") },
                                        selectedContentColor = SproutPrimary,
                                        unselectedContentColor = SproutTextMuted,
                                        modifier = Modifier.testTag("nav_tab_permissions")
                                    )

                                    Tab(
                                        selected = uiState.selectedTab == 6,
                                        onClick = { focusManager.clearFocus(); viewModel.setSelectedTab(6) },
                                        icon = {
                                            BadgedBox(
                                                badge = {
                                                    if (syncHealth.pendingQueueCount > 0) {
                                                        Badge(containerColor = SproutAmber) {
                                                            Text("${syncHealth.pendingQueueCount}")
                                                        }
                                                    }
                                                }
                                            ) {
                                                Icon(Icons.Default.CloudSync, contentDescription = "Sync Engine")
                                            }
                                        },
                                        text = { Text("Sync") },
                                        selectedContentColor = SproutPrimary,
                                        unselectedContentColor = SproutTextMuted,
                                        modifier = Modifier.testTag("nav_tab_sync")
                                    )
                                }
                        }
                    },
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            AnimatedContent(
                                targetState = uiState.selectedTab,
                                transitionSpec = { fadeIn() togetherWith fadeOut() },
                                label = "tab_transition"
                            ) { tab ->
                                when (tab) {
                                    0 -> PrivateChatScreen(
                                        chatMessages = chatMessages,
                                        chatInput = uiState.chatInput,
                                        isChatLoading = uiState.isChatLoading,
                                        onChatInputChanged = { viewModel.onChatInputChanged(it) },
                                        onSendChatMessage = { viewModel.sendChatMessage() },
                                        onDelegateToAgent = { viewModel.delegateChatToAgent(it) },
                                        onClearChat = { viewModel.clearChatHistory() }
                                    )
                                    1 -> AgentCommandHubScreen(
                                        uiState = uiState,
                                        taskHistory = taskHistory,
                                        onCommandInputChanged = { viewModel.onCommandInputChanged(it) },
                                        onExecuteCommand = { viewModel.executeUserCommand(it) },
                                        onOpenVoiceInput = { viewModel.toggleVoiceListening(true) }
                                    )
                                    2 -> SystemPromptAndSecurityScreen(
                                        onSnackbarMessage = { msg ->
                                            scope.launch { snackbarHostState.showSnackbar(msg) }
                                        }
                                    )
                                    3 -> AiModelConfigScreen(
                                        adSkippedCount = adSkippedCount,
                                        onConfigUpdated = {
                                            viewModel.dismissSnackbar()
                                        }
                                    )
                                    4 -> TelegramBotScreen(
                                        telegramToken = uiState.telegramBotToken,
                                        telegramChatId = uiState.telegramChatId,
                                        isConnected = uiState.isTelegramConnected,
                                        botName = uiState.telegramBotName,
                                        isTesting = uiState.isTestingTelegram,
                                        onTokenChanged = { viewModel.onTelegramTokenChanged(it) },
                                        onChatIdChanged = { viewModel.onTelegramChatIdChanged(it) },
                                        onTestConnect = { viewModel.testAndConnectTelegramBot() },
                                        onSendBroadcast = { viewModel.sendTelegramBroadcast(it) },
                                        onSimulateRemoteCommand = { viewModel.handleIncomingTelegramRemoteCommand(it) }
                                    )
                                    5 -> PermissionsCenterScreen(
                                        isAccessibilityActive = isAccessibilityActive
                                    )
                                    6 -> Column(modifier = Modifier.fillMaxSize()) {
                                        SyncEngineScreen(
                                            syncHealth = syncHealth,
                                            auditLogs = auditLogs,
                                            onTriggerScenario = { viewModel.triggerSimulatedEdgeCase(it) },
                                            onForceSync = { viewModel.forceExponentialSync() },
                                            onClearLogs = { viewModel.clearAuditLogs() }
                                        )
                                    }
                                }
                            }

                            if (uiState.isListeningVoice) {
                                VoiceCommandOverlay(
                                    isListening = true,
                                    transcript = uiState.voiceTranscript,
                                    onTranscriptChanged = { viewModel.onVoiceTranscriptChanged(it) },
                                    onExecuteVoiceCommand = { cmd ->
                                        viewModel.executeUserCommand(cmd)
                                    },
                                    onDismiss = { viewModel.toggleVoiceListening(false) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
