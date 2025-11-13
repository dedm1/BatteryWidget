package com.dedm.batterywidget.system

import java.text.DecimalFormat
import java.util.Locale

object TemperatureFormatter {
    private val decimalFormat = DecimalFormat("#0.0")

    fun formatCelsius(value: Double?): String =
        value?.let { "${decimalFormat.format(it)}°C" } ?: "Нет данных"

    fun verbalize(value: Double?): String =
        value?.let { "${decimalFormat.format(it)} градусов Цельсия" } ?: "Температура недоступна"
}

