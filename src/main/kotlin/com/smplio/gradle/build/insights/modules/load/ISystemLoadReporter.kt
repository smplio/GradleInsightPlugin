package com.smplio.gradle.build.insights.modules.load

import com.smplio.gradle.build.insights.reporters.IReporter
import java.util.concurrent.ConcurrentLinkedQueue

interface ISystemLoadReporter: IReporter {
    fun reportSystemLoad(measurements: ConcurrentLinkedQueue<Pair<Long, List<Pair<String, Number>>>>)
}