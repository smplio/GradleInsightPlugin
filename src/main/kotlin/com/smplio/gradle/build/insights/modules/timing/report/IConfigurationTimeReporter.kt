package com.smplio.gradle.build.insights.modules.timing.report

interface IConfigurationTimeReporter {
    fun reportConfigurationTime(configurationTimeReport: ConfigurationTimeReport)
}