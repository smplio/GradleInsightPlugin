package com.smplio.gradle.build.insights.modules.timing.report_providers

import com.smplio.gradle.build.insights.modules.timing.models.ConfigurationInfo
import com.smplio.gradle.build.insights.modules.timing.models.Measured
import com.smplio.gradle.build.insights.modules.timing.report.ConfigurationTimeReport
import com.smplio.gradle.build.insights.report.timing.IConfigurationTimeReportProvider
import org.gradle.api.Project
import java.util.concurrent.ConcurrentHashMap

class ConfigurationTimeReportProvider: IConfigurationTimeReportProvider {

    private val configurationStartTimes = ConcurrentHashMap<String, Long>()
    private val configurationTimeline = mutableListOf<Measured<ConfigurationInfo>>()

    override fun provideConfigurationTimeReport(): ConfigurationTimeReport? {
        return configurationTimeline
    }

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
}