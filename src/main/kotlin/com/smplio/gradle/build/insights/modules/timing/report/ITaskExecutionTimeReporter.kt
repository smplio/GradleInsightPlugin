package com.smplio.gradle.build.insights.modules.timing.report

import com.smplio.gradle.build.insights.reporters.IReporter

interface ITaskExecutionTimeReporter: IReporter {
    fun reportTaskExecutionTime(taskExecutionTimeReport: TaskExecutionTimeReport)
}