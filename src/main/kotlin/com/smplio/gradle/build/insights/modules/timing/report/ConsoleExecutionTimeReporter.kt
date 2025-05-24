package com.smplio.gradle.build.insights.modules.timing.report
import java.time.Duration

class ConsoleExecutionTimeReporter: IExecutionTimeReporter {

    private var executionTimeReport: ExecutionTimeReport? = null

    override fun reportExecutionTime(executionTimeReport: ExecutionTimeReport) {
        this.executionTimeReport = executionTimeReport
    }

    private fun createProgressBar(progress: Float): String {
        val numberOfBars = ((progress * 25).toInt() - 1).coerceAtLeast(0)
        return "${"=".repeat(numberOfBars)}>${" ".repeat(24 - numberOfBars)}"
    }

    override fun submitReport() {
        executionTimeReport?.let { report ->
            val firstTaskStartTime = report.taskExecutionTimeline.minOf { it.startTime }
            val lastTaskEndTime = report.taskExecutionTimeline.maxOf { it.endTime }
            val totalTaskExecutionTime: Long = lastTaskEndTime - firstTaskStartTime
            val longestTaskName: Int = report.taskExecutionTimeline.maxOf { it.measuredInstance.path.length } + 1

            println("Build took: ${Duration.ofMillis(report.buildInfo.duration).seconds}s")

            for (measuredTaskInfo in report.taskExecutionTimeline) {
                val taskInfo = measuredTaskInfo.measuredInstance
                val taskDuration = Duration.ofMillis(measuredTaskInfo.duration).seconds
                val progress = createProgressBar((measuredTaskInfo.duration) * 1.0f / totalTaskExecutionTime)
                val taskNamePadded = taskInfo.path.padStart(longestTaskName).padEnd(longestTaskName + 1)
                println("|${progress}| $taskNamePadded | ${"${taskDuration}s".padStart(4)}")
            }
        }
    }
}