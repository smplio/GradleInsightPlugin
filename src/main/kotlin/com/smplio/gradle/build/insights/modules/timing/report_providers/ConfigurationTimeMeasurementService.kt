package com.smplio.gradle.build.insights.modules.timing.report_providers

import com.smplio.gradle.build.insights.modules.timing.models.ConfigurationInfo
import com.smplio.gradle.build.insights.modules.timing.models.Measured
import com.smplio.gradle.build.insights.modules.timing.report.ConfigurationTimeReport
import com.smplio.gradle.build.insights.report.timing.IConfigurationTimeReportProvider
import org.gradle.api.Project
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

abstract class ConfigurationTimeMeasurementService : BuildService<BuildServiceParameters.None>,
    IConfigurationTimeReportProvider,
    OperationCompletionListener
{
    private val configurationStartTimes = ConcurrentHashMap<String, Long>()
    private val configurationTimeline = ConcurrentLinkedQueue<Measured<ConfigurationInfo>>()

    fun onBeforeProject(project: Project) {
        configurationStartTimes[project.displayName] = System.currentTimeMillis()
    }

    fun onAfterProject(project: Project) {
        val startTime = configurationStartTimes[project.displayName] ?: return
        configurationTimeline.add(Measured(
            measuredInstance = ConfigurationInfo(
                projectName = project.path,
            ),
            startTime = startTime,
            endTime = System.currentTimeMillis(),
        ))
    }

    override fun onFinish(event: FinishEvent) {}

    override fun provideConfigurationTimeReport(): ConfigurationTimeReport? {
        return configurationTimeline.toList()
    }
}