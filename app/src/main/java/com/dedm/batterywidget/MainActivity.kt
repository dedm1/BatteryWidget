package com.dedm.batterywidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.dedm.batterywidget.ui.theme.BatteryWidgetTheme
import com.dedm.batterywidget.timezone.TimeZoneAutomationService
import com.dedm.batterywidget.timezone.TimeZoneOption
import com.dedm.batterywidget.widget.BatteryWidgetProvider
import com.dedm.batterywidget.widget.DateWidgetProvider
import kotlin.reflect.KClass
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BatteryWidgetTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        widgets = WidgetCatalog.entries,
                        onAddWidgetClick = { entry ->
                            requestAddWidget(entry)
                        }
                    )
                }
            }
        }
    }

    private fun requestAddWidget(entry: WidgetEntry) {
        val appWidgetManager = getSystemService(AppWidgetManager::class.java) ?: return
        val provider = ComponentName(this, entry.providerClass.java)

        if (appWidgetManager.isRequestPinAppWidgetSupported) {
            appWidgetManager.requestPinAppWidget(provider, /* extras = */ null, /* successCallback = */ null)
        } else {
            Toast.makeText(this, R.string.main_manual_hint, Toast.LENGTH_LONG).show()
        }
    }
}

@Composable
fun MainScreen(
    widgets: List<WidgetEntry>,
    onAddWidgetClick: (WidgetEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentInstant by produceState(initialValue = Instant.now()) {
        while (true) {
            value = Instant.now()
            delay(60_000L)
        }
    }
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        CurrentDateTimeHeader(
            currentInstant = currentInstant
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(id = R.string.main_title),
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = stringResource(id = R.string.main_description),
                style = MaterialTheme.typography.bodyLarge
            )
        }

        TimeSettingsSection(
            currentInstant = currentInstant,
            onRequestAutomation = { option ->
                if (TimeZoneAutomationService.isEnabled(context)) {
                    TimeZoneAutomationService.submitRequest(context, option)
                } else {
                    Toast.makeText(
                        context,
                        R.string.main_time_service_required,
                        Toast.LENGTH_LONG
                    ).show()
                }
            },
            onOpenAccessibilitySettings = {
                runCatching {
                    context.startActivity(
                        android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                }.onFailure {
                    Toast.makeText(
                        context,
                        R.string.main_time_open_settings_error,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        )

        Text(
            text = stringResource(id = R.string.main_section_available_widgets),
            style = MaterialTheme.typography.titleMedium
        )

        WidgetGallery(
            widgets = widgets,
            onAddWidgetClick = onAddWidgetClick
        )
    }
}

@Composable
private fun CurrentDateTimeHeader(
    currentInstant: Instant,
    modifier: Modifier = Modifier
) {
    val systemZone = remember { ZoneId.systemDefault() }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault()) }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()) }
    val zonedDateTime = currentInstant.atZone(systemZone)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = zonedDateTime.format(dateFormatter),
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = zonedDateTime.format(timeFormatter),
            style = MaterialTheme.typography.displayLarge
        )
    }
}

@Composable
private fun WidgetGallery(
    widgets: List<WidgetEntry>,
    onAddWidgetClick: (WidgetEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(widgets) { entry ->
            WidgetCard(entry = entry, onAddWidgetClick = onAddWidgetClick)
        }
    }
}

