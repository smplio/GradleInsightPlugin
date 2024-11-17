package com.smplio.gradle.build.insights.modules.timing

import com.smplio.gradle.build.insights.modules.timing.models.BuildInfo
import com.smplio.gradle.build.insights.modules.timing.models.ConfigurationInfo
import com.smplio.gradle.build.insights.modules.timing.models.Measured
import com.smplio.gradle.build.insights.modules.timing.models.TaskInfo
import com.smplio.gradle.build.insights.modules.timing.report.*
import org.gradle.StartParameter
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.provider.ListProperty
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
import java.util.concurrent.ConcurrentLinkedQueue

abstract class ExecutionTimeMeasurementService : BuildService<ExecutionTimeMeasurementService.Parameters>,
    OperationCompletionListener,
    AutoCloseable
{

    interface Parameters: BuildServiceParameters {
        val startParameters: Property<SerializableStartParameter>
        val reporter: Property<IExecutionTimeReporter>
        val buildStartTime: Property<Long>
        val configurationsTimeline: ListProperty<Measured<ConfigurationInfo>>
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
            ))
        }
    }

    override fun close() {
        val lastExecutedTaskEndTime = lastTaskEndTime ?: -1
        val report = ExecutionTimeReport(
            requestedTasks = parameters.startParameters.get().taskNames,
            buildHostInfo = BuildHostInfo(),
            buildInfo = Measured(
                measuredInstance = BuildInfo(
                    status = BuildInfo.ExecutionStatus.Success().takeIf {
                        taskExecutionTimeline.isNotEmpty() && taskExecutionTimeline.none { it.measuredInstance.status is TaskInfo.ExecutionStatus.Failed }
                    } ?: BuildInfo.ExecutionStatus.Failed()
                ),
                startTime = parameters.buildStartTime.get(),
                endTime = lastExecutedTaskEndTime,
            ),
            configurationTimeline = parameters.configurationsTimeline.get(),
            taskExecutionTimeline = taskExecutionTimeline.toList(),
        )
        parameters.reporter.get().reportExecutionTime(report)
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