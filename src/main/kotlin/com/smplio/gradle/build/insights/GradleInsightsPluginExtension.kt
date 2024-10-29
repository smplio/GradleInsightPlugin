package com.smplio.gradle.build.insights

import com.smplio.gradle.build.insights.modules.timing.ExecutionTimeMeasurementConfiguration
import com.smplio.gradle.build.insights.vcs.IVCSDataProvider
import com.smplio.gradle.build.insights.vcs.providers.GitDataProvider
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested


abstract class GradleInsightsPluginExtension(project: Project) {

    val gatherHtmlReport: Property<Boolean> = project.objects.property(Boolean::class.java).convention(true)
    val vcsDataProvider: Property<IVCSDataProvider> = project.objects.property(IVCSDataProvider::class.java).convention(
        GitDataProvider()
    )

    @Nested
    abstract fun getExecutionTimeMeasurementConfiguration(): ExecutionTimeMeasurementConfiguration

    fun measureExecutionTime(action: Action<ExecutionTimeMeasurementConfiguration>) {
        action.execute(getExecutionTimeMeasurementConfiguration())
    }

    companion object {
        const val EXTENSION_NAME = "gradleInsights"
    }
}
