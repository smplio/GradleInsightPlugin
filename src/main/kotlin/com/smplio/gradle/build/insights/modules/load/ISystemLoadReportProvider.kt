package com.smplio.gradle.build.insights.modules.load

import com.smplio.gradle.build.insights.reporters.IReportProvider

interface ISystemLoadReportProvider: IReportProvider {
    fun provideSystemLoadReport(): SystemLoadReport?
}