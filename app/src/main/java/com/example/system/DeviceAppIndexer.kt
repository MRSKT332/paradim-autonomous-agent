package com.example.system

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.util.DisplayMetrics

data class InstalledAppInfo(
    val appName: String,
    val packageName: String,
    val cornerLocation: String, // e.g. "Top-Left (15%, 20%)", "Top-Right", "Center"
    val xPercent: Float,
    val yPercent: Float,
    val isSystemApp: Boolean = false,
    val isMediaApp: Boolean = false
)

object DeviceAppIndexer {

    // Pre-registered package constants for reliable instant intents
    const val PKG_YOUTUBE = "com.google.android.youtube"
    const val PKG_SPOTIFY = "com.spotify.music"
    const val PKG_MOVIEBOX = "com.moviebox.app"
    const val PKG_WHATSAPP = "com.whatsapp"
    const val PKG_TELEGRAM = "org.telegram.messenger"
    const val PKG_TERMUX = "com.termux"

    fun getInstalledApps(context: Context): List<InstalledAppInfo> {
        val pm = context.packageManager
        val appsList = mutableListOf<InstalledAppInfo>()

        val installedPackages = try {
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
        } catch (e: Exception) {
            emptyList<ApplicationInfo>()
        }

        var index = 0
        val total = installedPackages.size.coerceAtLeast(1)

        for (app in installedPackages) {
            val name = pm.getApplicationLabel(app).toString()
            val pkg = app.packageName

            // Compute deterministic corner grid location (3x3 grid)
            val row = (index / 3) % 3
            val col = index % 3
            val xPerc = 0.2f + col * 0.3f
            val yPerc = 0.15f + row * 0.25f

            val cornerText = when {
                row == 0 && col == 0 -> "Top-Left Corner"
                row == 0 && col == 2 -> "Top-Right Corner"
                row == 2 && col == 0 -> "Bottom-Left Corner"
                row == 2 && col == 2 -> "Bottom-Right Corner"
                row == 1 && col == 1 -> "Center Screen"
                row == 0 -> "Top Center"
                row == 2 -> "Bottom Center"
                col == 0 -> "Middle-Left"
                else -> "Middle-Right"
            }

            val isMedia = pkg.contains("youtube") || pkg.contains("spotify") || pkg.contains("movie") || pkg.contains("music") || pkg.contains("player")

            appsList.add(
                InstalledAppInfo(
                    appName = name,
                    packageName = pkg,
                    cornerLocation = cornerText,
                    xPercent = xPerc,
                    yPercent = yPerc,
                    isSystemApp = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                    isMediaApp = isMedia
                )
            )
            index++
        }

        // Always include default core target apps if not listed
        val knownDefaults = listOf(
            InstalledAppInfo("YouTube", PKG_YOUTUBE, "Top-Left Corner (15%, 15%)", 0.15f, 0.15f, isMediaApp = true),
            InstalledAppInfo("Spotify", PKG_SPOTIFY, "Top-Right Corner (85%, 15%)", 0.85f, 0.15f, isMediaApp = true),
            InstalledAppInfo("Movie Box", PKG_MOVIEBOX, "Center Screen (50%, 50%)", 0.50f, 0.50f, isMediaApp = true),
            InstalledAppInfo("Termux Local AI", PKG_TERMUX, "Bottom-Left Corner (15%, 85%)", 0.15f, 0.85f, isSystemApp = true)
        )

        val existingPkgs = appsList.map { it.packageName }.toSet()
        for (k in knownDefaults) {
            if (!existingPkgs.contains(k.packageName)) {
                appsList.add(0, k)
            }
        }

        return appsList
    }

    /**
     * Creates direct deep link or search intent for instant media playback (YouTube, Spotify, Movie Box)
     * eliminating slow UI search loops.
     */
    fun createMediaPlayIntent(context: Context, query: String, targetApp: String): Intent? {
        val cleanQuery = query.trim()
        val encodedQuery = Uri.encode(cleanQuery)

        return when {
            targetApp.contains("youtube", ignoreCase = true) -> {
                // Direct YouTube Search or Watch Intent
                Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=$encodedQuery")).apply {
                    setPackage(PKG_YOUTUBE)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            targetApp.contains("spotify", ignoreCase = true) -> {
                // Direct Spotify Media Search Intent
                Intent("android.media.action.MEDIA_PLAY_FROM_SEARCH").apply {
                    putExtra("query", cleanQuery)
                    setPackage(PKG_SPOTIFY)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            targetApp.contains("movie", ignoreCase = true) -> {
                // Movie Box app launch / search intent
                val pm = context.packageManager
                val launchIntent = pm.getLaunchIntentForPackage(PKG_MOVIEBOX)
                launchIntent?.apply {
                    putExtra("query", cleanQuery)
                    putExtra("search_term", cleanQuery)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            else -> {
                // Generic Media Search Intent
                Intent("android.media.action.MEDIA_PLAY_FROM_SEARCH").apply {
                    putExtra("query", cleanQuery)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
        }
    }
}
