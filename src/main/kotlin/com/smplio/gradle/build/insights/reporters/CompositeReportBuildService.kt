package com.smplio.gradle.build.insights.reporters

import com.smplio.gradle.build.insights.modules.load.ISystemLoadReporter
import com.smplio.gradle.build.insights.modules.timing.report.ExecutionTimeReport
import com.smplio.gradle.build.insights.modules.timing.report.IExecutionTimeReporter
import org.gradle.StartParameter
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.provider.ListProperty
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import java.io.Serializable
import java.util.concurrent.ConcurrentLinkedQueue

abstract class CompositeReportBuildService : BuildService<CompositeReportBuildService.Parameters>,
    ISystemLoadReporter,
    IExecutionTimeReporter,
    AutoCloseable
{

    interface Parameters: BuildServiceParameters {
        val reporters: ListProperty<IReporter>
    }

    override fun close() {
        submitReport()
    }

    override fun reportExecutionTime(executionTimeReport: ExecutionTimeReport) {
        parameters.reporters.orNull?.filterIsInstance<IExecutionTimeReporter>()?.forEach { reporter ->
            reporter.reportExecutionTime(executionTimeReport)
        }
    }

    override fun reportSystemLoad(measurements: ConcurrentLinkedQueue<Pair<Long, List<Pair<String, Number>>>>) {
        parameters.reporters.orNull?.filterIsInstance<ISystemLoadReporter>()?.forEach { reporter ->
            reporter.reportSystemLoad(measurements)
        }
    }

    override fun submitReport() {
        parameters.reporters.orNull?.forEach { reporter ->
            reporter.submitReport()
        }
    }

    class SerializableStartParameter private constructor(val taskNames: List<String>) : Serializable {
        companion object {
            fun create(
                startParameter: StartParameter,
                taskExecutionGraph: TaskExecutionGraph? = null,
            ): SerializableStartParameter {
                val taskNameToPathMapping = HashMap<String, String>()
                taskExecutionGraph?.allTasks?.forEach { taskNameToPathMapping[it.name] = it.path }
                val startTaskNames = startParameter.taskNames.map { taskName -> taskNameToPathMapping[taskName] ?: taskName }
                return SerializableStartParameter(
                    taskNames = startTaskNames,
                )
            }
        }
    }
}