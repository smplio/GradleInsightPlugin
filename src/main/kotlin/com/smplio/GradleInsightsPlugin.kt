package com.smplio

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ReportingBasePlugin
import org.gradle.build.event.BuildEventsListenerRegistry
import javax.inject.Inject

class GradleInsightsPlugin @Inject constructor(private val registry: BuildEventsListenerRegistry): Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply(ReportingBasePlugin::class.java)

        val timerRecorder = project.gradle.sharedServices.registerIfAbsent(TimerService::class.java.simpleName, TimerService::class.java) {}
        project.gradle.taskGraph.whenReady {
            registry.onTaskCompletion(timerRecorder)
        }
    }
}
