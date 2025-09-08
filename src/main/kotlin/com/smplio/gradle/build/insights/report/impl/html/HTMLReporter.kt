package com.smplio.gradle.build.insights.report.impl.html

import com.smplio.gradle.build.insights.report.load.ISystemLoadReportReceiver
import com.smplio.gradle.build.insights.modules.timing.report.ConfigurationTimeReport
import com.smplio.gradle.build.insights.modules.timing.report.ExecutionStats
import com.smplio.gradle.build.insights.report.timing.IConfigurationTimeReportReceiver
import com.smplio.gradle.build.insights.modules.timing.report.TaskExecutionTimeReport
import com.smplio.gradle.build.insights.report.execution.IExecutionStatsReceiver
import com.smplio.gradle.build.insights.report.timing.ITaskExecutionTimeReportReceiver
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
) : IExecutionStatsReceiver,
    IConfigurationTimeReportReceiver,
    ITaskExecutionTimeReportReceiver,
    ISystemLoadReportReceiver
{
    private val uniqueReportFolder = project.layout.buildDirectory.get().dir("build-report").dir(UUID.randomUUID().toString()).asFile
    private val styleCssPath = uniqueReportFolder.toPath().resolve("style.css").absolutePathString()
    private val reportHtmlFile = uniqueReportFolder.toPath().resolve("index.html").toFile()

    private var configurationTimeJson: String? = null
    private var executionTimeJson: String? = null
    private var systemLoadJson: String? = null

    override fun reportExecutionStats(stats: ExecutionStats) {
        stats.configurationTimeline?.let { reportConfigurationTime(it) }
        stats.taskExecutionTimeline?.let { reportTaskExecutionTime(it) }
    }

    override fun reportTaskExecutionTime(taskExecutionTimeReport: TaskExecutionTimeReport) {
        val tasks = JSONArray()
        for (measuredTaskInfo in taskExecutionTimeReport) {
            val task = measuredTaskInfo.measuredInstance
            tasks.put(JSONObject().apply {
                put("type", "task")
                put("name", task.path)
                put("start", measuredTaskInfo.startTime)
                put("end", measuredTaskInfo.endTime)
                put("status", task.status.toString())
                put("status_description", task.status.description)
            })
        }
        executionTimeJson = tasks.toString(4)
    }

    override fun reportConfigurationTime(configurationTimeReport: ConfigurationTimeReport) {
        val projects = JSONArray()
        for (measuredConfigurationInfo in configurationTimeReport) {
            val configurationInfo = measuredConfigurationInfo.measuredInstance
            projects.put(JSONObject().apply {
                put("type", "configuration")
                put("name", configurationInfo.projectName)
                put("start", measuredConfigurationInfo.startTime)
                put("end", measuredConfigurationInfo.endTime)
            })
        }
        configurationTimeJson = projects.toString(4)
    }

    override fun reportSystemLoad(measurements: ConcurrentLinkedQueue<Pair<Long, List<Pair<String, Number>>>>) {
        systemLoadJson = JSONArray().also {
            measurements.forEach { measurementSet -> it.put(JSONObject().also {
                it.put("timestamp", measurementSet.first)
                for (measurement in measurementSet.second) {
                    it.put(measurement.first, measurement.second)
                }
            })}
        }.toString(4)
    }

    override fun submitReport() {
        uniqueReportFolder.mkdirs()

        val scriptJsText = javaClass.getResourceAsStream("/script.js")?.reader()?.use { it.readText() }
        val chartJsText = javaClass.getResourceAsStream("/chart@4.4.6.js")?.reader()?.use { it.readText() }
        val buildChartsJsText = javaClass.getResourceAsStream("/build_charts.js")?.reader()?.use { it.readText() }

        val html = javaClass.getResourceAsStream("/index.html")?.reader()?.readText()?.format(
            configurationTimeJson ?: "",
            executionTimeJson ?: "",
            systemLoadJson ?: "",
            scriptJsText,
            chartJsText,
            buildChartsJsText,
        )

        reportHtmlFile.bufferedWriter(Charsets.UTF_8).use { writer ->
            html?.let { writer.write(it) }
        }

        javaClass.getResourceAsStream("/style.css")?.let { resourceStream ->
            Files.copy(
                resourceStream,
                Path(styleCssPath),
                StandardCopyOption.REPLACE_EXISTING,
            )
        }

        configurationTimeJson = null
        executionTimeJson = null
        systemLoadJson = null

        println("Build insights report is available in file://${reportHtmlFile.absolutePath}")
    }
}