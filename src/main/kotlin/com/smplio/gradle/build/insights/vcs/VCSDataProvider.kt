package com.smplio.gradle.build.insights.vcs

import java.io.File

interface VCSDataProvider {
    fun get(projectDir: File): VCSData?
}