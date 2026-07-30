package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutinesAndRadarScreen(
    onBroadcastTelegram: (String) -> Unit,
    onDelegateAgentTask: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    val friends by DailyRoutineManager.friends.collectAsState()
    val newsDigests by DailyRoutineManager.newsDigests.collectAsState()
    val dailyTasks by DailyRoutineManager.dailyTasks.collectAsState()
    val announcements by DailyRoutineManager.announcements.collectAsState()
    val isGeneratingNews by DailyRoutineManager.isGeneratingNews.collectAsState()

    var selectedNewsGenre by remember { mutableStateOf("All") }
    var showAddFriendDialog by remember { mutableStateOf(false) }
    var activeGeneratedWish by remember { mutableStateOf<Pair<String, String>?>(null) }
    var isGeneratingWishForFriend by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        DailyRoutineManager.init(context)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. ANNOUNCEMENTS CORNER
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberSurfaceDark),
                border = BorderStroke(1.dp, SproutEmerald.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().testTag("announcements_corner_card")
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
                                imageVector = Icons.Default.Campaign,
                                contentDescription = null,
                                tint = SproutEmerald,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Announcements Corner",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Text(
                                text = "Live updates on news, birthdays & automated daily routines",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = SproutEmerald.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "${announcements.size} Live",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = SproutEmerald,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (announcements.isEmpty()) {
                        Text(
                            text = "No active announcements.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    } else {
                        val latest = announcements.first()
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = CyberDarkBg,
                            border = BorderStroke(1.dp, CyberBorderDark),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                val (icon, tint) = when (latest.type) {
                                    "NEWS" -> Icons.Default.Newspaper to CyanPrimary
                                    "BIRTHDAY" -> Icons.Default.Cake to SproutAmber
                                    "ROUTINE" -> Icons.Default.Schedule to SproutPrimary
                                    else -> Icons.Default.Info to SproutEmerald
                                }
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = tint,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = latest.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = latest.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. 24-HOUR MAINSTREAM NEWS DIGEST
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberSurfaceDark),
                border = BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().testTag("news_digest_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(CyanPrimary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Public,
                                    contentDescription = null,
                                    tint = CyanPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "24h Mainstream News Digest",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Finance, Politics, Social Media, Tech, Sports & Entertainment",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            scope.launch {
                                val genres = listOf("Finance", "World Politics", "Tech & AI", "Social Media", "Entertainment", "Sports")
                                DailyRoutineManager.fetch24HourMainstreamNewsDigest(genres)
                                Toast.makeText(context, "📰 24h News Digest Updated via Pollinations AI!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isGeneratingNews,
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isGeneratingNews) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Fetching 24h Global News...", color = Color.Black, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Fetch 24h Mainstream Digest (Pollinations AI)", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val genresList = listOf("All", "Finance", "World Politics", "Tech & AI", "Social Media", "Entertainment", "Sports")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(genresList) { genre ->
                            FilterChip(
                                selected = selectedNewsGenre == genre,
                                onClick = { selectedNewsGenre = genre },
                                label = { Text(genre) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyanPrimary,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val filteredNews = remember(selectedNewsGenre, newsDigests) {
                        if (selectedNewsGenre == "All") newsDigests else newsDigests.filter { it.genre.contains(selectedNewsGenre, ignoreCase = true) }
                    }

                    if (filteredNews.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = CyberDarkBg,
                            border = BorderStroke(1.dp, CyberBorderDark),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Tap button above to generate fresh 24h mainstream news digest across all genres.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            filteredNews.forEach { news ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = CyberDarkBg,
                                    border = BorderStroke(1.dp, CyberBorderDark),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = CyanPrimary.copy(alpha = 0.2f)
                                            ) {
                                                Text(
                                                    text = news.genre.uppercase(),
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = CyanPrimary,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }

                                            Row {
                                                IconButton(
                                                    onClick = {
                                                        val text = "📰 [24h News Digest - ${news.genre}]\n${news.headline}\n\n${news.summary}\n\nKey Points:\n" + news.highlights.joinToString("\n") { "• $it" }
                                                        clipboardManager.setText(AnnotatedString(text))
                                                        Toast.makeText(context, "Copied news digest!", Toast.LENGTH_SHORT).show()
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = TextMuted, modifier = Modifier.size(16.dp))
                                                }

                                                IconButton(
                                                    onClick = {
                                                        val text = "📰 [24h News Digest - ${news.genre}]\n${news.headline}\n\n${news.summary}"
                                                        onBroadcastTelegram(text)
                                                        Toast.makeText(context, "Broadcasting news to Telegram...", Toast.LENGTH_SHORT).show()
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(imageVector = Icons.Default.Send, contentDescription = "Telegram", tint = CyanPrimary, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = news.headline,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = TextPrimary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = news.summary,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )

                                        if (news.highlights.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            news.highlights.forEach { hl ->
                                                Row(verticalAlignment = Alignment.Top) {
                                                    Text("• ", color = CyanPrimary, style = MaterialTheme.typography.bodySmall)
                                                    Text(hl, color = TextMuted, style = MaterialTheme.typography.bodySmall)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. CLOSEST FRIENDS BIRTHDAY RADAR
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberSurfaceDark),
                border = BorderStroke(1.dp, SproutAmber.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().testTag("birthday_radar_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(SproutAmber.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Cake,
                                    contentDescription = null,
                                    tint = SproutAmber,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Closest Friends Birthday Radar",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Instagram, Telegram, Facebook & Phone Wishes",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                            }
                        }

                        IconButton(
                            onClick = { showAddFriendDialog = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(imageVector = Icons.Default.AddCircle, contentDescription = "Add Friend", tint = SproutAmber)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (friends.isEmpty()) {
                        Text(
                            text = "No closest friends added yet. Tap + to track birthdays.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            friends.forEach { friend ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = CyberDarkBg,
                                    border = BorderStroke(1.dp, CyberBorderDark),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .clip(CircleShape)
                                                        .background(SproutAmber.copy(alpha = 0.15f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = friend.name.take(1).uppercase(),
                                                        fontWeight = FontWeight.Bold,
                                                        color = SproutAmber
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(
                                                        text = friend.name,
                                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                        color = TextPrimary
                                                    )
                                                    Text(
                                                        text = "${friend.birthMonthDay} • ${friend.platform}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = TextMuted
                                                    )
                                                }
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Button(
                                                    onClick = {
                                                        scope.launch {
                                                            isGeneratingWishForFriend = friend.id
                                                            val wish = DailyRoutineManager.generateBirthdayWish(friend)
                                                            activeGeneratedWish = Pair(friend.name, wish)
                                                            isGeneratingWishForFriend = null
                                                        }
                                                    },
                                                    enabled = isGeneratingWishForFriend != friend.id,
                                                    colors = ButtonDefaults.buttonColors(containerColor = SproutAmber),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    if (isGeneratingWishForFriend == friend.id) {
                                                        CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.Black, strokeWidth = 2.dp)
                                                    } else {
                                                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("AI Wish", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }

                                                IconButton(
                                                    onClick = { DailyRoutineManager.removeFriend(context, friend.id) },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Delete", tint = TextMuted, modifier = Modifier.size(14.dp))
                                                }
                                            }
                                        }

                                        if (friend.customNotes.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Note: ${friend.customNotes}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    activeGeneratedWish?.let { (friendName, wishText) ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SproutAmber.copy(alpha = 0.1f),
                            border = BorderStroke(1.dp, SproutAmber.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "🎂 Generated Wish for $friendName",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = SproutAmber
                                    )
                                    IconButton(
                                        onClick = { activeGeneratedWish = null },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Close, contentDescription = "Dismiss", tint = SproutAmber)
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = wishText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(wishText))
                                            Toast.makeText(context, "Copied wish to clipboard!", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = SproutAmber),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Copy Wish", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = {
                                            onBroadcastTelegram("🎉 Birthday Wish for $friendName:\n\n$wishText")
                                            Toast.makeText(context, "Pushed birthday wish to Telegram!", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Send, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Telegram", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. DAILY AUTOMATION TASKS & ROUTINES
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberSurfaceDark),
                border = BorderStroke(1.dp, SproutEmerald.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().testTag("daily_routines_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(SproutEmerald.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = SproutEmerald,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Daily Routine Automation Engine",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Scheduled tasks, briefing alerts & device diagnostics",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    dailyTasks.forEach { task ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = CyberDarkBg,
                            border = BorderStroke(1.dp, if (task.isCompletedToday) SproutEmerald.copy(alpha = 0.5f) else CyberBorderDark),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = SproutEmerald.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = task.scheduledTime,
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = SproutEmerald,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = task.title,
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = TextPrimary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = task.description,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Switch(
                                        checked = task.isEnabled,
                                        onCheckedChange = { DailyRoutineManager.toggleTaskEnabled(context, task.id) },
                                        colors = SwitchDefaults.colors(checkedThumbColor = SproutEmerald)
                                    )

                                    Button(
                                        onClick = {
                                            onDelegateAgentTask("Execute daily routine: ${task.title}")
                                            DailyRoutineManager.markTaskCompleted(context, task.id, true)
                                            Toast.makeText(context, "Executing ${task.title}...", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = SproutEmerald.copy(alpha = 0.2f)),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text("Run Now", color = SproutEmerald, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. INSTALLED PHONE APPS AUTOMATION HUB
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberSurfaceDark),
                border = BorderStroke(1.dp, SproutPrimary.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().testTag("phone_apps_hub_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SproutPrimary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Apps,
                                contentDescription = null,
                                tint = SproutPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Installed Phone Apps Launcher & Task Shortcuts",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Text(
                                text = "Direct agent delegation to perform mobile tasks",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val quickApps = listOf(
                        "Instagram" to Icons.Default.CameraAlt,
                        "Telegram" to Icons.Default.Send,
                        "Facebook" to Icons.Default.Group,
                        "WhatsApp" to Icons.Default.Chat,
                        "Chrome" to Icons.Default.Language,
                        "Settings" to Icons.Default.Settings,
                        "YouTube" to Icons.Default.PlayCircle,
                        "Maps" to Icons.Default.Map,
                        "Photos" to Icons.Default.PhotoLibrary,
                        "Gmail" to Icons.Default.Email
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        quickApps.chunked(2).forEach { rowApps ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowApps.forEach { (appName, appIcon) ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = CyberDarkBg,
                                        border = BorderStroke(1.dp, CyberBorderDark),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                onDelegateAgentTask("Open $appName and check latest activity or complete user tasks.")
                                                Toast.makeText(context, "Delegated $appName task to agent!", Toast.LENGTH_SHORT).show()
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(imageVector = appIcon, contentDescription = null, tint = SproutPrimary, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(appName, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                                                Text("Tap to Automate", style = MaterialTheme.typography.labelSmall, color = TextMuted, fontSize = 9.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddFriendDialog) {
        var friendName by remember { mutableStateOf("") }
        var birthDate by remember { mutableStateOf("") }
        var platform by remember { mutableStateOf("Instagram") }
        var notes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddFriendDialog = false },
            title = { Text("Add Closest Friend to Radar", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = friendName,
                        onValueChange = { friendName = it },
                        label = { Text("Friend Name") },
                        placeholder = { Text("e.g. Alex Rivera") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = birthDate,
                        onValueChange = { birthDate = it },
                        label = { Text("Birthday (Month & Day)") },
                        placeholder = { Text("e.g. Aug 15 or 08-15") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Primary Wish Platform:", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("Instagram", "Telegram", "Facebook", "WhatsApp").forEach { p ->
                            FilterChip(
                                selected = platform == p,
                                onClick = { platform = p },
                                label = { Text(p, fontSize = 10.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes / Relationship") },
                        placeholder = { Text("e.g. Best friend from college") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (friendName.isNotBlank() && birthDate.isNotBlank()) {
                            DailyRoutineManager.addFriend(
                                context,
                                ClosestFriend(
                                    name = friendName,
                                    birthMonthDay = birthDate,
                                    platform = platform,
                                    customNotes = notes
                                )
                            )
                            showAddFriendDialog = false
                        }
                    },
                    enabled = friendName.isNotBlank() && birthDate.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = SproutAmber)
                ) {
                    Text("Add Friend", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddFriendDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            },
            containerColor = CyberSurfaceDark
        )
    }
}
