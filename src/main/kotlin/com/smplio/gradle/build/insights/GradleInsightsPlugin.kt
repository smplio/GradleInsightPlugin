package com.smplio.gradle.build.insights

import com.smplio.gradle.build.insights.modules.graph.GraphBuilder
import com.smplio.gradle.build.insights.modules.timing.TimerService
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ReportingBasePlugin
import org.gradle.build.event.BuildEventsListenerRegistry
import javax.inject.Inject

@Suppress("unused")
class GradleInsightsPlugin @Inject constructor(private val registry: BuildEventsListenerRegistry): Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply(ReportingBasePlugin::class.java)

        val pluginConfig = project.extensions.create(
            GradleInsightsPluginExtension.EXTENSION_NAME,
            GradleInsightsPluginExtension::class.java,
            project,
        )
        val timeMeasurementEnabled = pluginConfig.getExecutionTimeMeasurementConfiguration().enabled.get()

        val sharedServices = project.gradle.sharedServices
        val timerService = sharedServices.registerIfAbsent(TimerService::class.java.simpleName, TimerService::class.java) {}

        if (timeMeasurementEnabled) {
            project.gradle.taskGraph.whenReady {
                registry.onTaskCompletion(timerService)
            }
        }

        GraphBuilder().also {
            if (timeMeasurementEnabled) {
                it.buildTaskDependencyGraph(project, project.gradle.startParameter.taskNames)
            }
            it.buildProjectDependencyGraph(project, listOf("implementation", "api"))
        }
    }
}
