package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.example.api.TelegramBotManager
import com.example.system.DeviceLocatorAndSkillsHelper
import com.example.system.LocationReport
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class GpsTrackingService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var botToken: String = ""
    private var chatId: String = ""
    private var trackingIntervalMs: Long = 30000L // Default 30s interval

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    processAndPublishLocation(location)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP_TRACKING) {
            stopTracking()
            return START_NOT_STICKY
        }

        // Read preferences
        val prefs = getSharedPreferences("paradim_prefs", Context.MODE_PRIVATE)
        botToken = intent?.getStringExtra(EXTRA_BOT_TOKEN) ?: prefs.getString("telegram_token", "") ?: ""
        chatId = intent?.getStringExtra(EXTRA_CHAT_ID) ?: prefs.getString("telegram_chat_id", "") ?: ""
        trackingIntervalMs = intent?.getLongExtra(EXTRA_INTERVAL_MS, 30000L) ?: 30000L

        startForeground(NOTIFICATION_ID, createNotification("Initializing GPS Tracking..."))
        _isTrackingActive.value = true

        startLocationUpdates()
        return START_STICKY
    }

    private fun startLocationUpdates() {
        try {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, trackingIntervalMs)
                .setMinUpdateIntervalMillis(trackingIntervalMs / 2)
                .setWaitForAccurateLocation(false)
                .build()

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            // Location permission missing
            _isTrackingActive.value = false
            stopSelf()
        } catch (e: Exception) {
            // Fallback for older API versions
            try {
                @Suppress("DEPRECATION")
                val legacyRequest = LocationRequest.create().apply {
                    priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                    interval = trackingIntervalMs
                    fastestInterval = trackingIntervalMs / 2
                }
                fusedLocationClient.requestLocationUpdates(
                    legacyRequest,
                    locationCallback,
                    Looper.getMainLooper()
                )
            } catch (ex: Exception) {
                _isTrackingActive.value = false
            }
        }
    }

    private fun processAndPublishLocation(location: Location) {
        serviceScope.launch {
            val lat = location.latitude
            val lng = location.longitude
            val speedKmh = location.speed * 3.6f // m/s to km/h
            val accuracy = location.accuracy
            val altitude = if (location.hasAltitude()) "${location.altitude.toInt()}m" else "N/A"

            var addressText = "Lat: $lat, Lng: $lng"
            try {
                if (Geocoder.isPresent()) {
                    val geocoder = Geocoder(this@GpsTrackingService, Locale.getDefault())
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(lat, lng, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val a = addresses[0]
                        val parts = mutableListOf<String>()
                        if (!a.featureName.isNullOrBlank()) parts.add(a.featureName)
                        if (!a.thoroughfare.isNullOrBlank()) parts.add(a.thoroughfare)
                        if (!a.locality.isNullOrBlank()) parts.add(a.locality)
                        if (!a.adminArea.isNullOrBlank()) parts.add(a.adminArea)
                        if (parts.isNotEmpty()) addressText = parts.joinToString(", ")
                    }
                }
            } catch (e: Exception) {
                // Ignore geocoder exception
            }

            val (batteryLevel, isCharging) = DeviceLocatorAndSkillsHelper.getBatteryStatus(this@GpsTrackingService)
            val mapsUrl = "https://maps.google.com/?q=$lat,$lng"

            val report = LocationReport(
                latitude = lat,
                longitude = lng,
                accuracyMeters = accuracy,
                googleMapsUrl = mapsUrl,
                address = addressText,
                batteryPercent = batteryLevel,
                isCharging = isCharging
            )

            _lastReportedLocation.value = report

            // Update foreground notification status text
            val notifManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notifManager.notify(NOTIFICATION_ID, createNotification("Live Tracking: $addressText (${speedKmh.toInt()} km/h)"))

            // Send to Telegram Bot endpoint if configured
            if (botToken.isNotBlank() && chatId.isNotBlank()) {
                val statusText = if (speedKmh > 2f) "🚗 Moving (${speedKmh.toInt()} km/h)" else "🅿️ Stationary"
                val telegramPayload = """
                    📡 *LIVE GPS TRACKER UPDATE*
                    
                    📍 *Address:* $addressText
                    🌐 *Coordinates:* `$lat, $lng` (±${accuracy.toInt()}m)
                    🗺️ *Map Link:* [Open Google Maps]($mapsUrl)
                    ⚡ *Status:* $statusText | Alt: $altitude
                    🔋 *Battery:* $batteryLevel% ${if (isCharging) "⚡ (Charging)" else "🔋 (Discharging)"}
                    ⏰ *Timestamp:* ${java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(java.util.Date())}
                """.trimIndent()

                TelegramBotManager.sendTelegramNotification(botToken, chatId, telegramPayload)
            }
        }
    }

    private fun stopTracking() {
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        } catch (e: Exception) {
            // Ignore
        }
        _isTrackingActive.value = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        } catch (e: Exception) {
            // Ignore
        }
        _isTrackingActive.value = false
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "GPS Device Location Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background GPS coordinates and status monitor"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(contentText: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("📍 Paradim Anti-Theft GPS Tracker")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "gps_tracking_channel"
        const val NOTIFICATION_ID = 9001

        const val ACTION_START_TRACKING = "com.example.service.START_TRACKING"
        const val ACTION_STOP_TRACKING = "com.example.service.STOP_TRACKING"

        const val EXTRA_BOT_TOKEN = "extra_bot_token"
        const val EXTRA_CHAT_ID = "extra_chat_id"
        const val EXTRA_INTERVAL_MS = "extra_interval_ms"

        private val _isTrackingActive = MutableStateFlow(false)
        val isTrackingActive: StateFlow<Boolean> = _isTrackingActive.asStateFlow()

        private val _lastReportedLocation = MutableStateFlow<LocationReport?>(null)
        val lastReportedLocation: StateFlow<LocationReport?> = _lastReportedLocation.asStateFlow()

        fun startTracking(context: Context, botToken: String = "", chatId: String = "", intervalMs: Long = 30000L) {
            val intent = Intent(context, GpsTrackingService::class.java).apply {
                action = ACTION_START_TRACKING
                putExtra(EXTRA_BOT_TOKEN, botToken)
                putExtra(EXTRA_CHAT_ID, chatId)
                putExtra(EXTRA_INTERVAL_MS, intervalMs)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopTracking(context: Context) {
            val intent = Intent(context, GpsTrackingService::class.java).apply {
                action = ACTION_STOP_TRACKING
            }
            context.startService(intent)
        }
    }
}
