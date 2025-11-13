package com.dedm.batterywidget.system

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

object BatteryTemperatureProvider {

    /**
     * Возвращает температуру аккумулятора в градусах Цельсия или null, если данные недоступны.
     */
    fun getTemperatureCelsius(context: Context): Double? {
        val batteryStatus: Intent? = context.registerReceiver(
            /* receiver = */ null,
            /* filter = */ IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            /* broadcastPermission = */ Context.RECEIVER_NOT_EXPORTED
        )

        val temperatureTenths = batteryStatus
            ?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
            ?.takeUnless { it == Int.MIN_VALUE }

        return temperatureTenths?.div(10.0)
    }
}

