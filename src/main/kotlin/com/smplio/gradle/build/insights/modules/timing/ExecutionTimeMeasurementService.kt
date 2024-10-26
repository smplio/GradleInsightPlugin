package com.smplio.gradle.build.insights.modules.timing

import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener
import org.gradle.tooling.events.task.TaskFinishEvent
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

abstract class ExecutionTimeMeasurementService : BuildService<BuildServiceParameters.None>,
    OperationCompletionListener,
    AutoCloseable
{

    private val buildStartTime: Instant = Instant.now()
    private val taskDurations: ConcurrentHashMap<String, Long> = ConcurrentHashMap()

    override fun onFinish(event: FinishEvent) {
        if (event is TaskFinishEvent) {
            val durationMs = event.result.endTime - event.result.startTime
            taskDurations[event.descriptor.taskPath] = Duration.ofMillis(durationMs).seconds
        }
    }

    override fun close() {
        println("Total duration: ${Duration.between(buildStartTime, Instant.now()).seconds}")
        println("Per task stats: ")

        for (taskPath in taskDurations.keys()) {
            println("$taskPath:\t${taskDurations[taskPath]}")
        }
    }
}