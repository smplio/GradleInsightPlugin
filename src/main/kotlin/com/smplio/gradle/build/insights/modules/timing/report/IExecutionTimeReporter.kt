package com.smplio.gradle.build.insights.modules.timing.report

import java.io.Serializable

interface IExecutionTimeReporter: Serializable {
    fun processReport(executionTimeReport: ExecutionTimeReport)
}