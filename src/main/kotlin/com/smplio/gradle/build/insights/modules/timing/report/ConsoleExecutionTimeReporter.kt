package com.smplio.gradle.build.insights.modules.timing.report
import java.time.Duration

class ConsoleExecutionTimeReporter: IExecutionTimeReporter {
    override fun processExecutionReport(executionTimeReport: ExecutionTimeReport) {
        val totalBuildTime: Long = executionTimeReport.totalExecutionTimeMs
        val longestTaskName: Int = executionTimeReport.tasksExecutionStats.map { it.taskName.length }.max() + 1

        println("Build took: ${Duration.ofMillis(totalBuildTime).seconds}s")

        for (taskStats in executionTimeReport.tasksExecutionStats) {
            val taskDuration = Duration.ofMillis(taskStats.endTime - taskStats.startTime).seconds
            val progress = createProgressBar((taskStats.endTime - taskStats.startTime) * 1.0f / totalBuildTime)
            val taskNamePadded = taskStats.taskName.padStart(longestTaskName).padEnd(longestTaskName + 1)
            println("|${progress}| $taskNamePadded | ${"${taskDuration}s".padStart(4)}")
        }
    }

    private fun createProgressBar(progress: Float): String {
        val numberOfBars = ((progress * 25).toInt() - 1).coerceAtLeast(0)
        return "${"=".repeat(numberOfBars)}>${" ".repeat(24 - numberOfBars)}"
    }
}