package com.smplio.gradle.build.insights.modules.load

import com.codahale.metrics.Gauge
import com.codahale.metrics.MetricRegistry
import java.lang.management.ManagementFactory
import java.time.Duration

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
        CpuLoadSampler.sampleJvmProcessCpuPercent()
    })

    // CPU usage (percent) aggregated across all descendant processes started by Gradle that run outside this JVM (100% equals one fully utilized core)
    class GradleDescendantsCpuPercentMetric : SystemLoadMetric<Double>("gradleDescendantsCpuPercent", {
        CpuLoadSampler.sampleChildrenCpuPercent()
    })

    private object CpuLoadSampler {
        private val processors: Int = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)

        // JVM process sampling state
        private var lastWallTimeJvmNanos: Long = System.nanoTime()
        private var lastCpuTimeJvmNanos: Long = currentJvmProcessCpuTimeNanos()

        // Children sampling state
        private var lastWallTimeChildrenNanos: Long = System.nanoTime()
        private var lastCpuTimeChildrenNanos: Long = currentChildrenCpuTimeNanos()

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

        @Synchronized
        fun sampleChildrenCpuPercent(): Double {
            val now = System.nanoTime()
            val cpuNow = currentChildrenCpuTimeNanos()
            val deltaCpu = (cpuNow - lastCpuTimeChildrenNanos).coerceAtLeast(0L)
            val deltaWall = (now - lastWallTimeChildrenNanos).coerceAtLeast(1L)
            lastCpuTimeChildrenNanos = cpuNow
            lastWallTimeChildrenNanos = now
            val pct = (deltaCpu.toDouble() / deltaWall.toDouble()) * 100.0
            // With 100% meaning one full core, allow up to cores*100%
            return pct.coerceIn(0.0, 100.0 * processors)
        }

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

        private fun currentChildrenCpuTimeNanos(): Long {
            return try {
                var sum = 0L
                val current = ProcessHandle.current()
                // Use descendants to include nested children
                current.descendants().forEach { descendant ->
                    try {
                        val d: Duration? = descendant.info().totalCpuDuration().orElse(null)
                        if (d != null) {
                            sum += d.toNanos()
                        }
                    } catch (e: Throwable) {
                        // ignore processes we cannot inspect
                        e.printStackTrace()
                    }
                }
                sum
            } catch (e: Throwable) {
                e.printStackTrace()
                0L
            }
        }
    }
}

fun <T: Number> MetricRegistry.register(metric: SystemLoadMetric<T>) {
    register(metric.name, metric)
}