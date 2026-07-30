package com.example.ai

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class ChatMessage(
    @field:Json(name = "role") val role: String = "user",
    @field:Json(name = "content") val content: String? = ""
)

@JsonClass(generateAdapter = true)
data class OpenAiChatRequest(
    @field:Json(name = "model") val model: String,
    @field:Json(name = "messages") val messages: List<ChatMessage>,
    @field:Json(name = "temperature") val temperature: Double = 0.5,
    @field:Json(name = "max_tokens") val maxTokens: Int = 1024,
    @field:Json(name = "top_p") val topP: Double = 1.0
)

@JsonClass(generateAdapter = true)
data class ChatChoice(
    @field:Json(name = "message") val message: ChatMessage? = null
)

@JsonClass(generateAdapter = true)
data class OpenAiChatResponse(
    @field:Json(name = "choices") val choices: List<ChatChoice>? = null
)

interface OpenAiCompatibleApi {
    @POST
    suspend fun createChatCompletion(
        @Url url: String,
        @Header("Authorization") authorization: String?,
        @Body request: OpenAiChatRequest
    ): OpenAiChatResponse
}

object MultiModelLlmClient {

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val api: OpenAiCompatibleApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.openai.com/") // Placeholder base URL, @Url overrides
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OpenAiCompatibleApi::class.java)
    }

    fun cleanApiKey(key: String): String {
        var k = key.trim().removeSurrounding("\"").removeSurrounding("'")
        if (k.startsWith("Bearer ", ignoreCase = true)) {
            k = k.substring(7).trim()
        }
        return k
    }

    suspend fun queryPollinationsAiText(
        prompt: String,
        systemInstruction: String? = null,
        model: String = "openai"
    ): String = withContext(Dispatchers.IO) {
        try {
            val jsonBody = JSONObject().apply {
                val msgs = JSONArray()
                if (!systemInstruction.isNullOrBlank()) {
                    msgs.put(JSONObject().apply {
                        put("role", "system")
                        put("content", systemInstruction)
                    })
                }
                msgs.put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
                put("messages", msgs)
                put("model", model.ifBlank { "openai" })
            }

            val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url("https://text.pollinations.ai/openai/")
                .post(requestBody)
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (response.isSuccessful && bodyStr.isNotBlank()) {
                    if (bodyStr.trim().startsWith("{")) {
                        try {
                            val json = JSONObject(bodyStr)
                            val choices = json.optJSONArray("choices")
                            if (choices != null && choices.length() > 0) {
                                val msgObj = choices.getJSONObject(0).optJSONObject("message")
                                val text = msgObj?.optString("content")
                                if (!text.isNullOrBlank()) return@withContext text
                            }
                        } catch (e: Exception) {
                            // Fallback to raw response
                        }
                    }
                    return@withContext bodyStr
                } else {
                    return@withContext "Pollinations AI Error: HTTP ${response.code} ${response.message}"
                }
            }
        } catch (e: Exception) {
            return@withContext "Pollinations AI Error: ${e.localizedMessage ?: e.message}"
        }
    }

    suspend fun queryLlm(
        config: LlmConfiguration,
        prompt: String,
        systemInstruction: String? = null
    ): String = withContext(Dispatchers.IO) {
        if (config.provider == LlmProvider.POLLINATIONS_AI) {
            return@withContext queryPollinationsAiText(prompt, systemInstruction, config.modelName)
        }

        if (config.provider == LlmProvider.GEMINI) {
            if (config.apiKey.isBlank()) {
                // Auto fallback to Pollinations AI when Gemini API key is not configured
                return@withContext queryPollinationsAiText(prompt, systemInstruction, "openai")
            }
            return@withContext GeminiClient.queryGemini(config, prompt, systemInstruction)
        }

        if (config.apiKey.isBlank()) {
            // Auto fallback to Pollinations AI for free public access when no API key is provided
            return@withContext queryPollinationsAiText(prompt, systemInstruction, "openai")
        }

        try {
            val cleanBase = config.baseUrl.trim().removeSurrounding("\"").removeSurrounding("'")
            val endpointUrl = when {
                cleanBase.endsWith("/chat/completions") -> cleanBase
                cleanBase.endsWith("/chat/completions/") -> cleanBase.removeSuffix("/")
                cleanBase.endsWith("/") -> "${cleanBase}chat/completions"
                else -> "${cleanBase}/chat/completions"
            }

            val cleanKey = cleanApiKey(config.apiKey)
            val authHeader = if (cleanKey.isNotBlank()) "Bearer $cleanKey" else null

            val messages = mutableListOf<ChatMessage>()
            if (!systemInstruction.isNullOrBlank()) {
                messages.add(ChatMessage(role = "system", content = systemInstruction))
            }
            messages.add(ChatMessage(role = "user", content = prompt))

            val req = OpenAiChatRequest(
                model = config.modelName.trim().ifBlank { config.provider.defaultModel },
                messages = messages
            )

            val res = api.createChatCompletion(endpointUrl, authHeader, req)
            val replyText = res.choices?.firstOrNull()?.message?.content
            replyText ?: "No response from ${config.provider.displayName}"
        } catch (e: retrofit2.HttpException) {
            val errorBodyStr = try { e.response()?.errorBody()?.string() } catch (ex: Exception) { null }
            val detail = if (!errorBodyStr.isNullOrBlank()) errorBodyStr else e.message()
            "API_ERROR [${config.provider.name} HTTP ${e.code()}]: $detail"
        } catch (e: Exception) {
            "API_ERROR [${config.provider.name}]: ${e.localizedMessage ?: e.message}"
        }
    }

    fun generatePollinationsImageUrl(
        prompt: String,
        width: Int = 1024,
        height: Int = 1024,
        model: String = "flux"
    ): String {
        val encodedPrompt = java.net.URLEncoder.encode(prompt, "UTF-8")
        val seed = (100000..999999).random()
        return "https://image.pollinations.ai/prompt/$encodedPrompt?width=$width&height=$height&seed=$seed&model=$model&nologo=true"
    }
}
