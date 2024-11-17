package com.smplio.gradle.build.insights.modules.timing.models

import java.io.Serializable

class Measured<T>(
    val measuredInstance: T,
    val startTime: Long,
    val endTime: Long,
): Serializable {
    val duration: Long = endTime - startTime
}
