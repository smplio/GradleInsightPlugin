package com.smplio.gradle.build.insights.modules.timing.report

import com.smplio.gradle.build.insights.vcs.VCSData
import java.net.InetAddress

data class ExecutionTimeReport(
    val requestedTasks: List<String>,
//    val vcsData: VCSData?,
//    val buildHostInfo: BuildHostInfo?,
    val buildStartTime: Long,
    val configurationDuration: DurationReport,
    val tasksDuration: DurationReport,
    val tasksExecutionStats: List<TaskExecutionStats>,
)

data class TaskExecutionStats(
    val taskName: String,
    val status: ExecutionStatus,
    val statusDescription: String,
    val duration: DurationReport,
)

data class DurationReport(
    val startTime: Long,
    val endTime: Long,
) {
    fun getDuration(): Long = endTime - startTime
}

data class BuildHostInfo(
    val userName: String = System.getProperty("user.name"),
    val hostName: String = InetAddress.getLocalHost().hostName,
    val osArchitecture: String = System.getProperty("os.arch"),
    val osName: String = System.getProperty("os.name"),
    val osVersion: String = System.getProperty("os.version"),
    val javaVersion: String = System.getProperty("java.version"),
)

enum class ExecutionStatus {
    SUCCESS,
    SKIPPED,
    FAILED,
    UNKNOWN,
}
