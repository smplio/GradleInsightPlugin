package com.smplio.gradle.build.insights

import com.smplio.gradle.build.insights.modules.graph.GraphBuilder.Node
import com.smplio.gradle.build.insights.modules.graph.orderGraph
import com.smplio.gradle.build.insights.modules.graph.printGraphViz
import org.gradle.api.DefaultTask
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import javax.inject.Inject

abstract class BuildProjectDependencyGraphTask @Inject constructor(
    objectFactory: ObjectFactory,
): DefaultTask() {
    @Option(
        option = "configurations",
        description="Configurations to build graph for",
    )
    val configurations: ListProperty<String> = (objectFactory.listProperty(
        String::class.java
    ) as ListProperty<String>).convention(listOf(
        "implementation",
        "api",
    ))



    @get:Input
    @get:Option(
        option = "rootNode",
        description = "Root node of a project dependency graph",
    )
    internal abstract val rootNode: Property<Node>

    @TaskAction
    fun printGraph() {
        val providedRootNode = rootNode.get()
        val graphTopology = orderGraph(providedRootNode)
        printGraphViz(providedRootNode, graphTopology)
    }
}