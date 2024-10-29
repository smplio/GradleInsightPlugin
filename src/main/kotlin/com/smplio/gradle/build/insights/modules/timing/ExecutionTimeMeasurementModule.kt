package com.smplio.gradle.build.insights.modules.timing

import com.smplio.gradle.build.insights.modules.graph.GraphBuilder
import com.smplio.gradle.build.insights.modules.timing.report.IExecutionTimeReporter
import org.gradle.api.Project
import org.gradle.build.event.BuildEventsListenerRegistry

class ExecutionTimeMeasurementModule(
    private val project: Project,
    private val registry: BuildEventsListenerRegistry,
    private val configuration: ExecutionTimeMeasurementConfiguration,
    private val reporter: IExecutionTimeReporter,
) {
    fun initialize() {
        val enabled = configuration.enabled.get()

        if (enabled) {
            var configurationStartTime: Long = -1
            project.gradle.beforeProject {
                if (configurationStartTime == -1L) {
                    configurationStartTime = System.currentTimeMillis()
                }
            }

            project.gradle.taskGraph.whenReady {
                val configurationEndTime = System.currentTimeMillis()

                val sharedServices = project.gradle.sharedServices
                val timerService = sharedServices.registerIfAbsent(
                    ExecutionTimeMeasurementService::class.java.simpleName,
                    ExecutionTimeMeasurementService::class.java,
                ) {
                    it.parameters.startParameters.set(ExecutionTimeMeasurementService.SerializableStartParameter(project.gradle.startParameter))
                    it.parameters.reporter.set(reporter)
                    it.parameters.configurationStartTime.set(configurationStartTime)
                    it.parameters.configurationEndTime.set(configurationEndTime)
                }
                registry.onTaskCompletion(timerService)
            }
            GraphBuilder().buildTaskDependencyGraph(project, project.gradle.startParameter.taskNames)
        }
    }
}