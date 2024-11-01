package com.smplio.gradle.build.insights.modules.load

import com.codahale.metrics.*
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener
import java.util.concurrent.TimeUnit

abstract class SystemLoadService: BuildService<BuildServiceParameters.None>,
    OperationCompletionListener,
    AutoCloseable
{

    private val registry: MetricRegistry = MetricRegistry()
    private val metricsReporter: LocalCacheReporter
    var reporter: ISystemLoadReporter? = null

    init {
        registry.register(SystemLoadMetric.SystemLoadAverageMetric())
        registry.register(SystemLoadMetric.HeapUsedMetric())
        registry.register(SystemLoadMetric.HeapMaxMetric())

        metricsReporter = LocalCacheReporter(
            registry,
            "LocalCacheRegistry",
            MetricFilter.ALL,
            TimeUnit.SECONDS,
            TimeUnit.MILLISECONDS,
        )
        metricsReporter.start(0, 5, TimeUnit.SECONDS)
    }

    override fun onFinish(event: FinishEvent?) {}

    override fun close() {
        reporter?.let {
            metricsReporter.close(it)
        } ?: kotlin.run {
            metricsReporter.close()
        }
    }
}