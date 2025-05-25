package com.smplio.gradle.build.insights.modules.load

import java.util.concurrent.ConcurrentLinkedQueue

typealias SystemLoadReport = ConcurrentLinkedQueue<Pair<Long, List<Pair<String, Number>>>>
