package com.dedm.batterywidget.metrics.providers

import android.app.ActivityManager
import android.content.Context
import com.dedm.batterywidget.metrics.MetricProvider
import com.dedm.batterywidget.metrics.MetricSnapshot
import com.dedm.batterywidget.metrics.MetricType
import com.dedm.batterywidget.metrics.formatters.NumberFormatters
import java.time.Duration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MemoryMetricProvider : MetricProvider {

    override val type: MetricType = MetricType.MEMORY

    override val minUpdateInterval: Duration = Duration.ofSeconds(20)

    override suspend fun getSnapshot(
        context: Context,
        previousSnapshot: MetricSnapshot?
    ): MetricSnapshot? = withContext(Dispatchers.Default) {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            ?: return@withContext previousSnapshot

        val info = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(info)
        val totalBytes = info.totalMem
        val usedBytes = totalBytes - info.availMem

        if (totalBytes <= 0) {
            return@withContext previousSnapshot
        }

        val usedPercent = (usedBytes.toDouble() / totalBytes.toDouble()) * 100.0
        val usedGb = NumberFormatters.formatGigabytes(usedBytes)
        val totalGb = NumberFormatters.formatGigabytes(totalBytes)

        MetricSnapshot(
            type = type,
            value = usedPercent.coerceIn(0.0, 100.0),
            range = PERCENT_RANGE,
            primaryText = "$usedGb / $totalGb ГБ",
            secondaryText = "${NumberFormatters.formatPercent(usedPercent)}%",
            contentDescription = "Использование оперативной памяти $usedGb из $totalGb гигабайт"
        )
    }

    companion object {
        private val PERCENT_RANGE = 0.0..100.0
    }
}


