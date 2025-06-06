package com.smplio.gradle.build.insights

import com.smplio.gradle.build.insights.modules.graph.GraphBuilder
import com.smplio.gradle.build.insights.modules.load.SystemLoadModule
import com.smplio.gradle.build.insights.modules.timing.ExecutionTimeMeasurementModule
import com.smplio.gradle.build.insights.report.CompositeReportBuildService
import com.smplio.gradle.build.insights.report.impl.html.HTMLReporter
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

        val executionTimeMeasurementModule = ExecutionTimeMeasurementModule(
            project,
            registry,
            pluginConfig.getExecutionTimeMeasurementConfiguration(),
        )
        executionTimeMeasurementModule.initialize()

        val systemLoadModule = SystemLoadModule(
            project,
            registry,
        )
        systemLoadModule.initialize()

        val compositeReportBuildService = project.gradle.sharedServices.registerIfAbsent(
            CompositeReportBuildService::class.java.simpleName,
            CompositeReportBuildService::class.java,
        ) { buildServiceSpec ->
            buildServiceSpec.parameters.reporters.set(mutableListOf(
                pluginConfig.getExecutionTimeMeasurementConfiguration().executionTimeReporter.get(),
            ).also { list ->
                if (pluginConfig.gatherHtmlReport.get()) {
                    list.add(HTMLReporter(project))
                }
            })
            executionTimeMeasurementModule.getConfigurationTimeReportProvider().let {
                buildServiceSpec.parameters.configurationTimeReportProvider.set(it)
            }
            executionTimeMeasurementModule.getExecutionTimeReportProvider()?.let {
                buildServiceSpec.parameters.executionTimeReportService.set(it)
            }
            systemLoadModule.getSystemLoadReportProvider()?.let {
                buildServiceSpec.parameters.systemLoadReportService.set(it)
            }
        }
        registry.onTaskCompletion(compositeReportBuildService)

        GraphBuilder().also {
            it.buildProjectDependencyGraph(project)
        }
    }
}
