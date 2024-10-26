package com.smplio.gradle.build.insights

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested
import javax.inject.Inject


abstract class GradleInsightsPluginExtension(project: Project) {

    @Nested
    abstract fun getExecutionTimeMeasurementConfiguration(): ExecutionTimeMeasurementConfiguration

    fun measureExecutionTime(action: Action<ExecutionTimeMeasurementConfiguration>) {
        action.execute(getExecutionTimeMeasurementConfiguration())
    }

    companion object {
        const val EXTENSION_NAME = "gradleInsights"
    }
}

abstract class ExecutionTimeMeasurementConfiguration @Inject constructor(project: Project) {
    val enabled: Property<Boolean> = project.objects.property(Boolean::class.java).convention(false)
}
