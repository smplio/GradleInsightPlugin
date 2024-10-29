package com.smplio.gradle.build.insights.reporters.html

import com.smplio.gradle.build.insights.modules.load.ISystemLoadReporter
import com.smplio.gradle.build.insights.modules.timing.report.ExecutionTimeReport
import com.smplio.gradle.build.insights.modules.timing.report.IExecutionTimeReporter
import org.gradle.api.Project
import org.json.JSONArray
import org.json.JSONObject
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.ConcurrentLinkedQueue

class HTMLReporter(project: Project): IExecutionTimeReporter, ISystemLoadReporter {
    private val reportFolder = project.layout.buildDirectory.dir("build-report").get().asFile
    private val tasksFile = reportFolder.toPath().resolve("tasks.json").toFile()
    private val systemLoadFile = reportFolder.toPath().resolve("systemLoad.json").toFile()

    private var taskExecutionTime: Long? = null

    override fun processExecutionReport(executionTimeReport: ExecutionTimeReport) {
        taskExecutionTime = executionTimeReport.totalExecutionTimeMs
        val tasks = JSONArray()
        val startTime = executionTimeReport.tasksExecutionStats.minOfOrNull { it.startTime } ?: 0
        for (taskStats in executionTimeReport.tasksExecutionStats) {
            tasks.put(JSONObject().apply {
                put("name", taskStats.taskName)
                put("start", taskStats.startTime - startTime)
                put("end", taskStats.endTime - startTime)
            })
        }

        reportFolder.mkdirs()
        tasksFile.bufferedWriter(Charsets.UTF_8).use {
            it.write(tasks.toString(4))
        }


        tryGenerateReport()
    }

    override fun reportSystemLoad(measurements: ConcurrentLinkedQueue<Pair<Long, List<Pair<String, Number>>>>) {
        val systemLoadJson = JSONArray().also {
            measurements.forEach { measurementSet -> it.put(JSONObject().also {
                it.put("timestamp", measurementSet.first)
                for (measurement in measurementSet.second) {
                    it.put(measurement.first, measurement.second)
                }
            })}
        }

        reportFolder.mkdirs()
        systemLoadFile.bufferedWriter(Charsets.UTF_8).use {
            it.write(systemLoadJson.toString(4))
        }

        tryGenerateReport()
    }

    private fun tryGenerateReport() {
        if (!tasksFile.exists() || !systemLoadFile.exists()) {
            println("Failed attempt to generate report ${tasksFile.exists()} ${systemLoadFile.exists()}")
            return
        }

        val html = javaClass.getResourceAsStream("/index.html")?.reader()?.readText()?.format(
            taskExecutionTime ?: 0,
            tasksFile.reader().readText(),
            systemLoadFile.reader().readText(),
        )

        reportFolder.toPath().resolve("index.html").toFile().bufferedWriter(Charsets.UTF_8).use {
            it.write(html)
        }

        println(reportFolder.absolutePath)

        Files.copy(
            javaClass.getResourceAsStream("/chart.js"),
            reportFolder.toPath().resolve("chart.js"),
            StandardCopyOption.REPLACE_EXISTING,
        )

        Files.copy(
            javaClass.getResourceAsStream("/script.js"),
            reportFolder.toPath().resolve("script.js"),
            StandardCopyOption.REPLACE_EXISTING,
        )

        Files.copy(
            javaClass.getResourceAsStream("/style.css"),
            reportFolder.toPath().resolve("style.css"),
            StandardCopyOption.REPLACE_EXISTING,
        )

        tasksFile.delete()
        systemLoadFile.delete()
    }
}