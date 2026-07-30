package com.example.ai

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
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

    suspend fun queryLlm(
        config: LlmConfiguration,
        prompt: String,
        systemInstruction: String? = null
    ): String = withContext(Dispatchers.IO) {
        if (config.provider == LlmProvider.GEMINI) {
            return@withContext GeminiClient.queryGemini(config, prompt, systemInstruction)
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
}
