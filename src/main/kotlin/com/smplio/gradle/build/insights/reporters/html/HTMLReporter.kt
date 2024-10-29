package com.smplio.gradle.build.insights.reporters.html

import com.smplio.gradle.build.insights.modules.load.ISystemLoadReporter
import com.smplio.gradle.build.insights.modules.timing.report.DurationReport
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

    private var taskDuration: DurationReport? = null
    private var configurationDuration: DurationReport? = null

    override fun processExecutionReport(executionTimeReport: ExecutionTimeReport) {
        taskDuration = executionTimeReport.tasksDuration
        configurationDuration = executionTimeReport.configurationDuration

        val tasks = JSONArray()
        for (taskStats in executionTimeReport.tasksExecutionStats) {
            tasks.put(JSONObject().apply {
                put("type", "task")
                put("name", taskStats.taskName)
                put("start", taskStats.duration.startTime)
                put("end", taskStats.duration.endTime)
            })
        }
        tasks.put(JSONObject().apply {
            put("type", "configuration")
            put("name", "Configuration")
            put("start", configurationDuration?.startTime ?: 0)
            put("end", configurationDuration?.endTime ?: 0)
        })

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
            configurationDuration?.startTime ?: -1,
            configurationDuration?.endTime ?: -1,
            taskDuration?.getDuration() ?: -1,
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

        tasksFile.deleteOnExit()
        systemLoadFile.deleteOnExit()
    }
}