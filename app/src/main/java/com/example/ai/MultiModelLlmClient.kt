package com.example.ai

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import android.util.Log
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
        var k = key.replace("\r", "").replace("\n", "").trim().removeSurrounding("\"").removeSurrounding("'")
        if (k.startsWith("Bearer ", ignoreCase = true)) {
            k = k.substring(7).trim()
        }
        return k
    }

    fun maskKey(key: String): String {
        val clean = cleanApiKey(key)
        if (clean.isBlank()) return "<NONE>"
        return if (clean.length > 4) "***" + clean.takeLast(4) else "***"
    }

    suspend fun queryPollinationsAiText(
        prompt: String,
        systemInstruction: String? = null,
        model: String = "openai",
        apiKey: String = ""
    ): String = withContext(Dispatchers.IO) {
        try {
            val cleanKey = cleanApiKey(apiKey)
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
            val reqBuilder = Request.Builder()
                .url("https://text.pollinations.ai/openai/")
                .post(requestBody)

            if (cleanKey.isNotBlank()) {
                reqBuilder.header("Authorization", "Bearer $cleanKey")
                Log.d("MultiModelLlmClient", "Pollinations request using Authorization token: ${maskKey(cleanKey)}")
            } else {
                Log.d("MultiModelLlmClient", "Pollinations request sent without API key")
            }

            val request = reqBuilder.build()

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
                            Log.w("MultiModelLlmClient", "Pollinations JSON parse fallback: ${e.message}")
                        }
                    }
                    return@withContext bodyStr
                } else {
                    Log.e("MultiModelLlmClient", "Pollinations AI Error: HTTP ${response.code} ${response.message} body=$bodyStr")
                    val parsedErrorMsg = try {
                        if (bodyStr.trim().startsWith("{")) {
                            val json = JSONObject(bodyStr)
                            val errObj = json.optJSONObject("error")
                            if (errObj != null) {
                                val code = errObj.optString("code", "")
                                val msg = errObj.optString("message", "")
                                if (code.isNotBlank() || msg.isNotBlank()) "Error[$code]: $msg" else bodyStr
                            } else {
                                val msg = json.optString("message", "")
                                val detail = json.optString("detail", "")
                                val errStr = json.optString("error", "")
                                listOf(msg, detail, errStr).firstOrNull { it.isNotBlank() } ?: bodyStr
                            }
                        } else bodyStr
                    } catch (e: Exception) {
                        bodyStr.ifBlank { response.message }
                    }
                    return@withContext "Pollinations AI Error: HTTP ${response.code} - $parsedErrorMsg"
                }
            }
        } catch (e: Exception) {
            Log.e("MultiModelLlmClient", "Pollinations AI Exception (${e.javaClass.simpleName}): ${e.localizedMessage}", e)
            return@withContext "Pollinations AI Error: ${e.javaClass.simpleName}: ${e.localizedMessage ?: e.message}\n${Log.getStackTraceString(e)}"
        }
    }

    suspend fun queryLlm(
        config: LlmConfiguration,
        prompt: String,
        systemInstruction: String? = null
    ): String = withContext(Dispatchers.IO) {
        val cleanKey = cleanApiKey(config.apiKey)
        val maskedKeyStr = maskKey(cleanKey)
        Log.d("MultiModelLlmClient", "queryLlm VALIDATION: provider=${config.provider.displayName}, baseUrl='${config.baseUrl}', model='${config.modelName}', key='$maskedKeyStr'")

        if (config.provider == LlmProvider.POLLINATIONS_AI) {
            return@withContext queryPollinationsAiText(prompt, systemInstruction, config.modelName, config.apiKey)
        }

        if (config.provider == LlmProvider.GEMINI) {
            if (cleanKey.isBlank()) {
                val buildConfigKey = try { com.example.BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }
                if (buildConfigKey.isBlank() || buildConfigKey == "MY_GEMINI_API_KEY") {
                    Log.d("MultiModelLlmClient", "No Gemini API key provided, falling back to Pollinations AI")
                    return@withContext queryPollinationsAiText(prompt, systemInstruction, "openai", config.apiKey)
                }
            }
            return@withContext GeminiClient.queryGemini(config, prompt, systemInstruction)
        }

        if (cleanKey.isBlank()) {
            // Auto fallback to Pollinations AI for free public access when no API key is provided
            Log.d("MultiModelLlmClient", "No API key provided for ${config.provider.displayName}, falling back to Pollinations AI")
            return@withContext queryPollinationsAiText(prompt, systemInstruction, "openai", config.apiKey)
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

            Log.d("MultiModelLlmClient", "Sending OpenAI request to $endpointUrl model=${req.model}")
            val res = api.createChatCompletion(endpointUrl, authHeader, req)
            val replyText = res.choices?.firstOrNull()?.message?.content
            replyText ?: "No response from ${config.provider.displayName}"
        } catch (e: retrofit2.HttpException) {
            val errorBodyStr = try { e.response()?.errorBody()?.string() } catch (ex: Exception) { null }
            Log.e("MultiModelLlmClient", "queryLlm HttpException code=${e.code()} msg=${e.message()} body=$errorBodyStr", e)
            val detail = if (!errorBodyStr.isNullOrBlank()) errorBodyStr else e.message()
            "API_ERROR [${config.provider.name} HTTP ${e.code()}]: $detail"
        } catch (e: Exception) {
            Log.e("MultiModelLlmClient", "queryLlm Exception: ${e.localizedMessage}", e)
            "API_ERROR [${config.provider.name} ${e.javaClass.simpleName}]: ${e.localizedMessage ?: e.message}\n${Log.getStackTraceString(e)}"
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
