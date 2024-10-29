package com.smplio.gradle.build.insights.modules.load

import java.io.Serializable
import java.util.concurrent.ConcurrentLinkedQueue

interface ISystemLoadReporter: Serializable {
    fun reportSystemLoad(measurements: ConcurrentLinkedQueue<Pair<Long, List<Pair<String, Number>>>>)
}