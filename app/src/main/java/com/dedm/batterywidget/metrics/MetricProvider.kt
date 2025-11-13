package com.dedm.batterywidget.metrics

import android.content.Context
import java.time.Duration

interface MetricProvider {
    val type: MetricType

    /**
     * Минимальный интервал между последовательными запросами данных у конкретного провайдера.
     * Репозиторий обязан уважать ограничение, чтобы не нагружать систему частыми опросами.
     */
    val minUpdateInterval: Duration

    /**
     * Выполняет запрос текущего значения метрики.
     *
     * @param context контекст приложения.
     * @param previousSnapshot последнее валидное значение, возвращенное провайдером, если доступно.
     *
     * @return актуальное значение метрики или null, если данные сейчас недоступны.
     */
    suspend fun getSnapshot(
        context: Context,
        previousSnapshot: MetricSnapshot?
    ): MetricSnapshot?
}


