package com.dedm.batterywidget.timezone

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import android.view.accessibility.AccessibilityManager
import android.accessibilityservice.AccessibilityServiceInfo
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.dedm.batterywidget.R
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.roundToInt

class TimeZoneAutomationService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        handler = Handler(Looper.getMainLooper())
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (checkIfUserLeftSettings(event)) {
            return
        }

        val automationContext = buildAutomationContext(event) ?: return
        val request = automationContext.request
        val root = automationContext.root

        when (request.stage) {
            Stage.DISABLE_AUTO -> handleDisableAutoStage(request, root)
            Stage.OPEN_TIME_ZONE -> handleOpenTimeZoneStage(request, root)
            Stage.ENTER_SEARCH -> handleEnterSearchStage(request, root)
            Stage.DONE -> Unit
        }
    }

    private fun checkIfUserLeftSettings(event: AccessibilityEvent?): Boolean {
        val shouldCheck = event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        val request = pendingRequest.get()
        val userLeft = shouldCheck && request != null && run {
            val packageName = event?.packageName?.toString().orEmpty()
            val isSettingsPackage = packageName.startsWith(SETTINGS_PACKAGE_PREFIX) ||
                    packageName.startsWith(SAMSUNG_SETTINGS_PACKAGE_PREFIX)
            if (!isSettingsPackage) {
                finishForManualSelection()
                true
            } else {
                false
            }
        }
        return userLeft
    }

    private fun handleDisableAutoStage(request: TimeZoneRequest, root: AccessibilityNodeInfo) {
        request.incrementDisableAttempts()
        val actionTaken = disableAutomaticTimeZone(root)
        if (actionTaken != null) {
            if (actionTaken) {
                showToast(R.string.main_time_hint_disable_auto_done)
            } else {
                showToast(R.string.main_time_hint_disable_auto_skip)
            }
            request.advance(Stage.OPEN_TIME_ZONE)
        } else if (request.shouldShowManualDisableHint()) {
            showToast(R.string.main_time_hint_verify_manual_toggle)
            request.advance(Stage.OPEN_TIME_ZONE)
        }
    }

    private fun handleOpenTimeZoneStage(request: TimeZoneRequest, root: AccessibilityNodeInfo) {
        if (openTimeZoneList(root)) {
            showToast(R.string.main_time_hint_opened_time_zone)
            request.advance(Stage.ENTER_SEARCH)
        } else if (request.shouldShowManualZoneHint()) {
            showToast(R.string.main_time_hint_opened_manual)
            request.markManualZoneHintShown()
        }
    }

    private fun handleEnterSearchStage(request: TimeZoneRequest, root: AccessibilityNodeInfo) {
        if (enterSearchQuery(root, request.option)) {
            showManualSelectionHint(request.option)
            finishForManualSelection()
        }
    }

    override fun onInterrupt() {
        // No-op
    }

    private fun buildAutomationContext(event: AccessibilityEvent?): AutomationContext? {
        val request = pendingRequest.get()
        val root = rootInActiveWindow
        val packageName = event?.packageName?.toString().orEmpty()
        // Поддержка как стандартных настроек Android, так и Samsung
        val isSettingsEvent = packageName.startsWith(SETTINGS_PACKAGE_PREFIX) ||
                packageName.startsWith(SAMSUNG_SETTINGS_PACKAGE_PREFIX)

        var context: AutomationContext? = null
        if (request != null && root != null && isSettingsEvent) {
            context = if (request.isExpired()) {
                finishWithError()
                null
            } else {
                AutomationContext(request, root)
            }
        }
        return context
    }

    private fun disableAutomaticTimeZone(root: AccessibilityNodeInfo): Boolean? {
        val switchNode = root.findNode { node ->
            val text = (node.text ?: node.contentDescription)?.toString()?.lowercase(Locale.getDefault())
            val className = node.className?.toString()
            val matchesToggle = text?.contains("автомат") == true || text?.contains("automatic") == true
            val isSwitch = className == SWITCH_CLASS || className == SWITCH_COMPAT_CLASS
            matchesToggle && isSwitch
        }
        return when {
            switchNode == null -> null
            switchNode.isCheckable && switchNode.isChecked -> {
                switchNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                true
            }
            else -> false
        }
    }

    private fun openTimeZoneList(root: AccessibilityNodeInfo): Boolean {
        val keywordGroups = listOf(
            listOf("часовой пояс", "часовые пояса", "time zone"),
            listOf("регион", "region")
        )
        var handled = false
        keywordGroups.forEach { keywords ->
            if (!handled) {
                val targetNode = keywords.firstNotNullOfOrNull { text ->
                    root.findNode { node ->
                        node.text?.toString()?.lowercase(Locale.getDefault())?.contains(text) == true ||
                            node.contentDescription?.toString()?.lowercase(Locale.getDefault())?.contains(text) == true
                    }
                }
                handled = targetNode
                    ?.findClickable()
                    ?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    ?: false
            }
        }
        return handled
    }

    private fun enterSearchQuery(root: AccessibilityNodeInfo, option: TimeZoneOption): Boolean {
        val editText = findSearchEditText(root)
        return if (editText != null) {
            handleEditTextInput(editText, option)
        } else {
            tryClickSearchButton(root, option)
        }
    }

    private fun findSearchEditText(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        return root.findNode { node ->
            val className = node.className?.toString() ?: ""
            val viewId = node.viewIdResourceName ?: ""
            val isEditable = node.isEditable
            
            className == EDIT_TEXT_CLASS ||
            className.contains("EditText", ignoreCase = true) ||
            className.contains("SemEditText", ignoreCase = true) ||
            viewId == SEARCH_TEXT_ID ||
            viewId.contains("search", ignoreCase = true) ||
            viewId.contains("edit", ignoreCase = true) ||
            (isEditable && className.contains("Text", ignoreCase = true))
        }
    }

    private fun handleEditTextInput(editText: AccessibilityNodeInfo, option: TimeZoneOption): Boolean {
        return if (!editText.isFocused) {
            focusAndSetTextWithDelay(editText, option)
            true
        } else {
            setTextToEditText(editText, option)
            true
        }
    }

    private fun focusAndSetTextWithDelay(editText: AccessibilityNodeInfo, option: TimeZoneOption) {
        editText.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        handler?.postDelayed({
            val rootAfterDelay = rootInActiveWindow
            val focusedEditText = rootAfterDelay?.findNode { node ->
                node.isFocused && node.isEditable
            }
            if (focusedEditText != null) {
                setTextToEditText(focusedEditText, option)
            }
        }, 200)
    }

    private fun setTextToEditText(editText: AccessibilityNodeInfo, option: TimeZoneOption) {
        val args = Bundle().apply {
            putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                option.searchQuery
            )
        }
        editText.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    private fun tryClickSearchButton(root: AccessibilityNodeInfo, option: TimeZoneOption): Boolean {
        val searchButton = root.findNode { node ->
            val description = (node.contentDescription ?: node.text)?.toString()?.lowercase(Locale.getDefault()) ?: ""
            description.contains("поиск") || description.contains("search")
        }
        if (searchButton != null) {
            searchButton.findClickable()?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            handler?.postDelayed({
                val rootAfterDelay = rootInActiveWindow
                if (rootAfterDelay != null) {
                    enterSearchQuery(rootAfterDelay, option)
                }
            }, 300)
            return false
        }
        return false
    }

    private fun finishWithError() {
        pendingRequest.getAndSet(null)
        showToast(R.string.main_time_automation_failed)
    }

    private fun finishForManualSelection() {
        pendingRequest.getAndSet(null)
    }

    private fun showToast(@StringRes message: Int) {
        val context = applicationContext
        handler?.post {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    private fun showToast(message: String) {
        val context = applicationContext
        handler?.post {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    private fun showManualSelectionHint(option: TimeZoneOption) {
        val message = getString(
            R.string.main_time_hint_select_result,
            option.cityName,
            buildOffsetLabel(option.offsetHours)
        )
        showToast(message)
    }

    companion object {
        private const val SETTINGS_PACKAGE_PREFIX = "com.android.settings"
        private const val SAMSUNG_SETTINGS_PACKAGE_PREFIX = "com.samsung.android.settings"
        private const val SWITCH_CLASS = "android.widget.Switch"
        private const val SWITCH_COMPAT_CLASS = "androidx.appcompat.widget.SwitchCompat"
        private const val EDIT_TEXT_CLASS = "android.widget.EditText"
        private const val SEARCH_TEXT_ID = "android:id/search_src_text"
        private const val REQUEST_TIMEOUT_MS = 15_000L
        private const val MANUAL_DISABLE_ATTEMPT_THRESHOLD = 3

        private var handler: Handler? = null
        private val pendingRequest = AtomicReference<TimeZoneRequest?>()

        fun isEnabled(context: Context): Boolean {
            val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
            val enabledByManager = accessibilityManager
                ?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
                ?.any {
                    val serviceInfo = it.resolveInfo?.serviceInfo
                    serviceInfo?.packageName == context.packageName &&
                        serviceInfo.name == TimeZoneAutomationService::class.java.name
                } ?: false
            if (enabledByManager) {
                return true
            }

            val flattenedName = android.content.ComponentName(
                context.packageName,
                TimeZoneAutomationService::class.java.name
            ).flattenToString()
            val shortName = "${context.packageName}/${TimeZoneAutomationService::class.java.simpleName}"
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            val accessibilityEnabled = Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED,
                0
            )
            val serviceList = enabledServices?.split(":") ?: emptyList()
            val isServiceListed = serviceList.any { entry ->
                val normalized = entry.trim()
                normalized.equals(flattenedName, ignoreCase = true) ||
                    normalized.equals(shortName, ignoreCase = true)
            }
            return accessibilityEnabled == 1 && isServiceListed
        }

        fun submitRequest(context: Context, option: TimeZoneOption) {
            pendingRequest.set(TimeZoneRequest(option))
            showAutomationToast(context, R.string.main_time_automation_running)
            val intents = buildList {
                addAll(createZonePickerIntents())
                addAll(createLegacyTimeZoneIntents())
                add(Intent(Settings.ACTION_DATE_SETTINGS))
            }
            val launched = intents.firstNotNullOfOrNull { intent ->
                runCatching {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    ContextCompat.startActivity(context, intent, null)
                    true
                }.getOrNull()
            }
            if (launched != true) {
                showAutomationToast(context, R.string.main_time_open_settings_error)
                pendingRequest.set(null)
            }
        }

        private fun showAutomationToast(context: Context, @StringRes textRes: Int) {
            Toast.makeText(context.applicationContext, textRes, Toast.LENGTH_LONG).show()
        }

        fun buildOffsetLabel(offset: Double): String {
            val sign = if (offset >= 0) "+" else "-"
            val absOffset = kotlin.math.abs(offset)
            val totalMinutes = (absOffset * 60.0).roundToInt()
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            val hoursStr = hours.toString().padStart(2, '0')
            val minutesStr = minutes.toString().padStart(2, '0')
            return "utc$sign$hoursStr:$minutesStr"
        }

        private fun createZonePickerIntents(): List<Intent> {
            return listOf(
                Intent(Intent.ACTION_MAIN).setClassName(
                    "com.android.settings",
                    "com.android.settings.datetime.timezone.TimeZoneSettingsActivity"
                ),
                Intent(Intent.ACTION_MAIN).setClassName(
                    "com.android.settings",
                    "com.android.settings.Settings\$ZonePickerActivity"
                ),
                Intent("com.android.settings.action.TIME_ZONE_SETTINGS"),
                // Samsung-специфичные интенты
                Intent(Intent.ACTION_MAIN).setClassName(
                    "com.samsung.android.settings",
                    "com.samsung.android.settings.datetime.timezone.TimeZoneSettingsActivity"
                ),
                Intent(Intent.ACTION_MAIN).setClassName(
                    "com.samsung.android.settings",
                    "com.samsung.android.settings.datetime.timezone.ZonePickerActivity"
                )
            )
        }

        private fun createLegacyTimeZoneIntents(): List<Intent> {
            return listOf(
                Intent(Intent.ACTION_MAIN).setClassName(
                    "com.android.settings",
                    "com.android.settings.Settings\$TimeZoneSettingsActivity"
                ),
                Intent(Intent.ACTION_MAIN).setClassName(
                    "com.android.settings",
                    "com.android.settings.Settings\$DateTimeSettingsActivity"
                )
            )
        }
    }

    private data class TimeZoneRequest(
        val option: TimeZoneOption,
        var stage: Stage = Stage.DISABLE_AUTO,
        val createdAt: Long = System.currentTimeMillis(),
        var manualZoneHintShown: Boolean = false,
        var manualDisableHintShown: Boolean = false,
        var disableStageAttempts: Int = 0
    ) {
        fun advance(next: Stage) {
            stage = next
            if (next != Stage.DISABLE_AUTO) {
                disableStageAttempts = 0
            }
        }

        fun isExpired(): Boolean = System.currentTimeMillis() - createdAt > REQUEST_TIMEOUT_MS

        fun shouldShowManualZoneHint(): Boolean {
            val elapsed = System.currentTimeMillis() - createdAt
            return !manualZoneHintShown && elapsed > 4_000L
        }

        fun markManualZoneHintShown() {
            manualZoneHintShown = true
        }

        fun incrementDisableAttempts() {
            disableStageAttempts += 1
        }

        fun shouldShowManualDisableHint(): Boolean {
            val readyToShow = !manualDisableHintShown && disableStageAttempts >= MANUAL_DISABLE_ATTEMPT_THRESHOLD
            if (readyToShow) {
                manualDisableHintShown = true
            }
            return readyToShow
        }
    }

    private enum class Stage {
        DISABLE_AUTO,
        OPEN_TIME_ZONE,
        ENTER_SEARCH,
        DONE
    }

    private data class AutomationContext(
        val request: TimeZoneRequest,
        val root: AccessibilityNodeInfo
    )
}

private fun AccessibilityNodeInfo.findNode(predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
    var result: AccessibilityNodeInfo? = null
    if (predicate(this)) {
        result = this
    } else {
        var index = 0
        while (index < childCount && result == null) {
            val child = getChild(index)
            if (child != null) {
                result = child.findNode(predicate)
            }
            index++
        }
    }
    return result
}

private fun AccessibilityNodeInfo.findClickable(): AccessibilityNodeInfo? {
    var current: AccessibilityNodeInfo? = this
    var clickable: AccessibilityNodeInfo? = null
    while (current != null && clickable == null) {
        if (current.isClickable) {
            clickable = current
        } else {
            current = current.parent
        }
    }
    return clickable
}


