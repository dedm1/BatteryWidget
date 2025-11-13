package com.dedm.batterywidget.metrics.providers

import android.content.Context
import com.dedm.batterywidget.metrics.MetricProvider
import com.dedm.batterywidget.metrics.MetricSnapshot
import com.dedm.batterywidget.metrics.MetricType
import com.dedm.batterywidget.system.BatteryTemperatureProvider
import com.dedm.batterywidget.system.TemperatureFormatter
import java.time.Duration

class TemperatureMetricProvider : MetricProvider {

    override val type: MetricType = MetricType.TEMPERATURE

    override val minUpdateInterval: Duration = Duration.ofSeconds(45)

    override suspend fun getSnapshot(
        context: Context,
        previousSnapshot: MetricSnapshot?
    ): MetricSnapshot? {
        val temperature = BatteryTemperatureProvider.getTemperatureCelsius(context)
            ?: return previousSnapshot

        val clamped = temperature.coerceIn(TEMPERATURE_RANGE.start, TEMPERATURE_RANGE.endInclusive)

        return MetricSnapshot(
            type = type,
            value = clamped,
            range = TEMPERATURE_RANGE,
            primaryText = TemperatureFormatter.formatCelsius(temperature),
            secondaryText = null,
            contentDescription = TemperatureFormatter.verbalize(temperature)
        )
    }

    companion object {
        private val TEMPERATURE_RANGE = 0.0..65.0
    }
}


