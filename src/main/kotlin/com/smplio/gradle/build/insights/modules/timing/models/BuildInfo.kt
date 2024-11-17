package com.smplio.gradle.build.insights.modules.timing.models

import java.io.Serializable

data class BuildInfo(
    val status: ExecutionStatus,
): Serializable {
    sealed class ExecutionStatus {
        class Success : ExecutionStatus()
        class Failed : ExecutionStatus()

        override fun toString(): String {
            return when (this) {
                is Success -> "SUCCESS"
                is Failed -> "FAILED"
            }
        }
    }
}
