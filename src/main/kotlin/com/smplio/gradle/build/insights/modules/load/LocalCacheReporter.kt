package com.smplio.gradle.build.insights.modules.load

import com.codahale.metrics.*
import com.codahale.metrics.Timer
import java.util.SortedMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit

class LocalCacheReporter(
    registry: MetricRegistry,
    name: String,
    filter: MetricFilter,
    rateUnit: TimeUnit,
    durationUnit: TimeUnit,
): ScheduledReporter(
    registry,
    name,
    filter,
    rateUnit,
    durationUnit,
), ISystemLoadReportProvider {
    private val measurements: ConcurrentLinkedQueue<Pair<Long, List<Pair<String, Number>>>> = ConcurrentLinkedQueue()

    override fun report(
        gauges: SortedMap<String, Gauge<Any>>?,
        counters: SortedMap<String, Counter>?,
        histograms: SortedMap<String, Histogram>?,
        meters: SortedMap<String, Meter>?,
        timers: SortedMap<String, Timer>?
    ) {
        val measurementTime = System.currentTimeMillis()

        if (gauges == null) return

        val measurementsValues = mutableListOf<Pair<String, Number>>()
        for (gauge in gauges) {
            val gaugeValue = gauge.value.value
            if (gaugeValue is Number) {
                measurementsValues.add(gauge.key to gaugeValue)
            }
        }
        measurements.add(measurementTime to measurementsValues)
    }

    override fun provideSystemLoadReport(): SystemLoadReport? {
        super.close()
        return measurements
    }
}