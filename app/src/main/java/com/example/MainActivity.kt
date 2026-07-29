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

                                NavigationBar(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 8.dp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .windowInsetsPadding(WindowInsets.navigationBars)
                                ) {
                                NavigationBarItem(
                                    selected = uiState.selectedTab == 0,
                                    onClick = { viewModel.setSelectedTab(0) },
                                    icon = { Icon(Icons.Default.Chat, contentDescription = "Private Chat") },
                                    label = { Text("Chat") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = SproutPrimary,
                                        selectedTextColor = SproutPrimary,
                                        indicatorColor = SproutPrimary.copy(alpha = 0.2f),
                                        unselectedIconColor = SproutTextMuted,
                                        unselectedTextColor = SproutTextMuted
                                    ),
                                    modifier = Modifier.testTag("nav_tab_chat")
                                )

                                NavigationBarItem(
                                    selected = uiState.selectedTab == 1,
                                    onClick = { viewModel.setSelectedTab(1) },
                                    icon = { Icon(Icons.Default.SmartToy, contentDescription = "Autonomous Agent") },
                                    label = { Text("Agent") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = SproutPrimary,
                                        selectedTextColor = SproutPrimary,
                                        indicatorColor = SproutPrimary.copy(alpha = 0.2f),
                                        unselectedIconColor = SproutTextMuted,
                                        unselectedTextColor = SproutTextMuted
                                    ),
                                    modifier = Modifier.testTag("nav_tab_agent")
                                )

                                NavigationBarItem(
                                    selected = uiState.selectedTab == 2,
                                    onClick = { viewModel.setSelectedTab(2) },
                                    icon = { Icon(Icons.Default.Tune, contentDescription = "Directives & Lock") },
                                    label = { Text("Directives") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = SproutPrimary,
                                        selectedTextColor = SproutPrimary,
                                        indicatorColor = SproutPrimary.copy(alpha = 0.2f),
                                        unselectedIconColor = SproutTextMuted,
                                        unselectedTextColor = SproutTextMuted
                                    ),
                                    modifier = Modifier.testTag("nav_tab_directives")
                                )

                                NavigationBarItem(
                                    selected = uiState.selectedTab == 3,
                                    onClick = { viewModel.setSelectedTab(3) },
                                    icon = { Icon(Icons.Default.Psychology, contentDescription = "AI Models") },
                                    label = { Text("AI Models") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = SproutPrimary,
                                        selectedTextColor = SproutPrimary,
                                        indicatorColor = SproutPrimary.copy(alpha = 0.2f),
                                        unselectedIconColor = SproutTextMuted,
                                        unselectedTextColor = SproutTextMuted
                                    ),
                                    modifier = Modifier.testTag("nav_tab_ai_models")
                                )

                                NavigationBarItem(
                                    selected = uiState.selectedTab == 4,
                                    onClick = { viewModel.setSelectedTab(4) },
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
                                    label = { Text("Telegram") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = SproutAmber,
                                        selectedTextColor = SproutAmber,
                                        indicatorColor = SproutAmber.copy(alpha = 0.2f),
                                        unselectedIconColor = SproutTextMuted,
                                        unselectedTextColor = SproutTextMuted
                                    ),
                                    modifier = Modifier.testTag("nav_tab_telegram")
                                )

                                NavigationBarItem(
                                    selected = uiState.selectedTab == 5,
                                    onClick = { viewModel.setSelectedTab(5) },
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
                                    label = { Text("Perms") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = SproutPrimary,
                                        selectedTextColor = SproutPrimary,
                                        indicatorColor = SproutPrimary.copy(alpha = 0.2f),
                                        unselectedIconColor = SproutTextMuted,
                                        unselectedTextColor = SproutTextMuted
                                    ),
                                    modifier = Modifier.testTag("nav_tab_permissions")
                                )

                                NavigationBarItem(
                                    selected = uiState.selectedTab == 6,
                                    onClick = { viewModel.setSelectedTab(6) },
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
                                    label = { Text("Sync") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = SproutPrimary,
                                        selectedTextColor = SproutPrimary,
                                        indicatorColor = SproutPrimary.copy(alpha = 0.2f),
                                        unselectedIconColor = SproutTextMuted,
                                        unselectedTextColor = SproutTextMuted
                                    ),
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
