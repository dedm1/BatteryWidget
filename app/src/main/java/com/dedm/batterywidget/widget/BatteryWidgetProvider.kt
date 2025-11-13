package com.dedm.batterywidget.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.dedm.batterywidget.MainActivity
import com.dedm.batterywidget.R
import com.dedm.batterywidget.metrics.MetricRepository
import com.dedm.batterywidget.metrics.providers.MemoryMetricProvider
import com.dedm.batterywidget.metrics.providers.StorageMetricProvider
import com.dedm.batterywidget.metrics.providers.TemperatureMetricProvider
import com.dedm.batterywidget.widget.render.WidgetRemoteViewsFactory
import com.dedm.batterywidget.widget.state.WidgetState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BatteryWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val appContext = context.applicationContext
        appWidgetIds.forEach { widgetId ->
            requestUpdate(appContext, appWidgetManager, widgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH_ALL) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, BatteryWidgetProvider::class.java))
            onUpdate(context, manager, ids)
        }
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        updateScope.coroutineContext.cancelChildren()
    }

    private fun requestUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        widgetId: Int
    ) {
        Log.d(TAG, "requestUpdate: обновление виджета widgetId=$widgetId")
        updateScope.launch {
            val state = loadWidgetState(context)
            Log.d(TAG, "requestUpdate: состояние загружено, метрик=${state.metrics.size}")
            val remoteViews = WidgetRemoteViewsFactory
                .create(context, state)
                .apply {
                    setOnClickPendingIntent(R.id.widgetRoot, createLaunchIntent(context))
                }

            withContext(Dispatchers.Main.immediate) {
                appWidgetManager.updateAppWidget(widgetId, remoteViews)
                Log.d(TAG, "requestUpdate: виджет обновлен")
            }
        }
    }

    private suspend fun loadWidgetState(context: Context): WidgetState {
        Log.d(TAG, "loadWidgetState: начало загрузки метрик")
        val snapshots = metricRepository.loadSnapshots(context.applicationContext)
        Log.d(TAG, "loadWidgetState: загружено ${snapshots.size} метрик")
        snapshots.forEach { snapshot ->
            Log.d(TAG, "loadWidgetState: ${snapshot.type} = ${snapshot.primaryText}, value=${snapshot.value}")
        }
        return WidgetState(metrics = snapshots)
    }

    private fun createLaunchIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0

        return PendingIntent.getActivity(context, 0, intent, flags)
    }

    companion object {
        private const val TAG = "BatteryWidgetProvider"
        const val ACTION_REFRESH_ALL = "com.dedm.batterywidget.widget.ACTION_REFRESH_ALL"

        private val updateScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        private val metricRepository = MetricRepository(
            listOf(
                TemperatureMetricProvider(),
                MemoryMetricProvider(),
                StorageMetricProvider()
            )
        )
    }
}


