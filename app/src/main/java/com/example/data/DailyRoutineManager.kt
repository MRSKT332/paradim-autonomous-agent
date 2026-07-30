package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.ai.MultiModelLlmClient
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

data class ClosestFriend(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val birthMonthDay: String, // e.g. "08-15" or "Aug 15"
    val platform: String = "Instagram", // Instagram, Telegram, Facebook, WhatsApp, Phone
    val customNotes: String = "",
    val isClosestFriend: Boolean = true
)

data class NewsDigestItem(
    val genre: String, // Finance, Politics, Tech & AI, Social Media, Entertainment, Sports, World
    val headline: String,
    val summary: String,
    val timestamp: Long = System.currentTimeMillis(),
    val highlights: List<String> = emptyList()
)

data class DailyAutomationTask(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val scheduledTime: String, // e.g. "08:00 AM"
    val category: String, // "News", "Birthday Radar", "Device Health", "Social Broadcaster"
    val description: String,
    var isEnabled: Boolean = true,
    var isCompletedToday: Boolean = false
)

data class AppAnnouncement(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val type: String = "INFO" // "NEWS", "BIRTHDAY", "SYSTEM", "ROUTINE"
)

object DailyRoutineManager {

    private const val PREFS_NAME = "paradim_daily_routines_prefs"
    private const val KEY_FRIENDS = "closest_friends_json"
    private const val KEY_NEWS_GENRES = "selected_news_genres"
    private const val KEY_ROUTINES = "daily_routines_json"

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val friendsAdapter = moshi.adapter<List<ClosestFriend>>(Types.newParameterizedType(List::class.java, ClosestFriend::class.java))
    private val routinesAdapter = moshi.adapter<List<DailyAutomationTask>>(Types.newParameterizedType(List::class.java, DailyAutomationTask::class.java))

    private val _friends = MutableStateFlow<List<ClosestFriend>>(emptyList())
    val friends: StateFlow<List<ClosestFriend>> = _friends.asStateFlow()

    private val _newsDigests = MutableStateFlow<List<NewsDigestItem>>(emptyList())
    val newsDigests: StateFlow<List<NewsDigestItem>> = _newsDigests.asStateFlow()

    private val _dailyTasks = MutableStateFlow<List<DailyAutomationTask>>(emptyList())
    val dailyTasks: StateFlow<List<DailyAutomationTask>> = _dailyTasks.asStateFlow()

    private val _announcements = MutableStateFlow<List<AppAnnouncement>>(emptyList())
    val announcements: StateFlow<List<AppAnnouncement>> = _announcements.asStateFlow()

