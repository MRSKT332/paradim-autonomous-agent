package com.example.security

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object SecurityPolicyManager {

    private const val PREFS_NAME = "paradim_security_policy"
    private const val KEY_PANIC_KILL_SWITCH = "panic_kill_switch"
    private const val KEY_BIOMETRIC_LOCK = "biometric_lock_enabled"
    private const val KEY_SECURITY_PIN = "security_pin"
    private const val KEY_PATTERN_SEQUENCE = "pattern_sequence"
    private const val KEY_APP_LOCK_ENABLED = "app_lock_enabled"
    private const val KEY_LOCKED_PACKAGES = "locked_packages"
    private const val KEY_BLACKLISTED_KEYWORDS = "blacklisted_keywords"
    private const val KEY_CUSTOM_SYSTEM_PROMPT = "custom_system_prompt"
    private const val KEY_SPROUT_THEME = "sprout_theme_enabled"

    private val defaultBlacklist = setOf("factory reset", "wipe device", "format sdcard", "delete root", "uninstall security")
    private val defaultLockedApps = setOf("com.whatsapp", "org.telegram.messenger", "com.android.bank", "com.google.android.apps.photos")

    private val defaultSystemPrompt = """
        Always prioritize user safety and privacy.
        Skip ads automatically on YouTube and media apps.
        For WhatsApp or messaging tasks, prepare draft messages clearly before sending.
        Keep actions concise, robust, and fast.
    """.trimIndent()

    private val _panicKillSwitchActive = MutableStateFlow(false)
    val panicKillSwitchActive: StateFlow<Boolean> = _panicKillSwitchActive.asStateFlow()

    private val _biometricLockEnabled = MutableStateFlow(false)
    val biometricLockEnabled: StateFlow<Boolean> = _biometricLockEnabled.asStateFlow()

    private val _appLockEnabled = MutableStateFlow(true)
    val appLockEnabled: StateFlow<Boolean> = _appLockEnabled.asStateFlow()

    private val _patternSequence = MutableStateFlow<List<Int>>(listOf(0, 1, 2, 4, 6)) // Default Z pattern
    val patternSequence: StateFlow<List<Int>> = _patternSequence.asStateFlow()

    private val _lockedAppPackages = MutableStateFlow<Set<String>>(defaultLockedApps)
    val lockedAppPackages: StateFlow<Set<String>> = _lockedAppPackages.asStateFlow()

    private val _blacklistedKeywords = MutableStateFlow<Set<String>>(defaultBlacklist)
    val blacklistedKeywords: StateFlow<Set<String>> = _blacklistedKeywords.asStateFlow()

    private val _customSystemPrompt = MutableStateFlow(defaultSystemPrompt)
    val customSystemPrompt: StateFlow<String> = _customSystemPrompt.asStateFlow()

    private val _isSproutGreenTheme = MutableStateFlow(true)
    val isSproutGreenTheme: StateFlow<Boolean> = _isSproutGreenTheme.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _panicKillSwitchActive.value = prefs.getBoolean(KEY_PANIC_KILL_SWITCH, false)
        _biometricLockEnabled.value = prefs.getBoolean(KEY_BIOMETRIC_LOCK, false)
        _appLockEnabled.value = prefs.getBoolean(KEY_APP_LOCK_ENABLED, true)
        _blacklistedKeywords.value = prefs.getStringSet(KEY_BLACKLISTED_KEYWORDS, defaultBlacklist) ?: defaultBlacklist
        _lockedAppPackages.value = prefs.getStringSet(KEY_LOCKED_PACKAGES, defaultLockedApps) ?: defaultLockedApps
        _customSystemPrompt.value = prefs.getString(KEY_CUSTOM_SYSTEM_PROMPT, defaultSystemPrompt) ?: defaultSystemPrompt
        _isSproutGreenTheme.value = prefs.getBoolean(KEY_SPROUT_THEME, true)

        val patternString = prefs.getString(KEY_PATTERN_SEQUENCE, "0,1,2,4,6") ?: "0,1,2,4,6"
        _patternSequence.value = patternString.split(",").mapNotNull { it.toIntOrNull() }
    }

    fun setPanicKillSwitch(context: Context, active: Boolean) {
        _panicKillSwitchActive.value = active
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_PANIC_KILL_SWITCH, active).apply()
    }

    fun setBiometricLock(context: Context, enabled: Boolean) {
        _biometricLockEnabled.value = enabled
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_BIOMETRIC_LOCK, enabled).apply()
    }

    fun setAppLockEnabled(context: Context, enabled: Boolean) {
        _appLockEnabled.value = enabled
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_APP_LOCK_ENABLED, enabled).apply()
    }

    fun setPatternSequence(context: Context, sequence: List<Int>) {
        _patternSequence.value = sequence
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_PATTERN_SEQUENCE, sequence.joinToString(",")).apply()
    }

    fun verifyPattern(sequence: List<Int>): Boolean {
        return _patternSequence.value == sequence
    }

    fun toggleAppLockForPackage(context: Context, pkg: String) {
        val updated = _lockedAppPackages.value.toMutableSet()
        if (updated.contains(pkg)) {
            updated.remove(pkg)
        } else {
            updated.add(pkg)
        }
        _lockedAppPackages.value = updated
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(KEY_LOCKED_PACKAGES, updated).apply()
    }

    fun isPackageLocked(pkg: String): Boolean {
        return _appLockEnabled.value && _lockedAppPackages.value.contains(pkg)
    }

    fun setMasterPin(context: Context, pin: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SECURITY_PIN, pin).apply()
    }

    fun verifyMasterPin(context: Context, pin: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val storedPin = prefs.getString(KEY_SECURITY_PIN, "1234") ?: "1234"
        return storedPin == pin
    }

    fun addBlacklistedKeyword(context: Context, keyword: String) {
        val updated = _blacklistedKeywords.value.toMutableSet()
        updated.add(keyword.lowercase().trim())
        _blacklistedKeywords.value = updated
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(KEY_BLACKLISTED_KEYWORDS, updated).apply()
    }

    fun removeBlacklistedKeyword(context: Context, keyword: String) {
        val updated = _blacklistedKeywords.value.toMutableSet()
        updated.remove(keyword.lowercase().trim())
        _blacklistedKeywords.value = updated
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(KEY_BLACKLISTED_KEYWORDS, updated).apply()
    }

    fun setCustomSystemPrompt(context: Context, promptText: String) {
        _customSystemPrompt.value = promptText
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_CUSTOM_SYSTEM_PROMPT, promptText).apply()
    }

    fun setSproutThemeEnabled(context: Context, enabled: Boolean) {
        _isSproutGreenTheme.value = enabled
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_SPROUT_THEME, enabled).apply()
    }

    fun evaluateCommandSafety(command: String): Pair<Boolean, String?> {
        if (_panicKillSwitchActive.value) {
            return Pair(false, "EMERGENCY_PANIC_KILL_SWITCH_ACTIVE: All agent operations are blocked.")
        }

        val lower = command.lowercase().trim()
        for (kw in _blacklistedKeywords.value) {
            if (lower.contains(kw.lowercase())) {
                return Pair(false, "SECURITY_POLICY_VIOLATION: Command contains blocked phrase '$kw'")
            }
        }

        return Pair(true, null)
    }
}
