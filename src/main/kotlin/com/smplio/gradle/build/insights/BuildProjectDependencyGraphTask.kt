package com.smplio.gradle.build.insights

import com.smplio.gradle.build.insights.modules.graph.GraphBuilder.Node
import com.smplio.gradle.build.insights.modules.graph.bfs
import com.smplio.gradle.build.insights.modules.graph.orderGraph
import com.smplio.gradle.build.insights.modules.graph.printGraphViz
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import javax.inject.Inject

abstract class BuildProjectDependencyGraphTask @Inject constructor(
    objectFactory: ObjectFactory,
): DefaultTask() {
    @get:Input
    @get:Option(
        option = "configurations",
        description="Configurations to build graph for",
    )
    val configurations: Property<String> = objectFactory.property(String::class.java)

    @get:Input
    internal abstract val project: Property<Project>

    @TaskAction
    fun buildProjectDependencyGraph() {

        val project = project.get()

        val nodes = HashMap<String, Node>()
        val rootNode = Node(project)
        nodes[project.path] = rootNode
        bfs(rootNode) { currentProjectNode, visitedProjects ->
            val newWave = mutableSetOf<Node>()
            for (configuration in configurations.get().split(',')) {
                val dependencies = currentProjectNode.item.configurations.findByName(configuration)?.dependencies ?: continue
                for (dependency in dependencies) {
                    if (dependency !is ProjectDependency) continue
                    val dependencyProject = dependency.dependencyProject

                    if (!nodes.containsKey(dependencyProject.path)) {
                        nodes[dependencyProject.path] = Node(dependencyProject)
                    }

                    nodes[currentProjectNode.item.path]?.nodeDependsOn?.add(
                        Node.Link(
                            nodes[dependencyProject.path] ?: continue,
                            configuration,
                        )
                    )

                    if (!visitedProjects.contains(nodes[dependencyProject.path])) {
                        newWave.add(nodes[dependencyProject.path] ?: continue)
                    }
                }
            }
            return@bfs newWave
        }

        val graphTopology = orderGraph(rootNode)
        printGraphViz(rootNode, graphTopology)
    }
}