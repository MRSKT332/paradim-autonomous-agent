package com.example.ai

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AgentCorrectionRule(
    val id: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

object AgentMemoryManager {

    private const val PREFS_NAME = "paradim_agent_memory"
    private const val KEY_CORRECTIONS = "agent_corrections_set"

    private val defaultRules = listOf(
        "When user gives a compound command (e.g., 'open X and send Y' or 'open X and search Y'), do NOT stop after launching the app. You must complete ALL sub-tasks: search recipient/item, type input text, and click send/submit.",
        "For Telegram, WhatsApp, or SMS messaging tasks, search for the target contact, open the chat window, type the specified text message, and click the send button."
    )

    private val _correctionRules = MutableStateFlow<List<AgentCorrectionRule>>(emptyList())
    val correctionRules: StateFlow<List<AgentCorrectionRule>> = _correctionRules.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedSet = prefs.getStringSet(KEY_CORRECTIONS, null)

        val list = if (savedSet.isNullOrEmpty()) {
            defaultRules.mapIndexed { idx, rule -> AgentCorrectionRule("rule_$idx", rule) }
        } else {
            savedSet.mapIndexed { idx, rule -> AgentCorrectionRule("saved_$idx", rule) }
        }

        _correctionRules.value = list
        if (savedSet == null) {
            saveToPrefs(context, list)
        }
    }

    private fun saveToPrefs(context: Context, rules: List<AgentCorrectionRule>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val stringSet = rules.map { it.text }.toSet()
        prefs.edit().putStringSet(KEY_CORRECTIONS, stringSet).apply()
    }

    fun addCorrection(context: Context, correctionText: String): AgentCorrectionRule {
        val cleanText = correctionText.trim()
        val current = _correctionRules.value.toMutableList()
        val existing = current.find { it.text.equals(cleanText, ignoreCase = true) }
        if (existing != null) return existing

        val newRule = AgentCorrectionRule("rule_${System.currentTimeMillis()}", cleanText)
        current.add(0, newRule) // Newest first
        _correctionRules.value = current
        saveToPrefs(context, current)
        return newRule
    }

    fun removeCorrection(context: Context, ruleId: String) {
        val current = _correctionRules.value.filterNot { it.id == ruleId }
        _correctionRules.value = current
        saveToPrefs(context, current)
    }

    fun clearAllCorrections(context: Context) {
        _correctionRules.value = emptyList()
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_CORRECTIONS).apply()
    }

    fun getFormattedCorrectionsForSystemPrompt(): String {
        val rules = _correctionRules.value
        if (rules.isEmpty()) return "No specific user feedback rules recorded yet."

        return rules.mapIndexed { idx, rule ->
            "[Rule ${idx + 1}] ${rule.text}"
        }.joinToString("\n")
    }
}
