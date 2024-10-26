package com.smplio.modules.graph

import com.smplio.modules.graph.GraphBuilder.Node.Link
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.execution.TaskExecutionGraph
import kotlin.collections.set
import kotlin.math.max

class GraphBuilder(
    // some settings here
) {

    fun buildTaskDependencyGraph(project: Project, tasks: List<String>) {
        for (taskName in tasks) {
            val startTask = if (taskName.startsWith(":")) {
                project.tasks.findByPath(taskName)
            } else {
                project.tasks.findByName(taskName)
            } ?: continue

            project.gradle.taskGraph.whenReady {
                val rootNode = buildGraph(it, HashMap(), startTask)
                val graphTopology = orderGraph(rootNode)
                printGraphViz(rootNode, graphTopology)
            }
        }
    }

    fun buildProjectDependencyGraph(rootProject: Project, configurations: List<String>) {
        rootProject.allprojects { project ->
            project.task("buildProjectDependenciesGraph").doLast {
                val nodes = HashMap<String, Node>()
                val rootNode = Node(project)
                nodes[project.path] = rootNode
                bfs(rootNode) { currentProjectNode, visitedProjects ->
                    val newWave = mutableSetOf<Node>()
                    for (configuration in configurations) {
                        val dependencies = currentProjectNode.item.configurations.findByName(configuration)?.dependencies ?: continue
                        for (dependency in dependencies) {
                            if (dependency !is ProjectDependency) continue
                            val dependencyProject = dependency.dependencyProject

                            if (!nodes.containsKey(dependencyProject.path)) {
                                nodes[dependencyProject.path] = Node(dependencyProject)
                            }

                            nodes[currentProjectNode.item.path]?.nodeDependsOn?.add(
                                Link(
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
    }

    private fun buildGraph(
        graph: TaskExecutionGraph,
        graphMap: MutableMap<Project, Node>,
        rootTask: Task
    ): Node {
        val rootNode = Node(rootTask.project)
        graphMap[rootTask.project] = rootNode

        val visitedTasks = HashSet<Task>()

        val oldWave = mutableSetOf<Task>()
        val newWave = mutableSetOf(rootTask)

        while (newWave.isNotEmpty()) {

            oldWave.clear()
            oldWave.addAll(newWave)
            newWave.clear()

            for (currentTask in oldWave) {
                visitedTasks.add(currentTask)
                val dependencies = graph.getDependencies(currentTask)
                for (dependency in dependencies) {
                    val key = dependency.project

                    if (!graphMap.containsKey(key)) {
                        val node = Node(key)
                        graphMap[key] = node
                    }
                    if (!visitedTasks.contains(dependency)) {
                        newWave.add(dependency)
                    }
                    if (currentTask.project != dependency.project) {
                        graphMap[currentTask.project]?.nodeDependsOn?.add(
                            Link(graphMap[key]!!)
                        )
                    }
                }
            }
        }

        return graphMap[rootTask.project]!!
    }

    private fun orderGraph(rootNode: Node): HashMap<Int, MutableSet<Node>> {
        fun innerOrderGraph(currentNode: Node, depth: Int) {
            for (dependency in currentNode.nodeDependsOn) {
                innerOrderGraph(dependency.node, depth + 1)
            }
            currentNode.depth = max(currentNode.depth, depth)
        }

        innerOrderGraph(rootNode, 0)

        val result = HashMap<Int, MutableSet<Node>>()
        val visitedNodes = HashSet<Node>()

        val oldWave = mutableSetOf<Node>()
        val newWave = mutableSetOf(rootNode)

        while (newWave.isNotEmpty()) {

            oldWave.clear()
            oldWave.addAll(newWave)
            newWave.clear()

            for (currentNode in oldWave) {
                visitedNodes.add(currentNode)

                if (!result.containsKey(currentNode.depth)) {
                    result[currentNode.depth] = mutableSetOf(currentNode)
                } else {
                    result[currentNode.depth]?.add(currentNode)
                }

                for (dependency in currentNode.nodeDependsOn) {
                    if (!visitedNodes.contains(dependency.node)) {
                        newWave.add(dependency.node)
                    }
                }
            }
        }

        return result
    }

    private fun printGraphViz(rootNode: Node, topology: HashMap<Int, MutableSet<Node>>) {
        val styles = arrayOf(
            "",
            "[style=dashed]",
            "[style=dotted]",
        )

        val linkTypes = mutableListOf<String?>()
        for (topologyEntry in topology) {
            topologyEntry.value.flatMap { it.nodeDependsOn.map { it.linkType } }.forEach {
                if (!linkTypes.contains(it)) {
                    linkTypes.add(it)
                }
            }
        }

        val subgraphs = mutableListOf<String>()
        for (topologyEntry in topology) {
            subgraphs.add(
                """
                ${topologyEntry.value.joinToString("\n") { "\"${it.item.path}\" [shape=square]" }}
                {rank=same; ${topologyEntry.value.joinToString(" ") { "\"${it.item.path}\"" }}}
                
                """.trimIndent()
            )
        }

        val connections = mutableListOf<String>()

        bfs(rootNode) { currentNode, visitedNodes ->
            val newWave = mutableSetOf<Node>()
            for (dependency in currentNode.nodeDependsOn) {
                val label = styles[linkTypes.indexOf(dependency.linkType)]
                connections.add("\"${currentNode.item.path}\"->\"${dependency.node.item.path}\" $label")
                if (!visitedNodes.contains(dependency.node)) {
                    newWave.add(dependency.node)
                }
            }
            return@bfs newWave
        }

        val legendTemplate = """
        subgraph cluster_01 { 
            node [shape=plaintext]
            label = "Legend";
            legendKey [label=<<table border="0" cellpadding="2" cellspacing="0" cellborder="0">
            ${linkTypes.mapIndexed { idx, it -> "<tr><td align=\"right\" port=\"i$idx\">$it</td></tr>" }.joinToString("\n").prependIndent()}
            </table>>]
            legendDummyValue [label=<<table border="0" cellpadding="2" cellspacing="0" cellborder="0">
            ${linkTypes.mapIndexed { idx, _ -> "<tr><td port=\"i$idx\">&nbsp;</td></tr>" }.joinToString("\n").prependIndent()}
            </table>>]
            {rank=same; legendKey legendDummyValue}
            
            ${linkTypes.mapIndexed { idx, _ -> "legendKey:i$idx:e -> legendDummyValue:i$idx:w ${styles[idx]}" }.joinToString("\n")}
        }
        """.trimIndent()

        val hasLegend: Boolean = linkTypes.size > 0

        println(
"""
digraph hierachy {
    ranksep=1.7
    nodesep=.75
    
${if (hasLegend) legendTemplate.prependIndent() else ""}
    
${subgraphs.joinToString("\n").prependIndent()}
    
${connections.joinToString("\n").prependIndent()}

${if (hasLegend) "legendKey -> \"${topology[0]?.first()?.item?.path}\" [style=invis]".prependIndent() else ""}
}
""".trimIndent()
        )
    }

    inline fun <reified T> bfs(startItem: T, callable: (T, Set<T>) -> MutableSet<T>) {
        val oldWave = mutableSetOf<T>()
        val newWave = mutableSetOf(startItem)
        val visitedNodes = HashSet<T>()

        while (newWave.isNotEmpty()) {
            oldWave.clear()
            oldWave.addAll(newWave)
            newWave.clear()

            visitedNodes.addAll(oldWave)

            for (currentNode in oldWave) {
                newWave.addAll(callable(currentNode, visitedNodes))
            }
        }
    }

    private class Node(
        val item: Project,
        val nodeDependsOn: MutableSet<Link> = mutableSetOf(),
        var depth: Int = 0,
    ) {
        override fun equals(other: Any?): Boolean {
            val other = other as? Node ?: return false
            return other.item.path == item.path
        }

        override fun toString(): String {
            return "Node(project=$item, nodeDependsOn=${nodeDependsOn.joinToString { "${it.node.item.path}_${it.linkType}" }}, depth=$depth)"
        }

        override fun hashCode(): Int {
            return item.path.hashCode()
        }

        class Link(
            val node: Node,
            val linkType: String? = null,
        ) {
            override fun equals(other: Any?): Boolean {
                val other = other as? Link ?: return false
                return other.node == this.node && other.linkType == this.linkType
            }

            override fun hashCode(): Int {
                return node.hashCode() + linkType.hashCode()
            }
        }
    }
}