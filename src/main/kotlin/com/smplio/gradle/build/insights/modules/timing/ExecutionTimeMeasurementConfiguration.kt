package com.smplio.gradle.build.insights.modules.timing

import org.gradle.api.Project
import org.gradle.api.provider.Property
import javax.inject.Inject

abstract class ExecutionTimeMeasurementConfiguration @Inject constructor(project: Project) {
    val enabled: Property<Boolean> = project.objects.property(Boolean::class.java).convention(true)
}