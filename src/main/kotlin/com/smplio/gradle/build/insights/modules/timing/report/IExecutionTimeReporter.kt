package com.smplio.gradle.build.insights.modules.timing.report

import com.smplio.gradle.build.insights.reporters.IReporter

interface IExecutionTimeReporter: IReporter {
    fun reportExecutionTime(executionTimeReport: ExecutionTimeReport)
}