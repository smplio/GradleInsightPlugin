package com.smplio.gradle.build.insights.report

import com.smplio.gradle.build.insights.report.load.ISystemLoadReportReceiver
import com.smplio.gradle.build.insights.modules.load.SystemLoadService
import com.smplio.gradle.build.insights.modules.timing.models.BuildInfo
import com.smplio.gradle.build.insights.modules.timing.models.Measured
import com.smplio.gradle.build.insights.modules.timing.models.TaskInfo
import com.smplio.gradle.build.insights.modules.timing.report.BuildHostInfo
import com.smplio.gradle.build.insights.modules.timing.report.ExecutionStats
import com.smplio.gradle.build.insights.modules.timing.report_providers.ConfigurationTimeMeasurementService
import com.smplio.gradle.build.insights.report.timing.IConfigurationTimeReportReceiver
import com.smplio.gradle.build.insights.report.timing.ITaskExecutionTimeReportReceiver
import com.smplio.gradle.build.insights.modules.timing.report_providers.TaskExecutionTimeMeasurementService
import com.smplio.gradle.build.insights.report.execution.IExecutionStatsReceiver
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener
import kotlin.math.max
import kotlin.math.min

abstract class CompositeReportBuildService : BuildService<CompositeReportBuildService.Parameters>,
    OperationCompletionListener,
    AutoCloseable
{
    interface Parameters: BuildServiceParameters {
        val reporters: ListProperty<Provider<IReporter>>
        val systemLoadReportService: Property<SystemLoadService>
        val configurationTimeReportService: Property<ConfigurationTimeMeasurementService>
        val executionTimeReportService: Property<TaskExecutionTimeMeasurementService>
        val startParameters: Property<String>
    }

    override fun close() {
        val reporters = parameters.reporters.orNull?.map { it.get() } ?: return
        val systemLoadService = parameters.systemLoadReportService.orNull ?: return
        val executionTimeReportService = parameters.executionTimeReportService.orNull ?: return
        val configurationTimeReportProvider = parameters.configurationTimeReportService.orNull ?: return

        reporters.filterIsInstance<ISystemLoadReportReceiver>().forEach { reporter ->
            systemLoadService.provideSystemLoadReport()?.let {
                reporter.reportSystemLoad(it)
            }
        }

        reporters.filterIsInstance<ITaskExecutionTimeReportReceiver>().forEach { reporter ->
            executionTimeReportService.provideTaskExecutionTimeReport()?.let {
                reporter.reportTaskExecutionTime(it)
            }
        }

        reporters.filterIsInstance<IConfigurationTimeReportReceiver>().forEach { reporter ->
            configurationTimeReportProvider.provideConfigurationTimeReport()?.let {
                reporter.reportConfigurationTime(it)
            }
        }

        reporters.filterIsInstance<IExecutionStatsReceiver>().forEach { reporter ->
            val configurationTimeline = configurationTimeReportProvider.provideConfigurationTimeReport()
            val taskExecutionTimeline = executionTimeReportService.provideTaskExecutionTimeReport()

            reporter.reportExecutionStats(ExecutionStats(
                buildHostInfo = BuildHostInfo(),
                buildInfo = Measured(
                    measuredInstance = BuildInfo(
                        status = BuildInfo.ExecutionStatus.Success().takeIf {
                            !taskExecutionTimeline.isNullOrEmpty() && taskExecutionTimeline.none { it.measuredInstance.status is TaskInfo.ExecutionStatus.Failed }
                        } ?: BuildInfo.ExecutionStatus.Failed(),
                        startParameters = parameters.startParameters.orNull ?: ""
                    ),
                    startTime = min(configurationTimeline?.minOfOrNull { it.startTime } ?: 0L, taskExecutionTimeline?.minOfOrNull { it.startTime } ?: 0L),
                    endTime = max(configurationTimeline?.maxOfOrNull { it.endTime } ?: 0L, taskExecutionTimeline?.maxOfOrNull { it.endTime } ?: 0L),
                ),
                configurationTimeline = configurationTimeline,
                taskExecutionTimeline = taskExecutionTimeline,
            ))
        }

        reporters.forEach { reporter ->
            reporter.submitReport()
        }
    }

    override fun onFinish(event: FinishEvent?) {}
}