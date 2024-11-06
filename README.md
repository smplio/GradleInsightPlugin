# Gradle Build Insights
Gradle build insights plugin allows you to get a pic of how gradle evaluates and executes your build.

It allows you to:
- Build project dependency graph
- Record task execution time and state
- Record system load during build
- Generate an html report, consisting of execution timeline and system load

## Configuration
```
gradleInsights {
    gatherHtmlReport = true
    
    measureExecutionTime {
        enabled = true
        executionTimeReporter = object: IExectionTimeReporter {
            override fun reportExecutionTime(executionTimeReport: ExecutionTimeReport)
        }
    }
}
```

### HTML Report
Html report consists of task execution timeline and system load. 
Gathering is enabled by default, but can be disabled by `gatherHtmlReport = false`.

### Execution time measurement
You can collect execution measurement data by `measureExecutionTime.enabled = true`.
By default, results will be reported to html report, if enabled, and Console output. 
Last one can be modified by providing an instance of `com.smplio.gradle.build.insights.modules.timing.report.IExectionTimeReporter` to `measureExecutionTime.executionTimeReporter`.
Instance can be both pre-shipped or user-defined.

### System load
Plugin measures system load parameters, such as used and available heap and CPU load average.

_Currently, system load stats are only published with HTML report_
