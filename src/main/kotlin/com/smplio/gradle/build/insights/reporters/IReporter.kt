package com.smplio.gradle.build.insights.reporters

import java.io.Serializable

interface IReporter: Serializable {
    fun submitReport()
}