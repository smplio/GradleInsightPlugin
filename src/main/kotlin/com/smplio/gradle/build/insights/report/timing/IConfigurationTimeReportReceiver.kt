package com.smplio.gradle.build.insights.report.timing

import com.smplio.gradle.build.insights.modules.timing.report.ConfigurationTimeReport
import com.smplio.gradle.build.insights.report.IReporter

interface IConfigurationTimeReportReceiver: IReporter {
    fun reportConfigurationTime(configurationTimeReport: ConfigurationTimeReport)
}