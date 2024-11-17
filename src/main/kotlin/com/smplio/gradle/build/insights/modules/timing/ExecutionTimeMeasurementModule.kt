package com.smplio.gradle.build.insights.modules.timing

import com.smplio.gradle.build.insights.reporters.CompositeReporter
import com.smplio.gradle.build.insights.reporters.html.HTMLReporter
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.build.event.BuildEventsListenerRegistry
import com.smplio.gradle.build.insights.modules.timing.ExecutionTimeMeasurementService.SerializableStartParameter
import com.smplio.gradle.build.insights.modules.timing.models.ConfigurationInfo
import com.smplio.gradle.build.insights.modules.timing.models.Measured
import java.util.concurrent.ConcurrentHashMap

class ExecutionTimeMeasurementModule(
    private val project: Project,
    private val registry: BuildEventsListenerRegistry,
    private val configuration: ExecutionTimeMeasurementConfiguration,
    private val gatherHtmlReport: Property<Boolean>,
) {
    fun initialize() {
        val buildStartTime = System.currentTimeMillis()

        val configurationStartTimes = ConcurrentHashMap<String, Long>()

        val configurationTimeline = mutableListOf<Measured<ConfigurationInfo>>()

        project.gradle.beforeProject {
            configurationStartTimes[it.displayName] = System.currentTimeMillis()
        }

        project.gradle.afterProject {
            val startTime = configurationStartTimes[it.displayName] ?: return@afterProject
            configurationTimeline.add(Measured(
                measuredInstance = ConfigurationInfo(
                    projectName = it.path,
                ),
                startTime = startTime,
                endTime = System.currentTimeMillis(),
            ))
        }

        project.gradle.taskGraph.whenReady {
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
                    it.parameters.buildStartTime.set(buildStartTime)
                    it.parameters.configurationsTimeline.set(configurationTimeline)
                }
                registry.onTaskCompletion(timerService)
            }
        }
    }
}
