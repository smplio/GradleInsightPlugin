package com.smplio.gradle.build.insights.modules.load

import com.codahale.metrics.Gauge
import com.codahale.metrics.MetricRegistry
import java.lang.management.ManagementFactory

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
}

fun <T: Number> MetricRegistry.register(metric: SystemLoadMetric<T>) {
    register(metric.name, metric)
}