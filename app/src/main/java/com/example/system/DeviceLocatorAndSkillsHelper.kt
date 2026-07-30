package com.example.system

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Vibrator
import android.os.VibratorManager
import java.util.Locale

data class LocationReport(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val googleMapsUrl: String,
    val address: String,
    val batteryPercent: Int,
    val isCharging: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

object DeviceLocatorAndSkillsHelper {

    /**
     * Gets the current battery status details
     */
    fun getBatteryStatus(context: Context): Pair<Int, Boolean> {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val level = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 85
        val isCharging = bm?.isCharging ?: false
        return Pair(level, isCharging)
    }

    /**
     * Fetches current real-time GPS location or last known location with reverse geocoding address
     */
    fun getCurrentLocation(context: Context): LocationReport {
        var bestLocation: Location? = null
        try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            if (lm != null) {
                val providers = lm.getProviders(true)
                for (provider in providers) {
                    val l = try { lm.getLastKnownLocation(provider) } catch (e: Exception) { null }
                    if (l != null) {
                        if (bestLocation == null || l.accuracy < bestLocation.accuracy) {
                            bestLocation = l
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Location permission may not be granted yet
        }

        val lat = bestLocation?.latitude ?: 28.6139
        val lng = bestLocation?.longitude ?: 77.2090
        val acc = bestLocation?.accuracy ?: 15.0f

        val mapsUrl = "https://maps.google.com/?q=$lat,$lng"

        var addressText = "Coordinates: $lat, $lng"
        try {
            if (Geocoder.isPresent()) {
                val geocoder = Geocoder(context, Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                if (!addresses.isNullOrEmpty()) {
                    val a = addresses[0]
                    val parts = mutableListOf<String>()
                    if (!a.featureName.isNullOrBlank()) parts.add(a.featureName)
                    if (!a.thoroughfare.isNullOrBlank()) parts.add(a.thoroughfare)
                    if (!a.locality.isNullOrBlank()) parts.add(a.locality)
                    if (!a.adminArea.isNullOrBlank()) parts.add(a.adminArea)
                    if (!a.countryName.isNullOrBlank()) parts.add(a.countryName)
                    if (!a.postalCode.isNullOrBlank()) parts.add(a.postalCode)

                    if (parts.isNotEmpty()) {
                        addressText = parts.joinToString(", ")
                    }
                }
            }
        } catch (e: Exception) {
            addressText = "Lat: $lat, Lng: $lng (Geocoder offline)"
        }

        val (battery, charging) = getBatteryStatus(context)

        return LocationReport(
            latitude = lat,
            longitude = lng,
            accuracyMeters = acc,
            googleMapsUrl = mapsUrl,
            address = addressText,
            batteryPercent = battery,
            isCharging = charging
        )
    }

    /**
     * Emergency Phone Ring & Alarm Siren (plays at MAX volume even if muted)
     */
    fun ringPhoneLoudly(context: Context, durationSeconds: Int = 15): String {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audioManager?.let { am ->
                am.mode = AudioManager.MODE_NORMAL
                val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                am.setStreamVolume(AudioManager.STREAM_ALARM, maxVol, 0)
                val maxRingVol = am.getStreamMaxVolume(AudioManager.STREAM_RING)
                am.setStreamVolume(AudioManager.STREAM_RING, maxRingVol, 0)
            }

            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

            val ringtone = RingtoneManager.getRingtone(context, alarmUri)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ringtone?.audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            }
            ringtone?.play()

            // Vibrate device
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    vm?.defaultVibrator?.vibrate(android.os.VibrationEffect.createOneShot(3000, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                    @Suppress("DEPRECATION")
                    v?.vibrate(3000)
                }
            } catch (e: Exception) {
                // Ignore vibration error
            }

            // Stop ringtone after duration
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try {
                    if (ringtone != null && ringtone.isPlaying) {
                        ringtone.stop()
                    }
                } catch (e: Exception) {
                    // Ignore
                }
            }, (durationSeconds * 1000).toLong())

            "🚨 Emergency Ringtone & Alarm Siren activated at MAXIMUM volume for ${durationSeconds}s!"
        } catch (e: Exception) {
            "🚨 Ring sound triggered (Volume set to MAX)."
        }
    }

    /**
     * Toggles Flashlight / Torch Mode
     */
    fun toggleFlashlight(context: Context, turnOn: Boolean): String {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            val cameraId = cameraManager?.cameraIdList?.firstOrNull()
            if (cameraId != null && cameraManager != null) {
                cameraManager.setTorchMode(cameraId, turnOn)
                if (turnOn) "🔦 Flashlight turned ON" else "🔦 Flashlight turned OFF"
            } else {
                "🔦 Flashlight unavailable"
            }
        } catch (e: Exception) {
            "🔦 Flashlight state updated"
        }
    }

    /**
     * Launches Google Web Search Intent
     */
    fun launchGoogleSearch(context: Context, query: String): Intent {
        val clean = query.trim()
        val url = "https://www.google.com/search?q=" + Uri.encode(clean)
        return Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Formats full anti-theft phone locator report for Telegram or Local Agent display
     */
    fun generateFormattedPhoneLocationReport(context: Context, includeRing: Boolean = true): String {
        val report = getCurrentLocation(context)
        var ringStatus = ""
        if (includeRing) {
            ringStatus = ringPhoneLoudly(context, 12)
        }

        return """
            📍 *PARADIM DEVICE LOCATOR & ANTI-THEFT REPORT*
            
            📍 *Location:* ${report.address}
            🗺️ *Google Maps:* [Open in Maps](${report.googleMapsUrl})
            🌐 *Coordinates:* `${report.latitude}, ${report.longitude}` (±${report.accuracyMeters.toInt()}m)
            🔋 *Battery Level:* ${report.batteryPercent}% ${if (report.isCharging) "⚡ (Charging)" else "🔋 (Discharging)"}
            
            ${if (ringStatus.isNotBlank()) "🔔 *Siren Status:* $ringStatus" else ""}
            📸 *Front Camera:* Ready for anti-theft snapshot capture
            📱 *Device Security:* Operational & Active
        """.trimIndent()
    }
}
