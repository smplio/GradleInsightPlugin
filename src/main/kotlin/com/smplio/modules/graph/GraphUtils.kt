package com.smplio.modules.graph

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionGraph
import kotlin.collections.set
import kotlin.math.max

class GraphBuilder(
    // some settings here
) {

    fun buildTaskDependencyGraph(project: Project, tasksToExecute: List<String>) {
        for (taskName in tasksToExecute) {
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

    private fun buildGraph(graph: TaskExecutionGraph, graphMap: MutableMap<Project, Node>, rootTask: Task): Node {
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

                    if (key.path == ":core:domain" && currentTask.project.path == ":app") {
                        println(currentTask)
                        println(dependency)
                    }

                    if (!graphMap.containsKey(key)) {
                        val node = Node(key)
                        graphMap[key] = node
                    }
                    if (!visitedTasks.contains(dependency)) {
                        newWave.add(dependency)
                    }
                    if (currentTask.project != dependency.project) {
                        graphMap[currentTask.project]?.nodeDependsOn?.add(graphMap[key]!!)
                    }
                }
            }
        }

        return graphMap[rootTask.project]!!
    }

    private fun orderGraph(rootNode: Node): HashMap<Int, MutableSet<Project>> {
        fun innerOrderGraph(currentNode: Node, depth: Int) {
            for (dependency in currentNode.nodeDependsOn) {
                innerOrderGraph(dependency, depth + 1)
            }
            currentNode.depth = max(currentNode.depth, depth)
        }

        innerOrderGraph(rootNode, 0)

        val result = HashMap<Int, MutableSet<Project>>()
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
                    result[currentNode.depth] = mutableSetOf(currentNode.project)
                } else {
                    result[currentNode.depth]?.add(currentNode.project)
                }

                for (dependency in currentNode.nodeDependsOn) {
                    if (!visitedNodes.contains(dependency)) {
                        newWave.add(dependency)
                    }
                }
            }
        }

        return result
    }

    private fun printGraphViz(rootNode: Node, topology: HashMap<Int, MutableSet<Project>>) {
        val graphTemplate = """
        digraph hierachy {
            rankdir="LR"
            graph [ordering="out"];
            
        %s
            
        %s
        }
        """.trimIndent()
        val subgraphTemplate = """
        subgraph cluster_%d {
            label="Level %d"
        %s
        }
        """.trimIndent()

        val subgraphs = mutableListOf<String>()

        for (topologyEntry in topology) {
            subgraphs.add(
                subgraphTemplate.format(
                    topologyEntry.key,
                    topologyEntry.key,
                    topologyEntry.value.joinToString("\n") { "\"${it.path}\"".prependIndent() },
                )
            )
        }

        val connections = mutableListOf<String>()

        bfs(rootNode) { currentNode, visitedNodes ->
            val newWave = mutableSetOf<Node>()
            for (dependency in currentNode.nodeDependsOn) {
                connections.add("\"${currentNode.project.path}\"->\"${dependency.project.path}\"")
                if (!visitedNodes.contains(dependency)) {
                    newWave.add(dependency)
                }
            }
            return@bfs newWave
        }

        println(
            graphTemplate.format(
                subgraphs.joinToString("\n").prependIndent(),
                connections.joinToString("\n").prependIndent(),
            )
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
            for (currentNode in oldWave) {
                visitedNodes.add(currentNode)
                newWave.addAll(callable(currentNode, visitedNodes))
            }
        }
    }

    private class Node(
        val project: Project,
        val nodeDependsOn: MutableSet<Node> = mutableSetOf(),
        var depth: Int = 0,
    ) {
        override fun equals(other: Any?): Boolean {
            val other = other as? Node ?: return false
            return other.project.path == project.path
        }

        override fun toString(): String {
            return "Node(project=$project, nodeDependsOn=${nodeDependsOn.joinToString { it.project.toString() }}, depth=$depth)"
        }

        override fun hashCode(): Int {
            return project.hashCode()
        }
    }
}