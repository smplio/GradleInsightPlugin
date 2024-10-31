const cpuChart = document.getElementById("cpuChart");
const memoryChart = document.getElementById("memoryChart");

systemStats.sort((a, b) => a.timestamp - b.timestamp);
const labels = systemStats.map(value => value.timestamp);
const minTime =  Math.min(...labels);

const heapMaxMetrics = systemStats.map(value => value.heapMax);
const heapUsedMetrics = systemStats.map(value => value.heapUsed);
const systemLoadAverageMetrics = systemStats.map(value => value.systemLoadAverage);

function formatBytes(bytes, decimals = 2) {
    if (!+bytes) return '0 Bytes'

    const k = 1024
    const dm = decimals < 0 ? 0 : decimals
    const sizes = ['Bytes', 'KiB', 'MiB', 'GiB', 'TiB', 'PiB', 'EiB', 'ZiB', 'YiB']

    const i = Math.floor(Math.log(bytes) / Math.log(k))

    return `${parseFloat((bytes / Math.pow(k, i)).toFixed(dm))} ${sizes[i]}`
}

new Chart(cpuChart, {
    type: 'line',
    data: {
        labels: labels.map(value => Math.round((value - minTime) / 1000) + 's'),
        datasets: [
            {
                label: 'CPU usage',
                data: systemLoadAverageMetrics,
                fill: false,
                borderColor: 'rgb(75, 192, 192)',
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
            }
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