@Composable
private fun WidgetCard(
    entry: WidgetEntry,
    onAddWidgetClick: (WidgetEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .width(280.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(id = entry.titleRes),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(id = entry.descriptionRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            when (entry.providerClass) {
                BatteryWidgetProvider::class -> BatteryWidgetPreview()
                DateWidgetProvider::class -> DateWidgetPreview()
                else -> BatteryWidgetPreview()
            }

            Button(onClick = { onAddWidgetClick(entry) }) {
                Text(text = stringResource(id = R.string.main_add_widget))
            }
        }
    }
}

@Composable
private fun TimeSettingsSection(
    currentInstant: Instant,
    onRequestAutomation: (TimeZoneOption) -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val systemZone = remember { ZoneId.systemDefault() }
    val systemOffset = systemZone.rules.getOffset(Instant.now())
    val systemOffsetId = systemOffset.id.replace("Z", "+00:00")
    val systemZoneName = systemZone.getDisplayName(TextStyle.FULL, Locale.getDefault())
    val options = remember { TimeZoneOption.defaultList() }
    val systemOffsetHours = systemOffset.totalSeconds / 3600.0
    val initialIndex = remember(options, systemOffsetHours) {
        val exactMatch = options.indexOfFirst { it.offsetHours == systemOffsetHours }
        when {
            exactMatch >= 0 -> exactMatch
            options.isEmpty() -> 0
            systemOffsetHours < options.first().offsetHours -> 0
            systemOffsetHours > options.last().offsetHours -> options.lastIndex
            else -> options.indexOfLast { it.offsetHours <= systemOffsetHours }.coerceAtLeast(0)
        }
    }
    var selectedIndex by rememberSaveable { mutableStateOf(initialIndex) }
    val boundedLastIndex = options.lastIndex.coerceAtLeast(0)
    val selectedOption = options.getOrNull(selectedIndex) ?: run {
        val fallbackIndex = boundedLastIndex
        options.getOrElse(fallbackIndex) {
            TimeZoneOption(
                offsetHours = systemOffsetHours,
                zoneId = systemZone.id,
                cityName = systemZoneName,
                searchQuery = systemZoneName
            )
        }
    }
    val sign = if (selectedOption.offsetHours >= 0) "+" else ""
    var isServiceEnabled by rememberSaveable { mutableStateOf(TimeZoneAutomationService.isEnabled(context)) }
    var manualOverride by rememberSaveable { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isServiceEnabled = TimeZoneAutomationService.isEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val applyEnabled = isServiceEnabled || manualOverride
    val selectionState = TimeZoneSelectionState(
        options = options,
        currentInstant = currentInstant,
        selectedIndex = selectedIndex,
        selectedOption = selectedOption,
        offsetSign = sign
    )
    val automationState = TimeAutomationState(
        isServiceEnabled = isServiceEnabled,
        manualOverride = manualOverride,
        applyEnabled = applyEnabled
    )
    val handleApplyClick = {
        if (!automationState.applyEnabled) {
            Toast.makeText(
                context,
                R.string.main_time_service_required,
                Toast.LENGTH_LONG
            ).show()
        } else {
            onRequestAutomation(selectedOption)
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TimeSettingsHeader(
            systemZoneName = systemZoneName,
            systemOffsetId = systemOffsetId
        )
        TimeZoneSelectionSection(
            state = selectionState,
            onOptionSelected = { selectedIndex = it }
        )
        TimeSettingsAutomationActions(
            state = automationState,
            onManualOverrideChange = { manualOverride = it },
            onOpenAccessibilitySettings = onOpenAccessibilitySettings,
            onApplyClick = handleApplyClick
        )
    }
}

@Composable
private fun TimeZoneOptionRow(
    option: TimeZoneOption,
    currentInstant: Instant,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.05f)
    }
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val zonedDateTime = currentInstant.atZone(ZoneId.of(option.zoneId))
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()) }
    val timeLabel = timeFormatter.format(zonedDateTime)
    val offsetLabel = buildString {
        append("UTC")
        append(if (option.offsetHours >= 0) "+" else "-")
        val absOffset = kotlin.math.abs(option.offsetHours)
        val hours = absOffset.toInt()
        val minutes = ((absOffset - hours) * 60).toInt()
        append(hours.toString().padStart(2, '0'))
        if (minutes > 0) {
            append(":")
            append(minutes.toString().padStart(2, '0'))
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = option.cityName,
                style = MaterialTheme.typography.bodyLarge,
                color = contentColor
            )
            Text(
                text = offsetLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor.copy(alpha = 0.85f)
            )
        }
        Text(
            text = timeLabel,
            style = MaterialTheme.typography.titleMedium,
            color = contentColor
        )
    }
}

