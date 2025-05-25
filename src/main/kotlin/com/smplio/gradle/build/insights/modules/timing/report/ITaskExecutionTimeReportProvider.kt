package com.smplio.gradle.build.insights.modules.timing.report

import com.smplio.gradle.build.insights.reporters.IReportProvider

interface ITaskExecutionTimeReportProvider: IReportProvider {
    fun provideTaskExecutionTimeReport(): TaskExecutionTimeReport?
}