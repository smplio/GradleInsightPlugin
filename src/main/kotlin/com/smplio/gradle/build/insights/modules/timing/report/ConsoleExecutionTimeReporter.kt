package com.smplio.gradle.build.insights.modules.timing.report
import java.time.Duration

class ConsoleExecutionTimeReporter: IConfigurationTimeReporter, ITaskExecutionTimeReporter {

    private var configurationTimeReport: ConfigurationTimeReport? = null
    private var taskExecutionTimeReport: TaskExecutionTimeReport? = null

    override fun reportConfigurationTime(configurationTimeReport: ConfigurationTimeReport) {
        this.configurationTimeReport = configurationTimeReport
    }

    override fun reportTaskExecutionTime(taskExecutionTimeReport: TaskExecutionTimeReport) {
        this.taskExecutionTimeReport = taskExecutionTimeReport
    }

    private fun createProgressBar(progress: Float): String {
        val numberOfBars = ((progress * 25).toInt() - 1).coerceAtLeast(0)
        return "${"=".repeat(numberOfBars)}>${" ".repeat(24 - numberOfBars)}"
    }

    override fun submitReport() {
        var firstConfigurationStartTime = 0L
        var lastTaskEndTime = 0L
        configurationTimeReport?.let { report ->
            println("Configuration time:")
            firstConfigurationStartTime = report.minOf { it.startTime }
            val lastConfigurationEndTime = report.maxOf { it.endTime }
            val totalConfigurationTime: Long = lastConfigurationEndTime - firstConfigurationStartTime
            val longestProjectName: Int = report.maxOf { it.measuredInstance.projectName.length } + 1

            for (measuredConfigurationInfo in report) {
                val projectInfo = measuredConfigurationInfo.measuredInstance
                val configurationDuration = Duration.ofMillis(measuredConfigurationInfo.duration).seconds
                val progress = createProgressBar((measuredConfigurationInfo.duration) * 1.0f / totalConfigurationTime)
                val projectNamePadded = projectInfo.projectName.padStart(longestProjectName).padEnd(longestProjectName + 1)
                println("|${progress}| $projectNamePadded | ${"${configurationDuration}s".padStart(4)}")
            }
        }

        taskExecutionTimeReport?.let { report ->
            println("Task execution time:")
            val firstTaskStartTime = report.minOf { it.startTime }

            if (firstConfigurationStartTime == 0L) {
                firstConfigurationStartTime = firstTaskStartTime
            }

            lastTaskEndTime = report.maxOf { it.endTime }
            val totalTaskExecutionTime: Long = lastTaskEndTime - firstTaskStartTime
            val longestTaskName: Int = report.maxOf { it.measuredInstance.path.length } + 1

            for (measuredTaskInfo in report) {
                val taskInfo = measuredTaskInfo.measuredInstance
                val taskDuration = Duration.ofMillis(measuredTaskInfo.duration).seconds
                val progress = createProgressBar((measuredTaskInfo.duration) * 1.0f / totalTaskExecutionTime)
                val taskNamePadded = taskInfo.path.padStart(longestTaskName).padEnd(longestTaskName + 1)
                println("|${progress}| $taskNamePadded | ${"${taskDuration}s".padStart(4)}")
            }
        }

        if (firstConfigurationStartTime != 0L && lastTaskEndTime != 0L) {
            println("Build took: ${Duration.ofMillis(lastTaskEndTime - firstConfigurationStartTime).seconds}s")
        }
    }
}