@Composable
private fun TimeSettingsHeader(
    systemZoneName: String,
    systemOffsetId: String
) {
    Text(
        text = stringResource(id = R.string.main_section_time_settings),
        style = MaterialTheme.typography.titleMedium
    )
    Text(
        text = stringResource(id = R.string.main_time_description),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Text(
        text = stringResource(id = R.string.main_time_manual_note),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Text(
        text = stringResource(
            id = R.string.main_time_system_zone,
            systemZoneName,
            systemOffsetId
        ),
        style = MaterialTheme.typography.bodyMedium
    )
}

@Composable
private fun TimeZoneSelectionSection(
    state: TimeZoneSelectionState,
    onOptionSelected: (Int) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            state.options.forEachIndexed { index, option ->
                val isSelected = index == state.selectedIndex
                TimeZoneOptionRow(
                    option = option,
                    currentInstant = state.currentInstant,
                    isSelected = isSelected,
                    onClick = { onOptionSelected(index) }
                )
            }
        }
        Text(
            text = stringResource(
                id = R.string.main_time_selected_city,
                state.selectedOption.cityName
            ),
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = stringResource(
                id = R.string.main_time_offset_value,
                formatOffset(state.selectedOption.offsetHours)
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TimeSettingsAutomationActions(
    state: TimeAutomationState,
    onManualOverrideChange: (Boolean) -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onApplyClick: () -> Unit
) {
    OutlinedButton(onClick = onOpenAccessibilitySettings) {
        Text(text = stringResource(id = R.string.main_time_service_enable))
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Switch(
            checked = state.manualOverride || state.isServiceEnabled,
            onCheckedChange = { checked ->
                onManualOverrideChange(checked && !state.isServiceEnabled)
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
        )
        Text(
            text = stringResource(id = R.string.main_time_service_override_label),
            style = MaterialTheme.typography.bodyMedium
        )
    }

    Button(
        onClick = onApplyClick,
        enabled = state.applyEnabled
    ) {
        Text(text = stringResource(id = R.string.main_time_apply_selected))
    }
}

private fun formatOffset(offsetHours: Double): String {
    val sign = if (offsetHours >= 0) "+" else "-"
    val absOffset = kotlin.math.abs(offsetHours)
    val totalMinutes = (absOffset * 60.0).roundToInt()
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return buildString {
        append(sign)
        append(hours.toString().padStart(2, '0'))
        append(":")
        append(minutes.toString().padStart(2, '0'))
    }
}

private data class TimeZoneSelectionState(
    val options: List<TimeZoneOption>,
    val currentInstant: Instant,
    val selectedIndex: Int,
    val selectedOption: TimeZoneOption,
    val offsetSign: String
)

private data class TimeAutomationState(
    val isServiceEnabled: Boolean,
    val manualOverride: Boolean,
    val applyEnabled: Boolean
)

@Composable
private fun BatteryWidgetPreview(modifier: Modifier = Modifier) {
    val background = colorResource(id = R.color.widget_background)
    val ringBackground = colorResource(id = R.color.widget_ring_background)
    val ringColorStart = colorResource(id = R.color.widget_ring_progress_start)
    val ringColorMid = colorResource(id = R.color.widget_ring_progress_mid)
    val ringColorEnd = colorResource(id = R.color.widget_ring_progress_end)
    val textColor = colorResource(id = R.color.widget_on_primary)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(background)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val metrics = listOf(
            WidgetMetricUiModel(
                label = stringResource(id = R.string.widget_metric_temperature_emoji),
                primary = "31.2°C",
                secondary = "",
                ringBackground = ringBackground,
                ringColor = ringColorStart,
                textColor = textColor,
                progress = 0.4f
            ),
            WidgetMetricUiModel(
                label = stringResource(id = R.string.widget_metric_memory_emoji),
                primary = "5.2/12 ГБ",
                secondary = "43%",
                ringBackground = ringBackground,
                ringColor = ringColorMid,
                textColor = textColor,
                progress = 0.7f
            ),
            WidgetMetricUiModel(
                label = stringResource(id = R.string.widget_metric_storage_emoji),
                primary = "84/128 ГБ",
                secondary = "65%",
                ringBackground = ringBackground,
                ringColor = ringColorEnd,
                textColor = textColor,
                progress = 0.6f
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            metrics.forEach { metric ->
                WidgetMetricPreview(metric = metric)
            }
        }
    }
}

@Composable
private fun DateWidgetPreview(modifier: Modifier = Modifier) {
    val background = colorResource(id = R.color.widget_background)
    val textColor = colorResource(id = R.color.widget_on_primary)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(background)
            .padding(horizontal = 24.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "8 ноября 2026",
            style = MaterialTheme.typography.titleMedium,
            color = textColor
        )
        Text(
            text = "23:33",
            style = MaterialTheme.typography.displaySmall,
            color = textColor
        )
    }
}

@Composable
private fun WidgetMetricPreview(
    metric: WidgetMetricUiModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { metric.progress.coerceIn(0f, 1f) },
                strokeWidth = 5.dp,
                trackColor = metric.ringBackground,
                color = metric.ringColor,
                modifier = Modifier.size(52.dp)
            )
            Text(
                text = metric.label,
                style = MaterialTheme.typography.bodyLarge,
                color = metric.textColor
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = metric.primary,
                style = MaterialTheme.typography.bodyMedium,
                color = metric.textColor
            )
            if (metric.secondary.isNotBlank()) {
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = metric.secondary,
                    style = MaterialTheme.typography.labelSmall,
                    color = metric.textColor.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    BatteryWidgetTheme {
        MainScreen(
            widgets = WidgetCatalog.entries,
            onAddWidgetClick = {},
        )
    }
}


data class WidgetEntry(
    val providerClass: KClass<out AppWidgetProvider>,
    @androidx.annotation.StringRes val titleRes: Int,
    @androidx.annotation.StringRes val descriptionRes: Int
)

object WidgetCatalog {
    val entries: List<WidgetEntry> = listOf(
        WidgetEntry(
            providerClass = BatteryWidgetProvider::class,
            titleRes = R.string.widget_battery_title,
            descriptionRes = R.string.widget_battery_description
        ),
        WidgetEntry(
            providerClass = DateWidgetProvider::class,
            titleRes = R.string.widget_date_title,
            descriptionRes = R.string.widget_date_description
        )
    )
}

private data class WidgetMetricUiModel(
    val label: String,
    val primary: String,
    val secondary: String,
    val ringBackground: Color,
    val ringColor: Color,
    val textColor: Color,
    val progress: Float
)
