package com.smplio.gradle.build.insights.modules.timing.models

import java.io.Serializable

data class TaskInfo(
    val name: String,
    val path: String,
    val status: ExecutionStatus,
): Serializable {

    sealed class ExecutionStatus(val description: String) {
        class Success(description: String) : ExecutionStatus(description)
        class Skipped(description: String) : ExecutionStatus(description)
        class Failed(description: String) : ExecutionStatus(description)
        object Unknown : ExecutionStatus("")

        override fun toString(): String {
            return when (this) {
                is Success -> "SUCCESS"
                is Failed -> "FAILED"
                is Skipped -> "SKIPPED"
                is Unknown -> ""
            }
        }
    }
}
