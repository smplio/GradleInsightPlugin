package com.smplio.gradle.build.insights.vcs

import java.io.File

interface IVCSDataProvider {
    fun get(projectDir: File): VCSData?
}