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

        val pkgName = event.packageName?.toString() ?: ""
        // Never process accessibility events on our own app to avoid focus stealing & UI lag
        if (pkgName.contains("com.example") || pkgName.contains("com.aistudio")) {
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

    private fun checkAndSkipAds(node: AccessibilityNodeInfo) {
        val nodeText = node.text?.toString() ?: ""
        val contentDesc = node.contentDescription?.toString() ?: ""
        val viewId = node.viewIdResourceName ?: ""

        // Common ad-skip button triggers across YouTube & Video Apps
        val isSkipTrigger = nodeText.contains("Skip Ad", ignoreCase = true) ||
                nodeText.contains("Skip Ads", ignoreCase = true) ||
                nodeText.equals("Skip", ignoreCase = true) ||
                contentDesc.contains("Skip Ad", ignoreCase = true) ||
                viewId.contains("skip_ad_button", ignoreCase = true) ||
                viewId.contains("ad_skip_button", ignoreCase = true)

        if (isSkipTrigger && (node.isClickable || node.parent?.isClickable == true)) {
            val target = if (node.isClickable) node else node.parent
            val clicked = target?.performAction(AccessibilityNodeInfo.ACTION_CLICK) ?: false
            if (clicked) {
                _adSkippedCount.value = _adSkippedCount.value + 1
            }
            return
        }

        // Traverse child nodes
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            checkAndSkipAds(child)
            child.recycle()
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
