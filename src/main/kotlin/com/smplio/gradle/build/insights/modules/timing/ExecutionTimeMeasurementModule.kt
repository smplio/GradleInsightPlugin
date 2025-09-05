package com.smplio.gradle.build.insights.modules.timing

import com.smplio.gradle.build.insights.report.timing.IConfigurationTimeReportProvider
import org.gradle.api.Project
import org.gradle.build.event.BuildEventsListenerRegistry
import com.smplio.gradle.build.insights.modules.timing.report_providers.ConfigurationTimeReportProvider
import com.smplio.gradle.build.insights.modules.timing.report_providers.TaskExecutionTimeMeasurementService
import org.gradle.api.provider.Provider

class ExecutionTimeMeasurementModule(
    private val project: Project,
    private val registry: BuildEventsListenerRegistry,
    private val configuration: ExecutionTimeMeasurementConfiguration,
) {

    private var taskExecutionTimeMeasurementService: Provider<TaskExecutionTimeMeasurementService>? = null
    private val configurationTimeReportProvider = ConfigurationTimeReportProvider()

    fun initialize() {
        val buildStartTime = System.currentTimeMillis()

        project.gradle.beforeProject {
            configurationTimeReportProvider.onBeforeProject(it)
        }

        project.gradle.afterProject {
            configurationTimeReportProvider.onAfterProject(it)
        }

        if (configuration.enabled.get()) {
            taskExecutionTimeMeasurementService = project.gradle.sharedServices.registerIfAbsent(
                TaskExecutionTimeMeasurementService::class.java.simpleName,
                TaskExecutionTimeMeasurementService::class.java,
            ) {
                it.parameters.buildStartTime.set(buildStartTime)
            }.also {
                registry.onTaskCompletion(it)
            }
        }
    }

    fun getConfigurationTimeReportProvider(): IConfigurationTimeReportProvider {
        return configurationTimeReportProvider
    }

    fun getExecutionTimeReportProvider(): Provider<TaskExecutionTimeMeasurementService>? {
        return taskExecutionTimeMeasurementService
    }
}