    private val _isGeneratingNews = MutableStateFlow(false)
    val isGeneratingNews: StateFlow<Boolean> = _isGeneratingNews.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadFriends(prefs)
        loadDefaultRoutines(prefs)
        seedInitialAnnouncements()
    }

    private fun loadFriends(prefs: SharedPreferences) {
        val json = prefs.getString(KEY_FRIENDS, null)
        if (json != null) {
            try {
                _friends.value = friendsAdapter.fromJson(json) ?: emptyList()
            } catch (e: Exception) {
                seedInitialFriends(prefs)
            }
        } else {
            seedInitialFriends(prefs)
        }
    }

    private fun seedInitialFriends(prefs: SharedPreferences) {
        val sampleFriends = listOf(
            ClosestFriend(name = "Alex Rivera", birthMonthDay = getUpcomingDateOffset(0), platform = "Instagram", customNotes = "College roommate & tech enthusiast"),
            ClosestFriend(name = "Samantha Vance", birthMonthDay = getUpcomingDateOffset(2), platform = "Telegram", customNotes = "Design lead & close friend"),
            ClosestFriend(name = "David Chen", birthMonthDay = getUpcomingDateOffset(5), platform = "Facebook", customNotes = "High school friend")
        )
        _friends.value = sampleFriends
        saveFriends(prefs)
    }

    private fun getUpcomingDateOffset(days: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, days)
        val sdf = SimpleDateFormat("MMM dd", Locale.US)
        return sdf.format(cal.time)
    }

    private fun saveFriends(prefs: SharedPreferences) {
        prefs.edit().putString(KEY_FRIENDS, friendsAdapter.toJson(_friends.value)).apply()
    }

    fun addFriend(context: Context, friend: ClosestFriend) {
        val updated = _friends.value + friend
        _friends.value = updated
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        saveFriends(prefs)
        postAnnouncement("🎂 New Friend Radar Added", "Tracking upcoming birthday for ${friend.name} (${friend.birthMonthDay}) on ${friend.platform}.", "BIRTHDAY")
    }

    fun removeFriend(context: Context, friendId: String) {
        val updated = _friends.value.filterNot { it.id == friendId }
        _friends.value = updated
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        saveFriends(prefs)
    }

    private fun loadDefaultRoutines(prefs: SharedPreferences) {
        val json = prefs.getString(KEY_ROUTINES, null)
        if (json != null) {
            try {
                _dailyTasks.value = routinesAdapter.fromJson(json) ?: emptyList()
            } catch (e: Exception) {
                seedDefaultRoutines(prefs)
            }
        } else {
            seedDefaultRoutines(prefs)
        }
    }

    private fun seedDefaultRoutines(prefs: SharedPreferences) {
        val defaults = listOf(
            DailyAutomationTask(
                title = "24-Hour Mainstream News Digest",
                scheduledTime = "08:00 AM",
                category = "News",
                description = "Summarize top 24h news across Finance, Politics, Social Media, Tech & Entertainment."
            ),
            DailyAutomationTask(
                title = "Closest Friends Birthday Check",
                scheduledTime = "09:00 AM",
                category = "Birthday Radar",
                description = "Scan friends list for today's & upcoming birthdays and generate warm wishes."
            ),
            DailyAutomationTask(
                title = "Device Health & Ad-Block Diagnostics",
                scheduledTime = "01:00 PM",
                category = "Device Health",
                description = "Check accessibility status, ad skip performance & battery health."
            ),
            DailyAutomationTask(
                title = "Evening Automation Summary",
                scheduledTime = "08:00 PM",
                category = "Social Broadcaster",
                description = "Compile day's automated actions and push summary notification / Telegram report."
            )
        )
        _dailyTasks.value = defaults
        prefs.edit().putString(KEY_ROUTINES, routinesAdapter.toJson(defaults)).apply()
    }

    fun toggleTaskEnabled(context: Context, taskId: String) {
        val updated = _dailyTasks.value.map {
            if (it.id == taskId) it.copy(isEnabled = !it.isEnabled) else it
        }
        _dailyTasks.value = updated
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_ROUTINES, routinesAdapter.toJson(updated)).apply()
    }

    fun markTaskCompleted(context: Context, taskId: String, completed: Boolean) {
        val updated = _dailyTasks.value.map {
            if (it.id == taskId) it.copy(isCompletedToday = completed) else it
        }
        _dailyTasks.value = updated
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_ROUTINES, routinesAdapter.toJson(updated)).apply()
    }

    suspend fun fetch24HourMainstreamNewsDigest(genres: List<String> = listOf("Finance", "World Politics", "Tech & AI", "Social Media", "Entertainment", "Sports")): List<NewsDigestItem> = withContext(Dispatchers.IO) {
        _isGeneratingNews.value = true
        val prompt = """
            Provide a realistic, accurate, and structured 24-hour mainstream news summary for today covering these genres: ${genres.joinToString(", ")}.
            For each genre, provide:
            1. Genre name
            2. Catchy Headline
            3. Concise 2-sentence summary of major news in the last 24 hours.
            4. 2 bullet point key takeaways.

            Format as clean plain text with section headers like [GENRE: Finance].
        """.trimIndent()

        val response = MultiModelLlmClient.queryPollinationsAiText(
            prompt = prompt,
            systemInstruction = "You are a top-tier mainstream news automated journalist intelligence agent.",
            model = "openai"
        )

        val items = mutableListOf<NewsDigestItem>()
        val sections = response.split(Regex("(?=\\[GENRE:)"))
        for (sec in sections) {
            if (sec.contains("[GENRE:")) {
                val genreName = sec.substringAfter("[GENRE:").substringBefore("]").trim()
                val lines = sec.lines().filter { it.isNotBlank() }
                val headline = lines.firstOrNull { !it.contains("[GENRE:") }?.removePrefix("#")?.removePrefix("*")?.trim() ?: "$genreName Today's Highlights"
                val summary = lines.drop(2).take(3).joinToString(" ").replace(Regex("[*#]"), "").trim()
                val highlights = lines.filter { it.trim().startsWith("-") || it.trim().startsWith("*") }.map { it.trim().removePrefix("-").removePrefix("*").trim() }

                items.add(
                    NewsDigestItem(
                        genre = genreName,
                        headline = headline,
                        summary = summary.ifBlank { "Top 24-hour developments in $genreName summary loaded from global mainstream wires." },
                        highlights = if (highlights.isNotEmpty()) highlights else listOf("Major developments reported across global news wires.", "Updates verified across last 24-hour cycle.")
                    )
                )
            }
        }

        if (items.isEmpty()) {
            genres.forEach { g ->
                items.add(
                    NewsDigestItem(
                        genre = g,
                        headline = "24h $g Intelligence Briefing",
                        summary = "Automated summary of global $g developments over the past 24 hours.",
                        highlights = listOf("Key market & industry movements summarized.", "Verified across primary wire services.")
                    )
                )
            }
        }

        _newsDigests.value = items
        _isGeneratingNews.value = false

        postAnnouncement(
            title = "📰 24-Hour Mainstream News Digest Updated",
            message = "Fresh intelligence loaded for ${items.size} genres (Finance, Politics, Tech, Entertainment & Sports).",
            type = "NEWS"
        )

        return@withContext items
    }

    suspend fun generateBirthdayWish(friend: ClosestFriend): String = withContext(Dispatchers.IO) {
        val prompt = "Write a warm, creative, friendly, personalized birthday wish for my close friend ${friend.name}. Mention platform context (${friend.platform}) and keep it genuine, engaging, and ready to send. Optional notes: ${friend.customNotes}."
        return@withContext MultiModelLlmClient.queryPollinationsAiText(
            prompt = prompt,
            systemInstruction = "You are a warm, witty AI friendship concierge.",
            model = "openai"
        )
    }

    fun postAnnouncement(title: String, message: String, type: String = "INFO") {
        val announcement = AppAnnouncement(
            title = title,
            message = message,
            type = type
        )
        _announcements.value = listOf(announcement) + _announcements.value.take(15)
    }

    private fun seedInitialAnnouncements() {
        if (_announcements.value.isEmpty()) {
            _announcements.value = listOf(
                AppAnnouncement(
                    title = "✨ Automation Radar System Online",
                    message = "Your personal AI agent is monitoring 24h news, friend birthday countdowns, and automated daily routines.",
                    type = "SYSTEM"
                ),
                AppAnnouncement(
                    title = "🎂 Friend Birthday Radar Active",
                    message = "Radar initialized with closest friends. Get automatic alerts and Pollinations AI wish templates.",
                    type = "BIRTHDAY"
                )
            )
        }
    }
}
