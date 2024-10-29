package com.smplio.gradle.build.insights.modules.timing

import com.smplio.gradle.build.insights.modules.timing.report.*
import org.gradle.StartParameter
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener
import org.gradle.tooling.events.task.TaskFailureResult
import org.gradle.tooling.events.task.TaskFinishEvent
import org.gradle.tooling.events.task.TaskSkippedResult
import org.gradle.tooling.events.task.TaskSuccessResult
import java.io.Serializable

abstract class ExecutionTimeMeasurementService : BuildService<ExecutionTimeMeasurementService.Parameters>,
    OperationCompletionListener,
    AutoCloseable
{

    interface Parameters: BuildServiceParameters {
        val startParameters: Property<SerializableStartParameter>
        val reporter: Property<IExecutionTimeReporter>
        val configurationStartTime: Property<Long>
        val configurationEndTime: Property<Long>
    }

    private var buildStartTime: Long? = null
    private val taskExecutionReports: MutableList<TaskExecutionStats> = mutableListOf()

    override fun onFinish(event: FinishEvent) {
        if (event is TaskFinishEvent) {
            val result = event.result

            buildStartTime = buildStartTime?.let {
                if(it > result.startTime) {
                    result.startTime
                } else { it }
            } ?: result.startTime

            taskExecutionReports.add(TaskExecutionStats(
                event.descriptor.taskPath,
                when (result) {
                    is TaskSuccessResult -> ExecutionStatus.SUCCESS
                    is TaskSkippedResult -> ExecutionStatus.SKIPPED
                    is TaskFailureResult -> ExecutionStatus.FAILED
                    else -> ExecutionStatus.UNKNOWN
                },
                when (result) {
                    is TaskSuccessResult -> {
                        when {
                            result.isFromCache -> "FROM-CACHE"
                            result.isUpToDate -> "UP-TO-DATE"
                            else -> result.executionReasons?.joinToString() ?: "EXECUTED"
                        }
                    }
                    is TaskSkippedResult -> {
                        result.skipMessage
                    }
                    is TaskFailureResult -> {
                        result.failures.joinToString("\n\n") {
                            "${it.message}\n${it.description}\n${it.causes}"
                        }
                    }
                    else -> "UNKNOWN"
                },
                DurationReport(result.startTime, result.endTime),
            ))
        }
    }

    override fun close() {
        val lastExecutedTaskEndTime = taskExecutionReports.maxOf { it.duration.endTime }
        val report = ExecutionTimeReport(
            parameters.startParameters.get().taskNames,
            parameters.configurationStartTime.get(),
            DurationReport(parameters.configurationStartTime.get(), parameters.configurationEndTime.get()),
            DurationReport(buildStartTime ?: -1, lastExecutedTaskEndTime),
            taskExecutionReports,
        )
        parameters.reporter.get().reportExecutionTime(report)
    }

    class SerializableStartParameter(item: StartParameter) : Serializable {
        val taskNames: List<String> = item.taskNames
    }
}