package com.dedm.batterywidget.metrics

import android.content.Context
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

class MetricRepository(
    providers: Collection<MetricProvider>
) {

    private val providersByType: Map<MetricType, MetricProvider> =
        providers.associateBy { it.type }

    private val cachedSnapshots: MutableMap<MetricType, CachedSnapshot> =
        ConcurrentHashMap()

    suspend fun loadSnapshots(context: Context): List<MetricSnapshot> =
        providersByType.values.mapNotNull { provider ->
            val cached = cachedSnapshots[provider.type]
            val now = Instant.now()

            if (cached != null && !shouldRefresh(provider, cached, now)) {
                cached.snapshot
            } else {
                val fresh = provider.getSnapshot(context, cached?.snapshot)
                if (fresh != null) {
                    cachedSnapshots[provider.type] = CachedSnapshot(fresh, now)
                }
                fresh
            }
        }

    fun getCachedSnapshot(type: MetricType): MetricSnapshot? =
        cachedSnapshots[type]?.snapshot

    fun updateCachedSnapshot(snapshot: MetricSnapshot) {
        cachedSnapshots[snapshot.type] = CachedSnapshot(snapshot, Instant.now())
    }

    private fun shouldRefresh(
        provider: MetricProvider,
        cachedSnapshot: CachedSnapshot,
        now: Instant
    ): Boolean {
        val elapsed = Duration.between(cachedSnapshot.fetchedAt, now)
        return elapsed >= provider.minUpdateInterval
    }

    private data class CachedSnapshot(
        val snapshot: MetricSnapshot,
        val fetchedAt: Instant
    )
}


