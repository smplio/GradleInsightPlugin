package com.smplio.gradle.build.insights.vcs.providers

import com.smplio.gradle.build.insights.vcs.VCSData
import com.smplio.gradle.build.insights.vcs.IVCSDataProvider
import java.io.File
import java.io.Serializable
import java.util.concurrent.TimeUnit

class GitDataProvider: IVCSDataProvider {
    override fun get(projectDir: File): VCSData? {
        val processBuilder = ProcessBuilder().also {
            it.directory(projectDir)
        }

        val gitPathProcess = processBuilder.command("git", "config", "--show-toplevel").start()
        gitPathProcess.waitFor(1, TimeUnit.SECONDS)

        val gitBranchProcess = processBuilder.command("git", "rev-parse", "--abbrev-ref", "HEAD").start()
        gitBranchProcess.waitFor(1, TimeUnit.SECONDS)

        val gitCommitProcess = processBuilder.command("git", "rev-parse", "HEAD").start()
        gitCommitProcess.waitFor(1, TimeUnit.SECONDS)

        val gitDirtyProcess = processBuilder.command("git", "diff", "--stat").start()
        gitDirtyProcess.waitFor(1, TimeUnit.SECONDS)

        val gitCommitMessageProcess = processBuilder.command("git", "log", "-1", "--pretty=%Bg").start()
        gitCommitMessageProcess.waitFor(1, TimeUnit.SECONDS)

        val gitUserProcess = processBuilder.command("git", "show", "-s", "--format='%ae'", "HEAD").start()
        gitUserProcess.waitFor(1, TimeUnit.SECONDS)

        return VCSData(
            gitPathProcess.inputReader().readText().trim().takeIf { it.isNotBlank() } ?: projectDir.absolutePath,
            gitBranchProcess.inputReader().readText().trim(),
            gitCommitProcess.inputReader().readText().trim(),
            gitCommitMessageProcess.inputReader().readText().trim(),
            gitDirtyProcess.inputReader().readText().isNotBlank(),
            gitUserProcess.inputReader().readText().trim(),
        )
    }
}