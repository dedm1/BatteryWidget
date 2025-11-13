package com.dedm.batterywidget.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import com.dedm.batterywidget.R
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

class DateWidgetProvider : AppWidgetProvider() {

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        scheduleNextUpdate(context.applicationContext)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        cancelScheduledUpdate(context.applicationContext)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return
        if (action in timeRelatedActions) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, DateWidgetProvider::class.java))
            if (ids.isNotEmpty()) {
                onUpdate(context, manager, ids)
            } else {
                cancelScheduledUpdate(context.applicationContext)
            }
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val appContext = context.applicationContext
        appWidgetIds.forEach { widgetId ->
            updateAppWidget(appContext, appWidgetManager, widgetId)
        }
        if (appWidgetIds.isNotEmpty()) {
            scheduleNextUpdate(appContext)
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_date_time)

        val now = ZonedDateTime.now()
        val dateText = now.format(dateFormatter)
        val timeText = now.format(timeFormatter)

        views.setTextViewText(R.id.widgetDateText, dateText)
        views.setTextViewText(R.id.widgetTimeText, timeText)
        views.setContentDescription(
            R.id.widgetDateContainer,
            context.getString(R.string.widget_date_open_calendar_description)
        )
        views.setContentDescription(
            R.id.widgetTimeContainer,
            context.getString(R.string.widget_date_open_clock_description)
        )
        views.setOnClickPendingIntent(
            R.id.widgetDateContainer,
            calendarPendingIntent(context)
        )
        views.setOnClickPendingIntent(
            R.id.widgetTimeContainer,
            clockPendingIntent(context)
        )
        // Дублируем клики на сами тексты — на некоторых лаунчерах событие может не доходить до контейнера
        views.setOnClickPendingIntent(
            R.id.widgetDateText,
            calendarPendingIntent(context)
        )
        views.setOnClickPendingIntent(
            R.id.widgetTimeText,
            clockPendingIntent(context)
        )

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun scheduleNextUpdate(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val triggerAt = ZonedDateTime.now()
            .plusMinutes(1)
            .truncatedTo(ChronoUnit.MINUTES)
            .toInstant()
            .toEpochMilli()

        val pendingIntent = updatePendingIntent(context)
        alarmManager.cancel(pendingIntent)
        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pendingIntent
            )
        } catch (_: SecurityException) {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pendingIntent
            )
        }
    }

    private fun cancelScheduledUpdate(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        alarmManager.cancel(updatePendingIntent(context))
    }

    private fun calendarPendingIntent(context: Context): PendingIntent {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_APP_CALENDAR)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return PendingIntent.getActivity(
            context,
            REQUEST_CALENDAR,
            intent,
            pendingIntentFlags
        )
    }

    private fun clockPendingIntent(context: Context): PendingIntent {
        // Пробуем launcher intent для Samsung Clock (обходит AppsFilter блокировку)
        var intent: Intent? = context.packageManager.getLaunchIntentForPackage("com.sec.android.app.clockpackage")
        
        if (intent != null) {
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        } else {
            // Fallback: стандартный intent без явного пакета
            intent = Intent(android.provider.AlarmClock.ACTION_SHOW_ALARMS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }

        return PendingIntent.getActivity(
            context,
            REQUEST_CLOCK,
            intent,
            pendingIntentFlags
        )
    }


    private fun updatePendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, DateWidgetProvider::class.java).apply {
            action = ACTION_FORCE_UPDATE
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_UPDATE,
            intent,
            pendingIntentFlags
        )
    }

    companion object {
        private const val ACTION_FORCE_UPDATE =
            "com.dedm.batterywidget.widget.action.DATE_WIDGET_FORCE_UPDATE"

        private const val REQUEST_CALENDAR = 201
        private const val REQUEST_CLOCK = 202
        private const val REQUEST_UPDATE = 203

        private val timeRelatedActions = setOf(
            ACTION_FORCE_UPDATE,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_DATE_CHANGED
        )

        private val pendingIntentFlags: Int = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0

        private val locale: Locale = Locale.getDefault()
        private val dateFormatter: DateTimeFormatter =
            DateTimeFormatter.ofPattern("d MMMM yyyy", locale)
        private val timeFormatter: DateTimeFormatter =
            DateTimeFormatter.ofPattern("HH:mm", locale)
    }
}

