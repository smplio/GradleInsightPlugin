package com.smplio.gradle.build.insights.modules.timing.report
import java.time.Duration

class ConsoleExecutionTimeReporter: IExecutionTimeReporter {
    override fun reportExecutionTime(executionTimeReport: ExecutionTimeReport) {
        val totalTaskExecutionTime: Long = executionTimeReport.tasksDuration.getDuration()
        val longestTaskName: Int = executionTimeReport.tasksExecutionStats.maxOf { it.taskName.length } + 1

        println("Build took: ${Duration.ofMillis(executionTimeReport.tasksDuration.endTime - executionTimeReport.configurationDuration.startTime).seconds}s")

        for (taskStats in executionTimeReport.tasksExecutionStats) {
            val taskDuration = Duration.ofMillis(taskStats.duration.getDuration()).seconds
            val progress = createProgressBar((taskStats.duration.getDuration()) * 1.0f / totalTaskExecutionTime)
            val taskNamePadded = taskStats.taskName.padStart(longestTaskName).padEnd(longestTaskName + 1)
            println("|${progress}| $taskNamePadded | ${"${taskDuration}s".padStart(4)}")
        }
    }

    private fun createProgressBar(progress: Float): String {
        val numberOfBars = ((progress * 25).toInt() - 1).coerceAtLeast(0)
        return "${"=".repeat(numberOfBars)}>${" ".repeat(24 - numberOfBars)}"
    }
}