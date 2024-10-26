package com.smplio.gradle.build.insights.modules.timing

import com.smplio.gradle.build.insights.vcs.VCSData
import java.net.InetAddress

data class ExecutionTimeReport(
    val requestedTasks: List<String>,
    val vcsData: VCSData?,
    val buildHostInfo: BuildHostInfo?,
    val totalExecutionTimeMs: Long,
    val tasksExecutionStats: List<TaskExecutionStats>,
)

data class TaskExecutionStats(
    val taskName: String,
    val status: ExecutionStatus,
    val statusDescription: String,
    val executionTimeMs: Long,
)

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
