package com.smplio.gradle.build.insights.modules.timing

import com.smplio.gradle.build.insights.modules.timing.report_providers.ConfigurationTimeMeasurementService
import org.gradle.api.Project
import org.gradle.build.event.BuildEventsListenerRegistry
import com.smplio.gradle.build.insights.modules.timing.report_providers.TaskExecutionTimeMeasurementService
import org.gradle.api.provider.Provider

class ExecutionTimeMeasurementModule(
    private val project: Project,
    private val registry: BuildEventsListenerRegistry,
    private val configuration: ExecutionTimeMeasurementConfiguration,
) {

    private var configurationTimeMeasurementService: Provider<ConfigurationTimeMeasurementService>? = null
    private var taskExecutionTimeMeasurementService: Provider<TaskExecutionTimeMeasurementService>? = null

    fun initialize() {
        val buildStartTime = System.currentTimeMillis()

        if (configuration.enabled.get()) {
            configurationTimeMeasurementService = project.gradle.sharedServices.registerIfAbsent(
                ConfigurationTimeMeasurementService::class.java.simpleName,
                ConfigurationTimeMeasurementService::class.java,
            ) {}.also {
                registry.onTaskCompletion(it)
            }

            configurationTimeMeasurementService?.get()?.onBeforeProject(project)

            project.gradle.beforeProject {
                configurationTimeMeasurementService?.get()?.onBeforeProject(it)
            }
            project.gradle.afterProject {
                configurationTimeMeasurementService?.get()?.onAfterProject(it)
            }
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

    fun getConfigurationTimeTimeMeasurementService(): Provider<ConfigurationTimeMeasurementService>? {
        return configurationTimeMeasurementService
    }

    fun getExecutionTimeTimeMeasurementService(): Provider<TaskExecutionTimeMeasurementService>? {
        return taskExecutionTimeMeasurementService
    }
}
