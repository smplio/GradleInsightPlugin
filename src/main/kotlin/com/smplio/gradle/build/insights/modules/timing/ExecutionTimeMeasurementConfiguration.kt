package com.smplio.gradle.build.insights.modules.timing

import com.smplio.gradle.build.insights.modules.timing.report.ConsoleExecutionTimeReporter
import com.smplio.gradle.build.insights.modules.timing.report.IExecutionTimeReporter
import org.gradle.api.Project
import org.gradle.api.provider.Property
import javax.inject.Inject

abstract class ExecutionTimeMeasurementConfiguration @Inject constructor(project: Project) {
    val enabled: Property<Boolean> = project.objects.property(Boolean::class.java).convention(true)

    val executionTimeReporter: Property<IExecutionTimeReporter> = project.objects.property(
        IExecutionTimeReporter::class.java
    ).convention(ConsoleExecutionTimeReporter())
}