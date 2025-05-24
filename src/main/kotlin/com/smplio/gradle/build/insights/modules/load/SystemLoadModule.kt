package com.smplio.gradle.build.insights.modules.load

import com.smplio.gradle.build.insights.reporters.CompositeReportBuildService
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.build.event.BuildEventsListenerRegistry

class SystemLoadModule(
    private val project: Project,
    private val registry: BuildEventsListenerRegistry,
    private val reportBuildService: Provider<CompositeReportBuildService>,
) {

    fun initialize() {
        val sharedServices = project.gradle.sharedServices
        val systemLoadService = sharedServices.registerIfAbsent(
            SystemLoadService::class.java.simpleName,
            SystemLoadService::class.java,
        ) {
            it.parameters.reporter.set(reportBuildService)
        }

        registry.onTaskCompletion(systemLoadService)
    }
}