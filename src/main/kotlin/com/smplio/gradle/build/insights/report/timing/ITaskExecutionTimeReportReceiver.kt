package com.smplio.gradle.build.insights.report.timing

import com.smplio.gradle.build.insights.modules.timing.report.TaskExecutionTimeReport
import com.smplio.gradle.build.insights.report.IReporter

interface ITaskExecutionTimeReportReceiver: IReporter {
    fun reportTaskExecutionTime(taskExecutionTimeReport: TaskExecutionTimeReport)
}