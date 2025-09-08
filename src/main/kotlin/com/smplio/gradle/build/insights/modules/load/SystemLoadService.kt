package com.smplio.gradle.build.insights.modules.load

import com.codahale.metrics.*
import com.smplio.gradle.build.insights.report.load.ISystemLoadReportProvider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener
import java.util.concurrent.TimeUnit

abstract class SystemLoadService: BuildService<BuildServiceParameters.None>,
    OperationCompletionListener,
    ISystemLoadReportProvider
{

    private val registry: MetricRegistry = MetricRegistry()
    private val metricsReporter: LocalCacheReporter

    init {
        registry.register(SystemLoadMetric.SystemLoadAverageMetric())
        registry.register(SystemLoadMetric.HeapUsedMetric())
        registry.register(SystemLoadMetric.HeapMaxMetric())

        registry.register(SystemLoadMetric.GradleJvmCpuPercentMetric())
        registry.register(SystemLoadMetric.GradleDescendantsCpuPercentMetric())

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

    override fun provideSystemLoadReport(): SystemLoadReport? {
        return metricsReporter.provideSystemLoadReport()
    }
}