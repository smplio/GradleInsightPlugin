package com.smplio.gradle.build.insights.vcs

data class VCSData(
    val repository: String,
    val branch: String,
    val revision: String,
    val description: String,
    val dirty: Boolean,
    val author: String,
)