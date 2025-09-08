const cpuChart = document.getElementById("cpuChart");
const memoryChart = document.getElementById("memoryChart");

systemStats.sort((a, b) => a.timestamp - b.timestamp);
const labels = systemStats.map(value => value.timestamp);
const minTime =  Math.min(...labels);

const heapMaxMetrics = systemStats.map(value => value.heapMax);
const heapUsedMetrics = systemStats.map(value => value.heapUsed);
const gradleJvmCpuPercentMetrics = systemStats.map(value => value.gradleJvmCpuPercent);
const gradleDescendantsCpuPercentMetrics = systemStats.map(value => value.gradleDescendantsCpuPercent);

function formatBytes(bytes, decimals = 2) {
    if (!+bytes) return '0 B'

    const k = 1024
    const dm = decimals < 0 ? 0 : decimals
    const sizes = ['B', 'KB', 'MB', 'GB', 'TB']

    const i = Math.floor(Math.log(bytes) / Math.log(k))

    return `${parseFloat((bytes / Math.pow(k, i)).toFixed(dm))} ${sizes[i]}`
}

new Chart(cpuChart, {
    type: 'line',
    data: {
        labels: labels.map(value => Math.round((value - minTime) / 1000) + 's'),
        datasets: [
            {
                label: 'Gradle CPU usage',
                data: gradleJvmCpuPercentMetrics,
                fill: true,
                borderColor: 'rgb(85,160,223)',
                backgroundColor: 'rgb(85,180,223)',
                tension: 0.1
            },
            {
                label: 'Children CPU usage',
                data: gradleDescendantsCpuPercentMetrics,
                fill: true,
                borderColor: 'rgb(84,105,220)',
                backgroundColor: 'rgb(84,125,220)',
                tension: 0.1,
            },
        ],
    },
    options: {
        responsive: true,
        maintainAspectRatio: false,
        scales: {
            y: {
                beginAtZero: true,
                display: false,
            }
        },
        plugins: {
            stacked: true,
            legend: {
                align: 'start',
            },
            tooltip: {
                mode: 'index',
                callbacks: {
                    label: function(context) {
                        let label = context.dataset.label || '';

                        if (label) {
                            label += ': ';
                        }
                        if (context.parsed.y !== null) {
                            label += context.parsed.y;
                        }
                        label += '%';
                        return label;
                    }
                }
            },
        }
    }
});

new Chart(memoryChart, {
    type: 'line',
    data: {
        labels: labels.map(value => Math.round((value - minTime) / 1000) + 's'),
        datasets: [{
            label: 'Max heap available',
            data: heapMaxMetrics,
            fill: false,
            borderColor: 'rgb(151,75,192)',
            tension: 0.1
        },
            {
                label: 'Heap used',
                data: heapUsedMetrics,
                fill: false,
                borderColor: 'rgb(104,192,75)',
                tension: 0.1
            }
        ],
    },
    options: {
        responsive: true,
        maintainAspectRatio: false,
        scales: {
            y: {
                beginAtZero: true,
                display: false,
            }
        },
        plugins: {
            legend: {
                align: 'start',
            },
            tooltip: {
                mode: 'index',
                callbacks: {
                    label: function(context) {
                        let label = context.dataset.label || '';

                        if (label) {
                            label += ': ';
                        }
                        if (context.parsed.y !== null) {
                            label += formatBytes(context.parsed.y);
                        }
                        return label;
                    }
                }
            }
        }
    }
});

