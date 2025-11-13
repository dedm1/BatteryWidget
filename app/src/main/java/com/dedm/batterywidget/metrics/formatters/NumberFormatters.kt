package com.dedm.batterywidget.metrics.formatters

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.absoluteValue

object NumberFormatters {

    private val locale: Locale
        get() = Locale.getDefault()

    private val decimalSymbols: DecimalFormatSymbols
        get() = DecimalFormatSymbols(locale)

    fun decimal1(): DecimalFormat = DecimalFormat("#0.0", decimalSymbols)

    fun decimal0(): DecimalFormat = DecimalFormat("#0", decimalSymbols)

    fun formatGigabytes(bytes: Long): String {
        val gb = bytes.toDouble() / GIGABYTE
        val absGb = gb.absoluteValue
        val formatter = if (absGb >= 10) decimal0() else decimal1()
        return formatter.format(gb)
    }

    fun formatPercent(value: Double): String =
        decimal0().format(value)

    private const val GIGABYTE = 1024.0 * 1024 * 1024
}


