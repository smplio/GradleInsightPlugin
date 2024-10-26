package com.smplio.gradle.build.insights

import com.smplio.gradle.build.insights.modules.graph.GraphBuilder
import com.smplio.gradle.build.insights.modules.timing.ExecutionTimeMeasurementModule
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

        ExecutionTimeMeasurementModule(
            project,
            registry,
            pluginConfig.getExecutionTimeMeasurementConfiguration(),
        ).initialize()


        GraphBuilder().also {
            it.buildProjectDependencyGraph(project, listOf("implementation", "api"))
        }
    }
}
