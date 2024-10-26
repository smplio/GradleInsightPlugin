package com.smplio.gradle.build.insights

import com.smplio.gradle.build.insights.modules.timing.ExecutionTimeMeasurementConfiguration
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.tasks.Nested


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
