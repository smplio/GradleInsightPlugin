package com.smplio.gradle.build.insights.modules.timing

import com.smplio.gradle.build.insights.reporters.CompositeReporter
import com.smplio.gradle.build.insights.reporters.html.HTMLReporter
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.build.event.BuildEventsListenerRegistry
import com.smplio.gradle.build.insights.modules.timing.ExecutionTimeMeasurementService.SerializableStartParameter

class ExecutionTimeMeasurementModule(
    private val project: Project,
    private val registry: BuildEventsListenerRegistry,
    private val configuration: ExecutionTimeMeasurementConfiguration,
    private val gatherHtmlReport: Property<Boolean>,
) {
    fun initialize() {
        var configurationStartTime: Long = -1
        project.gradle.beforeProject {
            if (configurationStartTime == -1L) {
                configurationStartTime = System.currentTimeMillis()
            }
        }

        project.gradle.taskGraph.whenReady {
            val configurationEndTime = System.currentTimeMillis()

            val default = CompositeReporter(mutableListOf(
                configuration.executionTimeReporter.get(),
            ).also { list ->
                if (gatherHtmlReport.get()) {
                    list.add(HTMLReporter(project))
                }
            })

            if (configuration.enabled.get()) {

                val startParameter = SerializableStartParameter.create(
                    startParameter = project.gradle.startParameter,
                    taskExecutionGraph = it,
                )

                val sharedServices = project.gradle.sharedServices
                val timerService = sharedServices.registerIfAbsent(
                    ExecutionTimeMeasurementService::class.java.simpleName,
                    ExecutionTimeMeasurementService::class.java,
                ) {
                    it.parameters.startParameters.set(startParameter)
                    it.parameters.reporter.set(default)
                    it.parameters.configurationStartTime.set(configurationStartTime)
                    it.parameters.configurationEndTime.set(configurationEndTime)
                }
                registry.onTaskCompletion(timerService)
            }
        }
    }
}
