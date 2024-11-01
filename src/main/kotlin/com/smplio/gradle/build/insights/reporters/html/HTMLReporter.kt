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
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString

class HTMLReporter(
    project: Project,
): IExecutionTimeReporter, ISystemLoadReporter {
    private val baseReportFolder = project.layout.buildDirectory.get().dir("build-report").asFile
    private val tasksFile = baseReportFolder.toPath().resolve("tasks.json").toFile()
    private val systemLoadFile = baseReportFolder.toPath().resolve("systemLoad.json").toFile()

    private val uniqueReportFolder = project.layout.buildDirectory.get().dir("build-report").dir(UUID.randomUUID().toString()).asFile
    private val styleCssPath = uniqueReportFolder.toPath().resolve("style.css").absolutePathString()
    private val reportHtmlFile = uniqueReportFolder.toPath().resolve("index.html").toFile()

    private var taskDuration: DurationReport? = null
    private var configurationDuration: DurationReport? = null

    override fun reportExecutionTime(executionTimeReport: ExecutionTimeReport) {
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

        baseReportFolder.mkdirs()
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

        baseReportFolder.mkdirs()
        systemLoadFile.bufferedWriter(Charsets.UTF_8).use {
            it.write(systemLoadJson.toString(4))
        }

        tryGenerateReport()
    }

    private fun tryGenerateReport() {
        if (!tasksFile.exists() || !systemLoadFile.exists()) return

        uniqueReportFolder.mkdirs()

        val scriptJsText = javaClass.getResourceAsStream("/script.js")?.reader()?.use { it.readText() }
        val chartJsText = javaClass.getResourceAsStream("/chart@4.4.6.js")?.reader()?.use { it.readText() }
        val buildChartsJsText = javaClass.getResourceAsStream("/build_charts.js")?.reader()?.use { it.readText() }

        val html = javaClass.getResourceAsStream("/index.html")?.reader()?.readText()?.format(
            configurationDuration?.startTime ?: -1,
            configurationDuration?.endTime ?: -1,
            taskDuration?.getDuration() ?: -1,
            tasksFile.reader().use { it.readText() },
            systemLoadFile.reader().use { it.readText() },
            scriptJsText,
            chartJsText,
            buildChartsJsText,
        )

        reportHtmlFile.bufferedWriter(Charsets.UTF_8).use {
            it.write(html)
        }

        Files.copy(
            javaClass.getResourceAsStream("/style.css"),
            Path(styleCssPath),
            StandardCopyOption.REPLACE_EXISTING,
        )

        tasksFile.deleteOnExit()
        systemLoadFile.deleteOnExit()

        println("Report is available in ${reportHtmlFile.absolutePath}")
    }
}