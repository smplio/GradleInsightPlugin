package com.smplio.gradle.build.insights

import com.smplio.gradle.build.insights.modules.graph.GraphBuilder
import com.smplio.gradle.build.insights.modules.load.SystemLoadModule
import com.smplio.gradle.build.insights.modules.timing.ExecutionTimeMeasurementModule
import com.smplio.gradle.build.insights.reporters.CompositeReporter
import com.smplio.gradle.build.insights.reporters.html.HTMLReporter
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

        val vcsDataProvider = pluginConfig.vcsDataProvider.get()
        val executionTimeReporter = pluginConfig.getExecutionTimeMeasurementConfiguration().executionTimeReporter.get()
        val compositeReporter = CompositeReporter(
            if (pluginConfig.gatherHtmlReport.get()) {
                listOf(
                    HTMLReporter(
                        project,
                        vcsDataProvider,
                    ),
                    executionTimeReporter,
                )
            } else {
                listOf(
                    executionTimeReporter,
                )
            }
        )

        ExecutionTimeMeasurementModule(
            project,
            registry,
            pluginConfig.getExecutionTimeMeasurementConfiguration(),
            compositeReporter,
        ).initialize()

        SystemLoadModule(
            project,
            registry,
            compositeReporter,
        ).initialize()


        GraphBuilder().also {
            it.buildProjectDependencyGraph(project, listOf("implementation", "api"))
        }
    }
}
