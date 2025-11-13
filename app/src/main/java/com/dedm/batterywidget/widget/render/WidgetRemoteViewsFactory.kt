package com.dedm.batterywidget.widget.render

import android.content.Context
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.dedm.batterywidget.R
import com.dedm.batterywidget.metrics.MetricSnapshot
import com.dedm.batterywidget.metrics.MetricType
import com.dedm.batterywidget.widget.state.WidgetState
import kotlin.math.roundToInt

object WidgetRemoteViewsFactory {

    fun create(context: Context, state: WidgetState): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_battery)
        val snapshotMap = state.metrics.associateBy { it.type }

        MetricType.values().forEach { type ->
            val binding = bindings[type] ?: return@forEach
            val snapshot = snapshotMap[type]

            if (snapshot != null) {
                applySnapshot(context, views, binding, snapshot)
            } else {
                applyEmptyState(context, views, binding)
            }
        }

        return views
    }

    private fun applySnapshot(
        context: Context,
        views: RemoteViews,
        binding: Binding,
        snapshot: MetricSnapshot
    ) {
        views.setViewVisibility(binding.containerId, View.VISIBLE)
        views.setInt(
            binding.progressViewId,
            "setImageLevel",
            snapshot.progressLevel()
        )
        views.setTextViewText(binding.primaryTextViewId, snapshot.primaryText)

        val secondaryText = snapshot.secondaryText
        if (!secondaryText.isNullOrBlank()) {
            views.setViewVisibility(binding.secondaryTextViewId, View.VISIBLE)
            views.setTextViewText(binding.secondaryTextViewId, secondaryText)
        } else {
            views.setViewVisibility(binding.secondaryTextViewId, View.INVISIBLE)
        }

        val color = resolveProgressColor(context, snapshot)
        views.setInt(binding.progressViewId, "setColorFilter", color)

        val description = snapshot.contentDescription
            ?: context.getString(binding.contentDescriptionResId)
        views.setContentDescription(binding.iconContainerId, description)
    }

    private fun applyEmptyState(
        context: Context,
        views: RemoteViews,
        binding: Binding
    ) {
        views.setViewVisibility(binding.containerId, View.VISIBLE)
        views.setInt(binding.progressViewId, "setImageLevel", 0)
        views.setViewVisibility(binding.secondaryTextViewId, View.INVISIBLE)
        views.setTextViewText(
            binding.primaryTextViewId,
            context.getString(R.string.widget_metric_no_data)
        )
        views.setInt(
            binding.progressViewId,
            "setColorFilter",
            ContextCompat.getColor(context, R.color.widget_ring_empty)
        )
        views.setContentDescription(
            binding.iconContainerId,
            context.getString(binding.contentDescriptionResId)
        )
    }

    private data class Binding(
        val containerId: Int,
        val iconContainerId: Int,
        val progressViewId: Int,
        val primaryTextViewId: Int,
        val secondaryTextViewId: Int,
        @StringRes val contentDescriptionResId: Int
    )

    private fun MetricSnapshot.progressLevel(): Int =
        (progress.coerceIn(0f, 1f) * MAX_LEVEL).roundToInt()

    @ColorInt
    private fun resolveProgressColor(context: Context, snapshot: MetricSnapshot): Int =
        when (snapshot.type) {
            MetricType.TEMPERATURE -> colorForProgress(context, snapshot.progress)
            MetricType.MEMORY,
            MetricType.STORAGE -> colorForPercentage(context, snapshot.value)
        }

    @ColorInt
    private fun colorForProgress(context: Context, progress: Float): Int {
        val percent = (progress.coerceIn(0f, 1f) * 100f)
        val colorRes = when {
            percent >= 80f -> R.color.widget_ring_progress_end
            percent >= 50f -> R.color.widget_ring_progress_mid
            else -> R.color.widget_ring_progress_start
        }
        return ContextCompat.getColor(context, colorRes)
    }

    @ColorInt
    private fun colorForPercentage(context: Context, value: Double): Int {
        val percent = value.coerceIn(0.0, 100.0)
        val colorRes = when {
            percent >= 85.0 -> R.color.widget_ring_progress_end
            percent >= 50.0 -> R.color.widget_ring_progress_mid
            else -> R.color.widget_ring_progress_start
        }
        return ContextCompat.getColor(context, colorRes)
    }

    private val bindings: Map<MetricType, Binding> = mapOf(
        MetricType.TEMPERATURE to Binding(
            containerId = R.id.metricTemperatureContainer,
            iconContainerId = R.id.metricTemperatureIconContainer,
            progressViewId = R.id.metricTemperatureProgress,
            primaryTextViewId = R.id.metricTemperaturePrimaryText,
            secondaryTextViewId = R.id.metricTemperatureSecondaryText,
            contentDescriptionResId = R.string.widget_metric_temperature_description
        ),
        MetricType.MEMORY to Binding(
            containerId = R.id.metricMemoryContainer,
            iconContainerId = R.id.metricMemoryIconContainer,
            progressViewId = R.id.metricMemoryProgress,
            primaryTextViewId = R.id.metricMemoryPrimaryText,
            secondaryTextViewId = R.id.metricMemorySecondaryText,
            contentDescriptionResId = R.string.widget_metric_memory_description
        ),
        MetricType.STORAGE to Binding(
            containerId = R.id.metricStorageContainer,
            iconContainerId = R.id.metricStorageIconContainer,
            progressViewId = R.id.metricStorageProgress,
            primaryTextViewId = R.id.metricStoragePrimaryText,
            secondaryTextViewId = R.id.metricStorageSecondaryText,
            contentDescriptionResId = R.string.widget_metric_storage_description
        )
    )

    private const val MAX_LEVEL = 10000f
}

