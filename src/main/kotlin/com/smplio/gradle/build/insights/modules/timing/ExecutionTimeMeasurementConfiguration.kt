package com.smplio.gradle.build.insights.modules.timing

import com.smplio.gradle.build.insights.modules.timing.report.ConsoleExecutionTimeReporter
import com.smplio.gradle.build.insights.modules.timing.report.IExecutionTimeReporter
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

abstract class ExecutionTimeMeasurementConfiguration @Inject constructor(objects: ObjectFactory) {
    val enabled: Property<Boolean> = objects.property(Boolean::class.java).convention(true)

    val executionTimeReporter: Property<IExecutionTimeReporter> = (objects.property(
        IExecutionTimeReporter::class.java
    ) as Property<IExecutionTimeReporter>).convention(
        ConsoleExecutionTimeReporter()
    )
}