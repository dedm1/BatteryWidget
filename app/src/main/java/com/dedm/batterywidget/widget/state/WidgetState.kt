package com.dedm.batterywidget.widget.state

import com.dedm.batterywidget.metrics.MetricSnapshot
import java.time.Instant

data class WidgetState(
    val metrics: List<MetricSnapshot>,
    val generatedAt: Instant = Instant.now()
) {
    val isEmpty: Boolean = metrics.isEmpty()

    companion object {
        fun empty(): WidgetState = WidgetState(emptyList())
    }
}


