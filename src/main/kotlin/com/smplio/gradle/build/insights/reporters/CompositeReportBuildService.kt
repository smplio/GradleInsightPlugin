package com.smplio.gradle.build.insights.reporters

import com.smplio.gradle.build.insights.modules.load.ISystemLoadReporter
import com.smplio.gradle.build.insights.modules.load.SystemLoadService
import com.smplio.gradle.build.insights.modules.timing.report.IConfigurationTimeReportProvider
import com.smplio.gradle.build.insights.modules.timing.report.IConfigurationTimeReporter
import com.smplio.gradle.build.insights.modules.timing.report.ITaskExecutionTimeReporter
import com.smplio.gradle.build.insights.modules.timing.report_providers.TaskTaskExecutionTimeMeasurementService
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener

abstract class CompositeReportBuildService : BuildService<CompositeReportBuildService.Parameters>,
    OperationCompletionListener,
    AutoCloseable
{
    interface Parameters: BuildServiceParameters {
        val reporters: ListProperty<IReporter>
        val configurationTimeReportProvider: Property<IConfigurationTimeReportProvider>
        val systemLoadReportService: Property<SystemLoadService>
        val executionTimeReportService: Property<TaskTaskExecutionTimeMeasurementService>
    }

    override fun close() {
        val reporters = parameters.reporters.orNull ?: return
        val systemLoadService = parameters.systemLoadReportService.orNull ?: return
        val executionTimeReportService = parameters.executionTimeReportService.orNull ?: return
        val configurationTimeReportProvider = parameters.configurationTimeReportProvider.orNull ?: return

        reporters.filterIsInstance<ISystemLoadReporter>().forEach { reporter ->
            systemLoadService.provideSystemLoadReport()?.let {
                reporter.reportSystemLoad(it)
            }
        }

        reporters.filterIsInstance<ITaskExecutionTimeReporter>().forEach { reporter ->
            executionTimeReportService.provideTaskExecutionTimeReport()?.let {
                reporter.reportTaskExecutionTime(it)
            }
        }

        reporters.filterIsInstance<IConfigurationTimeReporter>().forEach { reporter ->
            configurationTimeReportProvider.provideConfigurationTimeReport()?.let {
                reporter.reportConfigurationTime(it)
            }
        }

        // val reportProviders = parameters.reportProviders.orNull ?: return
        //
        // reportProviders.forEach { reportProvider ->
        //     when (reportProvider) {
        //         is ISystemLoadReportProvider -> {
        //             reporters.filterIsInstance<ISystemLoadReporter>().forEach { reporter ->
        //                 reportProvider.provideSystemLoadReport()?.let {
        //                     reporter.reportSystemLoad(it)
        //                 }
        //             }
        //         }
        //         is IExecutionTimeReportProvider -> {
        //             reporters.filterIsInstance<IExecutionTimeReporter>().forEach { reporter ->
        //                 reportProvider.provideExecutionTimeReport()?.let {
        //                     reporter.reportExecutionTime(it)
        //                 }
        //             }
        //         }
        //     }
        // }

        reporters.forEach { reporter ->
            reporter.submitReport()
        }
    }

    override fun onFinish(event: FinishEvent?) {}
}