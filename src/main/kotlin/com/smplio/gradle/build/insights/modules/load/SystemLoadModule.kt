package com.smplio.gradle.build.insights.modules.load

import com.smplio.gradle.build.insights.reporters.CompositeReporter
import com.smplio.gradle.build.insights.reporters.html.HTMLReporter
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.build.event.BuildEventsListenerRegistry

class SystemLoadModule(
    private val project: Project,
    private val registry: BuildEventsListenerRegistry,
    private val gatherHtmlReport: Property<Boolean>,
) {

    fun initialize() {
        val sharedServices = project.gradle.sharedServices
        val systemLoadService = sharedServices.registerIfAbsent(
            SystemLoadService::class.java.simpleName,
            SystemLoadService::class.java,
        ) {}

        project.gradle.taskGraph.whenReady {
            val compositeReporter = CompositeReporter(
                if (gatherHtmlReport.get()) {
                    listOf(
                        HTMLReporter(
                            project,
                        ),
                    )
                } else {
                    emptyList()
                }
            )
            val service = systemLoadService.get()
            service.reporter = compositeReporter
        }
        registry.onTaskCompletion(systemLoadService)
    }
}