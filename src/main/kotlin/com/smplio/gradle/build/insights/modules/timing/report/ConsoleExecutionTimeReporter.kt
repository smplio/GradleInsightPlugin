package com.smplio.gradle.build.insights.modules.timing.report
import java.time.Duration

class ConsoleExecutionTimeReporter: IExecutionTimeReporter {
    override fun reportExecutionTime(executionTimeReport: ExecutionTimeReport) {
        val firstTaskStartTime = executionTimeReport.taskExecutionTimeline.minOf { it.startTime }
        val lastTaskEndTime = executionTimeReport.taskExecutionTimeline.maxOf { it.endTime }
        val totalTaskExecutionTime: Long = lastTaskEndTime - firstTaskStartTime
        val longestTaskName: Int = executionTimeReport.taskExecutionTimeline.maxOf { it.measuredInstance.path.length } + 1

        println("Build took: ${Duration.ofMillis(executionTimeReport.buildInfo.duration).seconds}s")

        for (measuredTaskInfo in executionTimeReport.taskExecutionTimeline) {
            val taskInfo = measuredTaskInfo.measuredInstance
            val taskDuration = Duration.ofMillis(measuredTaskInfo.duration).seconds
            val progress = createProgressBar((measuredTaskInfo.duration) * 1.0f / totalTaskExecutionTime)
            val taskNamePadded = taskInfo.path.padStart(longestTaskName).padEnd(longestTaskName + 1)
            println("|${progress}| $taskNamePadded | ${"${taskDuration}s".padStart(4)}")
        }
    }

    private fun createProgressBar(progress: Float): String {
        val numberOfBars = ((progress * 25).toInt() - 1).coerceAtLeast(0)
        return "${"=".repeat(numberOfBars)}>${" ".repeat(24 - numberOfBars)}"
    }
}