package com.smplio.gradle.build.insights.report

import java.io.Serializable

interface IReporter: Serializable {
    fun submitReport()
}