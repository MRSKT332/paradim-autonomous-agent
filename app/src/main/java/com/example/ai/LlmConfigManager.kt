package com.example.ai

import android.content.Context
import android.content.SharedPreferences

enum class LlmProvider(
    val displayName: String,
    val defaultBaseUrl: String,
    val defaultModel: String,
    val presetModels: List<String> = emptyList()
) {
    POLLINATIONS_AI(
        "Pollinations AI (100% Free - No Key)",
        "https://text.pollinations.ai/openai/",
        "openai",
        listOf("openai", "mistral", "qwen", "llama", "deepseek")
    ),
    GEMINI(
        "Google Gemini API",
        "https://generativelanguage.googleapis.com/",
        "gemini-1.5-flash",
        listOf("gemini-1.5-flash", "gemini-1.5-pro", "gemini-2.0-flash-exp")
    ),
    NVIDIA_NIM(
        "NVIDIA NIM Cloud (OSS & 120B+ Models)",
        "https://integrate.api.nvidia.com/v1/",
        "nvidia/llama-3.1-nemotron-70b-instruct",
        listOf(
            "nvidia/llama-3.1-nemotron-70b-instruct",
            "nvidia/nemotron-4-340b-instruct",
            "meta/llama-3.1-405b-instruct",
            "meta/llama-3.1-70b-instruct",
            "mistralai/mixtral-8x22b-instruct",
            "qwen/qwen2.5-72b-instruct",
            "deepseek-ai/deepseek-r1"
        )
    ),
    GROQ(
        "Groq Inference (Ultra Fast)",
        "https://api.groq.com/openai/v1/",
        "llama-3.3-70b-versatile",
        listOf("llama-3.3-70b-versatile", "mixtral-8x7b-32768", "deepseek-r1-distill-llama-70b")
    ),
    OPENAI(
        "OpenAI GPT-4o / Custom API",
        "https://api.openai.com/v1/",
        "gpt-4o-mini",
        listOf("gpt-4o-mini", "gpt-4o", "o3-mini")
    ),
    DEEPSEEK(
        "DeepSeek AI Direct API",
        "https://api.deepseek.com/v1/",
        "deepseek-chat",
        listOf("deepseek-chat", "deepseek-reasoner")
    ),
    OLLAMA_LOCAL(
        "Ollama / Termux Local LLM",
        "http://localhost:11434/v1/",
        "llama3",
        listOf("llama3", "mistral", "qwen2.5", "phi3")
    ),
    HUGGINGFACE(
        "HuggingFace Inference API",
        "https://api-inference.huggingface.co/v1/",
        "meta-llama/Llama-3.2-3B-Instruct",
        listOf("meta-llama/Llama-3.2-3B-Instruct", "mistralai/Mistral-7B-Instruct-v0.3")
    )
}

data class LlmConfiguration(
    val provider: LlmProvider = LlmProvider.GEMINI,
    val apiKey: String = "",
    val baseUrl: String = LlmProvider.GEMINI.defaultBaseUrl,
    val modelName: String = LlmProvider.GEMINI.defaultModel
)

object LlmConfigManager {
    private const val PREFS_NAME = "paradim_llm_settings"
    private const val KEY_PROVIDER = "provider"
    private const val KEY_API_KEY = "api_key"
    private const val KEY_BASE_URL = "base_url"
    private const val KEY_MODEL_NAME = "model_name"

    private var cachedConfig: LlmConfiguration? = null

    fun getConfig(context: Context): LlmConfiguration {
        if (cachedConfig != null) return cachedConfig!!
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val providerStr = prefs.getString(KEY_PROVIDER, LlmProvider.GEMINI.name) ?: LlmProvider.GEMINI.name
        val provider = try { LlmProvider.valueOf(providerStr) } catch (e: Exception) { LlmProvider.GEMINI }
        val apiKey = prefs.getString(KEY_API_KEY, "") ?: ""
        val baseUrl = prefs.getString(KEY_BASE_URL, provider.defaultBaseUrl) ?: provider.defaultBaseUrl
        val modelName = prefs.getString(KEY_MODEL_NAME, provider.defaultModel) ?: provider.defaultModel

        cachedConfig = LlmConfiguration(provider, apiKey, baseUrl, modelName)
        return cachedConfig!!
    }

    fun saveConfig(context: Context, config: LlmConfiguration) {
        cachedConfig = config
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_PROVIDER, config.provider.name)
            .putString(KEY_API_KEY, config.apiKey)
            .putString(KEY_BASE_URL, config.baseUrl)
            .putString(KEY_MODEL_NAME, config.modelName)
            .apply()
    }
}
