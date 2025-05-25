let timeScale = 1;
let timeWindowStart = Math.min(...tasks.map(value => value.start), ...configurations.map(value => value.start));
let timeWindowEnd = Math.max(...tasks.map(value => value.end), ...configurations.map(value => value.end));
const durationMs = timeWindowEnd - timeWindowStart;

// Draggable time window and handles
const taskTimeline = document.getElementById("task-timeline");

const timelineTrackEl = document.getElementById("timeline-track");
const timeWindowEl = document.getElementById("time-window");
const startHandle = document.getElementById("start-handle");
const endHandle = document.getElementById("end-handle");

const chartResizableContainers = document.getElementsByClassName("chart-resizable-container");
const chartsOverflowContainer = document.getElementById("charts-overflow-container");

let startX = 0;
let rowCount = 0;

let taskToDivMap = {};

function clamp(value, min, max) {
    return Math.max(min, Math.min(max, value));
}

function formatTime(timestamp) {
    const dtFormat = new Intl.DateTimeFormat('en-GB', {
        timeStyle: 'medium',
        timeZone: 'UTC'
    });

    return dtFormat.format(new Date(timestamp));
}

// Render configurations based on the time window
function createConfigurations() {
    configurations.forEach(project => {
        createTimelineBlock(project)
    })
}

// Render tasks based on the time window
function createTasks() {
    tasks.forEach(task => {
        createTimelineBlock(task)
    })
}

function createTimelineBlock(task) {
    let taskEl;
    if (task.name in taskToDivMap) {
        taskEl = taskToDivMap[task.name];
    } else {
        taskEl = document.createElement("div");
        taskEl.className = "timeline-item";
        taskEl.classList.add(task.type);
        if (task.type === "task") {
            let taskClass = "";
            switch(task.status) {
                case "SKIPPED":
                    if (task.status_description === "FROM-CACHE") {
                        taskClass = "cached-task";
                    } else {
                        taskClass = "skipped-task";
                    }
                    break;
                case "FAILED":
                    taskClass = "failed-task";
                    break;
                default:
                    taskClass = "successful-task";
                    break;
            }
            taskEl.classList.add(taskClass);
        }
        taskEl.classList.add("tooltip");
        taskEl.style.top = `${task.rowIdx * 25}px`;

        taskNameEl = document.createElement("span");
        taskNameEl.textContent = task.name;
        taskEl.appendChild(taskNameEl)

        tooltipEl = document.createElement("div");
        tooltipEl.className = "tooltiptext";

        let detailedStatus = task.status_description;
        if (task.status === "SUCCESS") {
            detailedStatus = "";
        }
        let statusBadge = `<span>Status: ${task.status}<br><pre>${detailedStatus}</pre></span><br>`;
        if (task.type !== "task") {
            statusBadge = "";
        }

        tooltipEl.innerHTML = `<span>${task.name}</span>\n<br>${statusBadge}<span>Started: ${formatTime(task.start)}</span>\n<span>Finished: ${formatTime(task.end)}</span>`;

        taskEl.appendChild(tooltipEl);

        taskToDivMap[task.name] = taskEl;
        taskTimeline.appendChild(taskEl);
    }

    taskEl.style.left = `${(task.start - timeWindowStart) * timeScale}px`;
    taskEl.style.width = `${(task.end - task.start) * timeScale}px`;

    if (task.end < timeWindowStart || task.start > timeWindowEnd) {
        taskEl.style.visibility = "hidden";
    } else {
        taskEl.style.visibility = "visible";
    }
}

// Calculate overlapping task rows
function calculateRows(tasks) {
    const sweepLineTasks = tasks.flatMap((task) => [
        {
            "time": task.start,
            "isStart": true,
            "task": task,
        },
        {
            "time": task.end,
            "isStart": false,
            "task": task,
        },
    ]);

    sweepLineTasks.sort((a, b) => {
        if (a.time !== b.time) {
            return a.time - b.time; // Sort by time
        }

        // If times are equal, end events come before start events
        if (a.isStart && !b.isStart) {
            return -1;
        }
        if (!a.isStart && b.isStart) {
            return 1;
        }

        // If both are start events at the same time, the one with longer duration comes first
        if (a.isStart && b.isStart) {
            return (b.task.end - b.task.start) - (a.task.end - a.task.start);
        }

        // If both are end events at the same time, maintain order
        return 0;
    })

    let maxHeight = 0;
    let currentHeight = 0;
    sweepLineTasks.forEach(item => {
        if (item.isStart) {
            currentHeight += 1;
        } else {
            currentHeight -= 1;
        }
        maxHeight = Math.max(maxHeight, currentHeight);
        console.assert(currentHeight >= 0);
    })

    const rows = Array(maxHeight).fill(null);
    sweepLineTasks.forEach(item => {
        if (item.isStart) {
            for (let i = 0; i < maxHeight; i++) {
                if (rows[i] != null) continue;

                rows[i] = item.task;
                item.task.rowIdx = i;
                break;
            }
        } else {
            for (let i = 0; i < maxHeight; i++) {
                if (rows[i] !== item.task) continue;

                rows[i] = null;
                break;
            }
        }
    })

    rowCount = maxHeight;
    taskTimeline.style.height = `${(rowCount + 4) * 25}px`;

    return sweepLineTasks.filter(value => value.isStart).map(value => value.task);
}

