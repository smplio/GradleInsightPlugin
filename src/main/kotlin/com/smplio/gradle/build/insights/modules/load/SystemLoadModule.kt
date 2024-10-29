package com.smplio.gradle.build.insights.modules.load

import org.gradle.api.Project
import org.gradle.build.event.BuildEventsListenerRegistry

class SystemLoadModule(
    private val project: Project,
    private val registry: BuildEventsListenerRegistry,
    private val reporter: ISystemLoadReporter,
) {

    fun initialize() {
        val sharedServices = project.gradle.sharedServices
        val systemLoadService = sharedServices.registerIfAbsent(
            SystemLoadService::class.java.simpleName,
            SystemLoadService::class.java,
        ) {
            it.parameters.reporter.set(reporter)
        }
        registry.onTaskCompletion(systemLoadService)
    }
}