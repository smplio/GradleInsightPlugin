package com.smplio.gradle.build.insights.report.load

import com.smplio.gradle.build.insights.modules.load.SystemLoadReport
import com.smplio.gradle.build.insights.report.IReporter

interface ISystemLoadReportReceiver: IReporter {
    fun reportSystemLoad(measurements: SystemLoadReport)
}