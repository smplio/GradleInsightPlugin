package com.smplio.gradle.build.insights.modules.timing

import com.smplio.gradle.build.insights.modules.graph.GraphBuilder
import org.gradle.api.Project
import org.gradle.build.event.BuildEventsListenerRegistry

class ExecutionTimeMeasurementModule(
    private val project: Project,
    private val registry: BuildEventsListenerRegistry,
    private val configuration: ExecutionTimeMeasurementConfiguration,
) {
    fun initialize() {
        val enabled = configuration.enabled.get()

        val sharedServices = project.gradle.sharedServices
        val timerService = sharedServices.registerIfAbsent(
            ExecutionTimeMeasurementService::class.java.simpleName,
            ExecutionTimeMeasurementService::class.java,
        ) {
            it.parameters.startParameters.set(ExecutionTimeMeasurementService.SerializableStartParameter(project.gradle.startParameter))
            it.parameters.projectDir.set(project.projectDir)
        }

        if (enabled) {
            project.gradle.taskGraph.whenReady {
                registry.onTaskCompletion(timerService)
            }
            GraphBuilder().buildTaskDependencyGraph(project, project.gradle.startParameter.taskNames)
        }
    }
}