package com.smplio.gradle.build.insights.modules.timing

import com.smplio.gradle.build.insights.modules.timing.report.BuildHostInfo
import com.smplio.gradle.build.insights.modules.timing.report.ExecutionStatus
import com.smplio.gradle.build.insights.modules.timing.report.ExecutionTimeReport
import com.smplio.gradle.build.insights.modules.timing.report.IExecutionTimeReporter
import com.smplio.gradle.build.insights.modules.timing.report.TaskExecutionStats
import com.smplio.gradle.build.insights.vcs.providers.GitDataProvider
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
import java.io.File
import java.io.Serializable
import java.time.Duration
import java.time.Instant

abstract class ExecutionTimeMeasurementService : BuildService<ExecutionTimeMeasurementService.Parameters>,
    OperationCompletionListener,
    AutoCloseable
{

    interface Parameters: BuildServiceParameters {
        val startParameters: Property<SerializableStartParameter>
        val projectDir: Property<File>
        val reporter: Property<IExecutionTimeReporter>
    }

    private var buildStartTime: Instant? = null
    private val taskExecutionReports: MutableList<TaskExecutionStats> = mutableListOf()

    override fun onFinish(event: FinishEvent) {
        if (event is TaskFinishEvent) {
            val result = event.result

            buildStartTime = buildStartTime?.let {
                if(it.toEpochMilli() > result.startTime) {
                    Instant.ofEpochMilli(result.startTime)
                } else { it }
            } ?: Instant.ofEpochMilli(result.startTime)

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
                result.startTime,
                result.endTime,
            ))
        }
    }

    override fun close() {
        val report = ExecutionTimeReport(
            parameters.startParameters.get().taskNames,
            GitDataProvider().get(parameters.projectDir.get()),
            BuildHostInfo(),
            Duration.between(
                buildStartTime,
                Instant.ofEpochMilli(taskExecutionReports.map { it.endTime }.max())
            ).toMillis(),
            taskExecutionReports,
        )
        parameters.reporter.get().processReport(report)
    }

    class SerializableStartParameter: Serializable {
        val taskNames: List<String>

        constructor(item: StartParameter) {
            taskNames = item.taskNames
        }
    }
}