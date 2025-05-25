package com.smplio.gradle.build.insights.modules.load

import com.smplio.gradle.build.insights.reporters.IReporter

interface ISystemLoadReporter: IReporter {
    fun reportSystemLoad(measurements: SystemLoadReport)
}