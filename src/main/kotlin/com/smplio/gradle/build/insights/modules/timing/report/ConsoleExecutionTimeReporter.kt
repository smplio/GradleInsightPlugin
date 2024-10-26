package com.smplio.gradle.build.insights.modules.timing.report

class ConsoleExecutionTimeReporter: IExecutionTimeReporter {
    override fun processReport(executionTimeReport: ExecutionTimeReport) {
        println(executionTimeReport)
    }
}