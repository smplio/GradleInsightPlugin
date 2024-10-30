package com.smplio.gradle.build.insights.modules.load

import com.codahale.metrics.*
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener
import java.util.concurrent.TimeUnit

abstract class SystemLoadService: BuildService<SystemLoadService.Parameters>,
    OperationCompletionListener,
    AutoCloseable
{

    private val registry: MetricRegistry = MetricRegistry()
    private val reporter: LocalCacheReporter

    interface Parameters: BuildServiceParameters {
        val reporter: Property<ISystemLoadReporter>
    }

    init {
        registry.register(SystemLoadMetric.SystemLoadAverageMetric())
        registry.register(SystemLoadMetric.HeapUsedMetric())
        registry.register(SystemLoadMetric.HeapMaxMetric())

        reporter = LocalCacheReporter(
            registry,
            "LocalCacheRegistry",
            MetricFilter.ALL,
            TimeUnit.SECONDS,
            TimeUnit.MILLISECONDS,
        )
        reporter.start(0, 5, TimeUnit.SECONDS)
    }

    override fun onFinish(event: FinishEvent?) {}

    override fun close() {
        reporter.close(parameters.reporter.get())
    }
}