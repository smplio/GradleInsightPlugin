package com.smplio.gradle.build.insights.report.timing

import com.smplio.gradle.build.insights.modules.timing.report.ConfigurationTimeReport

interface IConfigurationTimeReportReceiver {
    fun reportConfigurationTime(configurationTimeReport: ConfigurationTimeReport)
}