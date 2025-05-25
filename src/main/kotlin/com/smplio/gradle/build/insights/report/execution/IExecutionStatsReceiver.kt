package com.smplio.gradle.build.insights.report.execution

import com.smplio.gradle.build.insights.modules.timing.report.ExecutionStats

interface IExecutionStatsReceiver {
    fun reportExecutionStats(stats: ExecutionStats)
}