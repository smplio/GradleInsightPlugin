package com.smplio.gradle.build.insights.modules.timing.report

import com.smplio.gradle.build.insights.modules.timing.models.BuildInfo
import com.smplio.gradle.build.insights.modules.timing.models.ConfigurationInfo
import com.smplio.gradle.build.insights.modules.timing.models.Measured
import com.smplio.gradle.build.insights.modules.timing.models.TaskInfo
import java.net.InetAddress

data class ExecutionTimeReport(
    val requestedTasks: List<String>,
    val buildHostInfo: BuildHostInfo,
    val buildInfo: Measured<BuildInfo>,
    val configurationTimeline: List<Measured<ConfigurationInfo>>,
    val taskExecutionTimeline: List<Measured<TaskInfo>>,
)

data class BuildHostInfo(
    val userName: String = System.getProperty("user.name"),
    val hostName: String = InetAddress.getLocalHost().hostName,
    val osArchitecture: String = System.getProperty("os.arch"),
    val osName: String = System.getProperty("os.name"),
    val osVersion: String = System.getProperty("os.version"),
    val javaVersion: String = System.getProperty("java.version"),
)
