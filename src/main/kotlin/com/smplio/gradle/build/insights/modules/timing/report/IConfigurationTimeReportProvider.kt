package com.smplio.gradle.build.insights.modules.timing.report

import com.smplio.gradle.build.insights.reporters.IReportProvider

interface IConfigurationTimeReportProvider: IReportProvider {
    fun provideConfigurationTimeReport(): ConfigurationTimeReport?
}