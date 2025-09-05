package com.smplio.gradle.build.insights.report.execution

import com.smplio.gradle.build.insights.modules.timing.report.ExecutionStats
import com.smplio.gradle.build.insights.report.IReporter

interface IExecutionStatsReceiver: IReporter {
    fun reportExecutionStats(stats: ExecutionStats)
}