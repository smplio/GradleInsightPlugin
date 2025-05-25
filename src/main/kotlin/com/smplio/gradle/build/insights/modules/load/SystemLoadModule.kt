package com.smplio.gradle.build.insights.modules.load

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.build.event.BuildEventsListenerRegistry

class SystemLoadModule(
    private val project: Project,
    private val registry: BuildEventsListenerRegistry,
) {

    private var systemLoadService: Provider<SystemLoadService>? = null

    fun initialize() {
        systemLoadService = project.gradle.sharedServices.registerIfAbsent(
            SystemLoadService::class.java.simpleName,
            SystemLoadService::class.java
        ) {}.also {
            registry.onTaskCompletion(it)
        }
    }

    fun getSystemLoadReportProvider(): Provider<SystemLoadService>? {
        return systemLoadService
    }
}
