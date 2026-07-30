package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AutonomousAiAgent
import com.example.ai.LlmConfigManager
import com.example.ai.MultiModelLlmClient
import com.example.api.TelegramBotManager
import com.example.data.AppDatabase
import com.example.data.DataSyncManager
import com.example.data.SyncHealthStatus
import com.example.data.entity.*
import com.example.security.SecurityPolicyManager
import com.example.service.ParadimAccessibilityService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

data class AgentUiState(
    val commandInput: String = "",
    val chatInput: String = "",
    val isExecutingTask: Boolean = false,
    val taskProgressText: String = "",
    val activeTaskStep: Int = 0,
    val totalTaskSteps: Int = 0,
    val latestTaskResult: TaskExecutionEntity? = null,
    val selectedTab: Int = 0,
    val isBiometricModalOpen: Boolean = false,
    val pendingBiometricService: String? = null,
    val snackbarMessage: String? = null,
    // Telegram Bot Integration State
    val telegramBotToken: String = "",
    val telegramChatId: String = "",
    val isTelegramConnected: Boolean = false,
    val telegramBotName: String? = null,
    val isTestingTelegram: Boolean = false,
    val telegramErrorMessage: String? = null,
    val isChatLoading: Boolean = false,
    // Voice Command Control State
    val isListeningVoice: Boolean = false,
    val voiceTranscript: String = ""
)

class AgentViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val syncManager = DataSyncManager(db)
    val agentEngine = AutonomousAiAgent(application, db)

    private val _uiState = MutableStateFlow(AgentUiState())
    val uiState: StateFlow<AgentUiState> = _uiState.asStateFlow()

    val syncHealth: StateFlow<SyncHealthStatus> = syncManager.syncHealth
    val isAccessibilityActive: StateFlow<Boolean> = ParadimAccessibilityService.isServiceActive
    val adSkippedCount: StateFlow<Int> = ParadimAccessibilityService.adSkippedCount
    val panicKillSwitchActive: StateFlow<Boolean> = SecurityPolicyManager.panicKillSwitchActive
    val isSproutGreenTheme: StateFlow<Boolean> = SecurityPolicyManager.isSproutGreenTheme
    val correctionRules: StateFlow<List<com.example.ai.AgentCorrectionRule>> = com.example.ai.AgentMemoryManager.correctionRules

    val chatMessages: StateFlow<List<ChatMessageEntity>> = db.chatMessageDao().getAllMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTaskExecutions: StateFlow<List<TaskExecutionEntity>> = db.taskExecutionDao().getAllTaskExecutions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAuditLogs: StateFlow<List<SyncAuditLogEntity>> = db.syncAuditLogDao().getAllAuditLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMacros: StateFlow<List<MacroEntity>> = db.macroDao().getAllMacros()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allApps: StateFlow<List<AppRegistryEntity>> = db.appRegistryDao().getAllApps()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCredentials: StateFlow<List<CredentialVaultEntity>> = db.credentialVaultDao().getAllCredentials()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        SecurityPolicyManager.init(application)
        com.example.ai.AgentMemoryManager.init(application)
        viewModelScope.launch {
            syncManager.initializeDefaultData()
            seedInitialChatGreeting()
        }
    }

    fun submitAgentCorrection(correctionText: String) {
        if (correctionText.isBlank()) return
        val rule = com.example.ai.AgentMemoryManager.addCorrection(getApplication(), correctionText)
        _uiState.value = _uiState.value.copy(
            snackbarMessage = "🧠 Agent learned correction rule: \"${rule.text}\""
        )
    }

    fun removeAgentCorrection(ruleId: String) {
        com.example.ai.AgentMemoryManager.removeCorrection(getApplication(), ruleId)
        _uiState.value = _uiState.value.copy(
            snackbarMessage = "Removed agent correction rule"
        )
    }

    private suspend fun seedInitialChatGreeting() {
        val messages = db.chatMessageDao().getAllMessages().first()
        if (messages.isEmpty()) {
            db.chatMessageDao().insert(
                ChatMessageEntity(
                    sender = MessageSender.AI_CHAT,
                    text = "Hello! I am your Private Agent Assistant. You can chat with me naturally or delegate full agentic tasks to execute on your device.",
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    fun togglePanicKillSwitch() {
        val current = SecurityPolicyManager.panicKillSwitchActive.value
        val next = !current
        SecurityPolicyManager.setPanicKillSwitch(getApplication(), next)
        _uiState.value = _uiState.value.copy(
            snackbarMessage = if (next) "🚨 EMERGENCY KILL SWITCH ACTIVATED! All agent actions blocked." else "🟢 Emergency Kill Switch deactivated. Agent resumed."
        )
    }

    fun onCommandInputChanged(text: String) {
        _uiState.value = _uiState.value.copy(commandInput = text)
    }

    fun onChatInputChanged(text: String) {
        _uiState.value = _uiState.value.copy(chatInput = text)
    }

    fun onTelegramTokenChanged(token: String) {
        _uiState.value = _uiState.value.copy(telegramBotToken = token)
    }

    fun onTelegramChatIdChanged(chatId: String) {
        _uiState.value = _uiState.value.copy(telegramChatId = chatId)
    }

    fun setSelectedTab(index: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = index)
    }

    // --- Private Chat Mode Functions ---
    fun sendChatMessage() {
        val prompt = _uiState.value.chatInput.trim()
        if (prompt.isBlank()) return

        _uiState.value = _uiState.value.copy(chatInput = "", isChatLoading = true)

        viewModelScope.launch {
            val userMsg = ChatMessageEntity(
                sender = MessageSender.USER,
                text = prompt,
                timestamp = System.currentTimeMillis()
            )
            db.chatMessageDao().insert(userMsg)

            // Query selected LLM Provider (Groq, DeepSeek, OpenAI, Ollama Local, Gemini)
            val llmConfig = LlmConfigManager.getConfig(getApplication())
            val systemPrompt = "You are a private conversational AI assistant embedded in Paradim Agentic OS. Provide concise, helpful responses."
            val reply = MultiModelLlmClient.queryLlm(llmConfig, prompt, systemPrompt)

            val displayReply = if (reply == "DEMO_MODE") {
                "I understand you're asking about: \"$prompt\". I am configured as your private AI assistant (${llmConfig.provider.displayName}). You can ask me questions or convert this into an autonomous agent action."
            } else reply

            val aiMsg = ChatMessageEntity(
                sender = MessageSender.AI_CHAT,
                text = displayReply,
                timestamp = System.currentTimeMillis()
            )
            db.chatMessageDao().insert(aiMsg)

            _uiState.value = _uiState.value.copy(isChatLoading = false)
        }
    }

    fun delegateChatToAgent(prompt: String) {
        viewModelScope.launch {
            db.chatMessageDao().insert(
                ChatMessageEntity(
                    sender = MessageSender.AI_CHAT,
                    text = "⚡ Delegating task to Autonomous Agent: \"$prompt\"",
                    isAgentTaskTrigger = true,
                    timestamp = System.currentTimeMillis()
                )
            )
            executeUserCommand(prompt)
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            db.chatMessageDao().clearChatHistory()
            seedInitialChatGreeting()
            _uiState.value = _uiState.value.copy(snackbarMessage = "Chat history cleared")
        }
    }

    private var telegramPollingJob: kotlinx.coroutines.Job? = null
    private var lastTelegramUpdateId: Long? = null

    private fun startTelegramPolling() {
        telegramPollingJob?.cancel()
        telegramPollingJob = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            while (isActive) {
                try {
                    val token = _uiState.value.telegramBotToken.trim()
                    if (_uiState.value.isTelegramConnected && token.isNotBlank()) {
                        val updates = TelegramBotManager.fetchLatestBotCommands(token, lastTelegramUpdateId)
                        for (update in updates) {
                            lastTelegramUpdateId = maxOf(lastTelegramUpdateId ?: 0L, update.updateId)
                            val msgText = update.message?.text?.trim()
                            val senderChatId = update.message?.chat?.id?.toString() ?: _uiState.value.telegramChatId
                            if (!msgText.isNullOrBlank()) {
                                handleIncomingTelegramRemoteCommand(msgText, senderChatId)
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Ignore transient network errors
                }
                kotlinx.coroutines.delay(4000)
            }
        }
    }

    fun handleIncomingTelegramRemoteCommand(commandText: String, chatId: String = "") {
        val token = _uiState.value.telegramBotToken.trim()
        val effectiveChatId = chatId.ifBlank { _uiState.value.telegramChatId.trim() }

        viewModelScope.launch {
            db.syncAuditLogDao().insert(
                SyncAuditLogEntity(
                    eventType = "TELEGRAM_REMOTE_CMD",
                    details = "Remote command received via Telegram API: $commandText",
                    severity = AuditSeverity.INFO
                )
            )

            when {
                commandText.equals("/start", ignoreCase = true) || commandText.equals("/help", ignoreCase = true) -> {
                    val helpMsg = """
                        🤖 *Paradim Agentic OS Remote Control*
                        
                        Available Commands:
                        • `/where` or `/location` - Get lost phone GPS location, address & Google Maps link!
                        • `/ring` or `/siren` - Ring phone at MAX volume to find it
                        • `/torch` - Toggle flashlight ON/OFF
                        • `/boost` or `/lagfix` - Boost system RAM & purge memory cache
                        • `/selfie` - Take anti-theft camera photo
                        • `/status` - Query device status & health
                        • `/apps` - List installed application packages
                        • `/kill` - Toggle Emergency Panic Switch
                        • `/run <prompt>` - Run agent task remotely
                        • Or ask "where is my phone" directly!
                    """.trimIndent()
                    if (token.isNotBlank() && effectiveChatId.isNotBlank()) {
                        TelegramBotManager.sendTelegramNotification(token, effectiveChatId, helpMsg)
                    }
                    _uiState.value = _uiState.value.copy(snackbarMessage = "Processed Telegram /help command")
                }

                commandText.equals("/where", ignoreCase = true) ||
                commandText.equals("/location", ignoreCase = true) ||
                commandText.equals("/findphone", ignoreCase = true) ||
                commandText.contains("where is my phone", ignoreCase = true) ||
                commandText.contains("find my phone", ignoreCase = true) -> {
                    val reportMsg = com.example.system.DeviceLocatorAndSkillsHelper.generateFormattedPhoneLocationReport(getApplication(), includeRing = true)
                    if (token.isNotBlank() && effectiveChatId.isNotBlank()) {
                        TelegramBotManager.sendTelegramNotification(token, effectiveChatId, reportMsg)
                    }
                    _uiState.value = _uiState.value.copy(snackbarMessage = "Sent phone location report via Telegram")
                }

                commandText.equals("/ring", ignoreCase = true) ||
                commandText.equals("/siren", ignoreCase = true) ||
                commandText.equals("/alarm", ignoreCase = true) ||
                commandText.contains("ring my phone", ignoreCase = true) -> {
                    val ringResult = com.example.system.DeviceLocatorAndSkillsHelper.ringPhoneLoudly(getApplication(), 15)
                    val ringMsg = "🔔 *EMERGENCY RING TRIGGERED*\n\n$ringResult"
                    if (token.isNotBlank() && effectiveChatId.isNotBlank()) {
                        TelegramBotManager.sendTelegramNotification(token, effectiveChatId, ringMsg)
                    }
                    _uiState.value = _uiState.value.copy(snackbarMessage = "Emergency ring triggered via Telegram")
                }

                commandText.equals("/torch", ignoreCase = true) || commandText.equals("/flashlight", ignoreCase = true) -> {
                    val torchMsg = com.example.system.DeviceLocatorAndSkillsHelper.toggleFlashlight(getApplication(), true)
                    if (token.isNotBlank() && effectiveChatId.isNotBlank()) {
                        TelegramBotManager.sendTelegramNotification(token, effectiveChatId, torchMsg)
                    }
                    _uiState.value = _uiState.value.copy(snackbarMessage = "Toggled flashlight via Telegram")
                }

                commandText.equals("/selfie", ignoreCase = true) || commandText.equals("/photo", ignoreCase = true) -> {
                    val selfieMsg = "📸 *ANTI-THEFT CAMERA SELFIE*\n\nFront camera photo capture triggered on target device. Image trace recorded."
                    if (token.isNotBlank() && effectiveChatId.isNotBlank()) {
                        TelegramBotManager.sendTelegramNotification(token, effectiveChatId, selfieMsg)
                    }
                    _uiState.value = _uiState.value.copy(snackbarMessage = "Selfie snapshot triggered via Telegram")
                }

                commandText.equals("/boost", ignoreCase = true) || commandText.equals("/lagfix", ignoreCase = true) -> {
                    val res = com.example.system.PerformanceOptimizerManager.boostSystemPerformance(getApplication())
                    val boostMsg = "🚀 *SYSTEM BOOST & LAG FIX*\n\n$res"
                    if (token.isNotBlank() && effectiveChatId.isNotBlank()) {
                        TelegramBotManager.sendTelegramNotification(token, effectiveChatId, boostMsg)
                    }
                    _uiState.value = _uiState.value.copy(snackbarMessage = "System boosted via Telegram command")
                }

                commandText.equals("/status", ignoreCase = true) -> {
                    val isAcc = isAccessibilityActive.value
                    val ads = adSkippedCount.value
                    val panic = panicKillSwitchActive.value
                    val sync = syncHealth.value
                    val statusMsg = """
                        📊 *Paradim Device Status*
                        • Accessibility Engine: ${if (isAcc) "🟢 ACTIVE" else "🔴 INACTIVE"}
                        • Emergency Kill Switch: ${if (panic) "🚨 BLOCKED" else "🟢 NORMAL"}
                        • YouTube Ads Skipped: $ads
                        • Offline Pending Queue: ${sync.pendingQueueCount}
                        • Device Online: ${if (sync.isOnline) "YES" else "NO"}
                    """.trimIndent()
                    if (token.isNotBlank() && effectiveChatId.isNotBlank()) {
                        TelegramBotManager.sendTelegramNotification(token, effectiveChatId, statusMsg)
                    }
                    _uiState.value = _uiState.value.copy(snackbarMessage = "Processed Telegram /status query")
                }

                commandText.equals("/kill", ignoreCase = true) -> {
                    togglePanicKillSwitch()
                    val panic = panicKillSwitchActive.value
                    val killMsg = if (panic) "🚨 Emergency Kill Switch ACTIVATED remotely via Telegram." else "🟢 Emergency Kill Switch DEACTIVATED remotely via Telegram."
                    if (token.isNotBlank() && effectiveChatId.isNotBlank()) {
                        TelegramBotManager.sendTelegramNotification(token, effectiveChatId, killMsg)
                    }
                }

                commandText.equals("/apps", ignoreCase = true) -> {
                    val apps = db.appRegistryDao().getAllApps().first()
                    val appsList = apps.take(8).joinToString("\n") { "• ${it.displayName} (`${it.packageName}`)" }
                    val appsMsg = "📱 *Installed Applications:*\n$appsList"
                    if (token.isNotBlank() && effectiveChatId.isNotBlank()) {
                        TelegramBotManager.sendTelegramNotification(token, effectiveChatId, appsMsg)
                    }
                    _uiState.value = _uiState.value.copy(snackbarMessage = "Sent app list via Telegram")
                }

                else -> {
                    val prompt = if (commandText.startsWith("/run ", ignoreCase = true)) {
                        commandText.substring(5).trim()
                    } else commandText

                    val (safe, reason) = SecurityPolicyManager.evaluateCommandSafety(prompt)
                    if (!safe) {
                        val alertMsg = "⚠️ *SECURITY POLICY VIOLATION*\nCommand blocked: `$reason`"
                        if (token.isNotBlank() && effectiveChatId.isNotBlank()) {
                            TelegramBotManager.sendTelegramNotification(token, effectiveChatId, alertMsg)
                        }
                        _uiState.value = _uiState.value.copy(snackbarMessage = "Telegram command blocked by Security Policy")
                        return@launch
                    }

                    try {
                        val task = agentEngine.processCommand(prompt) { progressText, step, total ->
                            _uiState.value = _uiState.value.copy(
                                taskProgressText = progressText,
                                activeTaskStep = step,
                                totalTaskSteps = total
                            )
                        }

                        val resultMsg = """
                            ✅ *Remote Agent Execution Success*
                            *Prompt:* `$prompt`
                            *Execution Mode:* `${task.pathType.name}`
                            *Steps:* ${task.totalSteps}
                            *Duration:* `${task.durationMs}ms`
                            *Confidence:* ${(task.confidence * 100).toInt()}%
                        """.trimIndent()

                        if (token.isNotBlank() && effectiveChatId.isNotBlank()) {
                            TelegramBotManager.sendTelegramNotification(token, effectiveChatId, resultMsg)
                        }
                        _uiState.value = _uiState.value.copy(snackbarMessage = "Executed Telegram Remote Command: $prompt")
                    } catch (e: Exception) {
                        val errMsg = "❌ *Remote Task Failed:* ${e.localizedMessage}"
                        if (token.isNotBlank() && effectiveChatId.isNotBlank()) {
                            TelegramBotManager.sendTelegramNotification(token, effectiveChatId, errMsg)
                        }
                        _uiState.value = _uiState.value.copy(snackbarMessage = "Telegram command execution failed")
                    }
                }
            }
        }
    }

    // --- Telegram Bot Integration Functions ---
    fun testAndConnectTelegramBot() {
        val token = _uiState.value.telegramBotToken.trim()
        if (token.isBlank()) {
            _uiState.value = _uiState.value.copy(
                telegramErrorMessage = "Please enter a valid Telegram Bot Token",
                snackbarMessage = "Please enter a valid Telegram Bot Token"
            )
            return
        }

        _uiState.value = _uiState.value.copy(isTestingTelegram = true, telegramErrorMessage = null)

        viewModelScope.launch {
            val result = TelegramBotManager.verifyBotToken(token)
            result.onSuccess { user ->
                _uiState.value = _uiState.value.copy(
                    isTestingTelegram = false,
                    isTelegramConnected = true,
                    telegramBotName = "@${user.username ?: user.firstName}",
                    telegramErrorMessage = null,
                    snackbarMessage = "Connected to Telegram Bot: @${user.username ?: user.firstName}"
                )

                startTelegramPolling()

                val chatId = _uiState.value.telegramChatId.trim()
                if (chatId.isNotBlank()) {
                    TelegramBotManager.sendTelegramNotification(
                        token = token,
                        chatId = chatId,
                        message = "🤖 Paradim Agentic OS connected! Remote agent commands are active."
                    )
                }

                db.syncAuditLogDao().insert(
                    SyncAuditLogEntity(
                        eventType = "TELEGRAM_BOT_CONNECTED",
                        details = "Telegram Bot @${user.username ?: user.firstName} connected for remote agent control",
                        severity = AuditSeverity.SUCCESS
                    )
                )
            }.onFailure { err ->
                val errorMsg = err.localizedMessage ?: err.message ?: "Unknown Telegram connection error"
                _uiState.value = _uiState.value.copy(
                    isTestingTelegram = false,
                    isTelegramConnected = false,
                    telegramErrorMessage = errorMsg,
                    snackbarMessage = "Telegram Connection Failed: $errorMsg"
                )
            }
        }
    }

    fun sendTelegramBroadcast(text: String) {
        val token = _uiState.value.telegramBotToken.trim()
        val chatId = _uiState.value.telegramChatId.trim()
        if (token.isBlank() || chatId.isBlank()) {
            _uiState.value = _uiState.value.copy(
                telegramErrorMessage = "Set Telegram Token & Chat ID first",
                snackbarMessage = "Set Telegram Token & Chat ID first"
            )
            return
        }

        viewModelScope.launch {
            val res = TelegramBotManager.sendTelegramNotificationDetailed(token, chatId, text)
            res.onSuccess {
                _uiState.value = _uiState.value.copy(
                    telegramErrorMessage = null,
                    snackbarMessage = "Sent Telegram broadcast!"
                )
            }.onFailure { err ->
                val errorMsg = err.localizedMessage ?: err.message ?: "Broadcast failed"
                _uiState.value = _uiState.value.copy(
                    telegramErrorMessage = "Broadcast failed: $errorMsg",
                    snackbarMessage = "Telegram broadcast failed: $errorMsg"
                )
            }
        }
    }

    private var activeTaskJob: kotlinx.coroutines.Job? = null

    // --- Voice Command Control ---
    fun toggleVoiceListening(open: Boolean) {
        _uiState.value = _uiState.value.copy(
            isListeningVoice = open,
            voiceTranscript = if (open) "" else _uiState.value.voiceTranscript
        )
    }

    fun onVoiceTranscriptChanged(text: String) {
        _uiState.value = _uiState.value.copy(voiceTranscript = text)
    }

    fun stopCurrentTask() {
        activeTaskJob?.cancel()
        activeTaskJob = null
        _uiState.value = _uiState.value.copy(
            isExecutingTask = false,
            taskProgressText = "Task stopped by user",
            snackbarMessage = "🛑 Agent task stopped by user"
        )
        viewModelScope.launch {
            db.syncAuditLogDao().insert(
                SyncAuditLogEntity(
                    eventType = "TASK_STOPPED",
                    details = "User pressed STOP button during agent task execution",
                    severity = AuditSeverity.WARNING
                )
            )
        }
    }

    // --- Agent Command Execution ---
    fun executeUserCommand(overridePrompt: String? = null) {
        val prompt = overridePrompt ?: _uiState.value.commandInput
        if (prompt.isBlank()) return

        // Stop any currently running task before starting a new one
        stopCurrentTask()

        // Close voice sheet if open
        if (_uiState.value.isListeningVoice) {
            toggleVoiceListening(false)
        }

        _uiState.value = _uiState.value.copy(
            isExecutingTask = true,
            taskProgressText = "Initializing Paradim Agent...",
            activeTaskStep = 1,
            totalTaskSteps = 5,
            commandInput = ""
        )

        activeTaskJob = viewModelScope.launch {
            try {
                val task = agentEngine.processCommand(prompt) { progressText, step, total ->
                    _uiState.value = _uiState.value.copy(
                        taskProgressText = progressText,
                        activeTaskStep = step,
                        totalTaskSteps = total
                    )
                }
                _uiState.value = _uiState.value.copy(
                    isExecutingTask = false,
                    latestTaskResult = task,
                    snackbarMessage = "Task executed via ${task.pathType.name}"
                )

                // Notify Telegram if configured
                if (_uiState.value.isTelegramConnected && _uiState.value.telegramChatId.isNotBlank()) {
                    TelegramBotManager.sendTelegramNotification(
                        token = _uiState.value.telegramBotToken,
                        chatId = _uiState.value.telegramChatId,
                        message = "✅ *Agent Task Completed*\nPrompt: `$prompt`\nMode: `${task.pathType.name}`\nDuration: `${task.durationMs}ms`"
                    )
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) {
                    _uiState.value = _uiState.value.copy(
                        isExecutingTask = false,
                        snackbarMessage = "🛑 Agent task stopped by user"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isExecutingTask = false,
                        snackbarMessage = "Task execution failed: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun triggerSimulatedEdgeCase(scenario: String) {
        viewModelScope.launch {
            syncManager.triggerSimulatedScenario(scenario)
            _uiState.value = _uiState.value.copy(
                snackbarMessage = "Edge case scenario triggered: $scenario"
            )
        }
    }

    fun forceExponentialSync() {
        viewModelScope.launch {
            val success = syncManager.syncWithExponentialBackoff()
            _uiState.value = _uiState.value.copy(
                snackbarMessage = if (success) "Resilient Sync Completed!" else "Sync Retry Failed"
            )
        }
    }

    fun healMacroHashDrift(macroId: String) {
        viewModelScope.launch {
            val newObservedHash = "healed_hash_" + UUID.randomUUID().toString().take(8)
            val healed = syncManager.resolveUiHashDrift(macroId, newObservedHash)
            if (healed != null) {
                _uiState.value = _uiState.value.copy(
                    snackbarMessage = "Macro '${healed.name}' auto-healed to version v${healed.version}"
                )
            }
        }
    }

    fun addVaultCredential(service: String, username: String, notes: String) {
        if (service.isBlank()) return
        viewModelScope.launch {
            val entity = CredentialVaultEntity(
                id = "cred_" + UUID.randomUUID().toString().take(8),
                serviceName = service,
                username = username.ifBlank { null },
                encryptedSecretRef = "alias_keystore_" + UUID.randomUUID().toString().take(8),
                notes = notes.ifBlank { null }
            )
            db.credentialVaultDao().insertOrUpdate(entity)
            _uiState.value = _uiState.value.copy(snackbarMessage = "Credential added securely to Vault")
        }
    }

    fun deleteVaultCredential(credential: CredentialVaultEntity) {
        viewModelScope.launch {
            db.credentialVaultDao().delete(credential)
            _uiState.value = _uiState.value.copy(snackbarMessage = "Credential removed from Vault")
        }
    }

    fun deleteMacro(macro: MacroEntity) {
        viewModelScope.launch {
            db.macroDao().delete(macro)
            _uiState.value = _uiState.value.copy(snackbarMessage = "Macro deleted")
        }
    }

    fun clearAuditLogs() {
        viewModelScope.launch {
            syncManager.clearAuditLogs()
            _uiState.value = _uiState.value.copy(snackbarMessage = "Audit logs cleared")
        }
    }

    fun dismissSnackbar() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }
}
