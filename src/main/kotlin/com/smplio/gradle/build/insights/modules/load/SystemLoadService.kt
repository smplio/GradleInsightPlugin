package com.smplio.gradle.build.insights.modules.load

import com.codahale.metrics.*
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener
import java.util.concurrent.TimeUnit

abstract class SystemLoadService: BuildService<BuildServiceParameters.None>, OperationCompletionListener, AutoCloseable {

    private val registry: MetricRegistry = MetricRegistry()
    private val reporter: Reporter

    init {
        registry.register(SystemLoadMetric.SystemLoadAverageMetric())
        registry.register(SystemLoadMetric.HeapUsedMetric())
        registry.register(SystemLoadMetric.HeapMaxMetric())

        reporter = ConsoleReporter.forRegistry(registry)
            .convertRatesTo(TimeUnit.SECONDS)
            .convertDurationsTo(TimeUnit.MILLISECONDS)
            .build()
        reporter.start(5, TimeUnit.SECONDS);
    }

    override fun onFinish(event: FinishEvent?) {}

    override fun close() {
        println("Closing service")
        reporter.close()
    }
}