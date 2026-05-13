package com.smplio.gradle.build.insights.modules.load

import com.codahale.metrics.Gauge
import com.codahale.metrics.MetricRegistry
import java.lang.management.ManagementFactory
import java.time.Duration
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

sealed class SystemLoadMetric<T: Number>(val name: String, val metricProvider: () -> T): Gauge<T> {

    override fun getValue(): T = metricProvider()

    class SystemLoadAverageMetric : SystemLoadMetric<Double>("systemLoadAverage", {
        val osBean = ManagementFactory.getOperatingSystemMXBean()
        osBean.systemLoadAverage
    })

    class HeapMaxMetric : SystemLoadMetric<Long>("heapMax", {
        val memoryBean = ManagementFactory.getMemoryMXBean()
        memoryBean.heapMemoryUsage.max
    })

    class HeapUsedMetric : SystemLoadMetric<Long>("heapUsed", {
        val memoryBean = ManagementFactory.getMemoryMXBean()
        memoryBean.heapMemoryUsage.used
    })

    // CPU usage (percent) for everything running inside the current Gradle JVM (100% equals one fully utilized core)
    class GradleJvmCpuPercentMetric : SystemLoadMetric<Double>("gradleJvmCpuPercent", {
        CpuLoadSampler.instance.sampleJvmProcessCpuPercent()
    })

    // CPU usage (percent) aggregated across all descendant processes started by Gradle that run outside this JVM (100% equals one fully utilized core)
    class GradleDescendantsCpuPercentMetric : SystemLoadMetric<Double>("gradleDescendantsCpuPercent", {
        CpuLoadSampler.instance.sampleChildrenCpuPercent()
    })

    /**
     * Manages CPU sampling for both the Gradle JVM process and its descendants.
     *
     * Descendants are sampled at a higher frequency (every 500 ms) on a dedicated background
     * thread so that short-lived child processes (Kotlin/Java compiler daemons, workers, etc.)
     * are not missed between the 5-second reporting ticks.  Each time the main reporter calls
     * [sampleChildrenCpuPercent] it receives the **average** of all sub-samples collected since
     * the previous call, then the buffer is cleared for the next window.
     */
    class CpuLoadSampler private constructor() {

        private val processors: Int = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)

        // ---- JVM process sampling state ----
        @Volatile private var lastWallTimeJvmNanos: Long = System.nanoTime()
        @Volatile private var lastCpuTimeJvmNanos: Long = currentJvmProcessCpuTimeNanos()

        // ---- High-frequency children sampling (500 ms) ----
        // Each background tick appends one Double (CPU %) to this list.
        // sampleChildrenCpuPercent() drains and averages the list every ~5 s.
        private val childrenSamples: CopyOnWriteArrayList<Double> = CopyOnWriteArrayList()

        // Per-PID snapshot used by the background poller between its own ticks.
        private var pollerLastWallNanos: Long = System.nanoTime()
        private var pollerLastSnapshot: MutableMap<Long, Long> = snapshotChildrenCpu()
        private val pollerLock = Any()

        private val poller: ScheduledExecutorService =
            Executors.newSingleThreadScheduledExecutor { r ->
                Thread(r, "gradle-insights-children-cpu-poller").also { it.isDaemon = true }
            }

        init {
            poller.scheduleAtFixedRate(::pollChildren, 500, 500, TimeUnit.MILLISECONDS)
        }

        // ---- Public API ----

        @Synchronized
        fun sampleJvmProcessCpuPercent(): Double {
            val now = System.nanoTime()
            val cpuNow = currentJvmProcessCpuTimeNanos()
            val deltaCpu = (cpuNow - lastCpuTimeJvmNanos).coerceAtLeast(0L)
            val deltaWall = (now - lastWallTimeJvmNanos).coerceAtLeast(1L)
            lastCpuTimeJvmNanos = cpuNow
            lastWallTimeJvmNanos = now
            val pct = (deltaCpu.toDouble() / deltaWall.toDouble()) * 100.0
            return pct.coerceIn(0.0, 100.0 * processors)
        }

        /**
         * Returns the average children CPU % across all 500 ms sub-samples collected since
         * the previous call to this method.  The sample buffer is atomically drained on each
         * call so there is no double-counting between 5-second reporting windows.
         */
        fun sampleChildrenCpuPercent(): Double {
            // Atomically drain the buffer accumulated by the background poller.
            val drained = mutableListOf<Double>()
            val iter = childrenSamples.iterator()
            while (iter.hasNext()) drained.add(iter.next())
            childrenSamples.removeAll(drained.toSet())

            if (drained.isEmpty()) return 0.0
            return drained.average().coerceIn(0.0, 100.0 * processors)
        }

        fun shutdown() {
            poller.shutdown()
            try {
                poller.awaitTermination(2, TimeUnit.SECONDS)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }

        // ---- Background poller (runs every 500 ms) ----

        private fun pollChildren() {
            val pct = synchronized(pollerLock) {
                val now = System.nanoTime()
                val deltaWall = (now - pollerLastWallNanos).coerceAtLeast(1L)
                pollerLastWallNanos = now

                val currentSnapshot = snapshotChildrenCpu()

                var deltaCpu = 0L
                for ((pid, cpuNow) in currentSnapshot) {
                    val cpuBefore = pollerLastSnapshot[pid] ?: 0L
                    val d = cpuNow - cpuBefore
                    if (d > 0L) deltaCpu += d
                }
                pollerLastSnapshot = currentSnapshot

                (deltaCpu.toDouble() / deltaWall.toDouble()) * 100.0
            }
            childrenSamples.add(pct.coerceIn(0.0, 100.0 * processors))
        }

        // ---- Helpers ----

        private fun currentJvmProcessCpuTimeNanos(): Long {
            // Prefer com.sun.management.OperatingSystemMXBean for precise JVM CPU time
            val osBean = ManagementFactory.getOperatingSystemMXBean()
            val cpuTimeFromOsBean = try {
                val sunBean = osBean as? com.sun.management.OperatingSystemMXBean
                sunBean?.processCpuTime ?: -1L
            } catch (_: Throwable) {
                -1L
            }
            if (cpuTimeFromOsBean >= 0L) return cpuTimeFromOsBean

            // Fallback to ProcessHandle info
            return try {
                val duration: Duration? = ProcessHandle.current().info().totalCpuDuration().orElse(null)
                duration?.toNanos() ?: 0L
            } catch (_: Throwable) {
                0L
            }
        }

        /** Returns a map of PID → totalCpuDuration nanos for all current descendants. */
        private fun snapshotChildrenCpu(): MutableMap<Long, Long> {
            val snapshot = mutableMapOf<Long, Long>()
            try {
                ProcessHandle.current().descendants().forEach { descendant ->
                    try {
                        val d: Duration? = descendant.info().totalCpuDuration().orElse(null)
                        if (d != null) {
                            snapshot[descendant.pid()] = d.toNanos()
                        }
                    } catch (_: Throwable) {
                        // Process may have exited between enumeration and inspection — ignore.
                    }
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
            return snapshot
        }

        companion object {
            val instance: CpuLoadSampler by lazy { CpuLoadSampler() }
        }
    }
}

fun <T: Number> MetricRegistry.register(metric: SystemLoadMetric<T>) {
    register(metric.name, metric)
}
