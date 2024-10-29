const cpuChart = document.getElementById("cpuChart");
const memoryChart = document.getElementById("memoryChart");

systemStats.sort((a, b) => a.timestamp - b.timestamp);
const labels = systemStats.map(value => value.timestamp);
const minTime =  Math.min(...labels);

const heapMaxMetrics = systemStats.map(value => value.heapMax);
const heapUsedMetrics = systemStats.map(value => value.heapUsed);
const systemLoadAverageMetrics = systemStats.map(value => value.systemLoadAverage);

new Chart(cpuChart, {
    type: 'line',
    data: {
        labels: labels.map(value => ((value - minTime) / 1000) + 's'),
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
                display: false
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
        labels: labels.map(value => ((value - minTime) / 1000) + 's'),
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
                display: false
            }
        },
        plugins: {
            legend: {
                align: 'start',
            }
        }
    }
});

