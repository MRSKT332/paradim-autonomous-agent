package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ParadimAccessibilityService : AccessibilityService() {

    companion object {
        private val _isServiceActive = MutableStateFlow(false)
        val isServiceActive: StateFlow<Boolean> = _isServiceActive.asStateFlow()

        private val _latestCapturedNodeText = MutableStateFlow<String?>(null)
        val latestCapturedNodeText: StateFlow<String?> = _latestCapturedNodeText.asStateFlow()

        private val _adSkippedCount = MutableStateFlow(0)
        val adSkippedCount: StateFlow<Int> = _adSkippedCount.asStateFlow()

        var instance: ParadimAccessibilityService? = null
            private set
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main)

    private var lastAdCheckTimestamp: Long = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isServiceActive.value = true

        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_CLICKED or
                    AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            notificationTimeout = 300
        }
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val pkgName = event.packageName?.toString()?.lowercase() ?: ""
        // Never process accessibility events on our own app or non-media apps to avoid UI lag
        if (pkgName.isEmpty() || pkgName.contains("com.example") || pkgName.contains("com.aistudio") || pkgName.contains("android.launcher") || pkgName.contains("systemui")) {
            return
        }

        // Only inspect for video/media ads on YouTube, Spotify, and music apps
        if (!pkgName.contains("youtube") && !pkgName.contains("spotify") && !pkgName.contains("music") && !pkgName.contains("vlc")) {
            return
        }

        val textList = event.text
        if (textList.isNotEmpty()) {
            _latestCapturedNodeText.value = textList.joinToString(" ")
        }

        // Throttle ad checks to avoid main-thread scrolling lockups
        val now = System.currentTimeMillis()
        if (now - lastAdCheckTimestamp < 1000L && event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }
        lastAdCheckTimestamp = now

        // Active Auto Ad-Skipper for YouTube, Spotify & Media Apps
        val rootNode = rootInActiveWindow ?: return
        checkAndSkipAds(rootNode)
    }

    private fun checkAndSkipAds(rootNode: AccessibilityNodeInfo) {
        val keywords = listOf("Skip Ad", "Skip Ads", "Skip")
        for (kw in keywords) {
            val nodes = rootNode.findAccessibilityNodeInfosByText(kw) ?: continue
            for (node in nodes) {
                val target = if (node.isClickable) node else if (node.parent?.isClickable == true) node.parent else null
                if (target != null) {
                    val clicked = target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    if (clicked) {
                        _adSkippedCount.value = _adSkippedCount.value + 1
                        return
                    }
                }
            }
        }

        val adViewIds = listOf("skip_ad_button", "ad_skip_button")
        for (vid in adViewIds) {
            val nodes = rootNode.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/$vid") ?: continue
            for (node in nodes) {
                if (node.isClickable && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    _adSkippedCount.value = _adSkippedCount.value + 1
                    return
                }
            }
        }
    }

    override fun onInterrupt() {
        _isServiceActive.value = false
    }

    override fun onDestroy() {
        super.onDestroy()
        _isServiceActive.value = false
        instance = null
    }

    fun performClickById(viewId: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        val nodes = rootNode.findAccessibilityNodeInfosByViewId(viewId)
        if (nodes.isNullOrEmpty()) return false
        for (node in nodes) {
            if (node.isClickable) {
                return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
        }
        return false
    }

    fun clickFirstMatchingText(textQuery: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        val nodes = rootNode.findAccessibilityNodeInfosByText(textQuery)
        if (nodes.isNullOrEmpty()) return false
        for (node in nodes) {
            if (node.isClickable) {
                return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            } else if (node.parent?.isClickable == true) {
                return node.parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
        }
        return false
    }
}
