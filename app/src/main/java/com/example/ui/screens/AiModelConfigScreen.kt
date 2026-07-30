package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale

@Composable
fun AiModelConfigScreen(
    adSkippedCount: Int,
    onConfigUpdated: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentConfig by remember { mutableStateOf(LlmConfigManager.getConfig(context)) }

    var selectedProvider by remember { mutableStateOf(currentConfig.provider) }
    var apiKeyInput by remember { mutableStateOf(currentConfig.apiKey) }
    var baseUrlInput by remember { mutableStateOf(currentConfig.baseUrl) }
    var modelNameInput by remember { mutableStateOf(currentConfig.modelName) }

    var isTestingConnection by remember { mutableStateOf(false) }
    var testResultText by remember { mutableStateOf<String?>(null) }
    var showTermuxGuide by remember { mutableStateOf(false) }

    fun applyProviderPreset(p: LlmProvider) {
        selectedProvider = p
        baseUrlInput = p.defaultBaseUrl
        modelNameInput = p.defaultModel
    }

    fun saveSettings() {
        val cleanedKey = apiKeyInput.trim().removeSurrounding("\"").removeSurrounding("'")
        val cleanedBase = baseUrlInput.trim().removeSurrounding("\"").removeSurrounding("'")
        val cleanedModel = modelNameInput.trim().removeSurrounding("\"").removeSurrounding("'")

        val newConfig = LlmConfiguration(
            provider = selectedProvider,
            apiKey = cleanedKey,
            baseUrl = cleanedBase,
            modelName = cleanedModel
        )
        LlmConfigManager.saveConfig(context, newConfig)
        currentConfig = newConfig
        onConfigUpdated()
    }

    LaunchedEffect(selectedProvider, apiKeyInput, baseUrlInput, modelNameInput) {
        saveSettings()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(CyanPrimary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = "AI Models",
                        tint = CyanBright,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "AI Model Engine & Local Termux",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        text = "Configure Groq, DeepSeek, OpenAI, Ollama Local or Gemini",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }
        }

        // Active Ad Skipper Counter Banner
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberSurfaceDark),
                border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = null,
                            tint = EmeraldSuccess,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Auto Ad-Skipper Engine",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Text(
                                text = "Intercepts YouTube & media ads automatically",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = EmeraldSuccess.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "$adSkippedCount Ads Skipped",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = EmeraldSuccess,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // Provider Selector Cards
        item {
            Text(
                text = "Select Primary Intelligence Provider",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LlmProvider.values().forEach { provider ->
                    val isSelected = selectedProvider == provider
                    Surface(
                        onClick = { applyProviderPreset(provider) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) CyanPrimary.copy(alpha = 0.2f) else CyberSurfaceDark,
                        border = BorderStroke(1.dp, if (isSelected) CyanBright else CyberBorderDark),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("provider_select_${provider.name.lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { applyProviderPreset(provider) },
                                    colors = RadioButtonDefaults.colors(selectedColor = CyanBright)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = provider.displayName,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = provider.defaultModel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            if (provider == LlmProvider.POLLINATIONS_AI) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = EmeraldSuccess.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "100% FREE / NO KEY",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = EmeraldSuccess,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontSize = 9.sp
                                    )
                                }
                            } else if (provider == LlmProvider.OLLAMA_LOCAL) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = AmberWarning.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "OFFLINE / TERMUX",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = AmberWarning,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Configuration Inputs
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberSurfaceDark),
                border = BorderStroke(1.dp, CyberBorderDark),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Endpoint & Key Settings (${selectedProvider.displayName})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (selectedProvider != LlmProvider.OLLAMA_LOCAL) {
                        OutlinedTextField(
                            value = apiKeyInput,
                            onValueChange = { apiKeyInput = it },
                            label = { Text(if (selectedProvider == LlmProvider.POLLINATIONS_AI) "Inference API Key (Not Required)" else "Inference API Key") },
                            placeholder = { Text(if (selectedProvider == LlmProvider.POLLINATIONS_AI) "Leave empty (100% Free Public Endpoint)" else "e.g. nvapi-... or gsk_... or sk-...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("llm_api_key_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    OutlinedTextField(
                        value = baseUrlInput,
                        onValueChange = { baseUrlInput = it },
                        label = { Text("Base REST URL") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("llm_base_url_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = modelNameInput,
                        onValueChange = { modelNameInput = it },
                        label = { Text("Model ID (e.g. nvidia/nemotron-4-340b-instruct)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("llm_model_name_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    if (selectedProvider.presetModels.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Preset Models (${selectedProvider.displayName}):",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            selectedProvider.presetModels.chunked(2).forEach { rowModels ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    rowModels.forEach { preset ->
                                        FilterChip(
                                            selected = modelNameInput == preset,
                                            onClick = { modelNameInput = preset },
                                            label = {
                                                Text(
                                                    text = preset.substringAfterLast("/"),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontSize = 11.sp
                                                )
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                saveSettings()
                                isTestingConnection = true
                                testResultText = null
                                // Async Ping Test
                                val testConfig = LlmConfiguration(selectedProvider, apiKeyInput, baseUrlInput, modelNameInput)
                                kotlinx.coroutines.MainScope().launch {
                                    val res = MultiModelLlmClient.queryLlm(testConfig, "Respond with 'OK' if working.")
                                    testResultText = res
                                    isTestingConnection = false
                                }
                            },
                            enabled = !isTestingConnection,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("save_and_test_llm_btn"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                        ) {
                            if (isTestingConnection) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Testing...")
                            } else {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save & Test Connection")
                            }
                        }
                    }

                    if (testResultText != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = CyberSurfaceVariantDark,
                            border = BorderStroke(1.dp, if (testResultText!!.contains("API_ERROR")) AmberWarning else EmeraldSuccess)
                        ) {
                            Text(
                                text = "Response: $testResultText",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = if (testResultText!!.contains("API_ERROR")) AmberWarning else EmeraldSuccess,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }
        }

        // Termux Local AI Guide Expandable Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberSurfaceDark),
                border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showTermuxGuide = !showTermuxGuide }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                contentDescription = null,
                                tint = AmberWarning,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Offline Termux Local LLM Setup Guide",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                        }

                        Icon(
                            imageVector = if (showTermuxGuide) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = TextMuted
                        )
                    }

                    AnimatedVisibility(visible = showTermuxGuide) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            Text(
                                text = "Run a complete local AI model directly on your Android phone using Termux & Ollama without internet!",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.Black,
                                border = BorderStroke(1.dp, CyberBorderDark)
                            ) {
                                Text(
                                    text = """
                                        1. Open Termux on your Android phone
                                        2. Run: pkg update && pkg install ollama
                                        3. Start Ollama server: OLLAMA_HOST=0.0.0.0:11434 ollama serve
                                        4. In a 2nd Termux tab, run model: ollama run llama3
                                        5. In Paradim app, select 'Ollama / Termux Local' with Base URL: http://localhost:11434/v1/
                                    """.trimIndent(),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = AmberWarning,
                                    modifier = Modifier.padding(10.dp),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Pollinations AI Free Creative Studio & Image Generator
        item {
            var imagePrompt by remember { mutableStateOf("") }
            var selectedImageModel by remember { mutableStateOf("flux") }
            var generatedImageUrl by remember { mutableStateOf<String?>(null) }
            var isGeneratingImage by remember { mutableStateOf(false) }

            Card(
                colors = CardDefaults.cardColors(containerColor = CyberSurfaceDark),
                border = BorderStroke(1.dp, SproutEmerald.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SproutEmerald.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = SproutEmerald,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Pollinations AI Free Image Studio",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Text(
                                text = "Instant high-resolution AI art generation — 100% Free",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = imagePrompt,
                        onValueChange = { imagePrompt = it },
                        label = { Text("Visual Prompt") },
                        placeholder = { Text("e.g. Cyberpunk android agent in neon city, hyperrealistic 8k") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SproutEmerald,
                            unfocusedBorderColor = CyberBorderDark
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Model Architecture:",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("flux", "turbo", "flux-realism", "cyberpunk").forEach { m ->
                            FilterChip(
                                selected = selectedImageModel == m,
                                onClick = { selectedImageModel = m },
                                label = { Text(m.uppercase()) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SproutEmerald,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            if (imagePrompt.isNotBlank()) {
                                isGeneratingImage = true
                                val url = MultiModelLlmClient.generatePollinationsImageUrl(
                                    prompt = imagePrompt,
                                    width = 1024,
                                    height = 1024,
                                    model = selectedImageModel
                                )
                                generatedImageUrl = url
                                isGeneratingImage = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = imagePrompt.isNotBlank() && !isGeneratingImage,
                        colors = ButtonDefaults.buttonColors(containerColor = SproutEmerald),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generate Image Free", color = Color.Black, fontWeight = FontWeight.Bold)
                    }

                    if (generatedImageUrl != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, SproutEmerald.copy(alpha = 0.5f)),
                            color = Color.Black,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                AsyncImage(
                                    model = generatedImageUrl,
                                    contentDescription = "Pollinations Generated Image",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(260.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Generated with Pollinations.ai ($selectedImageModel)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SproutEmerald
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
