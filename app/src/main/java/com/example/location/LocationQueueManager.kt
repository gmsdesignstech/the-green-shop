package com.example.location

import java.util.concurrent.ConcurrentLinkedDeque

/**
 * Manages an in-memory queue of recorded GPS location updates for delivery tracking.
 * Provides offline resilience: when network is unavailable, locations are securely buffered
 * and can be uploaded once connectivity returns.
 *
 * Implements a strict retention policy (max items and age threshold) to prevent
 * memory buildup and comply with privacy guidelines.
 */
class LocationQueueManager(
    private val maxCapacity: Int = 300,
    private val maxRetentionAgeMillis: Long = 24 * 60 * 60 * 1000L // 24 hours
) {
    private val queue = ConcurrentLinkedDeque<LocationData>()

    /**
     * Enqueues a location while preventing immediate duplicates
     * and enforcing max capacity constraints.
     */
    fun enqueue(location: LocationData): Boolean {
        if (!location.isValid()) return false

        // Deduplication: if the last recorded point is virtually identical and recent, skip
        val last = queue.peekLast()
        if (last != null) {
            val isSameCoordinate = kotlin.math.abs(last.latitude - location.latitude) < 0.00001 &&
                    kotlin.math.abs(last.longitude - location.longitude) < 0.00001
            val timeDiff = location.timestamp - last.timestamp
            if (isSameCoordinate && timeDiff < 3_000L) {
                return false
            }
        }

        // Add to queue
        queue.addLast(location)

        // Prune if over capacity
        while (queue.size > maxCapacity) {
            queue.pollFirst()
        }

        pruneStale()
        return true
    }

    /**
     * Returns an unmodifiable snapshot of currently pending queued locations.
     */
    fun getPendingLocations(): List<LocationData> {
        pruneStale()
        return queue.toList()
    }

    /**
     * Removes and returns up to [batchSize] locations for transmission to backend.
     */
    fun drainBatch(batchSize: Int = 50): List<LocationData> {
        pruneStale()
        val batch = mutableListOf<LocationData>()
        while (batch.size < batchSize && queue.isNotEmpty()) {
            val item = queue.pollFirst() ?: break
            batch.add(item)
        }
        return batch
    }

    /**
     * Clears all queued locations.
     */
    fun clear() {
        queue.clear()
    }

    /**
     * Current size of the queue.
     */
    val size: Int
        get() = queue.size

    /**
     * Checks if the queue is empty.
     */
    val isEmpty: Boolean
        get() = queue.isEmpty()

    /**
     * Discards points older than [maxRetentionAgeMillis].
     */
    fun pruneStale() {
        val now = System.currentTimeMillis()
        val cutoff = now - maxRetentionAgeMillis

        val iterator = queue.iterator()
        while (iterator.hasNext()) {
            val loc = iterator.next()
            if (loc.timestamp < cutoff) {
                iterator.remove()
            }
        }
    }
}
