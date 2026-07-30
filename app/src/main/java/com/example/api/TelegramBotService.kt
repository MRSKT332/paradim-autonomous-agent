package com.example.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class TelegramSendMessageRequest(
    @field:Json(name = "chat_id") val chatId: String,
    @field:Json(name = "text") val text: String,
    @field:Json(name = "parse_mode") val parseMode: String? = null
)

@JsonClass(generateAdapter = true)
data class TelegramUser(
    @field:Json(name = "id") val id: Long,
    @field:Json(name = "is_bot") val isBot: Boolean,
    @field:Json(name = "first_name") val firstName: String,
    @field:Json(name = "username") val username: String? = null
)

@JsonClass(generateAdapter = true)
data class TelegramGetMeResponse(
    @field:Json(name = "ok") val ok: Boolean,
    @field:Json(name = "result") val result: TelegramUser? = null
)

@JsonClass(generateAdapter = true)
data class TelegramSendMessageResponse(
    @field:Json(name = "ok") val ok: Boolean,
    @field:Json(name = "description") val description: String? = null
)

@JsonClass(generateAdapter = true)
data class TelegramUpdateMessage(
    @field:Json(name = "message_id") val messageId: Long,
    @field:Json(name = "text") val text: String? = null,
    @field:Json(name = "chat") val chat: TelegramChat? = null
)

@JsonClass(generateAdapter = true)
data class TelegramChat(
    @field:Json(name = "id") val id: Long,
    @field:Json(name = "first_name") val firstName: String? = null
)

@JsonClass(generateAdapter = true)
data class TelegramUpdate(
    @field:Json(name = "update_id") val updateId: Long,
    @field:Json(name = "message") val message: TelegramUpdateMessage? = null
)

@JsonClass(generateAdapter = true)
data class TelegramGetUpdatesResponse(
    @field:Json(name = "ok") val ok: Boolean,
    @field:Json(name = "result") val result: List<TelegramUpdate>? = null
)

interface TelegramApi {
    @GET("bot{token}/getMe")
    suspend fun getMe(@Path("token") token: String): TelegramGetMeResponse

    @POST("bot{token}/sendMessage")
    suspend fun sendMessage(
        @Path("token") token: String,
        @Body request: TelegramSendMessageRequest
    ): TelegramSendMessageResponse

    @GET("bot{token}/getUpdates")
    suspend fun getUpdates(
        @Path("token") token: String,
        @Query("offset") offset: Long? = null,
        @Query("timeout") timeout: Int = 0
    ): TelegramGetUpdatesResponse
}

object TelegramBotManager {
    private const val BASE_URL = "https://api.telegram.org/"

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val retrofitService: TelegramApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(TelegramApi::class.java)
    }

    fun cleanToken(rawToken: String): String {
        var t = rawToken.trim().removeSurrounding("\"").removeSurrounding("'")
        if (t.startsWith("bot", ignoreCase = true) && t.length > 3 && t[3].isDigit()) {
            t = t.substring(3).trim()
        }
        return t
    }

    suspend fun verifyBotToken(token: String): Result<TelegramUser> = withContext(Dispatchers.IO) {
        val clean = cleanToken(token)
        if (clean.isBlank()) {
            return@withContext Result.failure(Exception("Telegram Bot Token is empty"))
        }
        try {
            val response = retrofitService.getMe(clean)
            if (response.ok && response.result != null) {
                Result.success(response.result)
            } else {
                Result.failure(Exception("Telegram API Error: invalid response"))
            }
        } catch (e: retrofit2.HttpException) {
            val errBody = try { e.response()?.errorBody()?.string() } catch (ex: Exception) { null }
            val detail = if (!errBody.isNullOrBlank()) errBody else e.message()
            Result.failure(Exception("Telegram HTTP ${e.code()}: $detail"))
        } catch (e: Exception) {
            Result.failure(Exception("Telegram Connection Error: ${e.localizedMessage ?: e.message}"))
        }
    }

    suspend fun sendTelegramNotification(token: String, chatId: String, message: String): Boolean = withContext(Dispatchers.IO) {
        val cleanTok = cleanToken(token)
        val cleanChat = chatId.trim().removeSurrounding("\"").removeSurrounding("'")
        if (cleanTok.isBlank() || cleanChat.isBlank()) return@withContext false

        try {
            val req = TelegramSendMessageRequest(chatId = cleanChat, text = message)
            val res = retrofitService.sendMessage(cleanTok, req)
            res.ok
        } catch (e: Exception) {
            false
        }
    }

    suspend fun fetchLatestBotCommands(token: String, lastUpdateId: Long?): List<TelegramUpdate> = withContext(Dispatchers.IO) {
        val cleanTok = cleanToken(token)
        if (cleanTok.isBlank()) return@withContext emptyList()

        try {
            val offset = if (lastUpdateId != null) lastUpdateId + 1 else null
            val res = retrofitService.getUpdates(token = cleanTok, offset = offset)
            if (res.ok) res.result ?: emptyList() else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
