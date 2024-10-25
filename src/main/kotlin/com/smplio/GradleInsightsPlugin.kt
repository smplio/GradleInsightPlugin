package com.smplio

import com.smplio.modules.graph.GraphBuilder
import com.smplio.modules.timing.TimerService
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ReportingBasePlugin
import org.gradle.build.event.BuildEventsListenerRegistry
import javax.inject.Inject

@Suppress("unused")
class GradleInsightsPlugin @Inject constructor(private val registry: BuildEventsListenerRegistry): Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply(ReportingBasePlugin::class.java)

        val sharedServices = project.gradle.sharedServices

        val timerService = sharedServices.registerIfAbsent(TimerService::class.java.simpleName, TimerService::class.java) {}
        project.gradle.taskGraph.whenReady {
            registry.onTaskCompletion(timerService)
        }

        GraphBuilder().buildTaskDependencyGraph(project, project.gradle.startParameter.taskNames)
    }
}