function updateRuler() {
    const rulerEl = document.getElementById("ruler");
    rulerEl.innerHTML = '';

    // Add marks for the entire timeline (every 10 secons, with special marks for each minute)
    let marksCount = 0;
    for (let time = 0; time <= durationMs / 1000; time++) {
        const position = time * timelineTrackEl.offsetWidth / (durationMs / 1000) - marksCount;

        const mark = document.createElement("div");
        mark.className = "mark";
        mark.style.left = `${position}px`;

        // Add minute and second marks
        if (time % 3600 === 0) {
            mark.classList.add("hour-mark");

            // Create label for minute mark
            const label = document.createElement("div");
            label.className = "time-label";
            label.textContent = `${time / 3600}h`;
            mark.appendChild(label);
            marksCount++;
        } else if (time % 60 === 0) {
            mark.classList.add("minute-mark");

            // Create label for minute mark
            const label = document.createElement("div");
            label.className = "time-label";
            label.textContent = `${time / 60}m`;
            mark.appendChild(label);
            marksCount++;
        } else if (time % 10 === 0) {
            mark.classList.add("second-mark");
            marksCount++;
        } else {
            continue; // Only add marks for each 5-second interval or full minute
        }

        rulerEl.appendChild(mark);
    }
}

function updateCharts() {
    for (const container of chartResizableContainers) {
        container.style.width = `${timelineTrackEl.offsetWidth * (timelineTrackEl.offsetWidth / timeWindowEl.offsetWidth)}px`;
    }

    let availableTaskScroll = taskTimeline.scrollWidth - taskTimeline.clientWidth;
    let availableScrollBar = timelineTrackEl.offsetWidth - timeWindowEl.offsetWidth;

    chartsOverflowContainer.scrollTo((availableTaskScroll * timeWindowEl.offsetLeft) / availableScrollBar, 0);
}

// Update the time window and render
function updateTimeWindow() {
    // const rulerEl = document.getElementById("ruler");
    let availableWidth = timelineTrackEl.offsetWidth;
    let selectedWindowWidth = timeWindowEl.offsetWidth;

    timeScale = availableWidth ** 2 / (durationMs * selectedWindowWidth);

    let availableTaskScroll = taskTimeline.scrollWidth - taskTimeline.clientWidth;
    let availableScrollBar = timelineTrackEl.offsetWidth - timeWindowEl.offsetWidth;

    taskTimeline.scrollTo((availableTaskScroll * timeWindowEl.offsetLeft) / availableScrollBar, 0);

    // Render tasks
    updateRuler();
    updateCharts();
    createConfigurations();
    createTasks();
}

let isDraggingWindow = false;
let isDraggingStart = false;
let isDraggingEnd = false;

function onMouseMove(e) {
    const deltaX = e.clientX - startX;

    if (isDraggingWindow) {
        let leftPx = clamp(
            timeWindowEl.offsetLeft + deltaX,
            0,
            timelineTrackEl.offsetWidth - timeWindowEl.offsetWidth
        )
        timeWindowEl.style.left = `${leftPx}px`;
    } else if (isDraggingStart) {
        timeWindowEl.style.width = `${clamp(timeWindowEl.offsetWidth - deltaX, 10, timelineTrackEl.offsetWidth)}px`;
        timeWindowEl.style.left = `${Math.max(0, timeWindowEl.offsetLeft + deltaX)}px`;
    } else if (isDraggingEnd) {
        timeWindowEl.style.width = `${Math.min(timelineTrackEl.offsetWidth, timeWindowEl.offsetWidth + deltaX)}px`;
    }

    startX = e.clientX;
    updateTimeWindow();
}

function onMouseUp() {
    isDraggingWindow = false;
    isDraggingStart = false;
    isDraggingEnd = false;
    document.removeEventListener("mousemove", onMouseMove);
    document.removeEventListener("mouseup", onMouseUp);
}

// Attach events for the time window and handles
timeWindowEl.addEventListener("mousedown", e => {
    isDraggingWindow = true;
    startX = e.clientX;
    document.addEventListener("mousemove", onMouseMove);
    document.addEventListener("mouseup", onMouseUp);
    e.stopPropagation();
    e.preventDefault();
});

startHandle.addEventListener("mousedown", e => {
    isDraggingStart = true;
    startX = e.clientX;
    document.addEventListener("mousemove", onMouseMove);
    document.addEventListener("mouseup", onMouseUp);
    e.stopPropagation();
    e.preventDefault();
});

endHandle.addEventListener("mousedown", e => {
    isDraggingEnd = true;
    startX = e.clientX;
    document.addEventListener("mousemove", onMouseMove);
    document.addEventListener("mouseup", onMouseUp);
    e.stopPropagation();
    e.preventDefault();
});

window.addEventListener ("resize", e => {
    updateTimeWindow();
});

// Initial render
configurations = calculateRows(configurations);
tasks = calculateRows(tasks);
updateTimeWindow();
