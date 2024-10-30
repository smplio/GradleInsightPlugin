package com.smplio.gradle.build.insights.vcs

import java.io.File
import java.io.Serializable

interface IVCSDataProvider: Serializable {
    fun get(projectDir: File): VCSData?
}