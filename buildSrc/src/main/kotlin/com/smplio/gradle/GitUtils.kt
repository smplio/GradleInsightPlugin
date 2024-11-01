package com.smplio.gradle

fun getArtifactVersionFromGit(): String {
    val runtime = Runtime.getRuntime()
    val gitCurrentTag = runtime.exec(arrayOf("git", "tag", "--points-at", "HEAD"))
    gitCurrentTag.waitFor()

    var gitTag = ""
    gitCurrentTag.inputStream.bufferedReader().use {
        gitTag = it.readText().split("\n").firstOrNull { it.startsWith("v") }?.trim() ?: ""
    }

    val gitCurrentCommit = runtime.exec(arrayOf("git", "rev-parse", "--short", "HEAD"))
    gitCurrentCommit.waitFor()

    var gitCommit = ""
    gitCurrentCommit.inputStream.bufferedReader().use {
        gitCommit = it.readText().trim()
    }

    return gitTag.replaceFirst("v", "").ifBlank {
        "$gitCommit-SNAPSHOT"
    }
}
