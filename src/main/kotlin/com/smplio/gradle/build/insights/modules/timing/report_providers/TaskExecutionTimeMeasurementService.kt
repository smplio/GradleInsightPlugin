package com.smplio.gradle.build.insights.modules.timing.report_providers

import com.smplio.gradle.build.insights.modules.timing.models.Measured
import com.smplio.gradle.build.insights.modules.timing.models.TaskInfo
import com.smplio.gradle.build.insights.modules.timing.report.TaskExecutionTimeReport
import com.smplio.gradle.build.insights.report.timing.ITaskExecutionTimeReportProvider
import com.smplio.gradle.build.insights.report.IReportProvider
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener
import org.gradle.tooling.events.task.TaskFailureResult
import org.gradle.tooling.events.task.TaskFinishEvent
import org.gradle.tooling.events.task.TaskSkippedResult
import org.gradle.tooling.events.task.TaskSuccessResult
import java.util.concurrent.ConcurrentLinkedQueue

abstract class TaskExecutionTimeMeasurementService : BuildService<TaskExecutionTimeMeasurementService.Parameters>,
    OperationCompletionListener,
    ITaskExecutionTimeReportProvider,
    IReportProvider
{

    interface Parameters: BuildServiceParameters {
        val buildStartTime: Property<Long>
    }

    private var firstTaskStartTime: Long? = null
    private var lastTaskEndTime: Long? = null
    private val taskExecutionTimeline: ConcurrentLinkedQueue<Measured<TaskInfo>> = ConcurrentLinkedQueue()

    override fun onFinish(event: FinishEvent) {
        if (event is TaskFinishEvent) {
            val result = event.result

            firstTaskStartTime = firstTaskStartTime?.let {
                if(it > result.startTime) {
                    result.startTime
                } else { it }
            } ?: result.startTime

            lastTaskEndTime = lastTaskEndTime?.let {
                if(it < result.endTime) {
                    result.endTime
                } else { it }
            } ?: result.endTime

            val taskResultDescription = when (result) {
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
                        "${it.message}\n${it.description}"
                    }
                }
                else -> ""
            }

            val taskStatus = when (result) {
                is TaskSuccessResult -> TaskInfo.ExecutionStatus.Success(taskResultDescription)
                is TaskSkippedResult -> TaskInfo.ExecutionStatus.Skipped(taskResultDescription)
                is TaskFailureResult -> TaskInfo.ExecutionStatus.Failed(taskResultDescription)
                else -> TaskInfo.ExecutionStatus.Unknown
            }

            val taskInfo = TaskInfo(
                name = event.descriptor.displayName,
                path = event.descriptor.taskPath,
                status = taskStatus,
            )

            taskExecutionTimeline.add(
                Measured(
                    measuredInstance = taskInfo,
                    startTime = result.startTime,
                    endTime = result.endTime,
                )
            )
        }
    }

    override fun provideTaskExecutionTimeReport(): TaskExecutionTimeReport? {
        return taskExecutionTimeline.toList()
    }
}