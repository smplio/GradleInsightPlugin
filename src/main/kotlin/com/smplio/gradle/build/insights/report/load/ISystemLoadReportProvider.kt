package com.smplio.gradle.build.insights.report.load

import com.smplio.gradle.build.insights.modules.load.SystemLoadReport
import com.smplio.gradle.build.insights.report.IReportProvider

interface ISystemLoadReportProvider: IReportProvider {
    fun provideSystemLoadReport(): SystemLoadReport?
}