package com.smplio.gradle.build.insights.reporters

import com.smplio.gradle.build.insights.modules.load.ISystemLoadReporter
import com.smplio.gradle.build.insights.modules.timing.report.ExecutionTimeReport
import com.smplio.gradle.build.insights.modules.timing.report.IExecutionTimeReporter
import java.util.concurrent.ConcurrentLinkedQueue

class CompositeReporter(
    private val reporters: List<IReporter>,
): IExecutionTimeReporter, ISystemLoadReporter {
    override fun reportExecutionTime(executionTimeReport: ExecutionTimeReport) {
        reporters.filterIsInstance<IExecutionTimeReporter>().forEach { reporter -> reporter.reportExecutionTime(executionTimeReport) }
    }

    override fun reportSystemLoad(measurements: ConcurrentLinkedQueue<Pair<Long, List<Pair<String, Number>>>>) {
        reporters.filterIsInstance<ISystemLoadReporter>().forEach { reporter -> reporter.reportSystemLoad(measurements) }
    }
}