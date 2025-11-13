package com.dedm.batterywidget.metrics.providers

import android.content.Context
import android.os.Environment
import android.os.StatFs
import com.dedm.batterywidget.metrics.MetricProvider
import com.dedm.batterywidget.metrics.MetricSnapshot
import com.dedm.batterywidget.metrics.MetricType
import com.dedm.batterywidget.metrics.formatters.NumberFormatters
import java.io.File
import java.time.Duration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StorageMetricProvider(
    private val storageDirectory: () -> File = { Environment.getDataDirectory() }
) : MetricProvider {

    override val type: MetricType = MetricType.STORAGE

    override val minUpdateInterval: Duration = Duration.ofMinutes(5)

    override suspend fun getSnapshot(
        context: Context,
        previousSnapshot: MetricSnapshot?
    ): MetricSnapshot? = withContext(Dispatchers.IO) {
        val directory = runCatching { storageDirectory() }.getOrNull()
            ?: return@withContext previousSnapshot

        val statFs = runCatching { StatFs(directory.path) }.getOrNull()
            ?: return@withContext previousSnapshot

        val blockSize = statFs.blockSizeLong
        val totalBlocks = statFs.blockCountLong
        val availableBlocks = statFs.availableBlocksLong

        val totalBytes = totalBlocks * blockSize
        val availableBytes = availableBlocks * blockSize
        val usedBytes = totalBytes - availableBytes

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
            contentDescription = "Использование памяти устройства $usedGb из $totalGb гигабайт"
        )
    }

    companion object {
        private val PERCENT_RANGE = 0.0..100.0
    }
}

