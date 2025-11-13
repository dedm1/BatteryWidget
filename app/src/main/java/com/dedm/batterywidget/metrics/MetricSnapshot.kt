package com.dedm.batterywidget.metrics

import java.time.Instant
import kotlin.math.max
import kotlin.math.min

data class MetricSnapshot(
    val type: MetricType,
    val value: Double,
    val range: ClosedFloatingPointRange<Double>,
    val primaryText: String,
    val secondaryText: String? = null,
    val contentDescription: String? = null,
    val timestamp: Instant = Instant.now()
) {
    val progress: Float = computeProgress(value, range)

    companion object {
        private fun computeProgress(
            value: Double,
            range: ClosedFloatingPointRange<Double>
        ): Float {
            val start = range.start
            val endInclusive = range.endInclusive
            if (start == endInclusive) {
                return 0f
            }
            val normalized = (value - start) / (endInclusive - start)
            val clamped = max(0.0, min(1.0, normalized))
            return clamped.toFloat()
        }
    }
}


